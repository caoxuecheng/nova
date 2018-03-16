package com.onescorpin.nifi.v2.ingest;

/*-
 * #%L
 * onescorpin-nifi-core-processors
 * %%
 * Copyright (C) 2017 Onescorpin
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.onescorpin.ingest.TableRegisterSupport;
import com.onescorpin.nifi.processor.AbstractNiFiProcessor;
import com.onescorpin.nifi.v2.thrift.ThriftService;
import com.onescorpin.util.ColumnSpec;
import com.onescorpin.util.TableRegisterConfiguration;
import com.onescorpin.util.TableType;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.onescorpin.nifi.v2.ingest.IngestProperties.NFLOW_CATEGORY;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.NFLOW_FIELD_SPECIFICATION;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.NFLOW_FORMAT_SPECS;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.NFLOW_NAME;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.FIELD_SPECIFICATION;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.PARTITION_SPECS;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.REL_FAILURE;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.REL_SUCCESS;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.TARGET_FORMAT_SPECS;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.TARGET_TBLPROPERTIES;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.THRIFT_SERVICE;


@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"hive", "ddl", "register", "onescorpin"})
@CapabilityDescription("Creates a set of standard nflow tables managed by the Think Big platform. ")
public class RegisterNflowTables extends AbstractNiFiProcessor {

    public static final PropertyDescriptor NFLOW_ROOT = new PropertyDescriptor.Builder()
        .name("Nflow Root Path")
        .description("Specify the full HDFS or S3 root path for the nflow,valid,invalid tables.")
        .required(true)
        .defaultValue("${hive.ingest.root}")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .expressionLanguageSupported(true)
        .build();
    public static final PropertyDescriptor MASTER_ROOT = new PropertyDescriptor.Builder()
        .name("Master Root Path")
        .description("Specify the HDFS or S3 folder root path for creating the master table")
        .required(true)
        .defaultValue("${hive.master.root}")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .expressionLanguageSupported(true)
        .build();
    public static final PropertyDescriptor PROFILE_ROOT = new PropertyDescriptor.Builder()
        .name("Profile Root Path")
        .description("Specify the HDFS or S3 folder root path for creating the profile table")
        .required(true)
        .defaultValue("${hive.profile.root}")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .expressionLanguageSupported(true)
        .build();
    private final static String DEFAULT_STORAGE_FORMAT = "STORED AS ORC";
    private final static String DEFAULT_NFLOW_FORMAT_OPTIONS = "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' STORED AS TEXTFILE";
    /**
     * Specify creation of all tables
     */
    public static final String ALL_TABLES = "ALL";
    /**
     * Property indicating which tables to register
     */
    public static final PropertyDescriptor TABLE_TYPE = new PropertyDescriptor.Builder()
        .name("Table Type")
        .description("Specifies the standard table type to create or ALL for standard set.")
        .required(true)
        .allowableValues(TableType.NFLOW.toString(), TableType.VALID.toString(), TableType.INVALID.toString(), TableType.PROFILE.toString(), TableType.MASTER.toString(), ALL_TABLES)
        .defaultValue(ALL_TABLES)
        .build();

    // Relationships
    private final Set<Relationship> relationships;
    private final List<PropertyDescriptor> propDescriptors;

    public RegisterNflowTables() {
        final Set<Relationship> r = new HashSet<>();
        r.add(REL_SUCCESS);
        r.add(REL_FAILURE);
        relationships = Collections.unmodifiableSet(r);

        final List<PropertyDescriptor> pds = new ArrayList<>();
        pds.add(THRIFT_SERVICE);
        pds.add(NFLOW_CATEGORY);
        pds.add(NFLOW_NAME);
        pds.add(TABLE_TYPE);
        pds.add(FIELD_SPECIFICATION);
        pds.add(PARTITION_SPECS);
        pds.add(NFLOW_FIELD_SPECIFICATION);
        pds.add(NFLOW_FORMAT_SPECS);
        pds.add(TARGET_FORMAT_SPECS);
        pds.add(TARGET_TBLPROPERTIES);
        pds.add(NFLOW_ROOT);
        pds.add(PROFILE_ROOT);
        pds.add(MASTER_ROOT);

        propDescriptors = Collections.unmodifiableList(pds);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propDescriptors;
    }


    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        // Verify flow file exists
        final FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        // Verify properties and attributes
        final String nflowFormatOptions = Optional.ofNullable(context.getProperty(NFLOW_FORMAT_SPECS).evaluateAttributeExpressions(flowFile).getValue())
            .filter(StringUtils::isNotEmpty)
            .orElse(DEFAULT_NFLOW_FORMAT_OPTIONS);
        final String targetFormatOptions = Optional.ofNullable(context.getProperty(TARGET_FORMAT_SPECS).evaluateAttributeExpressions(flowFile).getValue())
            .filter(StringUtils::isNotEmpty)
            .orElse(DEFAULT_STORAGE_FORMAT);
        final String targetTableProperties = context.getProperty(TARGET_TBLPROPERTIES).evaluateAttributeExpressions(flowFile).getValue();
        final ColumnSpec[] partitions = Optional.ofNullable(context.getProperty(PARTITION_SPECS).evaluateAttributeExpressions(flowFile).getValue())
            .filter(StringUtils::isNotEmpty)
            .map(ColumnSpec::createFromString)
            .orElse(new ColumnSpec[0]);
        final String tableType = context.getProperty(TABLE_TYPE).getValue();

        final ColumnSpec[] columnSpecs = Optional.ofNullable(context.getProperty(FIELD_SPECIFICATION).evaluateAttributeExpressions(flowFile).getValue())
            .filter(StringUtils::isNotEmpty)
            .map(ColumnSpec::createFromString)
            .orElse(new ColumnSpec[0]);
        if (columnSpecs == null || columnSpecs.length == 0) {
            getLog().error("Missing field specification");
            session.transfer(flowFile, IngestProperties.REL_FAILURE);
            return;
        }

        ColumnSpec[] nflowColumnSpecs = Optional.ofNullable(context.getProperty(NFLOW_FIELD_SPECIFICATION).evaluateAttributeExpressions(flowFile).getValue())
            .filter(StringUtils::isNotEmpty)
            .map(ColumnSpec::createFromString)
            .orElse(new ColumnSpec[0]);
        if (nflowColumnSpecs == null || nflowColumnSpecs.length == 0) {
            // Backwards compatibility with older templates we set the source and target to the same
            nflowColumnSpecs = columnSpecs;
        }

        final String entity = context.getProperty(IngestProperties.NFLOW_NAME).evaluateAttributeExpressions(flowFile).getValue();
        if (entity == null || entity.isEmpty()) {
            getLog().error("Missing nflow name");
            session.transfer(flowFile, IngestProperties.REL_FAILURE);
            return;
        }

        final String source = context.getProperty(IngestProperties.NFLOW_CATEGORY).evaluateAttributeExpressions(flowFile).getValue();
        if (source == null || source.isEmpty()) {
            getLog().error("Missing category name");
            session.transfer(flowFile, IngestProperties.REL_FAILURE);
            return;
        }

        final String nflowRoot = context.getProperty(NFLOW_ROOT).evaluateAttributeExpressions(flowFile).getValue();
        final String profileRoot = context.getProperty(PROFILE_ROOT).evaluateAttributeExpressions(flowFile).getValue();
        final String masterRoot = context.getProperty(MASTER_ROOT).evaluateAttributeExpressions(flowFile).getValue();
        final TableRegisterConfiguration config = new TableRegisterConfiguration(nflowRoot, profileRoot, masterRoot);

        // Register the tables
        final ThriftService thriftService = context.getProperty(THRIFT_SERVICE).asControllerService(ThriftService.class);

        try (final Connection conn = thriftService.getConnection()) {

            final TableRegisterSupport register = new TableRegisterSupport(conn, config);

            final boolean result;
            if (ALL_TABLES.equals(tableType)) {
                result = register.registerStandardTables(source, entity, nflowColumnSpecs, nflowFormatOptions, targetFormatOptions, partitions, columnSpecs, targetTableProperties);
            } else {
                result = register.registerTable(source, entity, nflowColumnSpecs, nflowFormatOptions, targetFormatOptions, partitions, columnSpecs, targetTableProperties, TableType.valueOf(tableType),
                                                true);
            }

            final Relationship relnResult = (result ? REL_SUCCESS : REL_FAILURE);
            session.transfer(flowFile, relnResult);
        } catch (final ProcessException | SQLException e) {
            getLog().error("Unable to obtain connection for {} due to {}; routing to failure", new Object[]{flowFile, e});
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}
