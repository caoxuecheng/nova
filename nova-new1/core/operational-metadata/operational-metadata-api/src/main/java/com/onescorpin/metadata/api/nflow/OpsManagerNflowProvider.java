package com.onescorpin.metadata.api.nflow;

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

import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.JobStatusCount;

import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provider interface for accessing/processing Nflows
 */
public interface OpsManagerNflowProvider {


    /**
     * Return the id representing the unique nflow identifier
     *
     * @return the unique nflow id
     */
    OpsManagerNflow.ID resolveId(Serializable id);

    /**
     * Find a nflow by its nflow name {@link OpsManagerNflow#getName()}
     *NOTE: This is Access Controlled and will show only those nflows a user has access to
     * @return the nflow
     */
    OpsManagerNflow findByName(String name);


    /**
     * Find a nflow by its nflow name {@link OpsManagerNflow#getName()}
     *NOTE: This is NOT Access Controlled and will show any nflow that matches
     * @return the nflow
     */
    OpsManagerNflow findByNameWithoutAcl(String name);

    /**
     * Find a nflow by its unique id
     * NOTE: This is Access Controlled and will show only those nflows a user has access to
     * @return the nflow
     */
    OpsManagerNflow findById(OpsManagerNflow.ID id);

    /**
     * Find all nflows matching a list of nflow ids
     *NOTE: This is Access Controlled and will show only those nflows a user has access to
     * @return the nflows matching the list of ids
     */
    List<? extends OpsManagerNflow> findByNflowIds(List<OpsManagerNflow.ID> ids);

    /**
     * Return all nflows
     * @return
     */
    List<? extends OpsManagerNflow> findAllWithoutAcl();

    /**
     * Find Nflows that have the same system name in the nova.NFLOW table
     * @return
     */
    List<? extends OpsManagerNflow> findNflowsWithSameName();

    /**
     *  Returns a list of all the nflow categorySystemName.nflowSystemName
     * NOTE: This is Access Controlled and will show only those nflows a user has access to
     * @return Returns a list of all the nflow categorySystemName.nflowSystemName
     */
    List<String> getNflowNames();


    /**
     * Get a Map of the category system name to list of nflows
     * NOTE: This is Access Controlled and will show only those nflows a user has access to
     * @return a Map of the category system name to list of nflows
     */
    public Map<String,List<OpsManagerNflow>> getNflowsGroupedByCategory();

    /**
     * Save a nflow
     */
    void save(List<? extends OpsManagerNflow> nflows);

    /**
     * Nflow Names to update the streaming flag
     * @param nflowNames set of category.nflow names
     * @param isStream true if stream, false if not
     */
    void updateStreamingFlag(Set<String> nflowNames, boolean isStream);


    /**
     * For Batch Nflows that may start many flowfiles/jobs at once in a short amount of time
     * we don't necessarily want to show all of those as individual jobs in ops manager as they may merge and join into a single ending flow.
     * For a flood of starting jobs if ops manager receives more than 1 starting event within this given interval it will supress the creation of the next Job
     * Set this to -1L or 0L to bypass and always create a job instance per starting flow file.
     * @param nflowNames a set of category.nflow names
     * @param timeBetweenBatchJobs  a time in millis to supress new job creation
     */
    void updateTimeBetweenBatchJobs(Set<String> nflowNames, Long timeBetweenBatchJobs);

    /**
     * save a nflow with a specific nflow id and name
     * This is used to save an initial record for a nflow when a nflow is created
     *
     * @return the saved nflow
     */
    OpsManagerNflow save(OpsManagerNflow.ID nflowManagerId, String systemName, boolean isStream, Long timeBetweenBatchJobs);

    /**
     * Delete a nflow and all of its operational metadata (i.e. jobs, steps, etc)
     */
    void delete(OpsManagerNflow.ID id);

    /**
     * Determine if a nflow is running
     *
     * @return true if the nflow is running a job now, false if not
     */
    boolean isNflowRunning(OpsManagerNflow.ID id);

    /**
     * Return summary health information about the nflows in the system
     *
     * @return summary health information about the nflows in the system
     */
    List<? extends NflowHealth> getNflowHealth();

    /**
     * Return summary health information about a specific nflow
     *
     * @return summary health information about a specific nflow
     */
    NflowHealth getNflowHealth(String nflowName);

    /**
     * Return job status count information for a given nflow and a timeframe grouped by day
     * Useful for generating timebased charts of job executions and their status by each day for a given nflow
     *
     * @param period time to look back from now
     * @return job status count information for a given nflow and a timeframe grouped by day
     */
    List<JobStatusCount> getJobStatusCountByDateFromNow(String nflowName, ReadablePeriod period);

    /**
     * find the latest job executions of the type {@link com.onescorpin.metadata.api.nflow.OpsManagerNflow.NflowType#CHECK}
     */
    List<? extends LatestNflowJobExecution> findLatestCheckDataJobs();

    /**
     * change the {@link BatchJobExecution#getStatus()} of all {@link com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution.JobStatus#FAILED} Jobs to be {@link
     * com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution.JobStatus#ABANDONED}
     */
    void abandonNflowJobs(String nflowName);

    List<? extends NflowSummary> findNflowSummary();

    DateTime getLastActiveTimeStamp(String nflowName);
}
