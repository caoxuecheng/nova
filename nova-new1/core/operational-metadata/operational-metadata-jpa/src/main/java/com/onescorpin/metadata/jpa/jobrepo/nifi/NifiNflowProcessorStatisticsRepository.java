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

import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats;
import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;

import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring data repository for {@link JpaNifiNflowProcessorStats}
 * A custom repository is used to call the stored procedure
 */
public interface NifiNflowProcessorStatisticsRepository extends JpaRepository<JpaNifiNflowProcessorStats, String>, QueryDslPredicateExecutor<JpaNifiNflowProcessorStats>, NifiNflowProcessorStatisticsRepositoryCustom
                                                                {

    @Query(value = "select distinct stats from JpaNifiNflowProcessorStats as stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
                   + " where stats.minEventTime between :startTime and :endTime "
                   + "and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<JpaNifiNflowProcessorStats> findWithinTimeWindowWithAcl(@Param("startTime") DateTime start, @Param("endTime") DateTime end);

    @Query(value = "select distinct stats from JpaNifiNflowProcessorStats as stats "
                   + "where stats.minEventTime between :startTime and :endTime ")
    List<JpaNifiNflowProcessorStats> findWithinTimeWindowWithoutAcl(@Param("startTime") DateTime start, @Param("endTime") DateTime end);

    @Query(value = "select max(stats.maxEventId) from JpaNifiNflowProcessorStats as stats")
    Long findMaxEventId();

    @Query(value = "select max(stats.maxEventId) from JpaNifiNflowProcessorStats as stats where stats.clusterNodeId = :clusterNodeId")
    Long findMaxEventId(@Param("clusterNodeId") String clusterNodeId);


    @Query(value = "select distinct stats from JpaNifiNflowProcessorStats as stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + " and nflow.name = :nflowName "
                   + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
                   + " where stats.minEventTime between :startTime and :endTime "
                   + "and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH
                   + " and stats.errorMessages is not null ")
    List<JpaNifiNflowProcessorStats> findWithErrorsWithinTimeWithAcl(@Param("nflowName") String nflowName, @Param("startTime") DateTime start, @Param("endTime") DateTime end);


    @Query(value = "select distinct stats from JpaNifiNflowProcessorStats as stats "
                   + " where stats.nflowName = :nflowName "
                   + " and stats.minEventTime between :startTime and :endTime "
                   + " and stats.errorMessages is not null ")
    List<JpaNifiNflowProcessorStats> findWithErrorsWithinTimeWithoutAcl(@Param("nflowName") String nflowName, @Param("startTime") DateTime start, @Param("endTime") DateTime end);

    @Query(value = "select distinct stats from JpaNifiNflowProcessorStats as stats "
                   + "join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
                   + " and nflow.name = :nflowName"
                   + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
                   + " where stats.minEventTime > :afterTimestamp "
                   + "and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH
                   + " and stats.errorMessages is not null ")
    List<JpaNifiNflowProcessorStats> findWithErrorsAfterTimeWithAcl(@Param("nflowName") String nflowName, @Param("afterTimestamp") DateTime afterTimestamp);

    @Query(value = "select distinct stats from JpaNifiNflowProcessorStats as stats "
                   + " where stats.nflowName = :nflowName"
                   + " and stats.minEventTime > :afterTimestamp "
                   + " and stats.errorMessages is not null ")
    List<JpaNifiNflowProcessorStats> findWithErrorsAfterTimeWithoutAcl(@Param("nflowName") String nflowName, @Param("afterTimestamp") DateTime afterTimestamp);


    @Query("select stats from JpaNifiNflowProcessorStats as stats "
           + " join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + " where stats.minEventTime = :latestTime "
           + " and stats.nflowName = :nflowName "
           + " and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<NifiNflowProcessorStats> findLatestFinishedStatsWithAcl(@Param("nflowName") String nflowName, @Param("latestTime") DateTime latestTime);

    @Query("select stats from JpaNifiNflowProcessorStats as stats "
           + " where stats.minEventTime = :latestTime "
           + " and stats.nflowName = :nflowName ")
    List<NifiNflowProcessorStats> findLatestFinishedStatsWithoutAcl(@Param("nflowName") String nflowName, @Param("latestTime") DateTime latestTime);

    @Query("select max(stats.minEventTime) as dateProjection from JpaNifiNflowProcessorStats as stats "
           + " join JpaOpsManagerNflow as nflow on nflow.name = stats.nflowName "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + " where stats.nflowName = :nflowName "
           + " and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    DateProjection findLatestFinishedTimeWithAcl(@Param("nflowName") String nflowName);

    @Query("select max(stats.minEventTime) as dateProjection from JpaNifiNflowProcessorStats as stats "
           + " where stats.nflowName = :nflowName")
    DateProjection findLatestFinishedTimeWithoutAcl(@Param("nflowName") String nflowName);

  //  @Procedure(name = "NifiNflowProcessorStats.compactNflowProcessorStats")
  //  String compactNflowProcessorStatistics();




}
