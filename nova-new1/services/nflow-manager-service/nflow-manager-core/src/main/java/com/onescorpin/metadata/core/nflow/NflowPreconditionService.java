/**
 *
 */
package com.onescorpin.metadata.core.nflow;

/*-
 * #%L
 * onescorpin-nflow-manager-core
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

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowOperationStatusEvent;
import com.onescorpin.metadata.api.event.nflow.OperationStatus;
import com.onescorpin.metadata.api.event.nflow.PreconditionTriggerEvent;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowPrecondition;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceNflow;
import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAssessment;
import com.onescorpin.metadata.sla.spi.ServiceLevelAssessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * Service for assessing {@link NflowPrecondition}
 */
public class NflowPreconditionService {

    private static final Logger log = LoggerFactory.getLogger(NflowPreconditionService.class);

    @Inject
    private ServiceLevelAssessor assessor;

    @Inject
    private NflowProvider nflowProvider;

    @Inject
    private MetadataAccess metadata;

    @Inject
    private MetadataEventService eventService;

    private NflowOperationListener listener = new NflowOperationListener();

    @PostConstruct
    public void addEventListener() {
        this.eventService.addListener(this.listener);
    }

    @PreDestroy
    public void removeEventListener() {
        this.eventService.removeListener(this.listener);
    }


    public ServiceLevelAssessment assess(NflowPrecondition precond) {
        ServiceLevelAgreement sla = precond.getAgreement();
        return this.assessor.assess(sla);
    }

    private void checkPrecondition(Nflow nflow, OperationStatus operationStatus) {
        NflowPrecondition precond = nflow.getPrecondition();

        if (precond != null) {
            log.debug("Checking precondition of nflow: {} ({})", nflow.getName(), nflow.getId());

            ServiceLevelAgreement sla = precond.getAgreement();
            boolean isAssess = sla.getObligationGroups().stream()
                .flatMap(obligationGroup -> obligationGroup.getObligations().stream())
                .flatMap(obligation -> obligation.getMetrics().stream())
                .anyMatch(metric -> isMetricDependentOnStatus(metric, operationStatus));

            if (isAssess) {
                ServiceLevelAssessment assessment = this.assessor.assess(sla);

                if (assessment.getResult() == AssessmentResult.SUCCESS) {
                    log.info("Firing precondition trigger event for nflow:{} ({})", nflow.getName(), nflow.getId());
                    this.eventService.notify(new PreconditionTriggerEvent(nflow.getId()));
                }
            } else {
                log.debug("Nflow {}.{} does not depend on nflow {}", nflow.getCategory(), nflow.getName(), operationStatus.getNflowName());
            }
        }
    }

    /**
     * To avoid nflows being triggered by nflows they do not depend on
     */
    private boolean isMetricDependentOnStatus(Metric metric, OperationStatus operationStatus) {
        return !(metric instanceof NflowExecutedSinceNflow) || operationStatus.getNflowName().equalsIgnoreCase(((NflowExecutedSinceNflow) metric).getCategoryAndNflow());
    }

    private class NflowOperationListener implements MetadataEventListener<NflowOperationStatusEvent> {

        @Override
        public void notify(NflowOperationStatusEvent event) {
            NflowOperation.State state = event.getData().getState();

            // TODO as precondition check criteria are not implemented yet, 
            // check all preconditions of nflows that have them.
            if (state == NflowOperation.State.SUCCESS) {
                metadata.read(() -> {
                    for (Nflow nflow : nflowProvider.getNflows()) {
                        // Don't check the precondition of the nflow that that generated this change event.
                        // TODO: this might not be the correct behavior but none of our current metrics
                        // need to be assessed when the nflow itself containing the precondition has changed state.
                        if (!nflow.getQualifiedName().equals(event.getData().getNflowName())) {
                            checkPrecondition(nflow, event.getData());
                        }
                    }
                    return null;
                }, MetadataAccess.SERVICE);
            }
        }
    }
}
