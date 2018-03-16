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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.onescorpin.Formatters;
import com.onescorpin.metadata.api.op.NflowDependencyDeltaResults;
import com.onescorpin.metadata.rest.model.event.NflowPreconditionTriggerEvent;
import com.onescorpin.nifi.core.api.metadata.MetadataConstants;
import com.onescorpin.nifi.core.api.precondition.NflowPreconditionEventService;
import com.onescorpin.nifi.core.api.precondition.PreconditionListener;
import com.onescorpin.nifi.v2.common.CommonProperties;

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
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static com.onescorpin.nifi.core.api.metadata.MetadataConstants.OPERATON_START_PROP;
import static com.onescorpin.nifi.v2.common.CommonProperties.NFLOW_CATEGORY;
import static com.onescorpin.nifi.v2.common.CommonProperties.NFLOW_NAME;

/**
 */
@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@Tags({"nflow", "trigger", "onescorpin"})
@CapabilityDescription(
    "Triggers the execution of a nflow whenever the conditions defined by its precondition have been met.  This process should be the first processor in a flow depends upon preconditions.")
public class TriggerNflow extends AbstractNflowProcessor {

    public static final PropertyDescriptor PRECONDITION_SERVICE = new PropertyDescriptor.Builder()
        .name("Nflow Precondition Event Service")
        .description("Service that manages preconditions which trigger nflow execution")
        .required(true)
        .identifiesControllerService(NflowPreconditionEventService.class)
        .build();
    public static final Relationship SUCCESS = new Relationship.Builder()
        .name("Success")
        .description("Relationship followed on successful precondition event.")
        .build();
    private static final String DEFAULT_EXECUTION_CONTEXT_KEY = "export.nova";
    public static final PropertyDescriptor MATCHING_EXECUTION_CONTEXT_KEYS = new PropertyDescriptor.Builder()
        .name("Matching Execution Context Keys")
        .description(
            "Comma separated list of Execution context keys or key fragments that will be applied to each of the dependent nflow execution context data set.  Only the execution context values starting with keys this set will be included in the flow file JSON content.   Any key (case insensitive) starting with one of these supplied keys will be included")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .defaultValue(DEFAULT_EXECUTION_CONTEXT_KEY)
        .expressionLanguageSupported(false)
        .required(true)
        .build();
    private static ObjectMapper MAPPER = new ObjectMapper();
    private Queue<NflowPreconditionTriggerEvent> triggerEventQueue = new LinkedBlockingQueue<>();
    private transient PreconditionListener preconditionListener;
    private transient String nflowId;

    @Override
    protected void init(ProcessorInitializationContext context) {
        super.init(context);

        MAPPER.registerModule(new JodaModule());
        MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        this.preconditionListener = createPreconditionListener();
    }

    @OnScheduled
    public void scheduled(ProcessContext context) {
        String category = context.getProperty(CommonProperties.NFLOW_CATEGORY).getValue();
        String nflowName = context.getProperty(CommonProperties.NFLOW_NAME).getValue();

        try {
            this.nflowId = getProviderService(context).getProvider().getNflowId(category, nflowName);
        } catch (Exception e) {
            getLog().warn("Failure retrieving nflow metadata" + category + "/" + nflowName, e);
            // TODO Swallowing for now until metadata client is working again
        }

        registerPreconditionListener(context, category, nflowName);
    }

    /* (non-Javadoc)
     * @see org.apache.nifi.processor.AbstractProcessor#onTrigger(org.apache.nifi.processor.ProcessContext, org.apache.nifi.processor.ProcessSession)
     */
    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
        FlowFile flowFile = produceFlowFile(context, session);

        while (flowFile != null) {
            String nflowName = context.getProperty(NFLOW_NAME).getValue();

            flowFile = session.putAttribute(flowFile, MetadataConstants.NFLOW_NAME_PROP, nflowName);
            flowFile = session.putAttribute(flowFile, OPERATON_START_PROP, Formatters.print(new DateTime()));

            session.transfer(flowFile, SUCCESS);

            flowFile = produceFlowFile(context, session);
            if (flowFile == null) {
                context.yield();
            }
        }
    }

    @Override
    protected void addProperties(List<PropertyDescriptor> props) {
        super.addProperties(props);
        props.add(PRECONDITION_SERVICE);
        props.add(NFLOW_CATEGORY);
        props.add(NFLOW_NAME);
        props.add(MATCHING_EXECUTION_CONTEXT_KEYS);
    }

    @Override
    protected void addRelationships(Set<Relationship> rels) {
        super.addRelationships(rels);
        rels.add(SUCCESS);
    }

    protected NflowPreconditionEventService getPreconditionService(ProcessContext context) {
        return context.getProperty(PRECONDITION_SERVICE).asControllerService(NflowPreconditionEventService.class);
    }

    protected FlowFile produceFlowFile(ProcessContext context, ProcessSession session) {
        NflowPreconditionTriggerEvent event = this.triggerEventQueue.poll();
        if (event != null) {
            //drain queue
            this.triggerEventQueue.clear();
            return createFlowFile(context, session, event);
        } else {
            return session.get();
        }
    }

    private List<String> getMatchingExecutionContextKeys(ProcessContext context) {
        String executionContextKeys = context.getProperty(MATCHING_EXECUTION_CONTEXT_KEYS).getValue();
        if (StringUtils.isBlank(executionContextKeys)) {
            executionContextKeys = DEFAULT_EXECUTION_CONTEXT_KEY;
        }
        //split and trim
        return new ArrayList<String>(Arrays.asList(executionContextKeys.trim().split("\\s*,\\s*")));
    }


    private FlowFile createFlowFile(ProcessContext context,
                                    ProcessSession session,
                                    NflowPreconditionTriggerEvent event) {

        getLog().info("createFlowFile for Nflow {}", new Object[]{this.nflowId});
        FlowFile file = null;
        if (this.nflowId != null) {
            NflowDependencyDeltaResults deltas = getProviderService(context).getProvider().getNflowDependentResultDeltas(this.nflowId);
            if (deltas != null && deltas.getDependentNflowNames() != null && !deltas.getDependentNflowNames().isEmpty()) {
                file = session.create();

                try {
                    List<String> keysToMatch = getMatchingExecutionContextKeys(context);
                    getLog().info("Reducing the Execution Context to match {} keys ", new Object[]{StringUtils.join((keysToMatch))});
                    deltas.reduceExecutionContextToMatchingKeys(keysToMatch);
                    String value = MAPPER.writeValueAsString(deltas);
                    //add the json as an attr value?
                    // file = session.putAttribute(file, ComponentAttributes.NFLOW_DEPENDENT_RESULT_DELTAS.key(), value);
                    //write the json back to the flow file content
                    file = session.write(file, new OutputStreamCallback() {
                        @Override
                        public void process(OutputStream outputStream) throws IOException {
                            outputStream.write(value.getBytes(StandardCharsets.UTF_8));
                        }
                    });
                } catch (JsonProcessingException e) {
                    getLog().warn("Failed to serialize nflow dependency result deltas", e);
                    // TODO Swallow the exception and produce the flow file anyway?
                }
            } else {
                getLog().debug("Found no dependent nflows");
            }
        }
        if (file == null) {
            file = session.get();
        }

        return file;
    }

    private void registerPreconditionListener(ProcessContext context, String category, String nflowName) {
        NflowPreconditionEventService precondService = getPreconditionService(context);

        precondService.addListener(category, nflowName, preconditionListener);
    }

    private PreconditionListener createPreconditionListener() {
        PreconditionListener listener = new PreconditionListener() {
            @Override
            public void triggered(NflowPreconditionTriggerEvent event) {
                getLog().debug("Precondition event triggered: {}", new Object[]{event});

                TriggerNflow.this.triggerEventQueue.add(event);
            }
        };
        return listener;
    }

}
