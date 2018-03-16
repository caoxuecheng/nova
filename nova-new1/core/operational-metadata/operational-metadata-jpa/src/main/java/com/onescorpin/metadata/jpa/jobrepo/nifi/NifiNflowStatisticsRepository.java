package com.onescorpin.metadata.jpa.jobrepo.nifi;

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
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring data repository for {@link JpaNifiNflowStats}
 */
public interface NifiNflowStatisticsRepository extends JpaRepository<JpaNifiNflowStats, String>, QueryDslPredicateExecutor<JpaNifiNflowStats> {

    @Query(value = "select distinct stats from JpaNifiNflowStats as stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW + " and (" + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH + ") "
                   + "where stats.nflowName = :nflowName ")
    JpaNifiNflowStats findLatestForNflowWithAcl(@Param("nflowName") String nflowName);

    @Query(value = "select distinct stats from JpaNifiNflowStats as stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + "where stats.nflowName = :nflowName ")
    JpaNifiNflowStats findLatestForNflowWithoutAcl(@Param("nflowName") String nflowName);

    @Query(value = "select distinct stats from JpaNifiNflowStats as stats "
                   + "where stats.nflowName = :nflowName ")
    List<JpaNifiNflowStats> findForNflowWithoutAcl(@Param("nflowName") String nflowName);

    @Query(value = "select distinct stats from JpaNifiNflowStats stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW + " and (" + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH + ") "
                   + "where nflow.isStream = true ")
    List<JpaNifiNflowStats> findStreamingNflowStatsWithAcl();

    @Query(value = "select distinct stats from JpaNifiNflowStats stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + "where nflow.isStream = true ")
    List<JpaNifiNflowStats> findStreamingNflowStatsWithoutAcl();

    @Query(value = "select distinct stats from JpaNifiNflowStats stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW + " and (" + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH + ") ")
    List<JpaNifiNflowStats> findNflowStatsWithAcl();

    @Query(value = "select distinct stats from JpaNifiNflowStats stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName ")
    List<JpaNifiNflowStats> findNflowStatsWithoutAcl();


}
