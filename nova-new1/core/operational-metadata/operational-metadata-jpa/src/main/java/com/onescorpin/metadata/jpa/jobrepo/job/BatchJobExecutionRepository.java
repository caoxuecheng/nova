package com.onescorpin.metadata.jpa.jobrepo.job;

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


import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.jpa.nflow.RepositoryType;
import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;

import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * Spring data repository for accessing {@link JpaBatchJobExecution}
 */
@RepositoryType(BatchJobExecutionSecuringRepository.class)
public interface BatchJobExecutionRepository extends JpaRepository<JpaBatchJobExecution, Long>, QueryDslPredicateExecutor<JpaBatchJobExecution> {

    @Query(value = "select distinct job from JpaBatchJobExecution as job "
                   + "join JpaNifiEventJobExecution as nifiEventJob on nifiEventJob.jobExecution.jobExecutionId = job.jobExecutionId  "
                   + "where nifiEventJob.flowFileId = :flowFileId")
    JpaBatchJobExecution findByFlowFile(@Param("flowFileId") String flowFileId);

    @Query(value = "select job from JpaBatchJobExecution as job "
                   + "join JpaBatchJobInstance jobInstance on jobInstance.id = job.jobInstance.id "
                   + "join JpaOpsManagerNflow nflow on nflow.id = jobInstance.nflow.id "
                   + "left join fetch JpaBatchJobExecutionContextValue executionContext on executionContext.jobExecutionId = job.jobExecutionId "
                   + "where nflow.name = :nflowName "
                   + "and job.status = 'COMPLETED' "
                   + "and job.endTime > :sinceDate ")
    Set<JpaBatchJobExecution> findJobsForNflowCompletedSince(@Param("nflowName") String nflowName, @Param("sinceDate") DateTime sinceDate);

    @Query("select job from JpaBatchJobExecution as job "
           + "join JpaBatchJobInstance  jobInstance on jobInstance.jobInstanceId = job.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow on nflow.id = jobInstance.nflow.id "
           + "where nflow.name = :nflowName "
           + "and job.endTimeMillis = (SELECT max(job2.endTimeMillis)"
           + "     from JpaBatchJobExecution as job2 "
           + "join JpaBatchJobInstance  jobInstance2 on jobInstance2.jobInstanceId = job2.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow2 on nflow2.id = jobInstance2.nflow.id "
           + "where nflow2.name = :nflowName "
           + "and job2.status = 'COMPLETED')"
           + "order by job.jobExecutionId DESC ")
    List<JpaBatchJobExecution> findLatestCompletedJobForNflow(@Param("nflowName") String nflowName);


    @Query("select job from JpaBatchJobExecution as job "
           + "join JpaBatchJobInstance  jobInstance on jobInstance.jobInstanceId = job.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow on nflow.id = jobInstance.nflow.id "
           + "where nflow.name = :nflowName "
           + "and job.endTimeMillis = (SELECT max(job2.endTimeMillis)"
           + "     from JpaBatchJobExecution as job2 "
           + "join JpaBatchJobInstance  jobInstance2 on jobInstance2.jobInstanceId = job2.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow2 on nflow2.id = jobInstance2.nflow.id "
           + "where nflow2.name = :nflowName )"
           + "order by job.jobExecutionId DESC ")
    List<JpaBatchJobExecution> findLatestFinishedJobForNflow(@Param("nflowName") String nflowName);

    @Query("select job from JpaBatchJobExecution as job "
           + "join JpaBatchJobInstance  jobInstance on jobInstance.jobInstanceId = job.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow on nflow.id = jobInstance.nflow.id "
           + "where nflow.name = :nflowName "
           + "and job.startTimeMillis = (SELECT max(job2.startTimeMillis)"
           + "     from JpaBatchJobExecution as job2 "
           + "join JpaBatchJobInstance  jobInstance2 on jobInstance2.jobInstanceId = job2.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow2 on nflow2.id = jobInstance2.nflow.id "
           + "where nflow2.name = :nflowName )"
           + "order by job.jobExecutionId DESC ")
    List<JpaBatchJobExecution> findLatestJobForNflow(@Param("nflowName") String nflowName);


    @Query("select job from JpaBatchJobExecution as job "
           + "join JpaBatchJobInstance  jobInstance on jobInstance.jobInstanceId = job.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow on nflow.id = jobInstance.nflow.id "
           + "where nflow.name = :nflowName "
           + "and job.startTimeMillis = (SELECT max(job2.startTimeMillis)"
           + "     from JpaBatchJobExecution as job2 "
           + "join JpaBatchJobInstance  jobInstance2 on jobInstance2.jobInstanceId = job2.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow2 on nflow2.id = jobInstance2.nflow.id "
           + "where nflow2.name = :nflowName "
           + "and job2.startTimeMillis >= :startTime)"
           + "order by job.jobExecutionId DESC ")
    List<JpaBatchJobExecution> findLatestJobForNflowWithStartTimeLimit(@Param("nflowName") String nflowName, @Param("startTime") Long startTime);

    @Query("select job from JpaBatchJobExecution as job "
           + "join JpaBatchJobInstance  jobInstance on jobInstance.jobInstanceId = job.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow on nflow.id = jobInstance.nflow.id "
           + "where nflow.name = :nflowName "
           + "and job.status in (:jobStatus) ")
    List<JpaBatchJobExecution> findJobsForNflowMatchingStatus(@Param("nflowName") String nflowName, @Param("jobStatus")BatchJobExecution.JobStatus... jobStatus );


    @Query("select job from JpaBatchJobExecution as job "
           + "join JpaBatchJobInstance  jobInstance on jobInstance.jobInstanceId = job.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow on nflow.id = jobInstance.nflow.id "
           + "where nflow.name = :nflowName "
           + "and job.startTimeMillis = (SELECT max(job2.startTimeMillis)"
           + "     from JpaBatchJobExecution as job2 "
           + "join JpaBatchJobInstance  jobInstance2 on jobInstance2.jobInstanceId = job2.jobInstance.jobInstanceId "
           + "join JpaOpsManagerNflow  nflow2 on nflow2.id = jobInstance2.nflow.id "
           + "where job2.endTime is null "
           + "and nflow2.name = :nflowName )"
           + "order by job.jobExecutionId DESC ")
    List<JpaBatchJobExecution> findLatestRunningJobForNflow(@Param("nflowName") String nflowName);

    @Query(value = "select case when(count(job)) > 0 then true else false end "
                   + " from JpaBatchJobExecution as job "
                   + "join JpaBatchJobInstance  jobInstance on jobInstance.jobInstanceId = job.jobInstance.jobInstanceId "
                   + "join JpaOpsManagerNflow  nflow on nflow.id = jobInstance.nflow.id "
                   + "where nflow.name = :nflowName "
                   + "and job.endTime is null")
    Boolean isNflowRunning(@Param("nflowName") String nflowName);
}
