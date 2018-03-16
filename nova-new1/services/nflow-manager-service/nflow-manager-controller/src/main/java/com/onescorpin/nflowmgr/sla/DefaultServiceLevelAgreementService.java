package com.onescorpin.nflowmgr.sla;

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

import com.google.common.collect.Lists;
import com.onescorpin.app.ServicesApplicationStartupListener;
import com.onescorpin.common.velocity.model.VelocityTemplate;
import com.onescorpin.common.velocity.service.VelocityTemplateProvider;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerNflowService;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowNotFoundExcepton;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreementProvider;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreementRelationship;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementActionTemplate;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementActionTemplateProvider;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementDescriptionProvider;
import com.onescorpin.metadata.jpa.common.JpaVelocityTemplate;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.rest.model.sla.Obligation;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ObligationGroup;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementActionConfiguration;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementActionValidation;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementDescription;
import com.onescorpin.metadata.sla.spi.ObligationGroupBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementEmailTemplate;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementScheduler;
import com.onescorpin.policy.PolicyPropertyTypes;
import com.onescorpin.policy.rest.model.FieldRuleProperty;
import com.onescorpin.rest.model.LabelValue;
import com.onescorpin.security.AccessController;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Service for interacting with SLA's
 */
public class DefaultServiceLevelAgreementService implements ServicesApplicationStartupListener, ServiceLevelAgreementService {

    private static final Logger log = LoggerFactory.getLogger(DefaultServiceLevelAgreementService.class);

    @Inject
    ServiceLevelAgreementProvider slaProvider;
    @Inject
    NflowServiceLevelAgreementProvider nflowSlaProvider;
    @Inject
    JcrMetadataAccess metadataAccess;
    @Inject
    ServiceLevelAgreementScheduler serviceLevelAgreementScheduler;
    @Inject
    private NflowManagerNflowService nflowManagerNflowService;
    @Inject
    private NflowProvider nflowProvider;
    @Inject
    private ServiceLevelAgreementModelTransform serviceLevelAgreementTransform;

    @Inject
    private AccessController accessController;

    @Inject
    private ServiceLevelAgreementDescriptionProvider serviceLevelAgreementDescriptionProvider;

    @Inject
    private VelocityTemplateProvider velocityTemplateProvider;

    @Inject
    private ServiceLevelAgreementActionTemplateProvider serviceLevelAgreementActionTemplateProvider;


    private List<ServiceLevelAgreementRule> serviceLevelAgreementRules;

    @Override
    public void onStartup(DateTime startTime) {
        discoverServiceLevelAgreementRules();
    }

    private List<ServiceLevelAgreementRule> discoverServiceLevelAgreementRules() {
        List<ServiceLevelAgreementRule> rules = ServiceLevelAgreementMetricTransformer.instance().discoverSlaMetrics();
        serviceLevelAgreementRules = rules;
        return serviceLevelAgreementRules;
    }

    @Override
    public List<ServiceLevelAgreementRule> discoverSlaMetrics() {
        List<ServiceLevelAgreementRule> rules = serviceLevelAgreementRules;
        if (rules == null) {
            rules = discoverServiceLevelAgreementRules();
        }

        nflowManagerNflowService
            .applyNflowSelectOptions(
                ServiceLevelAgreementMetricTransformer.instance().findPropertiesForRulesetMatchingRenderTypes(rules, new String[]{PolicyPropertyTypes.PROPERTY_TYPE.nflowChips.name(),
                                                                                                                                  PolicyPropertyTypes.PROPERTY_TYPE.nflowSelect.name(),
                                                                                                                                  PolicyPropertyTypes.PROPERTY_TYPE.currentNflow.name()}));

        return rules;
    }

    @Override
    public List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> getServiceLevelAgreements() {

        accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS);
        //find all as a Service account
        List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> agreementsList = this.metadataAccess.read(() -> {
            List<com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement> agreements = nflowSlaProvider.findAllAgreements();
            if (agreements != null) {
                return serviceLevelAgreementTransform.transformNflowServiceLevelAgreements(agreements);
            }

            return new ArrayList<>(0);
        }, MetadataAccess.SERVICE);

        if (accessController.isEntityAccessControlled()) {

            Map<String, com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement>
                serviceLevelAgreementMap = agreementsList.stream().collect(Collectors.toMap(com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement::getId, Function.identity()));
            //filter out those nflows user doesnt have access to
            List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> entityAccessControlledSlas = this.metadataAccess.read(() -> {
                List<com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement> agreements = nflowSlaProvider.findAllAgreements();
                if (agreements != null) {
                    List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement>
                        serviceLevelAgreements =
                        serviceLevelAgreementTransform.transformNflowServiceLevelAgreements(agreements);
                    return serviceLevelAgreements.stream().filter(agreement -> serviceLevelAgreementMap.get(agreement.getId()).getNflowsCount() == agreement.getNflowsCount())
                        .collect(Collectors.toList());
                }
                return new ArrayList<>(0);
            });

            return entityAccessControlledSlas;
        } else {
            return agreementsList;
        }


    }

    @Override
    public void enableServiceLevelAgreementSchedule(Nflow.ID nflowId) {
        metadataAccess.commit(() -> {
            List<NflowServiceLevelAgreement> agreements = nflowSlaProvider.findNflowServiceLevelAgreements(nflowId);
            if (agreements != null) {
                for (com.onescorpin.metadata.sla.api.ServiceLevelAgreement sla : agreements) {
                    serviceLevelAgreementScheduler.enableServiceLevelAgreement(sla);
                }
            }
        }, MetadataAccess.SERVICE);
    }

    @Override
    public void unscheduleServiceLevelAgreement(Nflow.ID nflowId, String categoryAndNflowName) {
        unscheduleServiceLevelAgreement(nflowId, categoryAndNflowName, false);
    }


    public void unscheduleServiceLevelAgreement(Nflow.ID nflowId, String categoryAndNflowName, boolean remove) {
        metadataAccess.commit(() -> {
            List<NflowServiceLevelAgreement> agreements = nflowSlaProvider.findNflowServiceLevelAgreements(nflowId);
            if (agreements != null) {
                for (com.onescorpin.metadata.sla.api.ServiceLevelAgreement sla : agreements) {

                    if (sla instanceof NflowServiceLevelAgreement && ((NflowServiceLevelAgreement) sla).getNflows().size() == 1) {
                        nflowSlaProvider.removeNflowRelationships(sla.getId());
                        slaProvider.removeAgreement(sla.getId());
                        serviceLevelAgreementScheduler.unscheduleServiceLevelAgreement(sla.getId());
                    } else {
                        serviceLevelAgreementScheduler.unscheduleServiceLevelAgreement(sla.getId());
                    }
                }
            }
        });
    }

    public void removeAndUnscheduleAgreementsForNflow(Nflow.ID nflowId, String categoryAndNflowName) {
        unscheduleServiceLevelAgreement(nflowId, categoryAndNflowName, true);
    }


    @Override
    public void disableServiceLevelAgreementSchedule(Nflow.ID nflowId) {
        metadataAccess.commit(() -> {
            List<NflowServiceLevelAgreement> agreements = nflowSlaProvider.findNflowServiceLevelAgreements(nflowId);
            if (agreements != null) {
                for (com.onescorpin.metadata.sla.api.ServiceLevelAgreement sla : agreements) {
                    serviceLevelAgreementScheduler.disableServiceLevelAgreement(sla);
                }
            }
        }, MetadataAccess.SERVICE);
    }

    public boolean hasServiceLevelAgreements(Nflow.ID id) {
        List<NflowServiceLevelAgreement> agreements = nflowSlaProvider.findNflowServiceLevelAgreements(id);
        return agreements != null && !agreements.isEmpty();
    }

    @Override
    public List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> getNflowServiceLevelAgreements(String nflowId) {
        return metadataAccess.read(() -> {

            Nflow.ID id = nflowProvider.resolveNflow(nflowId);

            boolean
                canAccess =
                accessController.isEntityAccessControlled() ? nflowManagerNflowService.checkNflowPermission(id.toString(), NflowAccessControl.ACCESS_NFLOW)
                                                            : accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS);
            if (canAccess) {
                List<NflowServiceLevelAgreement> agreements = nflowSlaProvider.findNflowServiceLevelAgreements(id);
                if (agreements != null) {
                    return serviceLevelAgreementTransform.transformNflowServiceLevelAgreements(agreements);
                }
            }
            return null;

        });
    }


    private com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement findNflowServiceLevelAgreementAsAdmin(String slaId, boolean deep) {
        com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement systemSla = metadataAccess.read(() -> {

            NflowServiceLevelAgreement agreement = nflowSlaProvider.findAgreement(slaProvider.resolve(slaId));
            if (agreement != null) {
                return serviceLevelAgreementTransform.toModel(agreement, deep);
            }
            return null;
        }, MetadataAccess.SERVICE);
        return systemSla;
    }

    /**
     * Check to see if the user can edit
     *
     * @param slaId an sla to check
     * @return true if user can edit the SLA, false if not
     */
    @Override
    public boolean canEditServiceLevelAgreement(String slaId) {
        com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement systemSla = findNflowServiceLevelAgreementAsAdmin(slaId, false);

        if (systemSla != null) {
            if (systemSla.getNflows() != null && accessController.isEntityAccessControlled()) {
                return systemSla.getNflows().stream().allMatch(nflow -> nflowManagerNflowService.checkNflowPermission(nflow.getId(), NflowAccessControl.EDIT_DETAILS));
            } else {
                accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENTS);
                return true;
            }
        }
        return false;
    }

    /**
     * check to see if the current user can read/view the SLA
     *
     * @param slaId an sla to check
     * @return true if user can read the SLA, false if not
     */
    @Override
    public boolean canAccessServiceLevelAgreement(String slaId) {
        com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement systemSla = findNflowServiceLevelAgreementAsAdmin(slaId, false);

        if (systemSla != null) {
            if (systemSla.getNflows() != null && accessController.isEntityAccessControlled()) {
                return systemSla.getNflows().stream().allMatch(nflow -> nflowManagerNflowService.checkNflowPermission(nflow.getId(), NflowAccessControl.ACCESS_NFLOW));
            } else {
                accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS);
                return true;
            }
        }
        return false;

    }

    @Override
    public ServiceLevelAgreement getServiceLevelAgreement(String slaId) {

        com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement systemSla = findNflowServiceLevelAgreementAsAdmin(slaId, false);

        //filter out if this SLA has nflows which the current user cannot access
        ServiceLevelAgreement serviceLevelAgreement = metadataAccess.read(() -> {

            NflowServiceLevelAgreement agreement = nflowSlaProvider.findAgreement(slaProvider.resolve(slaId));
            if (agreement != null) {
                com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement entityAccessControlledSla = serviceLevelAgreementTransform.toModel(agreement, false);
                if (systemSla.getNflowsCount() == entityAccessControlledSla.getNflowsCount()) {
                    return entityAccessControlledSla;
                }
            }
            return null;
        });

        return serviceLevelAgreement;


    }


    /**
     * get a SLA and convert it to the editable SLA form object
     */
    @Override
    public ServiceLevelAgreementGroup getServiceLevelAgreementAsFormObject(String slaId) {

        com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement systemSla = findNflowServiceLevelAgreementAsAdmin(slaId, true);

        if (systemSla != null) {

            return metadataAccess.read(() -> {
                //read it in as the current user
                NflowServiceLevelAgreement agreement = nflowSlaProvider.findAgreement(slaProvider.resolve(slaId));
                //ensure the nflow count match
                if (agreement.getNflows().size() != systemSla.getNflows().size()) {
                    throw new AccessControlException("Unable to access the SLA " + agreement.getName() + ".  You dont have proper access to one or more of the nflows associated with this SLA");
                }
                if (agreement != null) {
                    com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement modelSla = serviceLevelAgreementTransform.toModel(agreement, true);
                    ServiceLevelAgreementMetricTransformerHelper transformer = new ServiceLevelAgreementMetricTransformerHelper();
                    ServiceLevelAgreementGroup serviceLevelAgreementGroup = transformer.toServiceLevelAgreementGroup(modelSla);
                    nflowManagerNflowService
                        .applyNflowSelectOptions(
                            ServiceLevelAgreementMetricTransformer.instance()
                                .findPropertiesForRulesetMatchingRenderTypes(serviceLevelAgreementGroup.getRules(), new String[]{PolicyPropertyTypes.PROPERTY_TYPE.nflowChips.name(),
                                                                                                                                 PolicyPropertyTypes.PROPERTY_TYPE.nflowSelect.name(),
                                                                                                                                 PolicyPropertyTypes.PROPERTY_TYPE.currentNflow.name()}));

                    applyVelocityTemplateSelectionToActionActionItems(serviceLevelAgreementGroup.getActionConfigurations());
                    serviceLevelAgreementGroup.setCanEdit(modelSla.isCanEdit());
                    return serviceLevelAgreementGroup;
                }
                return null;

            });
        } else {
            return null;
        }
    }

    @Override
    public boolean removeAndUnscheduleAgreement(String id) {
        boolean canEdit = canEditServiceLevelAgreement(id);

        if (canEdit) {
            return metadataAccess.commit(() -> {
                com.onescorpin.metadata.sla.api.ServiceLevelAgreement.ID slaId = slaProvider.resolve(id);
                //attempt to find it
                com.onescorpin.metadata.sla.api.ServiceLevelAgreement sla = slaProvider.getAgreement(slaId);
                slaProvider.removeAgreement(slaId);
                serviceLevelAgreementScheduler.unscheduleServiceLevelAgreement(slaId);
                return true;
            });
        } else {
            return false;
        }

    }

    @Override
    public boolean removeAllAgreements() {
        return metadataAccess.commit(() -> {
            List<com.onescorpin.metadata.sla.api.ServiceLevelAgreement> agreements = slaProvider.getAgreements();
            if (agreements != null) {
                for (com.onescorpin.metadata.sla.api.ServiceLevelAgreement agreement : agreements) {
                    slaProvider.removeAgreement(agreement.getId());
                }
            }
            return true;
        });

    }

    @Override
    public List<ServiceLevelAgreementActionUiConfigurationItem> discoverActionConfigurations() {

        List<ServiceLevelAgreementActionUiConfigurationItem> actionItems = ServiceLevelAgreementActionConfigTransformer.instance().discoverActionConfigurations();
        applyVelocityTemplateSelectionToActionActionItems(actionItems);
        return actionItems;
    }


    private void applyVelocityTemplateSelectionToActionActionItems(List<ServiceLevelAgreementActionUiConfigurationItem> actionItems) {
        if (actionItems != null && !actionItems.isEmpty()) {

            Map<ServiceLevelAgreementActionUiConfigurationItem, List<FieldRuleProperty>>
                velocityTemplatePropertyMap =
                actionItems.stream().collect(Collectors.toMap(a -> a, a -> ServiceLevelAgreementMetricTransformer.instance()
                    .findPropertiesForRulesetMatchingRenderTypes(Lists.newArrayList(a), new String[]{PolicyPropertyTypes.PROPERTY_TYPE.velocityTemplate.name()})));

            velocityTemplatePropertyMap.entrySet().stream().forEach(e ->
                                                                    {
                                                                        List<? extends VelocityTemplate>
                                                                            availableTemplates =
                                                                            velocityTemplateProvider.findEnabledByType(e.getKey().getVelocityTemplateType());
                                                                        if (e.getValue() != null && !e.getValue().isEmpty()) {

                                                                            e.getValue().stream().forEach(p -> {
                                                                                if (availableTemplates.isEmpty()) {
                                                                                    p.setHidden(true);
                                                                                    //log hiding template property
                                                                                } else {
                                                                                    if (availableTemplates.size() == 1) {
                                                                                        p.setHidden(true);
                                                                                        p.setValue(availableTemplates.get(0).getId().toString());
                                                                                    } else {
                                                                                        List<LabelValue>
                                                                                            templateSelection =
                                                                                            availableTemplates.stream().filter(t -> t.isEnabled())
                                                                                                .map(t -> new LabelValue(t.getName(), t.getId().toString()))
                                                                                                .collect(Collectors.toList());
                                                                                        p.setSelectableValues(templateSelection);
                                                                                        if (StringUtils.isBlank(p.getValue())) {
                                                                                            p.setValue(availableTemplates.get(0).getId().toString());
                                                                                        }
                                                                                        if (p.getValues() == null) {
                                                                                            p.setValues(new ArrayList<>()); // reset the initial values to be an empty arraylist
                                                                                        }
                                                                                    }
                                                                                }
                                                                            });
                                                                        }

                                                                    });

        }
    }

    @Override
    public List<ServiceLevelAgreementActionValidation> validateAction(String actionConfigurationClassName) {
        return ServiceLevelAgreementActionConfigTransformer.instance().validateAction(actionConfigurationClassName);
    }


    @Override
    public ServiceLevelAgreement saveAndScheduleSla(ServiceLevelAgreementGroup serviceLevelAgreement) {
        return saveAndScheduleSla(serviceLevelAgreement, null);
    }


    private Set<VelocityTemplate.ID> findVelocityTemplates(ServiceLevelAgreementGroup agreement) {
        if (agreement.getActionConfigurations() == null) {
            return null;
        }
        return agreement.getActionConfigurations().stream().flatMap(a -> ServiceLevelAgreementMetricTransformer.instance()
            .findPropertiesForRulesetMatchingRenderTypes(Lists.newArrayList(a), new String[]{PolicyPropertyTypes.PROPERTY_TYPE.velocityTemplate.name()}).stream())
            .filter(r -> StringUtils.isNotBlank(r.getValue())).map(r -> velocityTemplateProvider.resolveId(r.getValue())).collect(Collectors.toSet());
    }

    /**
     * In order to Save an SLA if it is related to a Nflow(s) the user needs to have EDIT_DETAILS permission on the Nflow(s)
     *
     * @param serviceLevelAgreement the sla to save
     * @param nflow                  an option Nflow to relate to this SLA.  If this is not present the related nflows are also embedded in the SLA policies.  The Nflow is a pointer access to the current
     *                              nflow the user is editing if they are creating an SLA from the Nflow Details page. If creating an SLA from the main SLA page the nflow property will not be populated.
     */
    private ServiceLevelAgreement saveAndScheduleSla(ServiceLevelAgreementGroup serviceLevelAgreement, NflowMetadata nflow) {

        //ensure user has permissions to edit the SLA
        if (serviceLevelAgreement != null) {

            ServiceLevelAgreementMetricTransformerHelper transformer = new ServiceLevelAgreementMetricTransformerHelper();

            //Read the nflows on the SLA as a Service. Then verify the current user has access to edit these nflows
            List<String> nflowsOnSla = metadataAccess.read(() -> {
                List<String> nflowIds = new ArrayList<>();
                //all referencing Nflows
                List<String> systemCategoryAndNflowNames = transformer.getCategoryNflowNames(serviceLevelAgreement);

                for (String categoryAndNflow : systemCategoryAndNflowNames) {
                    //fetch and update the reference to the sla
                    String categoryName = StringUtils.trim(StringUtils.substringBefore(categoryAndNflow, "."));
                    String nflowName = StringUtils.trim(StringUtils.substringAfterLast(categoryAndNflow, "."));
                    Nflow nflowEntity = nflowProvider.findBySystemName(categoryName, nflowName);
                    if (nflowEntity != null) {
                        nflowIds.add(nflowEntity.getId().toString());
                    }
                }
                return nflowIds;
            }, MetadataAccess.SERVICE);

            boolean allowedToEdit = nflowsOnSla.isEmpty() ? true : nflowsOnSla.stream().allMatch(nflowId -> nflowManagerNflowService.checkNflowPermission(nflowId, NflowAccessControl.EDIT_DETAILS));

            if (allowedToEdit) {

                return metadataAccess.commit(() -> {

                    //Re read back in the Nflows for this session
                    Set<Nflow> slaNflows = new HashSet<Nflow>();
                    Set<Nflow.ID> slaNflowIds = new HashSet<Nflow.ID>();
                    nflowsOnSla.stream().forEach(nflowId -> {
                        Nflow nflowEntity = nflowProvider.findById(nflowProvider.resolveId(nflowId));
                        if (nflowEntity != null) {
                            slaNflows.add(nflowEntity);
                            slaNflowIds.add(nflowEntity.getId());
                        }
                    });

                    if (nflow != null) {
                        nflowManagerNflowService.checkNflowPermission(nflow.getId(), NflowAccessControl.EDIT_DETAILS);
                    }

                    if (nflow != null) {
                        transformer.applyNflowNameToCurrentNflowProperties(serviceLevelAgreement, nflow.getCategory().getSystemName(), nflow.getSystemNflowName());
                    }
                    ServiceLevelAgreement sla = transformer.getServiceLevelAgreement(serviceLevelAgreement);

                    ServiceLevelAgreementBuilder slaBuilder = null;
                    com.onescorpin.metadata.sla.api.ServiceLevelAgreement.ID existingId = null;
                    if (StringUtils.isNotBlank(sla.getId())) {
                        existingId = slaProvider.resolve(sla.getId());
                    }
                    if (existingId != null) {
                        slaBuilder = slaProvider.builder(existingId);
                    } else {
                        slaBuilder = slaProvider.builder();
                    }

                    slaBuilder.name(sla.getName()).description(sla.getDescription());
                    for (com.onescorpin.metadata.rest.model.sla.ObligationGroup group : sla.getGroups()) {
                        ObligationGroupBuilder groupBuilder = slaBuilder.obligationGroupBuilder(ObligationGroup.Condition.valueOf(group.getCondition()));
                        for (Obligation o : group.getObligations()) {
                            groupBuilder.obligationBuilder().metric(o.getMetrics()).description(o.getDescription()).build();
                        }
                        groupBuilder.build();
                    }
                    com.onescorpin.metadata.sla.api.ServiceLevelAgreement savedSla = slaBuilder.build();

                    List<ServiceLevelAgreementActionConfiguration> actions = transformer.getActionConfigurations(serviceLevelAgreement);

                    // now assign the sla checks
                    slaProvider.slaCheckBuilder(savedSla.getId()).removeSlaChecks().actionConfigurations(actions).build();

                    //relate them
                    Set<Nflow.ID> nflowIds = new HashSet<>();
                    NflowServiceLevelAgreementRelationship nflowServiceLevelAgreementRelationship = nflowSlaProvider.relateNflows(savedSla, slaNflows);
                    if (nflowServiceLevelAgreementRelationship != null && nflowServiceLevelAgreementRelationship.getNflows() != null) {
                        nflowIds = nflowServiceLevelAgreementRelationship.getNflows().stream().map(f -> f.getId()).collect(Collectors.toSet());
                    }

                    Set<VelocityTemplate.ID> velocityTemplates = findVelocityTemplates(serviceLevelAgreement);

                    //Update the JPA mapping in Ops Manager for this SLA and its related Nflows
                    serviceLevelAgreementDescriptionProvider.updateServiceLevelAgreement(savedSla.getId(), savedSla.getName(), savedSla.getDescription(), nflowIds, velocityTemplates);

                    com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement restModel = serviceLevelAgreementTransform.toModel(savedSla, slaNflows, true);
                    //schedule it
                    serviceLevelAgreementScheduler.scheduleServiceLevelAgreement(savedSla);
                    return restModel;

                });
            }
        }
        return null;
    }

    @Override
    public ServiceLevelAgreement saveAndScheduleNflowSla(ServiceLevelAgreementGroup serviceLevelAgreement, String nflowId) {

        NflowMetadata nflow = null;
        if (StringUtils.isNotBlank(nflowId)) {
            nflow = nflowManagerNflowService.getNflowById(nflowId);

        }
        if (nflow != null && nflowManagerNflowService.checkNflowPermission(nflow.getId(), NflowAccessControl.EDIT_DETAILS)) {
            ServiceLevelAgreement sla = saveAndScheduleSla(serviceLevelAgreement, nflow);
            return sla;
        } else {
            log.error("Error attempting to save and Schedule the Nflow SLA {} ({}) ", nflow != null ? nflow.getCategoryAndNflowName() : " NULL Nflow ", nflowId);
            throw new NflowNotFoundExcepton("Unable to create SLA for Nflow " + nflowId, nflowProvider.resolveNflow(nflowId));
        }


    }

    public List<ServiceLevelAgreementEmailTemplate> getServiceLevelAgreementEmailTemplates() {
        return metadataAccess.read(() -> {
            List<? extends ServiceLevelAgreementActionTemplate> serviceLevelAgreementActionTemplates = serviceLevelAgreementActionTemplateProvider.findAll();
            Map<VelocityTemplate.ID, List<ServiceLevelAgreementActionTemplate>>
                templatesByVelocityId =
                serviceLevelAgreementActionTemplates.stream().collect(Collectors.groupingBy(c -> c.getVelocityTemplate().getId()));

            return velocityTemplateProvider.findByType(ServiceLevelAgreementEmailTemplate.EMAIL_TEMPLATE_TYPE).stream()
                .map(t -> new ServiceLevelAgreementEmailTemplate(t.getId().toString(), t.getName(), t.getSystemName(), t.getTitle(), t.getTemplate(), t.isEnabled(), t.isDefault())).collect(
                    Collectors.toList());
        });
    }


    public List<SimpleServiceLevelAgreementDescription> getSlaReferencesForVelocityTemplate(String velocityTemplateId) {
        return metadataAccess.read(() -> {
            List<? extends ServiceLevelAgreementActionTemplate> templates = serviceLevelAgreementActionTemplateProvider.findByVelocityTemplate(velocityTemplateProvider.resolveId(velocityTemplateId));
            if (templates != null) {
                return templates.stream().map(t -> {
                    ServiceLevelAgreementDescription sla = t.getServiceLevelAgreementDescription();
                    return new SimpleServiceLevelAgreementDescription(sla.getSlaId().toString(), sla.getName(), sla.getDescription());
                }).collect(Collectors.toList());
            }
            return Collections.emptyList();
        });
    }


    public ServiceLevelAgreementEmailTemplate saveEmailTemplate(ServiceLevelAgreementEmailTemplate emailTemplate) {
        accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENTS);
        return metadataAccess.commit(() -> {
            JpaVelocityTemplate jpaVelocityTemplate = null;
            if (StringUtils.isNotBlank(emailTemplate.getId())) {
                VelocityTemplate.ID id = velocityTemplateProvider.resolveId(emailTemplate.getId());
                jpaVelocityTemplate = (JpaVelocityTemplate) velocityTemplateProvider.findById(id);
                jpaVelocityTemplate.setName(emailTemplate.getName());
                jpaVelocityTemplate.setTitle(emailTemplate.getSubject());
                jpaVelocityTemplate.setTemplate(emailTemplate.getTemplate());
                jpaVelocityTemplate.setEnabled(emailTemplate.isEnabled());
                jpaVelocityTemplate.setDefault(emailTemplate.isDefault());
                if (!emailTemplate.isEnabled()) {
                    List<? extends ServiceLevelAgreementActionTemplate> slaTemplates = serviceLevelAgreementActionTemplateProvider.findByVelocityTemplate(id);
                    if (slaTemplates != null && !slaTemplates.isEmpty()) {
                        throw new IllegalArgumentException("Unable to disable this template. There are " + slaTemplates.size() + " SLAs using it.");
                    }
                }
            } else {
                jpaVelocityTemplate =
                    new JpaVelocityTemplate(ServiceLevelAgreementEmailTemplate.EMAIL_TEMPLATE_TYPE, emailTemplate.getName(), emailTemplate.getName(), emailTemplate.getSubject(),
                                            emailTemplate.getTemplate(), emailTemplate.isEnabled());
            }

            VelocityTemplate template = velocityTemplateProvider.save(jpaVelocityTemplate);
            emailTemplate.setId(template.getId().toString());
            return emailTemplate;
        });
    }

}
