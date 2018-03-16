package com.onescorpin.nflowmgr.service;

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

import com.onescorpin.datalake.authorization.service.HadoopAuthorizationService;
import com.onescorpin.nflowmgr.InvalidOperationException;
import com.onescorpin.nflowmgr.rest.model.EntityVersion;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NflowVersions;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.UINflow;
import com.onescorpin.nflowmgr.rest.model.UserFieldCollection;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.category.NflowManagerCategoryService;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerNflowService;
import com.onescorpin.nflowmgr.service.nflow.NflowModelTransform;
import com.onescorpin.nflowmgr.service.template.NflowManagerTemplateService;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementService;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.CleanupTriggerEvent;
import com.onescorpin.metadata.api.event.nflow.NflowOperationStatusEvent;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.nifi.rest.client.NiFiComponentState;
import com.onescorpin.nifi.rest.client.NiFiRestClient;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.nifi.rest.support.NifiProcessUtil;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.Action;

import org.apache.nifi.web.api.dto.ConnectionDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

/**
 * Provides access to category, nflow, and template metadata stored in the metadata store.
 */
public class NflowManagerMetadataService implements MetadataService {

    private static final Logger log = LoggerFactory.getLogger(NflowManagerMetadataService.class);

    @Value("${nova.nflow.mgr.cleanup.timeout:60000}")
    private long cleanupTimeout;

    @Value("${nova.nflow.mgr.cleanup.delay:300}")
    private long cleanupDelay;

    @Inject
    NflowManagerCategoryService categoryProvider;

    @Inject
    NflowManagerTemplateService templateProvider;

    @Inject
    NflowManagerNflowService nflowProvider;

    @Inject
    LegacyNifiRestClient nifiRestClient;

    @Inject
    MetadataAccess metadataAccess;

    @Inject
    NflowModelTransform nflowModelTransform;

    @Inject
    private AccessController accessController;

    // Metadata event service
    @Inject
    private MetadataEventService eventService;

    // I had to use autowired instead of Inject to allow null values.
    @Autowired(required = false)
    @Qualifier("hadoopAuthorizationService")
    private HadoopAuthorizationService hadoopAuthorizationService;

    /**
     * NiFi REST client
     */
    @Inject
    private NiFiRestClient nifiClient;

    @Inject
    ServiceLevelAgreementService serviceLevelAgreementService;

    @Override
    public boolean checkNflowPermission(String id, Action action, Action... more) {
        return nflowProvider.checkNflowPermission(id, action, more);

    }

    @Override
    public RegisteredTemplate registerTemplate(RegisteredTemplate registeredTemplate) {
        return templateProvider.registerTemplate(registeredTemplate);
    }

    @Override
    public List<NifiProperty> getTemplateProperties(String templateId) {
        return templateProvider.getTemplateProperties(templateId);
    }

    public void deleteRegisteredTemplate(String templateId) {
        templateProvider.deleteRegisteredTemplate(templateId);
    }

    @Override
    public List<RegisteredTemplate> getRegisteredTemplates() {
        return templateProvider.getRegisteredTemplates();
    }


    @Override
    public RegisteredTemplate findRegisteredTemplateByName(String templateName) {
        return templateProvider.findRegisteredTemplateByName(templateName);
    }

    @Override
    public NifiNflow createNflow(NflowMetadata nflowMetadata) {
        NifiNflow nflow = nflowProvider.createNflow(nflowMetadata);
        if (nflow.isSuccess()) {
            if (nflow.isEnableAfterSave()) {
                enableNflow(nflow.getNflowMetadata().getId());
                //validate its enabled
                ProcessorDTO processorDTO = nflow.getNflowProcessGroup().getInputProcessor();
               Optional<ProcessorDTO> updatedProcessor = nifiRestClient.getNiFiRestClient().processors().findById(processorDTO.getParentGroupId(),processorDTO.getId());
               if(updatedProcessor.isPresent()){
                   if(!NifiProcessUtil.PROCESS_STATE.RUNNING.name().equalsIgnoreCase(updatedProcessor.get().getState())){
                       nflow.setSuccess(false);
                       nflow.getNflowProcessGroup().setInputProcessor(updatedProcessor.get());
                       nflow.getNflowProcessGroup().validateInputProcessor();
                       if(nflowMetadata.isNew() && nflow.getNflowMetadata().getId() != null){
                           //delete it
                           deleteNflow(nflow.getNflowMetadata().getId());
                       }
                   }
               }
            }
            //requery to get the latest version
            NflowMetadata updatedNflow = getNflowById(nflow.getNflowMetadata().getId());
            nflow.setNflowMetadata(updatedNflow);
        }
        return nflow;

    }


    @Override
    public void deleteNflow(@Nonnull final String nflowId) {
        // First check if this should be allowed.
        accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ADMIN_NFLOWS);
        nflowProvider.checkNflowPermission(nflowId, NflowAccessControl.DELETE);

        // Step 1: Fetch nflow metadata
        final NflowMetadata nflow = nflowProvider.getNflowById(nflowId);
        if (nflow == null) {
            throw new IllegalArgumentException("Unknown nflow: " + nflowId);
        }

        // Step 2: Check category permissions
        categoryProvider.checkCategoryPermission(nflow.getCategoryId(), CategoryAccessControl.CREATE_NFLOW);

        // Step 3: Check for dependent nflows
        if (nflow.getUsedByNflows() != null && !nflow.getUsedByNflows().isEmpty()) {
            final List<String> systemNames = nflow.getUsedByNflows().stream().map(NflowSummary::getCategoryAndNflowSystemName).collect(Collectors.toList());
            throw new IllegalStateException("Nflow is referenced by " + nflow.getUsedByNflows().size() + " other nflows: " + systemNames);
        }

        //check SLAs
        metadataAccess.read(() -> {
        boolean hasSlas = serviceLevelAgreementService.hasServiceLevelAgreements(nflowProvider.resolveNflow(nflowId));
        if(hasSlas) {
            log.error("Unable to delete "+nflow.getCategoryAndNflowDisplayName()+".  1 or more SLAs exist for this nflow. ");
            throw new IllegalStateException("Unable to delete the nflow. 1 or more Service Level agreements exist for this nflow " + nflow.getCategoryAndNflowDisplayName() + ".  Please delete the SLA's, or remove the nflow from the SLA's and try again.");
        }
        },MetadataAccess.SERVICE);

        // Step 4: Delete hadoop authorization security policies if they exists
        if (hadoopAuthorizationService != null) {
            metadataAccess.read(() -> {
                Nflow domainNflow = nflowModelTransform.nflowToDomain(nflow);
                String hdfsPaths = (String) domainNflow.getProperties().get(HadoopAuthorizationService.REGISTRATION_HDFS_FOLDERS);

                hadoopAuthorizationService.deleteHivePolicy(nflow.getSystemCategoryName(), nflow.getSystemNflowName());
                hadoopAuthorizationService.deleteHdfsPolicy(nflow.getSystemCategoryName(), nflow.getSystemNflowName(), HadoopAuthorizationService.convertNewlineDelimetedTextToList(hdfsPaths));
            });

        }

        // Step 5: Enable NiFi cleanup flow
        boolean needsCleanup = false;
        final ProcessGroupDTO nflowProcessGroup;
        final ProcessGroupDTO categoryProcessGroup = nifiRestClient.getProcessGroupByName("root", nflow.getSystemCategoryName(), false, true);

        if (categoryProcessGroup != null) {
            nflowProcessGroup = NifiProcessUtil.findFirstProcessGroupByName(categoryProcessGroup.getContents().getProcessGroups(), nflow.getSystemNflowName());
            if (nflowProcessGroup != null) {
                needsCleanup = nifiRestClient.setInputAsRunningByProcessorMatchingType(nflowProcessGroup.getId(), "com.onescorpin.nifi.v2.metadata.TriggerCleanup");
            }
        }

        // Step 6: Run NiFi cleanup flow
        if (needsCleanup) {
            // Wait for input processor to start
            try {
                Thread.sleep(cleanupDelay);
            } catch (InterruptedException e) {
                // ignored
            }

            cleanupNflow(nflow);
        }

        // Step 7: Remove nflow from NiFi
        if (categoryProcessGroup != null) {
            final Set<ConnectionDTO> connections = categoryProcessGroup.getContents().getConnections();
            for (ProcessGroupDTO processGroup : NifiProcessUtil.findProcessGroupsByNflowName(categoryProcessGroup.getContents().getProcessGroups(), nflow.getSystemNflowName())) {
                nifiRestClient.deleteProcessGroupAndConnections(processGroup, connections);
            }
        }

        // Step 8: Delete database entries
        nflowProvider.deleteNflow(nflowId);

    }

    /**
     * Changes the state of the specified nflow.
     *
     * @param nflowSummary the nflow
     * @param state       the new state
     * @return {@code true} if the nflow is in the new state, or {@code false} otherwise
     */
    private boolean updateNifiNflowRunningStatus(NflowSummary nflowSummary, Nflow.State state) {
        // Validate parameters
        if (nflowSummary == null || !nflowSummary.getState().equals(state.name())) {
            return false;
        }

        // Find the process group
        final Optional<ProcessGroupDTO> categoryGroup = nifiClient.processGroups().findByName("root", nflowSummary.getSystemCategoryName(), false, false);
        final Optional<ProcessGroupDTO> nflowGroup = categoryGroup.flatMap(group -> nifiClient.processGroups().findByName(group.getId(), nflowSummary.getSystemNflowName(), false, true));
        if (!nflowGroup.isPresent()) {
            log.warn("NiFi process group missing for nflow: {}.{}", nflowSummary.getSystemCategoryName(), nflowSummary.getSystemNflowName());
            return Nflow.State.DISABLED.equals(state);
        }

        // Update the state
        if (state.equals(Nflow.State.ENABLED)) {
            nifiClient.processGroups().schedule(nflowGroup.get().getId(), categoryGroup.get().getId(), NiFiComponentState.RUNNING);
        } else if (state.equals(Nflow.State.DISABLED)) {
            nifiRestClient.stopInputs(nflowGroup.get());
        }

        return true;
    }

    public NflowSummary enableNflow(String nflowId) {
        return metadataAccess.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            NflowMetadata nflowMetadata = nflowProvider.getNflowById(nflowId);

            if (nflowMetadata == null) {
                //nflow will not be found when user is allowed to export nflows but has no entity access to nflow with nflow id
                throw new NotFoundException("Nflow not found for id " + nflowId);
            }

            if (!nflowMetadata.getState().equals(Nflow.State.ENABLED.name())) {
                NflowSummary nflowSummary = nflowProvider.enableNflow(nflowId);

                boolean updatedNifi = updateNifiNflowRunningStatus(nflowSummary, Nflow.State.ENABLED);
                if (!updatedNifi) {
                    //rollback
                    throw new RuntimeException("Unable to enable Nflow " + nflowId);
                }
                return nflowSummary;
            }
            return new NflowSummary(nflowMetadata);
        });

    }

    public NflowSummary disableNflow(final String nflowId) {
        return metadataAccess.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            NflowMetadata nflowMetadata = nflowProvider.getNflowById(nflowId);

            if (nflowMetadata == null) {
                throw new NotFoundException("Nflow not found for id " + nflowId);
            }

            if (!nflowMetadata.getState().equals(Nflow.State.DISABLED.name())) {
                NflowSummary nflowSummary = nflowProvider.disableNflow(nflowId);
                boolean updatedNifi = updateNifiNflowRunningStatus(nflowSummary, Nflow.State.DISABLED);
                if (!updatedNifi) {
                    //rollback
                    throw new RuntimeException("Unable to disable Nflow " + nflowId);
                }
                return nflowSummary;
            }
            return new NflowSummary(nflowMetadata);
        });
    }

    @Override
    public Collection<NflowMetadata> getNflows() {
        return nflowProvider.getNflows();
    }

    @Override
    public Page<UINflow> getNflowsPage(boolean verbose, Pageable pageable, String filter) {
        return nflowProvider.getNflows(verbose, pageable, filter);
    }

    @Override
    public Collection<? extends UINflow> getNflows(boolean verbose) {
        return nflowProvider.getNflows(verbose);
    }

    @Override
    public List<NflowSummary> getNflowSummaryData() {
        return nflowProvider.getNflowSummaryData();
    }

    @Override
    public List<NflowSummary> getNflowSummaryForCategory(String categoryId) {
        return nflowProvider.getNflowSummaryForCategory(categoryId);
    }

    @Override
    public NflowMetadata getNflowByName(String categoryName, String nflowName) {
        return nflowProvider.getNflowByName(categoryName, nflowName);
    }

    @Override
    public NflowMetadata getNflowById(String nflowId) {
        return nflowProvider.getNflowById(nflowId);
    }

    @Override
    public NflowMetadata getNflowById(String nflowId, boolean refreshTargetTableSchema) {
        return nflowProvider.getNflowById(nflowId, refreshTargetTableSchema);
    }

    @Override
    public Collection<NflowCategory> getCategories() {
        return categoryProvider.getCategories();
    }

    @Override
    public Collection<NflowCategory> getCategories(boolean includeNflowDetails) {
        return categoryProvider.getCategories(includeNflowDetails);
    }

    @Override
    public NflowCategory getCategoryBySystemName(String name) {
        return categoryProvider.getCategoryBySystemName(name);
    }

    @Override
    public NflowCategory getCategoryById(String categoryId) {
        return categoryProvider.getCategoryById(categoryId);
    }

    @Override
    public void saveCategory(NflowCategory category) {
        categoryProvider.saveCategory(category);
    }

    @Override
    public boolean deleteCategory(String categoryId) throws InvalidOperationException {
        return categoryProvider.deleteCategory(categoryId);
    }

    /**
     * Runs the cleanup flow for the specified nflow.
     *
     * @param nflow the nflow to be cleaned up
     * @throws NflowCleanupFailedException  if the cleanup flow was started but failed to complete successfully
     * @throws NflowCleanupTimeoutException if the cleanup flow was started but failed to complete in the allotted time
     * @throws RuntimeException            if the cleanup flow could not be started
     */
    private void cleanupNflow(@Nonnull final NflowMetadata nflow) {
        // Create event listener
        final NflowCompletionListener listener = new NflowCompletionListener(nflow, Thread.currentThread());
        eventService.addListener(listener);

        try {
            // Trigger cleanup
            nflowProvider.enableNflowCleanup(nflow.getId());
            eventService.notify(new CleanupTriggerEvent(nflowProvider.resolveNflow(nflow.getId())));

            // Wait for completion
            long remaining = cleanupTimeout;
            while (remaining > 0 && (listener.getState() == null || listener.getState() == NflowOperation.State.STARTED)) {
                final long start = System.currentTimeMillis();
                try {
                    Thread.sleep(remaining);
                } catch (InterruptedException e) {
                    // ignored
                }
                remaining -= System.currentTimeMillis() - start;
            }
        } finally {
            eventService.removeListener(listener);
        }

        // Check result
        if (listener.getState() == null || listener.getState() == NflowOperation.State.STARTED) {
            throw new NflowCleanupTimeoutException("Cleanup timed out for nflow: " + nflow.getId());
        }
        if (listener.getState() != NflowOperation.State.SUCCESS) {
            throw new NflowCleanupFailedException("Cleanup state " + listener.getState() + " for nflow: " + nflow.getId());
        }
    }

    @Nonnull
    @Override
    public Set<UserProperty> getCategoryUserFields() {
        return categoryProvider.getUserProperties();
    }

    @Nonnull
    @Override
    public Optional<Set<UserProperty>> getNflowUserFields(@Nonnull final String categoryId) {
        return nflowProvider.getUserFields(categoryId);
    }
    
    @Nonnull
    @Override
    public NflowVersions getNflowVersions(String nflowId, boolean includeNflows) {
        return nflowProvider.getNflowVersions(nflowId, includeNflows);
    }
    
    public Optional<EntityVersion> getNflowVersion(String nflowId, String versionId, boolean includeContent) {
        return nflowProvider.getNflowVersion(nflowId, versionId, includeContent);
    }


    @Nonnull
    @Override
    public UserFieldCollection getUserFields() {
        final UserFieldCollection collection = new UserFieldCollection();
        collection.setCategoryFields(categoryProvider.getUserFields());
        collection.setNflowFields(nflowProvider.getUserFields());
        return collection;
    }

    @Override
    public void setUserFields(@Nonnull final UserFieldCollection userFields) {
        categoryProvider.setUserFields(userFields.getCategoryFields());
        nflowProvider.setUserFields(userFields.getNflowFields());
    }

    /**
     * Listens for a nflow completion then interrupts a target thread.
     */
    private static class NflowCompletionListener implements MetadataEventListener<NflowOperationStatusEvent> {

        /**
         * Name of the nflow to watch for
         */
        @Nonnull
        private final String nflowName;
        /**
         * Thread to interrupt
         */
        @Nonnull
        private final Thread target;
        /**
         * Current state of the nflow
         */
        @Nullable
        private NflowOperation.State state;

        /**
         * Constructs a {@code NflowCompletionListener} that listens for events for the specified nflow then interrupts the specified thread.
         *
         * @param nflow   the nflow to watch far
         * @param target the thread to interrupt
         */
        NflowCompletionListener(@Nonnull final NflowMetadata nflow, @Nonnull final Thread target) {
            this.nflowName = nflow.getCategoryAndNflowName();
            this.target = target;
        }

        /**
         * Gets the current state of the nflow.
         *
         * @return the nflow state
         */
        @Nullable
        public NflowOperation.State getState() {
            return state;
        }

        @Override
        public void notify(@Nonnull final NflowOperationStatusEvent event) {
            if (event.getData().getNflowName().equals(nflowName)) {
                state = event.getData().getState();
                target.interrupt();
            }
        }

    }

    /**
     * Update a given nflows datasources clearing its sources/destinations before revaluating the data
     * @param nflowId of the nflow rest model to update
     */
    public void updateNflowDatasources(String nflowId) {
        nflowProvider.updateNflowDatasources(nflowId);
    }

    /**
     * Iterate all of the nflows, clear all sources/destinations and reassign
     * Note this will be an expensive call if you have a lot of nflows
     */
    public void updateAllNflowsDatasources(){
        nflowProvider.updateAllNflowsDatasources();
    }
}
