package com.onescorpin.nflowmgr.service.nflow;

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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.onescorpin.datalake.authorization.service.HadoopAuthorizationService;
import com.onescorpin.nflowmgr.nifi.CreateNflowBuilder;
import com.onescorpin.nflowmgr.nifi.PropertyExpressionResolver;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCache;
import com.onescorpin.nflowmgr.rest.model.EntityVersion;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NflowVersions;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplateRequest;
import com.onescorpin.nflowmgr.rest.model.ReusableTemplateConnectionInfo;
import com.onescorpin.nflowmgr.rest.model.UINflow;
import com.onescorpin.nflowmgr.rest.model.UserField;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.UserPropertyTransform;
import com.onescorpin.nflowmgr.service.nflow.datasource.DerivedDatasourceFactory;
import com.onescorpin.nflowmgr.service.security.SecurityService;
import com.onescorpin.nflowmgr.service.template.NflowManagerTemplateService;
import com.onescorpin.nflowmgr.service.template.NiFiTemplateCache;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateService;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementService;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.datasource.DerivedDatasource;
import com.onescorpin.metadata.api.event.MetadataChange;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowChange;
import com.onescorpin.metadata.api.event.nflow.NflowChangeEvent;
import com.onescorpin.metadata.api.event.nflow.NflowPropertyChangeEvent;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowProperties;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.rest.model.sla.Obligation;
import com.onescorpin.metadata.sla.api.ObligationGroup;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.nifi.nflowmgr.NflowRollbackException;
import com.onescorpin.nifi.nflowmgr.InputOutputPort;
import com.onescorpin.nifi.rest.NiFiObjectCache;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.nifi.rest.model.NiFiPropertyDescriptorTransform;
import com.onescorpin.nifi.rest.model.NifiProcessGroup;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.nifi.rest.support.NifiPropertyUtil;
import com.onescorpin.policy.precondition.DependentNflowPrecondition;
import com.onescorpin.policy.precondition.Precondition;
import com.onescorpin.policy.precondition.transform.PreconditionPolicyTransformer;
import com.onescorpin.policy.rest.model.FieldRuleProperty;
import com.onescorpin.policy.rest.model.PreconditionRule;
import com.onescorpin.rest.model.LabelValue;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.Action;
import com.onescorpin.support.NflowNameUtil;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

public class DefaultNflowManagerNflowService implements NflowManagerNflowService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNflowManagerNflowService.class);

    private static final Pageable PAGE_ALL = new PageRequest(0, Integer.MAX_VALUE);

    /**
     * Event listener for precondition events
     */
    private final MetadataEventListener<NflowPropertyChangeEvent> nflowPropertyChangeListener = new NflowPropertyChangeDispatcher();

    @Inject
    NflowManagerTemplateProvider templateProvider;
    @Inject
    NflowManagerTemplateService templateRestProvider;
    @Inject
    NflowManagerPreconditionService nflowPreconditionModelTransform;
    @Inject
    NflowModelTransform nflowModelTransform;
    @Inject
    ServiceLevelAgreementProvider slaProvider;
    @Inject
    ServiceLevelAgreementService serviceLevelAgreementService;
    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;
    @Inject
    private DatasourceProvider datasourceProvider;
    /**
     * Metadata event service
     */
    @Inject
    private AccessController accessController;
    @Inject
    private MetadataEventService metadataEventService;
    @Inject
    private NiFiPropertyDescriptorTransform propertyDescriptorTransform;
    @Inject
    private DerivedDatasourceFactory derivedDatasourceFactory;
    // use autowired instead of Inject to allow null values.
    @Autowired(required = false)
    @Qualifier("hadoopAuthorizationService")
    private HadoopAuthorizationService hadoopAuthorizationService;

    @Inject
    private SecurityService securityService;

    @Inject
    protected CategoryProvider categoryProvider;

    @Inject
    protected NflowProvider nflowProvider;

    @Inject
    protected MetadataAccess metadataAccess;

    @Inject
    private NflowManagerTemplateService nflowManagerTemplateService;

    @Inject
    private RegisteredTemplateService registeredTemplateService;

    @Inject
    PropertyExpressionResolver propertyExpressionResolver;
    @Inject
    NifiFlowCache nifiFlowCache;

    @Inject
    private NiFiTemplateCache niFiTemplateCache;

    @Inject
    private LegacyNifiRestClient nifiRestClient;

    @Inject
    private NflowHiveTableService nflowHiveTableService;


    @Value("${nifi.remove.inactive.versioned.nflows:true}")
    private boolean removeInactiveNifiVersionedNflowFlows;

    @Value("${nifi.auto.align:true}")
    private boolean nifiAutoNflowsAlignAfterSave;

    @Inject
    private NiFiObjectCache niFiObjectCache;

    /**
     * Adds listeners for transferring events.
     */
    @PostConstruct
    public void addEventListener() {
        metadataEventService.addListener(nflowPropertyChangeListener);
    }

    /**
     * Removes listeners and stops transferring events.
     */
    @PreDestroy
    public void removeEventListener() {
        metadataEventService.removeListener(nflowPropertyChangeListener);
    }

    @Override
    public boolean checkNflowPermission(String id, Action action, Action... more) {
        if (accessController.isEntityAccessControlled()) {
            return metadataAccess.read(() -> {
                Nflow.ID domainId = nflowProvider.resolveId(id);
                Nflow domainNflow = nflowProvider.findById(domainId);

                if (domainNflow != null) {
                    domainNflow.getAllowedActions().checkPermission(action, more);
                    return true;
                } else {
                    return false;
                }
            });
        } else {
            return true;
        }
    }

    @Override
    public NflowMetadata getNflowByName(final String categoryName, final String nflowName) {
        NflowMetadata nflowMetadata = metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            Nflow domainNflow = nflowProvider.findBySystemName(categoryName, nflowName);
            if (domainNflow != null) {
                return nflowModelTransform.domainToNflowMetadata(domainNflow);
            }
            return null;
        });
        return nflowMetadata;
    }

    @Override
    public NflowMetadata getNflowById(final String id) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            return getNflowById(id, false);
        });

    }

    @Override
    public NflowMetadata getNflowById(final String id, final boolean refreshTargetTableSchema) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            NflowMetadata nflowMetadata = null;
            Nflow.ID domainId = nflowProvider.resolveId(id);
            Nflow domainNflow = nflowProvider.findById(domainId);
            if (domainNflow != null) {
                nflowMetadata = nflowModelTransform.domainToNflowMetadata(domainNflow);
            }
            if (refreshTargetTableSchema && nflowMetadata != null) {
                //commented out for now as some issues were found with nflows with TEXTFILE as their output
                //this will attempt to sync the schema stored in modeshape with that in Hive
                // nflowModelTransform.refreshTableSchemaFromHive(nflowMetadata);
            }
            return nflowMetadata;
        });

    }

    @Override
    public NflowVersions getNflowVersions(String nflowId, boolean includeContent) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            Nflow.ID domainId = nflowProvider.resolveId(nflowId);

            return nflowProvider.findVersions(domainId, includeContent)
                .map(list -> nflowModelTransform.domainToNflowVersions(list, domainId))
                .orElse((NflowVersions) null);
        });
    }

    @Override
    public Optional<EntityVersion> getNflowVersion(String nflowId, String versionId, boolean includeContent) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            Nflow.ID domainNflowId = nflowProvider.resolveId(nflowId);
            com.onescorpin.metadata.api.versioning.EntityVersion.ID domainVersionId = nflowProvider.resolveVersion(versionId);

            return nflowProvider.findVersion(domainNflowId, domainVersionId, includeContent)
                .map(version -> nflowModelTransform.domainToNflowVersion(version));
        });
    }

    @Override
    public Collection<NflowMetadata> getNflows() {
        return getNflows(PAGE_ALL, null).getContent();
    }

    public Page<NflowMetadata> getNflows(Pageable pageable, String filter) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            Page<Nflow> domainNflows = nflowProvider.findPage(pageable, filter);
            return domainNflows.map(d -> nflowModelTransform.domainToNflowMetadata(d));
        });

    }

    @Override
    public Collection<? extends UINflow> getNflows(boolean verbose) {
        if (verbose) {
            return getNflows();
        } else {
            return getNflowSummaryData();
        }

    }

    @Override
    public Page<UINflow> getNflows(boolean verbose, Pageable pageable, String filter) {
        if (verbose) {
            return getNflows(pageable, filter).map(UINflow.class::cast);
        } else {
            return getNflowSummaryData(pageable, filter).map(UINflow.class::cast);
        }

    }

    @Override
    public List<NflowSummary> getNflowSummaryData() {
        return getNflowSummaryData(PAGE_ALL, null).getContent().stream()
            .map(NflowSummary.class::cast)
            .collect(Collectors.toList());
    }

    public Page<NflowSummary> getNflowSummaryData(Pageable pageable, String filter) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            Page<Nflow> domainNflows = nflowProvider.findPage(pageable, filter);
            return domainNflows.map(d -> nflowModelTransform.domainToNflowSummary(d));
        });
    }

    @Override
    public List<NflowSummary> getNflowSummaryForCategory(final String categoryId) {
        return metadataAccess.read(() -> {
            List<NflowSummary> summaryList = new ArrayList<>();
            boolean hasPermission = this.accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);
            if (hasPermission) {

                Category.ID categoryDomainId = categoryProvider.resolveId(categoryId);
                List<? extends Nflow> domainNflows = nflowProvider.findByCategoryId(categoryDomainId);
                if (domainNflows != null && !domainNflows.isEmpty()) {
                    List<NflowMetadata> nflows = nflowModelTransform.domainToNflowMetadata(domainNflows);
                    for (NflowMetadata nflow : nflows) {
                        summaryList.add(new NflowSummary(nflow));
                    }
                }
            }
            return summaryList;
        });

    }

    @Override
    public List<NflowMetadata> getNflowsWithTemplate(final String registeredTemplateId) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            List<NflowMetadata> nflowMetadatas = null;
            NflowManagerTemplate.ID templateDomainId = templateProvider.resolveId(registeredTemplateId);
            List<? extends Nflow> domainNflows = nflowProvider.findByTemplateId(templateDomainId);
            if (domainNflows != null) {
                nflowMetadatas = nflowModelTransform.domainToNflowMetadata(domainNflows);
            }
            return nflowMetadatas;
        });
    }


    @Override
    public Nflow.ID resolveNflow(@Nonnull Serializable fid) {
        return metadataAccess.read(() -> nflowProvider.resolveNflow(fid));
    }


    /**
     * Create/Update a Nflow in NiFi. Save the metadata to Nova meta store.
     *
     * @param nflowMetadata the nflow metadata
     * @return an object indicating if the nflow creation was successful or not
     */
    public NifiNflow createNflow(final NflowMetadata nflowMetadata) {

        //functional access to be able to create a nflow
        this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

        if (nflowMetadata.getState() == null) {
            if (nflowMetadata.isActive()) {
                nflowMetadata.setState(Nflow.State.ENABLED.name());
            } else {
                nflowMetadata.setState(Nflow.State.DISABLED.name());
            }
        }

        if (StringUtils.isBlank(nflowMetadata.getId())) {
            nflowMetadata.setIsNew(true);
        }

        //Read all the nflows as System Service account to ensure the nflow name is unique
        if (nflowMetadata.isNew()) {
            metadataAccess.read(() -> {
                Nflow existing = nflowProvider.findBySystemName(nflowMetadata.getCategory().getSystemName(), nflowMetadata.getSystemNflowName());
                if (existing != null) {
                    throw new DuplicateNflowNameException(nflowMetadata.getCategoryName(), nflowMetadata.getNflowName());
                }
            }, MetadataAccess.SERVICE);
        }

        NifiNflow nflow = createAndSaveNflow(nflowMetadata);
        //register the audit for the update event
        if (nflow.isSuccess() && !nflowMetadata.isNew()) {
            Nflow.State state = Nflow.State.valueOf(nflowMetadata.getState());
            Nflow.ID id = nflowProvider.resolveId(nflowMetadata.getId());
            notifyNflowStateChange(nflowMetadata, id, state, MetadataChange.ChangeType.UPDATE);
        } else if (nflow.isSuccess() && nflowMetadata.isNew()) {
            //update the access control
            nflowMetadata.toRoleMembershipChangeList().stream().forEach(roleMembershipChange -> securityService.changeNflowRoleMemberships(nflow.getNflowMetadata().getId(), roleMembershipChange));
        }
        return nflow;

    }


    /**
     * Create/Update a Nflow in NiFi. Save the metadata to Nova meta store.
     *
     * @param nflowMetadata the nflow metadata
     * @return an object indicating if the nflow creation was successful or not
     */
    private NifiNflow createAndSaveNflow(NflowMetadata nflowMetadata) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        NifiNflow nflow = null;
        if (StringUtils.isBlank(nflowMetadata.getId())) {
            nflowMetadata.setIsNew(true);

            //If the nflow is New we need to ensure the user has CREATE_NFLOW entity permission
            if (accessController.isEntityAccessControlled()) {
                metadataAccess.read(() -> {
                    //ensure the user has rights to create nflows under the category
                    Category domainCategory = categoryProvider.findById(categoryProvider.resolveId(nflowMetadata.getCategory().getId()));
                    if (domainCategory == null) {
                        //throw exception
                        throw new MetadataRepositoryException("Unable to find the category " + nflowMetadata.getCategory().getSystemName());
                    }
                    domainCategory.getAllowedActions().checkPermission(CategoryAccessControl.CREATE_NFLOW);

                    //ensure the user has rights to create nflows using the template
                    NflowManagerTemplate domainTemplate = templateProvider.findById(templateProvider.resolveId(nflowMetadata.getTemplateId()));
                    if (domainTemplate == null) {
                        throw new MetadataRepositoryException("Unable to find the template " + nflowMetadata.getTemplateId());
                    }
                    //  domainTemplate.getAllowedActions().checkPermission(TemplateAccessControl.CREATE_NFLOW);
                });
            }

        } else if (accessController.isEntityAccessControlled()) {
            metadataAccess.read(() -> {
                //perform explict entity access check here as we dont want to modify the NiFi flow unless user has access to edit the nflow
                Nflow.ID domainId = nflowProvider.resolveId(nflowMetadata.getId());
                Nflow domainNflow = nflowProvider.findById(domainId);
                if (domainNflow != null) {
                    domainNflow.getAllowedActions().checkPermission(NflowAccessControl.EDIT_DETAILS);
                } else {
                    throw new NotFoundException("Nflow not found for id " + nflowMetadata.getId());
                }
            });
        }

        //replace expressions with values
        if (nflowMetadata.getTable() != null) {
            nflowMetadata.getTable().updateMetadataFieldValues();
        }

        if (nflowMetadata.getProperties() == null) {
            nflowMetadata.setProperties(new ArrayList<NifiProperty>());
        }

        //store ref to the originalNflowProperties before resolving and merging with the template
        List<NifiProperty> originalNflowProperties = nflowMetadata.getProperties();

        //get all the properties for the metadata
        RegisteredTemplate
            registeredTemplate =
            registeredTemplateService.findRegisteredTemplate(
                new RegisteredTemplateRequest.Builder().templateId(nflowMetadata.getTemplateId()).templateName(nflowMetadata.getTemplateName()).isNflowEdit(true).includeSensitiveProperties(true)
                    .build());

        //copy the registered template properties it a new list so it doest get updated
        List<NifiProperty> templateProperties =registeredTemplate.getProperties().stream().map(nifiProperty -> new NifiProperty(nifiProperty)).collect(Collectors.toList());
        //update the template properties with the nflowMetadata properties
        List<NifiProperty> matchedProperties =
            NifiPropertyUtil
                .matchAndSetPropertyByProcessorName(templateProperties, nflowMetadata.getProperties(), NifiPropertyUtil.PROPERTY_MATCH_AND_UPDATE_MODE.UPDATE_ALL_PROPERTIES);

        registeredTemplate.setProperties(templateProperties);
        nflowMetadata.setProperties(registeredTemplate.getProperties());
        nflowMetadata.setRegisteredTemplate(registeredTemplate);

        //resolve any ${metadata.} properties
        List<NifiProperty> resolvedProperties = propertyExpressionResolver.resolvePropertyExpressions(nflowMetadata);

        //decrypt the metadata
        nflowModelTransform.decryptSensitivePropertyValues(nflowMetadata);

        NflowMetadata.STATE state = NflowMetadata.STATE.NEW;
        try {
            state = NflowMetadata.STATE.valueOf(nflowMetadata.getState());
        } catch (Exception e) {
            //if the string isnt valid, disregard as it will end up disabling the nflow.
        }

        boolean enabled = (NflowMetadata.STATE.NEW.equals(state) && nflowMetadata.isActive()) || NflowMetadata.STATE.ENABLED.equals(state);

        // flag to indicate to enable the nflow later
        //if this is the first time for this nflow and it is set to be enabled, mark it to be enabled after we commit to the JCR store
        boolean enableLater = false;
        if (enabled && nflowMetadata.isNew()) {
            enableLater = true;
            enabled = false;
            nflowMetadata.setState(NflowMetadata.STATE.DISABLED.name());
        }

        CreateNflowBuilder
            nflowBuilder =
            CreateNflowBuilder
                .newNflow(nifiRestClient, nifiFlowCache, nflowMetadata, registeredTemplate.getNifiTemplateId(), propertyExpressionResolver, propertyDescriptorTransform, niFiObjectCache)
                .enabled(enabled)
                .removeInactiveVersionedProcessGroup(removeInactiveNifiVersionedNflowFlows)
                .autoAlign(nifiAutoNflowsAlignAfterSave)
                .withNiFiTemplateCache(niFiTemplateCache);

        if (registeredTemplate.isReusableTemplate()) {
            nflowBuilder.setReusableTemplate(true);
            nflowMetadata.setIsReusableNflow(true);
        } else {
            nflowBuilder.inputProcessorType(nflowMetadata.getInputProcessorType())
                .nflowSchedule(nflowMetadata.getSchedule()).properties(nflowMetadata.getProperties());
            if (registeredTemplate.usesReusableTemplate()) {
                for (ReusableTemplateConnectionInfo connection : registeredTemplate.getReusableTemplateConnections()) {
                    nflowBuilder.addInputOutputPort(new InputOutputPort(connection.getReusableTemplateInputPortName(), connection.getNflowOutputPortName()));
                }
            }
        }
        stopwatch.stop();
        log.debug("Time to prepare data for saving nflow in NiFi: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.reset();
        stopwatch.start();
        NifiProcessGroup
            entity = nflowBuilder.build();

        stopwatch.stop();
        log.debug("Time to save nflow in NiFi: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.reset();

        nflow = new NifiNflow(nflowMetadata, entity);

        //set the original nflowProperties back to the nflow
        nflowMetadata.setProperties(originalNflowProperties);
        //encrypt the metadata properties
        nflowModelTransform.encryptSensitivePropertyValues(nflowMetadata);

        if (entity.isSuccess()) {
            nflowMetadata.setNifiProcessGroupId(entity.getProcessGroupEntity().getId());

            try {
                stopwatch.start();
                saveNflow(nflowMetadata);
                nflow.setEnableAfterSave(enableLater);
                nflow.setSuccess(true);
                stopwatch.stop();
                log.debug("Time to saveNflow in Nova: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
                stopwatch.start();
                nflowBuilder.checkAndRemoveVersionedProcessGroup();

            } catch (Exception e) {
                nflow.setSuccess(false);
                nflow.addErrorMessage(e);
            }

        } else {
            nflow.setSuccess(false);
        }
        if (!nflow.isSuccess()) {
            if (!entity.isRolledBack()) {
                try {
                    nflowBuilder.rollback();
                } catch (NflowRollbackException rollbackException) {
                    log.error("Error rolling back nflow {}. {} ", nflowMetadata.getCategoryAndNflowName(), rollbackException.getMessage());
                    nflow.addErrorMessage("Error occurred in rolling back the Nflow.");
                }
                entity.setRolledBack(true);
            }
        }
        return nflow;
    }


    private void saveNflow(final NflowMetadata nflow) {


        metadataAccess.commit(() -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            List<? extends HadoopSecurityGroup> previousSavedSecurityGroups = null;
            // Store the old security groups before saving because we need to compare afterward
            if (!nflow.isNew()) {
                Nflow previousStateBeforeSaving = nflowProvider.findById(nflowProvider.resolveId(nflow.getId()));
                Map<String, String> userProperties = previousStateBeforeSaving.getUserProperties();
                previousSavedSecurityGroups = previousStateBeforeSaving.getSecurityGroups();
            }

            //if this is the first time saving this nflow create a new one
            Nflow domainNflow = nflowModelTransform.nflowToDomain(nflow);

            if (domainNflow.getState() == null) {
                domainNflow.setState(Nflow.State.ENABLED);
            }
            stopwatch.stop();
            log.debug("Time to transform the nflow to a domain object for saving: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();

            //initially save the nflow
            if (nflow.isNew()) {
                stopwatch.start();
                domainNflow = nflowProvider.update(domainNflow);
                stopwatch.stop();
                log.debug("Time to save the New nflow: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
            }

            final String domainId = domainNflow.getId().toString();
            final String nflowName = NflowNameUtil.fullName(domainNflow.getCategory().getSystemName(), domainNflow.getName());

            // Build preconditions
            stopwatch.start();
            assignNflowDependencies(nflow, domainNflow);
            stopwatch.stop();
            log.debug("Time to assignNflowDependencies: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();

            //Assign the datasources
            stopwatch.start();
            assignNflowDatasources(nflow, domainNflow);
            stopwatch.stop();
            log.debug("Time to assignNflowDatasources: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();

            stopwatch.start();
            boolean isStream = nflow.getRegisteredTemplate() != null ? nflow.getRegisteredTemplate().isStream() : false;
            Long timeBetweenBatchJobs = nflow.getRegisteredTemplate() != null ? nflow.getRegisteredTemplate().getTimeBetweenStartingBatchJobs() : 0L;
            //sync the nflow information to ops manager
            metadataAccess.commit(() -> opsManagerNflowProvider.save(opsManagerNflowProvider.resolveId(domainId), nflowName, isStream, timeBetweenBatchJobs));

            stopwatch.stop();
            log.debug("Time to sync nflow data with Operations Manager: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();

            // Update hadoop security group polices if the groups changed
            if (!nflow.isNew() && !ListUtils.isEqualList(previousSavedSecurityGroups, domainNflow.getSecurityGroups())) {
                stopwatch.start();
                List<? extends HadoopSecurityGroup> securityGroups = domainNflow.getSecurityGroups();
                List<String> groupsAsCommaList = securityGroups.stream().map(group -> group.getName()).collect(Collectors.toList());
                hadoopAuthorizationService.updateSecurityGroupsForAllPolicies(nflow.getSystemCategoryName(), nflow.getSystemNflowName(), groupsAsCommaList, domainNflow.getProperties());
                stopwatch.stop();
                log.debug("Time to update hadoop security groups: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                stopwatch.reset();
            }

            // Update Hive metastore
            stopwatch.start();
            final boolean hasHiveDestination = domainNflow.getDestinations().stream()
                .map(NflowDestination::getDatasource)
                .filter(DerivedDatasource.class::isInstance)
                .map(DerivedDatasource.class::cast)
                .anyMatch(datasource -> "HiveDatasource".equals(datasource.getDatasourceType()));
            if (hasHiveDestination) {
                try {
                    nflowHiveTableService.updateColumnDescriptions(nflow);
                } catch (final DataAccessException e) {
                    log.warn("Failed to update column descriptions for nflow: {}", nflow.getCategoryAndNflowDisplayName(), e);
                }
            }
            stopwatch.stop();
            log.debug("Time to update hive metastore: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();

            // Update Nova metastore
            stopwatch.start();
            domainNflow = nflowProvider.update(domainNflow);
            stopwatch.stop();
            log.debug("Time to call nflowProvider.update: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();
        }, (e) -> {
            if (nflow.isNew() && StringUtils.isNotBlank(nflow.getId())) {
                //Rollback ops Manager insert if it is newly created
                metadataAccess.commit(() -> {
                    opsManagerNflowProvider.delete(opsManagerNflowProvider.resolveId(nflow.getId()));
                });
            }
        });


    }

    /**
     * Looks for the Nflow Preconditions and assigns the Nflow Dependencies
     */
    private void assignNflowDependencies(NflowMetadata nflow, Nflow domainNflow) {
        final Nflow.ID domainNflowId = domainNflow.getId();
        List<PreconditionRule> preconditions = nflow.getSchedule().getPreconditions();
        if (preconditions != null) {
            PreconditionPolicyTransformer transformer = new PreconditionPolicyTransformer(preconditions);
            transformer.applyNflowNameToCurrentNflowProperties(nflow.getCategory().getSystemName(), nflow.getSystemNflowName());
            List<com.onescorpin.metadata.rest.model.sla.ObligationGroup> transformedPreconditions = transformer.getPreconditionObligationGroups();
            ServiceLevelAgreementBuilder
                preconditionBuilder =
                nflowProvider.buildPrecondition(domainNflow.getId()).name("Precondition for nflow " + nflow.getCategoryAndNflowName() + "  (" + domainNflow.getId() + ")");
            for (com.onescorpin.metadata.rest.model.sla.ObligationGroup precondition : transformedPreconditions) {
                for (Obligation group : precondition.getObligations()) {
                    preconditionBuilder.obligationGroupBuilder(ObligationGroup.Condition.valueOf(precondition.getCondition())).obligationBuilder().metric(group.getMetrics()).build();
                }
            }
            preconditionBuilder.build();

            //add in the lineage dependency relationships
            //will the nflow exist in the jcr store here if it is new??

            //store the existing list of dependent nflows to track and delete those that dont match
            Set<Nflow.ID> oldDependentNflowIds = new HashSet<Nflow.ID>();
            Set<Nflow.ID> newDependentNflowIds = new HashSet<Nflow.ID>();

            List<Nflow> dependentNflows = domainNflow.getDependentNflows();
            if (dependentNflows != null && !dependentNflows.isEmpty()) {
                dependentNflows.stream().forEach(dependentNflow -> {
                    oldDependentNflowIds.add(dependentNflow.getId());
                });
            }
            //find those preconditions that are marked as dependent nflow types
            List<Precondition> preconditionPolicies = transformer.getPreconditionPolicies();
            preconditionPolicies.stream().filter(precondition -> precondition instanceof DependentNflowPrecondition).forEach(dependentNflowPrecondition -> {
                DependentNflowPrecondition nflowPrecondition = (DependentNflowPrecondition) dependentNflowPrecondition;
                List<String> dependentNflowNames = nflowPrecondition.getDependentNflowNames();
                if (dependentNflowNames != null && !dependentNflowNames.isEmpty()) {
                    //find the nflow
                    for (String dependentNflowName : dependentNflowNames) {
                        Nflow dependentNflow = nflowProvider.findBySystemName(dependentNflowName);
                        if (dependentNflow != null) {
                            Nflow.ID newDependentNflowId = dependentNflow.getId();
                            newDependentNflowIds.add(newDependentNflowId);
                            //add and persist it if it doesnt already exist
                            if (!oldDependentNflowIds.contains(newDependentNflowId)) {
                                nflowProvider.addDependent(domainNflowId, dependentNflow.getId());
                            }
                        }
                    }
                }

            });
            //delete any of those dependent nflow ids from the oldDependentNflows that are not part of the newDependentNflowIds
            oldDependentNflowIds.stream().filter(oldNflowId -> !newDependentNflowIds.contains(oldNflowId))
                .forEach(dependentNflowToDelete -> nflowProvider.removeDependent(domainNflowId, dependentNflowToDelete));

        }
    }

    /**
     * Update a given nflows datasources clearing its sources/destinations before revaluating the data
     *
     * @param nflowId the id of the nflow rest model to update
     */
    public void updateNflowDatasources(String nflowId) {
        metadataAccess.commit(() -> {
            nflowProvider.removeNflowDestinations(nflowProvider.resolveId(nflowId));
            nflowProvider.removeNflowSources(nflowProvider.resolveId(nflowId));
        });

        metadataAccess.commit(() -> {
            Nflow domainNflow = nflowProvider.findById(nflowProvider.resolveId(nflowId));
            NflowMetadata nflow = nflowModelTransform.domainToNflowMetadata(domainNflow);
            assignNflowDatasources(nflow, domainNflow);
        });
    }

    /**
     * Iterate all of the nflows, clear all sources/destinations and reassign
     * Note this will be an expensive call
     */
    public void updateAllNflowsDatasources() {
        metadataAccess.commit(() -> {
            nflowProvider.findAll().stream().forEach(domainNflow -> {
                domainNflow.clearSourcesAndDestinations();
            });
        });

        metadataAccess.commit(() -> {
            nflowProvider.findAll().stream().forEach(domainNflow -> {
                NflowMetadata nflow = nflowModelTransform.domainToNflowMetadata(domainNflow);
                assignNflowDatasources(nflow, domainNflow);
            });
        });
    }

    /**
     * Assign the nflow sources/destinations
     *
     * @param nflow       the nflow rest model
     * @param domainNflow the domain nflow
     */
    private void assignNflowDatasources(NflowMetadata nflow, Nflow domainNflow) {
        final Nflow.ID domainNflowId = domainNflow.getId();
        Set<com.onescorpin.metadata.api.datasource.Datasource.ID> sources = new HashSet<com.onescorpin.metadata.api.datasource.Datasource.ID>();
        Set<com.onescorpin.metadata.api.datasource.Datasource.ID> destinations = new HashSet<com.onescorpin.metadata.api.datasource.Datasource.ID>();

        String uniqueName = NflowNameUtil.fullName(nflow.getCategory().getSystemName(), nflow.getSystemNflowName());

        RegisteredTemplate template = nflow.getRegisteredTemplate();
        if (template == null) {
            //fetch it for checks
            template = templateRestProvider.getRegisteredTemplate(nflow.getTemplateId());

        }
        //find Definition registration

        derivedDatasourceFactory.populateDatasources(nflow, template, sources, destinations);
        //remove the older sources only if they have changed

        if (domainNflow.getSources() != null) {
            Set<Datasource.ID>
                existingSourceIds =
                ((List<NflowSource>) domainNflow.getSources()).stream().filter(source -> source.getDatasource() != null).map(source1 -> source1.getDatasource().getId()).collect(Collectors.toSet());
            if (!sources.containsAll(existingSourceIds) || (sources.size() != existingSourceIds.size())) {
                //remove older sources
                //cant do it here for some reason.. need to do it in a separate transaction
                nflowProvider.removeNflowSources(domainNflowId);
            }
        }
        sources.stream().forEach(sourceId -> nflowProvider.ensureNflowSource(domainNflowId, sourceId));
        destinations.stream().forEach(sourceId -> nflowProvider.ensureNflowDestination(domainNflowId, sourceId));

    }

    @Override
    public void deleteNflow(@Nonnull final String nflowId) {
        metadataAccess.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ADMIN_NFLOWS);
            Nflow.ID nflowIdentifier = nflowProvider.resolveNflow(nflowId);
            Nflow nflow = nflowProvider.getNflow(nflowIdentifier);
            //unschedule any SLAs
            serviceLevelAgreementService.removeAndUnscheduleAgreementsForNflow(nflowIdentifier,nflow.getQualifiedName());
            nflowProvider.deleteNflow(nflow.getId());
            opsManagerNflowProvider.delete(opsManagerNflowProvider.resolveId(nflowId));
            return true;
        });
    }

    @Override
    public void enableNflowCleanup(@Nonnull String nflowId) {
        metadataAccess.commit(() -> {
            final Nflow.ID id = nflowProvider.resolveNflow(nflowId);
            return nflowProvider.mergeNflowProperties(id, ImmutableMap.of(NflowProperties.CLEANUP_ENABLED, "true"));
        });
    }

    private boolean enableNflow(final Nflow.ID nflowId) {
        return metadataAccess.commit(() -> {
            boolean enabled = nflowProvider.enableNflow(nflowId);
            Nflow domainNflow = nflowProvider.findById(nflowId);

            if (domainNflow != null) {
                domainNflow.setState(Nflow.State.ENABLED);
                nflowProvider.update(domainNflow);

                if (enabled) {
                    NflowMetadata nflowMetadata = nflowModelTransform.domainToNflowMetadata(domainNflow);
                    notifyNflowStateChange(nflowMetadata, nflowId, Nflow.State.ENABLED, MetadataChange.ChangeType.UPDATE);
                }
            }

            return enabled;
        });

    }


    // @Transactional(transactionManager = "metadataTransactionManager")
    private boolean disableNflow(final Nflow.ID nflowId) {
        return metadataAccess.commit(() -> {
            boolean disabled = nflowProvider.disableNflow(nflowId);
            Nflow domainNflow = nflowProvider.findById(nflowId);

            if (domainNflow != null) {
                domainNflow.setState(Nflow.State.DISABLED);
                nflowProvider.update(domainNflow);

                if (disabled) {
                    NflowMetadata nflowMetadata = nflowModelTransform.domainToNflowMetadata(domainNflow);
                    notifyNflowStateChange(nflowMetadata, nflowId, Nflow.State.DISABLED, MetadataChange.ChangeType.UPDATE);
                }
            }

            return disabled;
        });

    }

    public NflowSummary enableNflow(final String nflowId) {
        return metadataAccess.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            if (StringUtils.isNotBlank(nflowId)) {
                NflowMetadata nflowMetadata = getNflowById(nflowId);
                Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
                boolean enabled = enableNflow(domainId);
                //re fetch it
                if (enabled) {
                    nflowMetadata.setState(Nflow.State.ENABLED.name());
                    serviceLevelAgreementService.enableServiceLevelAgreementSchedule(domainId);

                }
                NflowSummary nflowSummary = new NflowSummary(nflowMetadata);
                //start any Slas

                return nflowSummary;
            }
            return null;
        });


    }

    public NflowSummary disableNflow(final String nflowId) {
        return metadataAccess.commit(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);

            if (StringUtils.isNotBlank(nflowId)) {
                NflowMetadata nflowMetadata = getNflowById(nflowId);
                Nflow.ID domainId = nflowProvider.resolveNflow(nflowId);
                boolean disabled = disableNflow(domainId);
                //re fetch it
                if (disabled) {
                    nflowMetadata.setState(Nflow.State.DISABLED.name());
                    serviceLevelAgreementService.disableServiceLevelAgreementSchedule(domainId);
                }
                NflowSummary nflowSummary = new NflowSummary(nflowMetadata);
                return nflowSummary;
            }
            return null;
        });

    }

    @Override
    /**
     * Applies new LableValue array to the FieldProperty.selectableValues {label = Category.Display Nflow Name, value=category.system_nflow_name}
     */
    public void applyNflowSelectOptions(List<FieldRuleProperty> properties) {
        if (properties != null && !properties.isEmpty()) {
            List<NflowSummary> nflowSummaries = getNflowSummaryData();
            List<LabelValue> nflowSelection = new ArrayList<>();

            for (NflowSummary nflowSummary : nflowSummaries) {
                boolean isDisabled = nflowSummary.getState() == Nflow.State.DISABLED.name();
                boolean
                    canEditDetails =
                    accessController.isEntityAccessControlled() ? nflowSummary.hasAction(NflowAccessControl.EDIT_DETAILS.getSystemName())
                                                                : accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_NFLOWS);
                Map<String, Object> labelValueProperties = new HashMap<>();
                labelValueProperties.put("nflow:disabled", isDisabled);
                labelValueProperties.put("nflow:editDetails", canEditDetails);
                nflowSelection.add(new LabelValue(nflowSummary.getCategoryAndNflowDisplayName() + (isDisabled ? " (DISABLED) " : ""), nflowSummary.getCategoryAndNflowSystemName(),
                                                 isDisabled ? "This nflow is currently disabled" : "", labelValueProperties));
            }

            nflowSelection.sort(Comparator.comparing(LabelValue::getLabel, String.CASE_INSENSITIVE_ORDER));
            for (FieldRuleProperty property : properties) {
                property.setSelectableValues(nflowSelection);
                if (property.getValues() == null) {
                    property.setValues(new ArrayList<>()); // reset the intial values to be an empty arraylist
                }
            }
        }
    }

    @Nonnull
    @Override
    public Set<UserField> getUserFields() {
        return metadataAccess.read(() -> {
            boolean hasPermission = this.accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);
            return hasPermission ? UserPropertyTransform.toUserFields(nflowProvider.getUserFields()) : Collections.emptySet();
        });
    }

    @Override
    public void setUserFields(@Nonnull final Set<UserField> userFields) {
        boolean hasPermission = this.accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.ADMIN_NFLOWS);
        if (hasPermission) {
            nflowProvider.setUserFields(UserPropertyTransform.toUserFieldDescriptors(userFields));
        }
    }

    @Nonnull
    @Override
    public Optional<Set<UserProperty>> getUserFields(@Nonnull final String categoryId) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_NFLOWS);

            final Optional<Set<UserFieldDescriptor>> categoryUserFields = categoryProvider.getNflowUserFields(categoryProvider.resolveId(categoryId));
            final Set<UserFieldDescriptor> globalUserFields = nflowProvider.getUserFields();
            if (categoryUserFields.isPresent()) {
                return Optional.of(UserPropertyTransform.toUserProperties(Collections.emptyMap(), Sets.union(globalUserFields, categoryUserFields.get())));
            } else {
                return Optional.empty();
            }
        });
    }

    private class NflowPropertyChangeDispatcher implements MetadataEventListener<NflowPropertyChangeEvent> {


        @Override
        public void notify(@Nonnull final NflowPropertyChangeEvent metadataEvent) {
            Properties oldProperties = metadataEvent.getData().getNifiPropertiesToDelete();
            metadataAccess.commit(() -> {
                Nflow nflow = nflowProvider.getNflow(nflowProvider.resolveNflow(metadataEvent.getData().getNflowId()));
                oldProperties.forEach((k, v) -> {
                    nflow.removeProperty((String) k);
                });
            }, MetadataAccess.SERVICE);
        }

    }


    /**
     * update the audit information for nflow state changes
     *
     * @param nflowId     the nflow id
     * @param state      the new state
     * @param changeType the event type
     */
    private void notifyNflowStateChange(NflowMetadata nflowMetadata, Nflow.ID nflowId, Nflow.State state, MetadataChange.ChangeType changeType) {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication() != null
                                    ? SecurityContextHolder.getContext().getAuthentication()
                                    : null;
        String nflowName = nflowMetadata != null ? nflowMetadata.getCategoryAndNflowName() : "";
        NflowChange change = new NflowChange(changeType, nflowName, nflowName, nflowId, state);
        NflowChangeEvent event = new NflowChangeEvent(change, DateTime.now(), principal);
        metadataEventService.notify(event);
    }
}
