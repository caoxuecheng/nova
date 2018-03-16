package com.onescorpin.metadata.api.jobrepo.job;

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

import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTO;

import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

/**
 * Provider for accessing and creating {@link BatchJobExecution}
 */
public interface BatchJobExecutionProvider extends BatchJobExecutionFilters {

    /**
     * Execution Context key that determines the type of job {@link com.onescorpin.metadata.api.nflow.OpsManagerNflow.NflowType}
     * This is is deprecated and the {@link #NIFI_NOVA_JOB_TYPE_PROPERTY} should be used instead
     */
    @Deprecated
    String NIFI_JOB_TYPE_PROPERTY = "tb.jobType";

    /**
     * Execution Context key that determines the type of job {@link com.onescorpin.metadata.api.nflow.OpsManagerNflow.NflowType}
     */
    String NIFI_NOVA_JOB_TYPE_PROPERTY = "nova.jobType";

    String NOVA_ALERT_ID_PROPERTY = "Nova Alert Id";

    /**
     * Execution context property name that references the name of the Nflow on a job
     */
    String NIFI_NFLOW_PROPERTY = "nflow";

    /**
     * Execution context property name that references the name of the category on a job
     */
    String NIFI_CATEGORY_PROPERTY = "category";

    /**
     * Execution context property name that appends the value of this property to the overall {@link BatchJobExecution#getExitMessage()}
     */
    String NIFI_JOB_EXIT_DESCRIPTION_PROPERTY = "nova.jobExitDescription";


    /**
     * Create a new job instance record for a provenance event
     *
     * @return the job execution
     */
    BatchJobInstance createJobInstance(ProvenanceEventRecordDTO event);

    /**
     * save the Provenance event and return the associated job execution
     *
     * @return the job execution
     */
    BatchJobExecution save(ProvenanceEventRecordDTO event, OpsManagerNflow nflow);

    /**
     * save the Provenance event and return the associated job execution
     *
     * @return the job execution
     */
    BatchJobExecution save(BatchJobExecution jobExecution, ProvenanceEventRecordDTO event);

    /**
     * find a job exeuction by its unique key
     *
     * @return the job execution
     */
    BatchJobExecution findByJobExecutionId(Long jobExecutionId);

    /**
     * save/update a job execution
     *
     * @return the updated job execution
     */
    BatchJobExecution save(BatchJobExecution jobExecution);


    /**
     * Abandons the Job
     * @param executionId the job id
     * @return the JobExecution
     */
    BatchJobExecution abandonJob(Long executionId);

    /**
     * find or create the job execution from the provenance event
     * This will create a new job execution if one does not exist for this event using the {@link ProvenanceEventRecordDTO#jobFlowFileId}
     *
     * @param event a provenance event
     * @param nflow  the nflow related to this event
     * @return the job execution
     */
    BatchJobExecution getOrCreateJobExecution(ProvenanceEventRecordDTO event, OpsManagerNflow nflow);

    /**
     * after a job execution is committed and returned this is called to store the job and its id back in a map for future reference
     */
    void updateNflowJobStartTime(BatchJobExecution jobExecution,OpsManagerNflow nflow);

    /**
     * find the job execution from the provenance event
     *
     * @param event a provenance event
     * @return the job execution
     */
    BatchJobExecution findJobExecution(ProvenanceEventRecordDTO event);

    /**
     * Returns all completed JobExecution records that were started since {@code sinceDate}
     *
     * @return the set of job executions
     */
    Set<? extends BatchJobExecution> findJobsForNflowCompletedSince(String nflowName, DateTime sinceDate);


    /**
     * Find all the running jobs for a nflow
     * @param nflowName the nflow name
     * @return a set of the jobs that are running/executing
     */
    List<? extends BatchJobExecution> findRunningJobsForNflow(String nflowName);

    /**
     * Returns the latest completed job execution for a nflow
     *
     * @return the job execution
     */
    BatchJobExecution findLatestCompletedJobForNflow(String nflowName);

    /**
     * Returns the latest job execution of any status for a nflow
     *
     * @return the job execution
     */
    BatchJobExecution findLatestJobForNflow(String nflowName);


    /**
     * Returns the latest job that is finished for a nflow
     * @param nflowName the nflow
     * @return the finished job
     */
    BatchJobExecution findLatestFinishedJobForNflow(String nflowName);

    /**
     * check if a nflow is running
     *
     * @return true if running, false if not
     */
    Boolean isNflowRunning(String nflowName);


    /**
     * when a streaming nflow indicates there is no new activity it needs to set the job for the nflow to STOPPED
     * @param nflow
     */
    void markStreamingNflowAsStopped(String nflow);

    void markStreamingNflowAsStarted(String nflow);

    /**
     * find all job executions matching a particular filter string, returning a paged result set
     *
     * @return a paged result set of job executions matching the filter and pageable criteria
     */
    Page<? extends BatchJobExecution> findAll(String filter, Pageable pageable);

    /**
     * Return a list of job status objects grouped by day
     *
     * @return a list of job status objects grouped by day
     */
    List<JobStatusCount> getJobStatusCountByDate();

    /**
     * Return a list of job status objects grouped by day using a supplied filter and looking back a specific period
     *
     * @return a list of job status objects grouped by day since the passed in period
     */
    List<JobStatusCount> getJobStatusCountByDateFromNow(ReadablePeriod period, String filter);

    /**
     * Return a list of job status objects matching a specific filter
     *
     * @param filter a filter string
     * @return a list of job status objects matching a specific filter
     */
    List<JobStatusCount> getJobStatusCount(String filter);


    List<BatchAndStreamingJobStatusCount> getBatchAndStreamingJobCounts(String filter);

    /**
     * Find all flowFiles that are related to the supplied flow file
     *
     * @param flowFileId a flowfile id
     * @return a list of related flowfile ids
     */
    List<String> findRelatedFlowFiles(String flowFileId);

    void notifyFailure(BatchJobExecution jobExecution, String nflowName, boolean isStream,String status);

    void notifyFailure(BatchJobExecution jobExecution, OpsManagerNflow nflow, boolean isStream, String status);

    void notifyStopped(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status);

    void notifySuccess(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status);

    void notifyBatchToStream(BatchJobExecution jobExecution, OpsManagerNflow nflow);

    void notifyStreamToBatch(BatchJobExecution jobExecution, OpsManagerNflow nflow);

}
