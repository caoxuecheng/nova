package com.onescorpin.metadata.api.sla;

/*-
 * #%L
 * onescorpin-operational-metadata-api
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

import com.onescorpin.common.velocity.model.VelocityTemplate;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementDescription;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Created by sr186054 on 7/24/17.
 */
public interface ServiceLevelAgreementDescriptionProvider {

    /**
     * Updates the Service Level Agreement (SLA) JPA mapping and its relationship to Nflows
     * @param slaId the SLA id
     * @param name the SLA Name
     * @param description the SLA Description
     * @param nflows a set of Nflow Ids related to this SLA
     */
    void updateServiceLevelAgreement(ServiceLevelAgreement.ID slaId, String name, String description, Set<Nflow.ID> nflows, Set<VelocityTemplate.ID> velocityTemplates);

    ServiceLevelAgreement.ID resolveId(Serializable ser);

    List< ? extends ServiceLevelAgreementDescription> findForNflow(OpsManagerNflow.ID nflowId);

    List<ServiceLevelAgreementDescription> findAll();

    ServiceLevelAgreementDescription findOne(ServiceLevelAgreement.ID id);
}
