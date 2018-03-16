package com.onescorpin.metadata.api.sla;

/*-
 * #%L
 * onescorpin-metadata-api
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
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

import java.util.List;
import java.util.Set;

/**
 */
public interface NflowServiceLevelAgreementProvider {

    /**
     * Find all SLAs and if they have any Nflow Relationships also add those into the resulting list
     */
    List<NflowServiceLevelAgreement> findAllAgreements();

    /**
     * Find the SLA and get the list of Nflows it is related to
     */
    NflowServiceLevelAgreement findAgreement(ServiceLevelAgreement.ID slaId);

    /**
     * Find all agreements associated to a given Nflow
     */
    List<NflowServiceLevelAgreement> findNflowServiceLevelAgreements(Nflow.ID nflowId);

    /**
     * relate an SLA to a set of Nflows
     */
    NflowServiceLevelAgreementRelationship relate(ServiceLevelAgreement sla, Set<Nflow.ID> nflowIds);

    /**
     * relate an SLA to a set of Nflows
     */
    NflowServiceLevelAgreementRelationship relateNflows(ServiceLevelAgreement sla, Set<Nflow> nflows);

    /**
     * Cleanup and remove Nflow Relationships on an SLA
     */
    boolean removeNflowRelationships(ServiceLevelAgreement.ID id);

    boolean removeAllRelationships(ServiceLevelAgreement.ID id);
}
