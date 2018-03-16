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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.onescorpin.nflowmgr.rest.Model;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreementCheck;
import com.onescorpin.metadata.sla.api.MetricAssessment;
import com.onescorpin.metadata.sla.api.Obligation;
import com.onescorpin.metadata.sla.api.ObligationAssessment;
import com.onescorpin.metadata.sla.api.ObligationGroup;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAssessment;
import com.onescorpin.metadata.sla.spi.ObligationBuilder;
import com.onescorpin.metadata.sla.spi.ObligationGroupBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.security.AccessController;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Transforms to/from  Domain/Rest
 */
public class ServiceLevelAgreementModelTransform {


    @Inject
    private AccessController accessController;

    public static final Function<ServiceLevelAgreement, com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement> DOMAIN_TO_SLA
        = new Function<ServiceLevelAgreement, com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement>() {
        @Override
        public com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement apply(ServiceLevelAgreement domain) {
            return toModel(domain, true);
        }
    };
    public static final Function<ServiceLevelAssessment, com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment> DOMAIN_TO_SLA_ASSMT
        = new Function<ServiceLevelAssessment, com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment>() {
        @Override
        public com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment apply(ServiceLevelAssessment domain) {
            com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement sla = toModel(domain.getAgreement(), false);

            com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment slAssmt
                = new com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment(sla,
                                                                                           domain.getTime(),
//                                                                                               null,
                                                                                           domain.getMessage(),
                                                                                           com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment.Result
                                                                                               .valueOf(domain.getResult().name()));
            for (ObligationAssessment domainObAssmt : domain.getObligationAssessments()) {
                com.onescorpin.metadata.rest.model.sla.ObligationAssessment obAssmt
                    = new com.onescorpin.metadata.rest.model.sla.ObligationAssessment(toModel(domainObAssmt.getObligation(), false),
                                                                                             com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment.Result
                                                                                                 .valueOf(domain.getResult().name()),
                                                                                             domainObAssmt.getMessage());
                for (MetricAssessment<?> domainMetAssmt : domainObAssmt.getMetricAssessments()) {
                    com.onescorpin.metadata.rest.model.sla.MetricAssessment metricAssmnt
                        = new com.onescorpin.metadata.rest.model.sla.MetricAssessment(domainMetAssmt.getMetric(),
                                                                                             com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment.Result
                                                                                                 .valueOf(domain.getResult().name()),
                                                                                             domainMetAssmt.getMessage());
                    obAssmt.addMetricAssessment(metricAssmnt);
                }

                slAssmt.addObligationAssessment(obAssmt);
            }

            return slAssmt;
        }
    };

    public static ServiceLevelAgreement generateDomain(com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement model,
                                                       ServiceLevelAgreementProvider provider) {
        ServiceLevelAgreementBuilder slaBldr = provider.builder()
            .name(model.getName())
            .description(model.getDescription());

        for (com.onescorpin.metadata.rest.model.sla.ObligationGroup grp : model.getGroups()) {
            ObligationGroupBuilder grpBldr = slaBldr.obligationGroupBuilder(ObligationGroup.Condition.valueOf(grp.getCondition()));

            for (com.onescorpin.metadata.rest.model.sla.Obligation ob : grp.getObligations()) {
                ObligationBuilder<?> obBldr = grpBldr.obligationBuilder().description(ob.getDescription());

                for (com.onescorpin.metadata.sla.api.Metric metric : ob.getMetrics()) {
                    obBldr.metric(metric);
                }

                obBldr.build();
            }

            grpBldr.build();
        }

        return slaBldr.build();
    }


    public static com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement toModel(ServiceLevelAgreement domain, boolean deep) {

        com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement sla
            = new com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement(domain.getId().toString(),
                                                                                      domain.getName(),
                                                                                      domain.getDescription());
        if (domain.getSlaChecks() != null) {
            List<ServiceLevelAgreementCheck> checks = new ArrayList<>();
            sla.setSlaChecks(checks);
            for (com.onescorpin.metadata.sla.spi.ServiceLevelAgreementCheck check : domain.getSlaChecks()) {
                ServiceLevelAgreementCheck restModel = new ServiceLevelAgreementCheck();
                restModel.setCronSchedule(check.getCronSchedule());
                if (deep) {
                    try {
                        restModel.setActionConfigurations(check.getActionConfigurations());
                    } catch (Exception e) {
                        if (ExceptionUtils.getRootCause(e) instanceof ClassNotFoundException) {
                            String msg = ExceptionUtils.getRootCauseMessage(e);
                            //get just the simpleClassName stripping the package info
                            msg = StringUtils.substringAfterLast(msg, ".");
                            sla.addSlaCheckError("Unable to find the SLA Action Configurations of type: " + msg
                                                 + ". Check with an administrator to ensure the correct plugin is installed with this SLA configuration. ");
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                }
                checks.add(restModel);
            }
        }

        if (deep) {
            if (domain.getObligationGroups().size() == 1 && domain.getObligationGroups().get(0).getCondition() == ObligationGroup.Condition.REQUIRED) {
                for (Obligation domainOb : domain.getObligations()) {
                    com.onescorpin.metadata.rest.model.sla.Obligation ob = toModel(domainOb, true);
                    sla.addObligation(ob);
                }
            } else {
                for (ObligationGroup domainGroup : domain.getObligationGroups()) {
                    //Force it to be required
                    //TODO Rework once the SLA page allows for Sufficient/Required settings
                    // TODO use the domainGroup.condition instead
                    com.onescorpin.metadata.rest.model.sla.ObligationGroup group
                        = new com.onescorpin.metadata.rest.model.sla.ObligationGroup(ObligationGroup.Condition.REQUIRED.name());

                    for (Obligation domainOb : domainGroup.getObligations()) {
                        com.onescorpin.metadata.rest.model.sla.Obligation ob = toModel(domainOb, true);
                        group.addObligation(ob);
                    }

                    sla.addGroup(group);
                }
            }
        }

        return sla;
    }

    public static com.onescorpin.metadata.rest.model.sla.Obligation toModel(Obligation domainOb, boolean deep) {
        com.onescorpin.metadata.rest.model.sla.Obligation ob
            = new com.onescorpin.metadata.rest.model.sla.Obligation();
        ob.setDescription(domainOb.getDescription());
        if (deep) {
            ob.setMetrics(Lists.newArrayList(domainOb.getMetrics()));
        }
        return ob;
    }

    /**
     * Nflow model transformer
     */
    @Nonnull
    private final Model model;

    /**
     * Constructs a {@code ServiceLevelAgreementModelTransform}.
     *
     * @param model the nflow model transformer
     */
    public ServiceLevelAgreementModelTransform(@Nonnull final Model model) {
        this.model = model;
    }

    /**
     * Transforms the specified domain object to a REST object.
     *
     * @param domain the domain object
     * @return the REST object
     */
    public com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement domainToNflowSlaShallow(com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement domain) {
        return toModel(domain, false);
    }

    /**
     * Transforms the specified domain objects to REST objects.
     *
     * @param slaList the domain objects.
     * @return the REST objects
     */
    public List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> transformNflowServiceLevelAgreements(
        List<com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement> slaList) {
        Collection<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> list = null;
        if (slaList != null) {
            list = Collections2.transform(slaList, this::domainToNflowSlaShallow);
            return new ArrayList<>(list);
        }
        return null;
    }

    /**
     * Transforms the specified domain object to a REST object.
     *
     * @param domain the domain
     * @param deep   {@code true} to include action configurations
     * @return the REST object
     */
    public NflowServiceLevelAgreement toModel(com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement domain, boolean deep) {
        return toModel(domain, (Set<com.onescorpin.metadata.api.nflow.Nflow>) domain.getNflows(), deep);
    }

    /**
     * Transforms the specified domain objects to REST objects.
     *
     * @param domain the SLA domain object
     * @param nflows  the nflow domain objects
     * @param deep   {@code true} to include action configurations
     * @return the SLA REST object
     */
    public NflowServiceLevelAgreement toModel(ServiceLevelAgreement domain, Set<com.onescorpin.metadata.api.nflow.Nflow> nflows, boolean deep) {
        com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement slaModel = toModel(domain, deep);
        NflowServiceLevelAgreement nflowServiceLevelAgreement = new NflowServiceLevelAgreement(slaModel);
        boolean canEdit = false;
        boolean canView = true;
        if (nflows != null && !nflows.isEmpty()) {
            final Set<Nflow> nflowModels = nflows.stream()
                .filter(nflow -> nflow != null)
                .map(model::domainToNflow)
                .collect(Collectors.toSet());
            nflowServiceLevelAgreement.setNflows(nflowModels);
            if (accessController.isEntityAccessControlled()) {
                //set the flag on the sla edit to true only if the user has access to edit the nflows assigned to this sla
                canEdit = nflows.stream().allMatch(nflow -> nflow.getAllowedActions().hasPermission(NflowAccessControl.EDIT_DETAILS));
                //can view
                canView = nflows.stream().allMatch(nflow -> nflow.getAllowedActions().hasPermission(NflowAccessControl.ACCESS_NFLOW));
            } else {
                canEdit = this.accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENTS);
            }

        } else {
            canEdit = this.accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENTS);
        }
        slaModel.setCanEdit(canEdit);
        if(nflowServiceLevelAgreement.getNflows() == null){
            nflowServiceLevelAgreement.setNflows(new HashSet<>());
        }
        nflowServiceLevelAgreement.setCanEdit(canEdit);
        return nflowServiceLevelAgreement;
    }
}
