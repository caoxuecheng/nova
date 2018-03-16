package com.onescorpin.nflowmgr.sla;

/*-
 * #%L
 * onescorpin-nflow-manager-service-api
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

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementActionValidation;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementDescription;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementEmailTemplate;

import java.util.List;

/**
 * Created by sr186054 on 7/23/17.
 */
public interface ServiceLevelAgreementService {


    List<ServiceLevelAgreementRule> discoverSlaMetrics();

    List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> getServiceLevelAgreements();

    void enableServiceLevelAgreementSchedule(Nflow.ID nflowId);

    void unscheduleServiceLevelAgreement(Nflow.ID nflowId,String categoryAndNflowName);

    void disableServiceLevelAgreementSchedule(Nflow.ID nflowId);

    boolean hasServiceLevelAgreements(Nflow.ID id );

    List<com.onescorpin.metadata.rest.model.sla.NflowServiceLevelAgreement> getNflowServiceLevelAgreements(String nflowId);

    boolean canEditServiceLevelAgreement(String slaId);

    boolean canAccessServiceLevelAgreement(String slaId);

    ServiceLevelAgreement getServiceLevelAgreement(String slaId);

    ServiceLevelAgreementGroup getServiceLevelAgreementAsFormObject(String slaId);

    /**
     * Remove and unschedule a given sla by its id
     * @param id
     * @return
     */
    boolean removeAndUnscheduleAgreement(String id);

    /**
     * Remove and Unschedule all SLA's for a nflowId
     * @param nflowId the nflow id
     */
    void removeAndUnscheduleAgreementsForNflow(Nflow.ID nflowId, String categoryAndNflowName);

    boolean removeAllAgreements();

    List<ServiceLevelAgreementActionUiConfigurationItem> discoverActionConfigurations();

    List<ServiceLevelAgreementActionValidation> validateAction(String actionConfigurationClassName);

    ServiceLevelAgreement saveAndScheduleSla(ServiceLevelAgreementGroup serviceLevelAgreement);

    ServiceLevelAgreement saveAndScheduleNflowSla(ServiceLevelAgreementGroup serviceLevelAgreement, String nflowId);

    List<SimpleServiceLevelAgreementDescription> getSlaReferencesForVelocityTemplate(String velocityTemplateId);

    ServiceLevelAgreementEmailTemplate saveEmailTemplate(ServiceLevelAgreementEmailTemplate emailTemplate);

    List<ServiceLevelAgreementEmailTemplate> getServiceLevelAgreementEmailTemplates();
}
