package com.onescorpin.nflowmgr.nifi;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCache;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.service.template.NiFiTemplateCache;
import com.onescorpin.nifi.nflowmgr.NflowCreationException;
import com.onescorpin.nifi.nflowmgr.NflowRollbackException;
import com.onescorpin.nifi.nflowmgr.InputOutputPort;
import com.onescorpin.nifi.nflowmgr.TemplateCreationHelper;
import com.onescorpin.nifi.nflowmgr.TemplateInstance;
import com.onescorpin.nifi.rest.NiFiObjectCache;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.nifi.rest.client.NifiClientRuntimeException;
import com.onescorpin.nifi.rest.client.NifiComponentNotFoundException;
import com.onescorpin.nifi.rest.client.layout.AlignProcessGroupComponents;
import com.onescorpin.nifi.rest.model.NiFiPropertyDescriptorTransform;
import com.onescorpin.nifi.rest.model.NifiError;
import com.onescorpin.nifi.rest.model.NifiProcessGroup;
import com.onescorpin.nifi.rest.model.NifiProcessorSchedule;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.nifi.rest.support.NifiConnectionUtil;
import com.onescorpin.nifi.rest.support.NifiConstants;
import com.onescorpin.nifi.rest.support.NifiNflowConstants;
import com.onescorpin.nifi.rest.support.NifiProcessUtil;
import com.onescorpin.nifi.rest.support.NifiPropertyUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.web.api.dto.ConnectableDTO;
import org.apache.nifi.web.api.dto.ConnectionDTO;
import org.apache.nifi.web.api.dto.FlowSnippetDTO;
import org.apache.nifi.web.api.dto.PortDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.apache.nifi.web.api.dto.TemplateDTO;
import org.apache.nifi.web.api.dto.status.ProcessGroupStatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Builds/updates a NiFi nflow flow based on a NiFi template and a Nflow Manager Nflow.
 */
public class CreateNflowBuilder {

    private static final Logger log = LoggerFactory.getLogger(CreateNflowBuilder.class);

    LegacyNifiRestClient restClient;
    NiFiTemplateCache niFiTemplateCache;
    TemplateCreationHelper templateCreationHelper;
    private NiFiObjectCache niFiObjectCache;
    private NifiFlowCache nifiFlowCache;
    private String templateId;
    private String category;
    private String nflowName;
    private boolean enabled = true;
    private NflowMetadata nflowMetadata;
    private PropertyExpressionResolver propertyExpressionResolver;
    private String inputProcessorType;
    private String reusableTemplateCategoryName = TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME;
    private boolean isReusableTemplate;
    private boolean newCategory = false;
    /**
     * if true it will remove the versioned process group with the <nflow> - timestamp
     * if false it will keep thhe versioned process group
     * These can be cleaned up later through the {@code CleanupStaleNflowRevisions} class
     */
    private boolean removeInactiveVersionedProcessGroup;
    /**
     * List of Input / Output Port connections
     */
    @Nonnull
    private List<InputOutputPort> inputOutputPorts = Lists.newArrayList();
    private NifiProcessGroup newProcessGroup = null;
    private ProcessGroupDTO previousNflowProcessGroup = null;
    private String version;
    private List<NifiProperty> properties;
    private NifiProcessorSchedule nflowSchedule;
    private NiFiPropertyDescriptorTransform propertyDescriptorTransform;
    private List<NifiProperty> modifiedProperties;
    private List<NifiError> errors = new ArrayList<>();
    /**
     * the category group in NiFi where this nflow resides
     **/
    private ProcessGroupDTO categoryGroup;

    private boolean autoAlign = true;


    protected CreateNflowBuilder(LegacyNifiRestClient restClient, NifiFlowCache nifiFlowCache, NflowMetadata nflowMetadata, String templateId, PropertyExpressionResolver propertyExpressionResolver,
                                NiFiPropertyDescriptorTransform propertyDescriptorTransform, NiFiObjectCache niFiObjectCache) {
        this.restClient = restClient;
        this.nifiFlowCache = nifiFlowCache;
        this.nflowMetadata = nflowMetadata;
        this.category = nflowMetadata.getCategory().getSystemName();
        this.nflowName = nflowMetadata.getSystemNflowName();
        this.templateId = templateId;
        this.templateCreationHelper = new TemplateCreationHelper(this.restClient, niFiObjectCache);
        this.templateCreationHelper.setTemplateProperties(nflowMetadata.getRegisteredTemplate().getProperties());
        this.propertyExpressionResolver = propertyExpressionResolver;
        this.propertyDescriptorTransform = propertyDescriptorTransform;
        this.niFiObjectCache = niFiObjectCache;
    }


    public static CreateNflowBuilder newNflow(LegacyNifiRestClient restClient, NifiFlowCache nifiFlowCache, NflowMetadata nflowMetadata, String templateId,
                                            PropertyExpressionResolver propertyExpressionResolver, NiFiPropertyDescriptorTransform propertyDescriptorTransform,
                                            NiFiObjectCache createNflowBuilderCache) {
        return new CreateNflowBuilder(restClient, nifiFlowCache, nflowMetadata, templateId, propertyExpressionResolver, propertyDescriptorTransform, createNflowBuilderCache);
    }

    public CreateNflowBuilder nflowSchedule(NifiProcessorSchedule nflowSchedule) {
        this.nflowSchedule = nflowSchedule;
        return this;
    }

    public CreateNflowBuilder reusableTemplateCategoryName(String reusableTemplateCategoryName) {
        this.reusableTemplateCategoryName = reusableTemplateCategoryName;
        return this;
    }

    public CreateNflowBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public CreateNflowBuilder removeInactiveVersionedProcessGroup(boolean removeInactiveVersionedProcessGroup) {
        this.removeInactiveVersionedProcessGroup = removeInactiveVersionedProcessGroup;
        return this;
    }

    public CreateNflowBuilder autoAlign(boolean autoAlign) {
        this.autoAlign = autoAlign;
        return this;
    }

    /**
     * Adds the specified Input Port and Output Port connection to this nflow.
     *
     * @param inputOutputPort the port connection
     * @return this nflow builder
     */
    public CreateNflowBuilder addInputOutputPort(@Nonnull final InputOutputPort inputOutputPort) {
        inputOutputPorts.add(inputOutputPort);
        return this;
    }

    public CreateNflowBuilder inputProcessorType(String inputProcessorType) {
        this.inputProcessorType = inputProcessorType;
        return this;
    }

    public CreateNflowBuilder properties(List<NifiProperty> properties) {
        this.properties = properties;
        return this;
    }

    public CreateNflowBuilder version(String version) {
        this.version = version;
        return this;
    }

    public CreateNflowBuilder setReusableTemplate(boolean isReusableTemplate) {
        this.isReusableTemplate = isReusableTemplate;
        return this;
    }

    private long eventTime(Stopwatch eventTime) {
        eventTime.stop();
        long elapsedTime = eventTime.elapsed(TimeUnit.MILLISECONDS);
        eventTime.reset();
        return elapsedTime;
    }

    public CreateNflowBuilder withNiFiTemplateCache(NiFiTemplateCache niFiTemplateCache){
        this.niFiTemplateCache = niFiTemplateCache;
        return this;
    }

    private TemplateDTO getTemplate(){
        if(niFiTemplateCache != null){
            return niFiTemplateCache.geTemplate(templateId,null);
        }
        else {
           return restClient.getTemplateById(templateId);
        }
    }

    /**
     * Build the NiFi flow instance
     *
     * @return an object indicating if the nflow flow was successfully built or not
     */
    public NifiProcessGroup build() throws NflowCreationException {
        try {
            log.info("Creating the nflow {}.{} ", category, nflowName);
            newProcessGroup = null;
            Stopwatch totalTime = Stopwatch.createStarted();
            Stopwatch eventTime = Stopwatch.createStarted();

            TemplateDTO template = getTemplate();
            if (template != null) {
                log.debug("Time to get Template {}.  ElapsedTime: {} ms", template.getName(), eventTime(eventTime));

                //create the encompassing process group
                eventTime.start();
                ProcessGroupDTO nflowProcessGroup = createProcessGroupForNflow();
                log.debug("Time to create process group.  ElapsedTime: {} ms", eventTime(eventTime));
                if (nflowProcessGroup != null) {
                    String processGroupId = nflowProcessGroup.getId();
                    //snapshot the existing controller services
                    eventTime.start();
                    templateCreationHelper.snapshotControllerServiceReferences();
                    log.debug("Time to snapshotControllerServices.  ElapsedTime: {} ms", eventTime(eventTime));

                    //create the flow from the template
                    eventTime.start();
                    TemplateInstance instance = templateCreationHelper.instantiateFlowFromTemplate(processGroupId, templateId);
                    FlowSnippetDTO nflowInstance = instance.getFlowSnippetDTO();
                    nflowProcessGroup.setContents(nflowInstance);
                    log.debug("Time to instantiateFlowFromTemplate.  ElapsedTime: {} ms", eventTime(eventTime));

                    eventTime.start();
                    String nflowCategoryId = nflowProcessGroup.getParentGroupId();
                    ProcessGroupDTO categoryGroup = this.categoryGroup;
                    if (categoryGroup == null) {
                        categoryGroup = this.categoryGroup = restClient.getProcessGroup(nflowCategoryId, false, false);
                    }
                    //update the group with this template?
                    updatePortConnectionsForProcessGroup(nflowProcessGroup, categoryGroup);
                    log.debug("Time to updatePortConnectionsForProcessGroup.  ElapsedTime: {} ms", eventTime(eventTime));

                    eventTime.start();
                    //mark the new services that were created as a result of creating the new flow from the template
                    templateCreationHelper.identifyNewlyCreatedControllerServiceReferences(instance);
                    log.debug("Time to identifyNewlyCreatedControllerServiceReferences.  ElapsedTime: {} ms", eventTime(eventTime));

                    eventTime.start();
                    //match the properties incoming to the defined properties
                    updateProcessGroupProperties(processGroupId, nflowProcessGroup.getName());
                    log.debug("Time to updateProcessGroupProperties.  ElapsedTime: {} ms", eventTime(eventTime));

                    eventTime.start();
                    //Fetch the Nflow Group now that it has the flow in it
                    ProcessGroupDTO entity = restClient.getProcessGroup(processGroupId, true, true);
                    log.debug("Time to getProcessGroup.  ElapsedTime: {} ms", eventTime(eventTime));

                    eventTime.start();
                    ProcessorDTO input = fetchInputProcessorForProcessGroup(entity);
                    ProcessorDTO cleanupProcessor = NifiProcessUtil.findFirstProcessorsByType(NifiProcessUtil.getInputProcessors(entity),
                                                                                              "com.onescorpin.nifi.v2.metadata.TriggerCleanup");
                    List<ProcessorDTO> nonInputProcessors = NifiProcessUtil.getNonInputProcessors(entity);
                    log.debug("Time to fetchInputProcessorForProcessGroup.  ElapsedTime: {} ms", eventTime(eventTime));

                    eventTime.start();
                    List<NifiProperty> updatedControllerServiceProperties = new ArrayList<>();
                    //update any references to the controller services and try to assign the value to an enabled service if it is not already
                    if (input != null) {
                        updatedControllerServiceProperties.addAll(templateCreationHelper.updateControllerServiceReferences(Lists.newArrayList(input), instance));
                    }
                    if (cleanupProcessor != null) {
                        updatedControllerServiceProperties.addAll(templateCreationHelper.updateControllerServiceReferences(Collections.singletonList(cleanupProcessor),instance));
                    }
                    updatedControllerServiceProperties.addAll(templateCreationHelper.updateControllerServiceReferences(nonInputProcessors, instance));
                    log.debug("Time to updatedControllerServiceProperties.  ElapsedTime: {} ms", eventTime(eventTime));

                    eventTime.start();
                    //refetch processors for updated errors
                    entity = restClient.getProcessGroup(processGroupId, true, true);
                    input = fetchInputProcessorForProcessGroup(entity);
                    nonInputProcessors = NifiProcessUtil.getNonInputProcessors(entity);

                    newProcessGroup = new NifiProcessGroup(entity, input, nonInputProcessors);
                    log.debug("Time to re-fetchInputProcessorForProcessGroup.  ElapsedTime: {} ms", eventTime(eventTime));
                    //Validate and if invalid Delete the process group
                    if (newProcessGroup.hasFatalErrors()) {
                        eventTime.start();
                        removeProcessGroup(entity);
                        // cleanupControllerServices();
                        newProcessGroup.setSuccess(false);
                        log.debug("Time to removeProcessGroup. Errors found.  ElapsedTime: {} ms", eventTime(eventTime));
                    } else {
                        eventTime.start();
                        //update the input schedule
                        updateNflowSchedule(newProcessGroup, input);
                        log.debug("Time to update nflow schedule.  ElapsedTime: {} ms", eventTime(eventTime));
                        eventTime.start();

                        //just need to update for this processgroup
                        Collection<ProcessorDTO> processors = NifiProcessUtil.getProcessors(entity);
                        Collection<ConnectionDTO> connections = NifiConnectionUtil.getAllConnections(entity);
                        nifiFlowCache.updateFlowForNflow(nflowMetadata, entity.getId(), processors, connections);
                        log.debug("Time to build flow graph with {} processors and {} connections.  ElapsedTime: {} ms", processors.size(), connections.size(), eventTime(eventTime));
                        /*

                        //Cache the processorIds to the respective flowIds for availability in the ProvenanceReportingTask
                        NifiVisitableProcessGroup group = nifiFlowCache.getFlowOrder(newProcessGroup.getProcessGroupEntity(), true);
                       log.debug("Time to get the flow order.  ElapsedTime: {} ms", eventTime(eventTime));

                        eventTime.start();
                        NifiFlowProcessGroup
                            flow =
                            new NifiFlowBuilder().build(
                                group);
                       log.debug("Time to build flow graph with {} processors.  ElapsedTime: {} ms", flow.getProcessorMap().size(), eventTime(eventTime));

                        eventTime.start();
                        nifiFlowCache.updateFlow(nflowMetadata, flow);
                       log.debug("Time to update NiFiFlowCache with {} processors.  ElapsedTime: {} ms", flow.getProcessorMap().size(), eventTime(eventTime));
                        */
                        eventTime.start();
                        //disable all inputs
                        restClient.disableInputProcessors(newProcessGroup.getProcessGroupEntity().getId());
                        log.debug("Time to disableInputProcessors.  ElapsedTime: {} ms", eventTime(eventTime));

                        eventTime.start();
                        //mark everything else as running
                        templateCreationHelper.markProcessorsAsRunning(newProcessGroup);
                        log.debug("Time to markNonInputsAsRunning.  ElapsedTime: {} ms", eventTime(eventTime));

                        //if desired start the input processor
                        if (input != null) {
                            eventTime.start();
                            if (enabled) {
                                markInputAsRunning(newProcessGroup, input);
                                ///make the input/output ports in the category group as running
                                if (hasConnectionPorts()) {
                                    templateCreationHelper.markConnectionPortsAsRunning(entity);
                                }
                            } else {
                                ///make the input/output ports in the category group as running
                                if (hasConnectionPorts()) {
                                    templateCreationHelper.markConnectionPortsAsRunning(entity);
                                }
                                markInputAsStopped(newProcessGroup, input);
                            }
                            log.debug("Time to mark input as {}.  ElapsedTime: {} ms", (enabled ? "Running" : "Stopped"), eventTime(eventTime));
                        }

                        if (newProcessGroup.hasFatalErrors()) {
                            eventTime.start();
                            rollback();
                            newProcessGroup.setRolledBack(true);
                            //  cleanupControllerServices();
                            newProcessGroup.setSuccess(false);
                            log.debug("Time to rollback on Fatal Errors.  ElapsedTime: {} ms", eventTime(eventTime));
                        }
                        List<NifiError> templateCreationErrors = templateCreationHelper.getErrors();
                        if (templateCreationErrors != null) {
                            errors.addAll(templateCreationErrors);
                        }

                        //add any global errors to the object
                        if (errors != null && !errors.isEmpty()) {
                            for (NifiError error : errors) {
                                newProcessGroup.addError(error);
                                if (error.isFatal()) {
                                    newProcessGroup.setSuccess(false);
                                    if (!newProcessGroup.isRolledBack()) {
                                        rollback();
                                        newProcessGroup.setRolledBack(true);
                                    }
                                }
                            }
                        }
                    }

                    eventTime.start();
                    templateCreationHelper.cleanupControllerServices();
                    //fix the nflow metadata controller service references
                    updateNflowMetadataControllerServiceReferences(updatedControllerServiceProperties);
                    log.debug("Time cleanup controller services.  ElapsedTime: {} ms", eventTime(eventTime));

                    //align items
                    if (this.autoAlign) {
                        eventTime.start();
                        log.info("Aligning Nflow flows in NiFi ");
                        AlignProcessGroupComponents alignProcessGroupComponents = new AlignProcessGroupComponents(restClient.getNiFiRestClient(), entity.getParentGroupId());
                        alignProcessGroupComponents.autoLayout();
                        //if this is a new nflowProcessGroup (i.e. new category), align the root level items also
                        //fetch the parent to get that id to align
                        if (newCategory) {
                            log.info("This is the first nflow created in the category {}.  Aligning the categories. ", nflowMetadata.getCategory().getSystemName());
                            new AlignProcessGroupComponents(restClient.getNiFiRestClient(), this.categoryGroup.getParentGroupId()).autoLayout();
                        }
                        log.info("Time align nflow process groups.  ElapsedTime: {} ms", eventTime(eventTime));

                    } else {
                        log.info("Skipping auto alignment in NiFi. You can always manually align this category and all of its nflows by using the rest api: /v1/nflowmgr/nifi/auto-align/{}",
                                 entity.getParentGroupId());
                        if (newCategory) {
                            log.info("To re align the categories: /v1/nflowmgr/nifi/auto-align/{}", this.categoryGroup.getParentGroupId());
                        }
                    }


                }
            }
            else {
                log.error("Unable to create/save the nflow {}.  Unable to find a template for id {}",nflowName,templateId);
                throw new NflowCreationException("Unable to create the nflow [" + nflowName + "]. Unable to find a template with id "+templateId);
            }
            log.info("Time save Nflow flow in NiFi.  ElapsedTime: {} ms", eventTime(totalTime));
            return newProcessGroup;
        } catch (NifiClientRuntimeException e) {
            throw new NflowCreationException("Unable to create the nflow [" + nflowName + "]. " + e.getMessage(), e);
        }
    }


    /**
     * update nflow metadata to point to the valid controller services
     */
    private void updateNflowMetadataControllerServiceReferences(List<NifiProperty> updatedControllerServiceProperties) {
        //map of the previous to new service values
        Map<String, String>
            controllerServiceChangeMap =
            updatedControllerServiceProperties.stream().collect(Collectors.toMap(p -> p.getProcessorNameTypeKey(), p -> p.getValue(), (service1, service2) -> service1));
        if (!updatedControllerServiceProperties.isEmpty()) {
            nflowMetadata.getProperties().stream().filter(property -> controllerServiceChangeMap.containsKey(property.getProcessorNameTypeKey())).forEach(
                (NifiProperty p) -> p.setValue(controllerServiceChangeMap.get(p.getProcessorNameTypeKey())));
        }
    }

    public ProcessGroupDTO rollback() throws NflowRollbackException {
        if (newProcessGroup != null) {
            try {
                removeProcessGroup(newProcessGroup.getProcessGroupEntity());
            } catch (NifiClientRuntimeException e) {
                log.error("Unable to delete the ProcessGroup on rollback {} ", e.getMessage());
            }
        }

        String
            parentGroupId =
            newProcessGroup != null ? newProcessGroup.getProcessGroupEntity().getParentGroupId()
                                    : (previousNflowProcessGroup != null ? previousNflowProcessGroup.getParentGroupId() : null);
        try {
            if (StringUtils.isNotBlank(parentGroupId)) {
                ProcessGroupDTO nflowGroup = restClient.getProcessGroupByName(parentGroupId, nflowName);
                //rename this group to be something else if for some reason we were not able to delete it
                if (nflowGroup != null) {
                    nflowGroup.setName(nflowGroup.getName() + ".rollback - " + new Date().getTime());
                    restClient.updateProcessGroup(nflowGroup);
                    nflowGroup = restClient.getProcessGroupByName(parentGroupId, nflowName);
                }

                //attempt to reset the last version back to this nflow process group... do so only if there is no nflow group with this name
                //there shouldn't be as we should have deleted it above
                if (nflowGroup == null) {
                    if (previousNflowProcessGroup != null) {

                        ProcessGroupDTO entity = restClient.getProcessGroup(previousNflowProcessGroup.getId(), false, false);
                        if (entity != null) {
                            entity.setName(nflowName);
                            entity = restClient.updateProcessGroup(entity);

                            ProcessGroupDTO categoryGroup = restClient.getProcessGroup(entity.getParentGroupId(), false, false);

                            updatePortConnectionsForProcessGroup(entity, categoryGroup);

                            //disable all inputs
                            restClient.disableInputProcessors(entity.getId());

                            //mark everything else as running
                            restClient.markProcessorGroupAsRunning(entity);
                            if (hasConnectionPorts()) {
                                templateCreationHelper.markConnectionPortsAsRunning(entity);
                            }

                            //Set the state correctly for the inputs
                            if (enabled) {
                                restClient.setInputProcessorState(entity.getId(),
                                                                  inputProcessorType, NifiProcessUtil.PROCESS_STATE.RUNNING);
                            } else {
                                restClient.setInputProcessorState(entity.getId(),
                                                                  inputProcessorType, NifiProcessUtil.PROCESS_STATE.STOPPED);
                            }
                            return entity;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new NflowRollbackException("Unable to rollback nflow [" + nflowName + "] with Parent Group Id of [" + parentGroupId + "] " + e.getMessage(), e);

        }
        return null;
    }


    private void connectNflowToReusableTemplate(ProcessGroupDTO nflowProcessGroup, ProcessGroupDTO categoryProcessGroup) throws NifiComponentNotFoundException {

        Stopwatch stopwatch = Stopwatch.createStarted();
        String categoryProcessGroupId = categoryProcessGroup.getId();
        String categoryParentGroupId = categoryProcessGroup.getParentGroupId();
        String categoryProcessGroupName = categoryProcessGroup.getName();
        String nflowProcessGroupId = nflowProcessGroup.getId();
        String nflowProcessGroupName = nflowProcessGroup.getName();

        ProcessGroupDTO reusableTemplateCategory = niFiObjectCache.getReusableTemplateCategoryProcessGroup();

        if (reusableTemplateCategory == null) {
            throw new NifiClientRuntimeException("Unable to find the Reusable Template Group. Please ensure NiFi has the 'reusable_templates' processgroup and appropriate reusable flow for this nflow."
                                                 + " You may need to import the base reusable template for this nflow.");
        }
        String reusableTemplateCategoryGroupId = reusableTemplateCategory.getId();
        stopwatch.stop();
        log.debug("Time to get reusableTemplateCategory: {} ", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.reset();

        Stopwatch totalStopWatch = Stopwatch.createUnstarted();
        for (InputOutputPort port : inputOutputPorts) {
            totalStopWatch.start();
            stopwatch.start();
            PortDTO reusableTemplatePort = niFiObjectCache.getReusableTemplateInputPort(port.getInputPortName());
            stopwatch.stop();
            log.debug("Time to get reusableTemplate inputPort {} : {} ", port.getInputPortName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();
            if (reusableTemplatePort != null) {

                String categoryOutputPortName = categoryProcessGroupName + " to " + port.getInputPortName();
                stopwatch.start();
                PortDTO categoryOutputPort = niFiObjectCache.getCategoryOutputPort(categoryProcessGroupId, categoryOutputPortName);
                stopwatch.stop();
                log.debug("Time to get categoryOutputPort {} : {} ", categoryOutputPortName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
                if (categoryOutputPort == null) {
                    stopwatch.start();
                    //create it
                    PortDTO portDTO = new PortDTO();
                    portDTO.setParentGroupId(categoryProcessGroupId);
                    portDTO.setName(categoryOutputPortName);
                    categoryOutputPort = restClient.getNiFiRestClient().processGroups().createOutputPort(categoryProcessGroupId, portDTO);
                    niFiObjectCache.addCategoryOutputPort(categoryProcessGroupId, categoryOutputPort);
                    stopwatch.stop();
                    log.debug("Time to create categoryOutputPort {} : {} ", categoryOutputPortName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    stopwatch.reset();

                }
                stopwatch.start();
                Set<PortDTO> nflowOutputPorts = nflowProcessGroup.getContents().getOutputPorts();
                String nflowOutputPortName = port.getOutputPortName();
                if (nflowOutputPorts == null || nflowOutputPorts.isEmpty()) {
                    nflowOutputPorts = restClient.getNiFiRestClient().processGroups().getOutputPorts(nflowProcessGroup.getId());
                }
                PortDTO nflowOutputPort = NifiConnectionUtil.findPortMatchingName(nflowOutputPorts, nflowOutputPortName);
                stopwatch.stop();
                log.debug("Time to create nflowOutputPort {} : {} ", nflowOutputPortName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
                if (nflowOutputPort != null) {
                    stopwatch.start();
                    //make the connection on the category from nflow to category
                    ConnectionDTO nflowOutputToCategoryOutputConnection = niFiObjectCache.getConnection(categoryProcessGroupId, nflowOutputPort.getId(), categoryOutputPort.getId());
                    stopwatch.stop();
                    log.debug("Time to get nflowOutputToCategoryOutputConnection: {} ", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    stopwatch.reset();
                    if (nflowOutputToCategoryOutputConnection == null) {
                        stopwatch.start();
                        //CONNECT NFLOW OUTPUT PORT TO THE Category output port
                        ConnectableDTO source = new ConnectableDTO();
                        source.setGroupId(nflowProcessGroupId);
                        source.setId(nflowOutputPort.getId());
                        source.setName(nflowProcessGroupName);
                        source.setType(NifiConstants.NIFI_PORT_TYPE.OUTPUT_PORT.name());
                        ConnectableDTO dest = new ConnectableDTO();
                        dest.setGroupId(categoryProcessGroupId);
                        dest.setName(categoryOutputPort.getName());
                        dest.setId(categoryOutputPort.getId());
                        dest.setType(NifiConstants.NIFI_PORT_TYPE.OUTPUT_PORT.name());
                        nflowOutputToCategoryOutputConnection = restClient.createConnection(categoryProcessGroupId, source, dest);
                        niFiObjectCache.addConnection(categoryProcessGroupId, nflowOutputToCategoryOutputConnection);
                        nifiFlowCache.addConnectionToCache(nflowOutputToCategoryOutputConnection);
                        stopwatch.stop();
                        log.debug("Time to create nflowOutputToCategoryOutputConnection: {} ", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                        stopwatch.reset();
                    }

                    stopwatch.start();
                    //connection made on parent (root) to reusable template
                    ConnectionDTO
                        categoryToReusableTemplateConnection = niFiObjectCache.getConnection(categoryProcessGroup.getParentGroupId(), categoryOutputPort.getId(), reusableTemplatePort.getId());
                    stopwatch.stop();
                    log.debug("Time to get categoryToReusableTemplateConnection: {} ", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                    stopwatch.reset();
                    //Now connect the category ProcessGroup to the global template
                    if (categoryToReusableTemplateConnection == null) {
                        stopwatch.start();
                        ConnectableDTO categorySource = new ConnectableDTO();
                        categorySource.setGroupId(categoryProcessGroupId);
                        categorySource.setId(categoryOutputPort.getId());
                        categorySource.setName(categoryOutputPortName);
                        categorySource.setType(NifiConstants.NIFI_PORT_TYPE.OUTPUT_PORT.name());
                        ConnectableDTO categoryToGlobalTemplate = new ConnectableDTO();
                        categoryToGlobalTemplate.setGroupId(reusableTemplateCategoryGroupId);
                        categoryToGlobalTemplate.setId(reusableTemplatePort.getId());
                        categoryToGlobalTemplate.setName(reusableTemplatePort.getName());
                        categoryToGlobalTemplate.setType(NifiConstants.NIFI_PORT_TYPE.INPUT_PORT.name());
                        categoryToReusableTemplateConnection = restClient.createConnection(categoryParentGroupId, categorySource, categoryToGlobalTemplate);
                        niFiObjectCache.addConnection(categoryParentGroupId, categoryToReusableTemplateConnection);
                        nifiFlowCache.addConnectionToCache(categoryToReusableTemplateConnection);
                        stopwatch.stop();
                        log.debug("Time to create categoryToReusableTemplateConnection: {} ", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                        stopwatch.reset();
                    }
                }


            }
            totalStopWatch.stop();
            log.debug("Time to connect nflow to {} port. ElapsedTime: {} ", port.getInputPortName(), totalStopWatch.elapsed(TimeUnit.MILLISECONDS));
            totalStopWatch.reset();
        }

    }

    private void connectNflowToReusableTemplatexx(String nflowGroupId, String nflowCategoryId) throws NifiComponentNotFoundException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        ProcessGroupDTO reusableTemplateCategory = niFiObjectCache.getReusableTemplateCategoryProcessGroup();

        if (reusableTemplateCategory == null) {
            throw new NifiClientRuntimeException("Unable to find the Reusable Template Group. Please ensure NiFi has the 'reusable_templates' processgroup and appropriate reusable flow for this nflow."
                                                 + " You may need to import the base reusable template for this nflow.");
        }
        String reusableTemplateCategoryGroupId = reusableTemplateCategory.getId();
        for (InputOutputPort port : inputOutputPorts) {
            stopwatch.start();
            restClient.connectNflowToGlobalTemplate(nflowGroupId, port.getOutputPortName(), nflowCategoryId, reusableTemplateCategoryGroupId, port.getInputPortName());
            stopwatch.stop();
            log.debug("Time to connect nflow to {} port. ElapsedTime: {} ", port.getInputPortName(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
    }


    private void ensureInputPortsForReuseableTemplate(String nflowGroupId) throws NifiComponentNotFoundException {
        ProcessGroupDTO template = restClient.getProcessGroup(nflowGroupId, false, false);
        String categoryId = template.getParentGroupId();
        restClient.createReusableTemplateInputPort(categoryId, nflowGroupId);

    }


    private boolean hasConnectionPorts() {
        return !inputOutputPorts.isEmpty() || isReusableTemplate;
    }


    private ProcessorDTO fetchInputProcessorForProcessGroup(ProcessGroupDTO entity) {
        // Find first processor by type
        final List<ProcessorDTO> inputProcessors = NifiProcessUtil.getInputProcessors(entity);
        String inputProcessorName = nflowMetadata != null ? nflowMetadata.getInputProcessorName() : null;
        final ProcessorDTO input = Optional.ofNullable(NifiProcessUtil.findFirstProcessorsByTypeAndName(inputProcessors, inputProcessorType, inputProcessorName))
            .orElseGet(() -> inputProcessors.stream()
                .filter(processor -> !processor.getType().equals(NifiProcessUtil.CLEANUP_TYPE))
                .findFirst()
                .orElse(null)
            );

        // Update cached type
        if (input != null) {
            inputProcessorType = input.getType();
        }

        return input;
    }

    private void updatePortConnectionsForProcessGroup(ProcessGroupDTO nflowGroup, ProcessGroupDTO categoryGroup) throws NifiComponentNotFoundException {
        //if the nflow has an outputPort that should go to a reusable Flow then make those connections
        if (!inputOutputPorts.isEmpty()) {
            connectNflowToReusableTemplate(nflowGroup, categoryGroup);
        }
        if (isReusableTemplate) {
            ensureInputPortsForReuseableTemplate(nflowGroup.getId());
        }
    }


    private ProcessGroupDTO createProcessGroupForNflow() throws NflowCreationException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        //create Category Process group
        this.categoryGroup =  niFiObjectCache.getCategoryProcessGroup(category);
        if (categoryGroup == null) {
            try {
                ProcessGroupDTO group = restClient.createProcessGroup(category);
                this.categoryGroup = group;
                this.newCategory = true;
                if(this.categoryGroup != null){
                    niFiObjectCache.addCategoryProcessGroup(this.categoryGroup);
                }
            } catch (Exception e) {
                //Swallow exception... it will be handled later
            }
        }
        if (this.categoryGroup == null) {
            throw new NflowCreationException("Unable to get or create the Process group for the Category " + category
                                            + ". Error occurred while creating instance of template " + templateId + " for Nflow "
                                            + nflowName);
        }
        stopwatch.stop();
        log.debug("Time to get/create Category Process Group:{} was: {} ms",category,stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.reset();

        stopwatch.start();
        //1 create the processGroup
        //check to see if the nflow exists... if so version off the old group and create a new group with this nflow
        ProcessGroupDTO nflowGroup = restClient.getProcessGroupByName(this.categoryGroup.getId(), nflowName);
        stopwatch.stop();
        log.debug("Time to find nflow Process Group: {} was: {} ms",nflowName,stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.reset();
        if (nflowGroup != null) {
            try {
                previousNflowProcessGroup = nflowGroup;
                templateCreationHelper.versionProcessGroup(nflowGroup);
            } catch (Exception e) {
                throw new NflowCreationException("Previous version of the nflow " + nflowName
                                                + " was found.  Error in attempting to version the previous nflow.  Please go into Nifi and address any issues with the Nflows Process Group", e);
            }
        }

        ProcessGroupDTO group = restClient.createProcessGroup(this.categoryGroup.getId(), nflowName);

        return group;
    }


    /**
     * removes the {@code previousNflowProcessGroup} from nifi
     */
    public void checkAndRemoveVersionedProcessGroup() {
        if (this.removeInactiveVersionedProcessGroup && previousNflowProcessGroup != null) {
            removeProcessGroup(previousNflowProcessGroup);
        }
    }


    /**
     * Removes a given processGroup from NiFi if nothing is in its queue
     */
    private void removeProcessGroup(ProcessGroupDTO processGroupDTO) {
        if (processGroupDTO != null) {
            try {
                //validate if nothing is in the queue then remove it
                Optional<ProcessGroupStatusDTO> statusDTO = restClient.getNiFiRestClient().processGroups().getStatus(processGroupDTO.getId());
                if (statusDTO.isPresent() && propertyDescriptorTransform.getQueuedCount(statusDTO.get()).equalsIgnoreCase("0")) {
                    //get connections linking to this group, delete them
                    Set<ConnectionDTO> connectionDTOs = restClient.getProcessGroupConnections(processGroupDTO.getParentGroupId());
                    if (connectionDTOs == null) {
                        connectionDTOs = new HashSet<>();
                    }
                    Set<ConnectionDTO>
                        versionedConnections =
                        connectionDTOs.stream().filter(connectionDTO -> connectionDTO.getDestination().getGroupId().equalsIgnoreCase(processGroupDTO.getId()) || connectionDTO.getSource().getGroupId()
                            .equalsIgnoreCase(processGroupDTO.getId()))
                            .collect(Collectors.toSet());
                    restClient.deleteProcessGroupAndConnections(processGroupDTO, versionedConnections);
                    log.info("removed the versioned processgroup {} ", processGroupDTO.getName());
                } else {
                    log.info("Unable to remove the versioned processgroup {} ", processGroupDTO.getName());
                }
            } catch (Exception e) {
                log.error("Unable to remove the versioned processgroup {} ", processGroupDTO.getName(), e);
            }
        }
    }


    /**
     * Updates a process groups properties
     */
    private void updateProcessGroupProperties(String processGroupId, String processGroupName) throws NflowCreationException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<NifiProperty> propertiesToUpdate = restClient.getPropertiesForProcessGroup(processGroupId);
        stopwatch.stop();
        log.debug("Time to get Properties in Nflow updateProcessGroupProperties: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        stopwatch.reset();
        stopwatch.start();
        //get the Root processGroup
        ProcessGroupDTO rootProcessGroup = niFiObjectCache.getRootProcessGroup();
        stopwatch.stop();
        log.debug("Time to get root Process Group in updateProcessGroupProperties: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.reset();

        stopwatch.start();
        modifiedProperties = new ArrayList<>();
        //resolve the static properties
        //first fill in any properties with static references
        List<NifiProperty> modifiedStaticProperties = propertyExpressionResolver.resolveStaticProperties(propertiesToUpdate);
        // now apply any of the incoming metadata properties to this

        List<NifiProperty> modifiedNflowMetadataProperties = NifiPropertyUtil.matchAndSetPropertyValues(rootProcessGroup.getName(),
                                                                                                       processGroupName,
                                                                                                       propertiesToUpdate, properties);
        modifiedProperties.addAll(modifiedStaticProperties);
        modifiedProperties.addAll(modifiedNflowMetadataProperties);

        stopwatch.stop();
        log.debug("Time to set modifiedProperties: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.reset();

        stopwatch.start();
        restClient.updateProcessGroupProperties(modifiedProperties);
        stopwatch.stop();
        log.debug("Time to update properties in the process group: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));


    }


    private void markInputAsRunning(NifiProcessGroup newProcessGroup, ProcessorDTO input) {
        setInputProcessorState(newProcessGroup, input, NifiProcessUtil.PROCESS_STATE.RUNNING);
    }

    private void markInputAsStopped(NifiProcessGroup newProcessGroup, ProcessorDTO input) {
        setInputProcessorState(newProcessGroup, input, NifiProcessUtil.PROCESS_STATE.STOPPED);
    }

    private void setInputProcessorState(NifiProcessGroup newProcessGroup, ProcessorDTO input, NifiProcessUtil.PROCESS_STATE state) {

        setInputProcessorState(newProcessGroup.getProcessGroupEntity(),
                               input, state);
    }

    /**
     * Sets the First processors in the {@code processGroup} matching the passed in {@code input} ProcessorType to the passed in {@code state}
     * If the input ins null it will use the default {@code inputType} supplied from the builder
     *
     * @param processGroup the group which should be inspected for the input processors
     * @param input        the processor type to match when finding the correct input
     * @param state        the state to set the matched input processor
     */
    private void setInputProcessorState(ProcessGroupDTO processGroup, ProcessorDTO input, NifiProcessUtil.PROCESS_STATE state) {
        try {
            if (input != null && (StringUtils.isBlank(inputProcessorType) || !inputProcessorType.equalsIgnoreCase(input.getType()))) {
                inputProcessorType = input.getType();
            }

            restClient.setInputProcessorState(processGroup.getId(),
                                              inputProcessorType, state);
        } catch (Exception error) {
            String
                errorMsg =
                "Unable to mark group as " + state + " for " + input.getName() + "("
                + inputProcessorType + ").";
            newProcessGroup
                .addError(newProcessGroup.getProcessGroupEntity().getId(), input.getId(), NifiError.SEVERITY.WARN,
                          errorMsg, "Process State");
            newProcessGroup.setSuccess(false);
        }
    }


    private void updateNflowSchedule(NifiProcessGroup newProcessGroup, ProcessorDTO input) {
        if (nflowSchedule != null && input != null) {
            String strategy = nflowSchedule.getSchedulingStrategy();
            String schedule = nflowSchedule.getSchedulingPeriod();
            //if the input is of type TriggerNflow then make the schedule for that processor Timer Driven in the flow
            if (inputProcessorType.equalsIgnoreCase(NifiNflowConstants.TRIGGER_NFLOW_PROCESSOR_CLASS)) {
                strategy = NifiNflowConstants.SCHEDULE_STRATEGIES.TIMER_DRIVEN.name();
                schedule = NifiNflowConstants.DEFAULT_TIGGER_NFLOW_PROCESSOR_SCHEDULE;
            }
            NifiProcessorSchedule scheduleCopy = new NifiProcessorSchedule(nflowSchedule);
            scheduleCopy.setProcessorId(input.getId());
            scheduleCopy.setSchedulingPeriod(schedule);
            scheduleCopy.setSchedulingStrategy(strategy);
            try {
                restClient.getNiFiRestClient().processors().schedule(scheduleCopy);
            } catch (Exception e) {
                String
                    errorMsg =
                    "Unable set Scheduling Information for nflow " + input.getName() + " on " + input.getType() + ". Please check to make sure you set the Timer or Cron Expression correctly";
                newProcessGroup.addError(input.getParentGroupId(), input.getId(), NifiError.SEVERITY.WARN, errorMsg, "Schedule");
                newProcessGroup.setSuccess(false);
            }
        }
    }
}



