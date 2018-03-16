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

import com.onescorpin.Formatters;
import com.onescorpin.metadata.api.sla.DatasourceUpdatedSinceNflowExecuted;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.event.NflowPreconditionTriggerEvent;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.nifi.core.api.metadata.MetadataConstants;
import com.onescorpin.nifi.core.api.metadata.MetadataProvider;
import com.onescorpin.nifi.core.api.precondition.NflowPreconditionEventService;
import com.onescorpin.nifi.core.api.precondition.PreconditionListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static com.onescorpin.nifi.core.api.metadata.MetadataConstants.NFLOW_ID_PROP;
import static com.onescorpin.nifi.core.api.metadata.MetadataConstants.OPERATON_START_PROP;
import static com.onescorpin.nifi.core.api.metadata.MetadataConstants.SRC_DATASET_ID_PROP;

/**
 */
@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@Tags({"nflow", "begin", "onescorpin"})
@CapabilityDescription(
    "Records the start of a nflow to be tracked and listens to events which may trigger a flow. This processor should be either the first processor or immediately follow the first processor in a flow.")
public class BeginNflow extends AbstractNflowProcessor {

    public static final PropertyDescriptor PRECONDITION_SERVICE = new PropertyDescriptor.Builder()
        .name("Nflow Precondition Event Service")
        .description("Service that manages preconditions that trigger nflow execution")
        .required(false)
        .identifiesControllerService(NflowPreconditionEventService.class)
        .build();
    public static final PropertyDescriptor NFLOW_NAME = new PropertyDescriptor.Builder()
        .name(NFLOW_ID_PROP)
        .displayName("Nflow name")
        .description("The unique name of the nflow that is beginning")
        .required(true)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();
    // TODO re-enable caching when we do more intelligent handling when the nflow and datasource info has been
    // removed from the metadata store.
//    private AtomicReference<String> nflowId = new AtomicReference<>();
//    private Set<Datasource> sourceDatasources = Collections.synchronizedSet(new HashSet<Datasource>());
    public static final PropertyDescriptor CATEGORY_NAME = new PropertyDescriptor.Builder()
        .name(NFLOW_ID_PROP)
        .displayName("Category name")
        .description("The unique name of the category that is beginning")
        .required(true)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();
    public static final PropertyDescriptor SRC_DATASOURCES_NAME = new PropertyDescriptor.Builder()
        .name(SRC_DATASET_ID_PROP)
        .displayName("Source datasource name")
        .description("The name of the datasource that this nflow will read from (optional)")
        .required(false)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();
    public static final Relationship SUCCESS = new Relationship.Builder()
        .name("Success")
        .description("Relationship followed on successful metadata capture.")
        .build();
    public static final Relationship FAILURE = new Relationship.Builder()
        .name("Failure")
        .description("Relationship followed on failed metadata capture.")
        .build();
    private Queue<NflowPreconditionTriggerEvent> pendingChanges = new LinkedBlockingQueue<>();
    private PreconditionListener preconditionListener;

    @Override
    protected void init(ProcessorInitializationContext context) {
        super.init(context);
    }

    @OnScheduled
    public Nflow ensureNflowMetadata(ProcessContext context) {
        MetadataProvider provider = getProviderService(context).getProvider();
        String nflowName = context.getProperty(NFLOW_NAME).getValue();
        String categoryName = context.getProperty(CATEGORY_NAME).getValue();
        Nflow nflow = provider.ensureNflow(categoryName, nflowName, "");
//        this.nflowId.set(nflow.getId());

        String datasourcesName = context.getProperty(SRC_DATASOURCES_NAME).getValue();

        if (!StringUtils.isEmpty(datasourcesName)) {
            String[] dsNameArr = datasourcesName.split("\\s*,\\s*");

            for (String dsName : dsNameArr) {
                setupSource(context, nflow, dsName.trim());
            }

            ensurePreconditon(context, nflow, dsNameArr);
            ensurePreconditonListener(context, nflow, dsNameArr);
        }

        return nflow;
    }

    protected void setupSource(ProcessContext context, Nflow nflow, String datasourceName) {
        MetadataProvider provider = getProviderService(context).getProvider();
        Datasource datasource = getSourceDatasource(context, datasourceName);

        if (datasource != null) {
            getLog().debug("ensuring nflow source - nflow: {} datasource: {}", new Object[]{nflow.getId(), datasource.getId()});
            provider.ensureNflowSource(nflow.getId(), datasource.getId());
//            this.sourceDatasources.add(datasource);
        } else {
            throw new ProcessException("Source datasource does not exist: " + datasourceName);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.nifi.processor.AbstractProcessor#onTrigger(org.apache.nifi.processor.ProcessContext, org.apache.nifi.processor.ProcessSession)
     */
    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = produceFlowFile(session);

        while (flowFile != null) {
            // TODO Remove when we do more intelligent handling when the nflow and datasource info has been
            // removed from the metadata store.
            Nflow nflow = ensureNflowMetadata(context);

            flowFile = session.putAttribute(flowFile, MetadataConstants.NFLOW_ID_PROP, nflow.getId().toString());
            flowFile = session.putAttribute(flowFile, OPERATON_START_PROP, Formatters.print(new DateTime()));

            session.transfer(flowFile, SUCCESS);

            flowFile = produceFlowFile(session);
            if (flowFile == null) {
                context.yield();
            }
        }
    }

    @Override
    protected void addProperties(List<PropertyDescriptor> props) {
        super.addProperties(props);
        props.add(PRECONDITION_SERVICE);
        props.add(NFLOW_NAME);
//        props.add(PRECONDITION_NAME);
        props.add(SRC_DATASOURCES_NAME);
    }

    @Override
    protected void addRelationships(Set<Relationship> rels) {
        super.addRelationships(rels);
        rels.add(SUCCESS);
        rels.add(FAILURE);
    }

    protected NflowPreconditionEventService getPreconditionService(ProcessContext context) {
        return context.getProperty(PRECONDITION_SERVICE).asControllerService(NflowPreconditionEventService.class);
    }

    protected FlowFile produceFlowFile(ProcessSession session) {
        NflowPreconditionTriggerEvent event = this.pendingChanges.poll();
        if (event != null) {
            return createFlowFile(session, event);
        } else {
            return session.get();
        }
    }

    private FlowFile createFlowFile(ProcessSession session,
                                    NflowPreconditionTriggerEvent event) {
        // TODO add changes to flow file
        return session.create();
    }

    private void ensurePreconditon(ProcessContext context, Nflow nflow, String[] dsNames) {
        MetadataProvider provider = getProviderService(context).getProvider();

        // If no precondition exits yet install one that depends on the datasources.
        if (nflow.getPrecondition() == null) {
            getLog().debug("Setting default nflow preconditions for: " + dsNames);

            Metric[] metrics = new Metric[dsNames.length];

            for (int idx = 0; idx < metrics.length; idx++) {
                DatasourceUpdatedSinceNflowExecuted metric = new DatasourceUpdatedSinceNflowExecuted(dsNames[idx], nflow.getSystemName());
                metrics[idx] = metric;
            }

            //  provider.ensurePrecondition(nflow.getId(), metrics);
        }
    }

    private void ensurePreconditonListener(ProcessContext context, Nflow nflow, String[] dsNames) {
        if (this.preconditionListener == null) {
            MetadataProvider provider = getProviderService(context).getProvider();
            NflowPreconditionEventService precondService = getPreconditionService(context);

            PreconditionListener listener = new PreconditionListener() {
                @Override
                public void triggered(NflowPreconditionTriggerEvent event) {
                    getLog().debug("Precondition event triggered: ", new Object[]{event});

                    BeginNflow.this.pendingChanges.add(event);
                }
            };

            for (String dsName : dsNames) {
                getLog().debug("Adding precondition listener for datasoure name: " + dsName);
                precondService.addListener("", dsName, listener);
            }

            this.preconditionListener = listener;
        }
    }

    protected Datasource getSourceDatasource(ProcessContext context, String datasourceName) {
        if (datasourceName != null) {
            Datasource dataset = findDatasource(context, datasourceName);

            if (dataset != null) {
                return dataset;
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

}
