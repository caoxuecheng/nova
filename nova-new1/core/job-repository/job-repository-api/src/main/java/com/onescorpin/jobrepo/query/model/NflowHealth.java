package com.onescorpin.jobrepo.query.model;

/*-
 * #%L
 * onescorpin-job-repository-api
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

import java.util.Date;

/**
 * Summary object indicating the health of a nflow
 */
public interface NflowHealth {

    /**
     * Return a count of jobs for this nflow that are healthy (either Successful, or had their Failures (if any) handled and "Abandoned")
     *
     * @return a count of the healthy jobs on this nflow
     */
    Long getHealthyCount();

    /**
     * set the count of healthy jobs for this nflow
     */
    void setHealthyCount(Long healthyCount);

    /**
     * Return a count of the jobs that are unhealthy (Failed, for this nflow)
     * @return the count of unhealthy jobs for this nflow
     */
    Long getUnhealthyCount();

    /**
     * set the unhealthy job count for this nflow
     */
    void setUnhealthyCount(Long unhealthyCount);


    /**
     * The nflow Id
     * @return the nflow id
     */
    String getNflowId();

    /**
     *
     * set the nflow id
     */
    void setNflowId(String nflowId);

    /**
     * Return the nflow name
     *
     * @return the nflow name
     */
    String getNflow();

    /**
     * set the nflow name
     */
    void setNflow(String nflow);

    /**
     * Return the latest job execution for this nflow
     *
     * @return the latest job execution for this fed
     */
    ExecutedNflow getLastOpNflow();

    /**
     * set the latest job execution for this nflow
     */
    void setLastOpNflow(ExecutedNflow lastOpNflow);

    /**
     * Return the average runtime for this nflow
     *
     * @return the average runtime, in millis, for this nflow
     */
    Long getAvgRuntime();

    /**
     * Set the average runtime, in millis, for this nflow
     */
    void setAvgRuntime(Long avgRuntime);

    /**
     * Return a date indicating the last time this nflow was unhealthy
     *
     * @return the date this nflow was last unhealthy
     */
    Date getLastUnhealthyTime();

    /**
     * set the date this nflow was last unhealthy
     */
    void setLastUnhealthyTime(Date lastUnhealthyTime);

    /**
     * Return true if healthy, false if not
     *
     * @return true if healthy, false if not
     */
    boolean isHealthy();

    /**
     * For a given Nflow, return the {@link STATE}
     *
     * @return the status of the nflow
     */
    String getNflowState(ExecutedNflow nflow);

    /**
     * Return the  {@link STATE}  of the latest job for this nflow
     *
     * @return the  {@link STATE}  of the latest job for this nflow
     */
    String getLastOpNflowState();


    /**
     *
     * @return true if streaming nflow
     */
    boolean isStream();

    /**
     * Set the streaming flag
     * @param stream true if stream, false if not
     */
    void setStream(boolean stream);


    /**
     *
     * @return the number of jobs running for the nflow
     */
    Long getRunningCount();

    /**
     * set the running count for this nflow
     * @param runningCount the number of jobs running for this nflow
     */
    void setRunningCount(Long runningCount);

    /**
     * State indicating if the Nflow has jobs Waiting (idle) or Running
     */
    enum STATE {
        WAITING, RUNNING
    }
}
