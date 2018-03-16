package com.onescorpin.metadata.jpa.nflow;

/*-
 * #%L
 * onescorpin-operational-metadata-jpa
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

import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring data repository to access the {@link JpaOpsManagerNflowHealth}
 */
@RepositoryType(NflowHealthSecuringRepository.class)
public interface NflowHealthRepository extends JpaRepository<JpaOpsManagerNflowHealth, JpaOpsManagerNflowHealth.OpsManagerNflowHealthNflowId> {

    @Query("select distinct health from JpaOpsManagerNflowHealth as health "
           + "join JpaOpsManagerNflow as nflow on nflow.name = health.nflowName "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + "where health.nflowName =:nflowName"
           + " and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<JpaOpsManagerNflowHealth> findByNflowNameWithAcl(@Param("nflowName") String nflowName);

    @Query("select distinct health from JpaOpsManagerNflowHealth as health "
           + "join JpaOpsManagerNflow as nflow on nflow.name = health.nflowName "
           + "where health.nflowName =:nflowName")
    List<JpaOpsManagerNflowHealth> findByNflowNameWithoutAcl(@Param("nflowName") String nflowName);
}
