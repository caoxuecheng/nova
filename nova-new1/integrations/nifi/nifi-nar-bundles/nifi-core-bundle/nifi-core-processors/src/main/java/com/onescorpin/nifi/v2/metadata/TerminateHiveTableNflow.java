/**
 *
 */
package com.onescorpin.nifi.v2.metadata;

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

import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.HiveTableDatasource;
import com.onescorpin.metadata.rest.model.op.DataOperation;
import com.onescorpin.metadata.rest.model.op.DataOperation.State;
import com.onescorpin.metadata.rest.model.op.Dataset;
import com.onescorpin.nifi.core.api.metadata.MetadataProvider;

import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;

import java.util.List;


/**
 */
@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@Tags({"nflow", "termination", "onescorpin"})
@CapabilityDescription("Records the termination of a nflow including the result of the process and table and partitions updated.")
public class TerminateHiveTableNflow extends AbstractTerminateNflow {

    @Override
    protected void addProperties(List<PropertyDescriptor> props) {
        super.addProperties(props);
        props.add(HiveTableProperties.DATABASE_NAME);
        props.add(HiveTableProperties.TABLE_NAME);
        props.add(HiveTableProperties.TABLE_LOCATION);
    }

    @Override
    protected Datasource createDestinationDatasource(ProcessContext context, String datasetName, String descr) {
        MetadataProvider provider = getProviderService(context).getProvider();
        String databaseName = context.getProperty(HiveTableProperties.DATABASE_NAME).getValue();
        String tableName = context.getProperty(HiveTableProperties.TABLE_NAME).getValue();

        return provider.ensureHiveTableDatasource(datasetName, "", databaseName, tableName);
    }

    @Override
    protected DataOperation completeOperation(ProcessContext context,
                                              FlowFile flowFile,
                                              Datasource dataset,
                                              DataOperation op,
                                              DataOperation.State state) {
        MetadataProvider provider = getProviderService(context).getProvider();

        if (state == State.SUCCESS) {
            HiveTableDatasource hds = (HiveTableDatasource) dataset;
            Dataset changeSet = provider.createDataset(hds, null);

            return provider.completeOperation(op.getId(), "", changeSet);
        } else {
            return provider.completeOperation(op.getId(), "", state);
        }

    }
}
