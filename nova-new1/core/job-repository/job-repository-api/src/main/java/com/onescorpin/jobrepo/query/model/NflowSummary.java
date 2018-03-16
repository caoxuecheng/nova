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


/**
 * Represents summary data for a nflow and Batch Job Execution status
 */
public interface NflowSummary {

    /**
     * Return the nflow name
     */
    String getNflow();

    /**
     * Return the state of the nflow (Waiting, Running)
     */
    String getState();

    /**
     * Return the last Nflow status
     */
    String getLastStatus();

    /**
     * flag indicating if the job is idle/waiting
     *
     * @return true if waiting, false if not
     */
    boolean isWaiting();

    /**
     * flag to indicate if the job is running
     *
     * @return true if the job is running , false if not
     */
    boolean isRunning();

    /**
     * return the time since this nflow finished
     *
     * @return the time in millis since the nflow has last finished
     */
    Long getTimeSinceEndTime();

    /**
     * format the millis to a readable time
     *
     * @return a formatted time of the {@link #getTimeSinceEndTime()}
     */
    String formatTimeMinSec(Long millis);

    /**
     * Return a formatted string of the {@link #getTimeSinceEndTime()}
     *
     * @return a formatted string of the {@link #getTimeSinceEndTime()}
     */
    String getTimeSinceEndTimeString();

    /**
     * Return time, in millis, of the run time for the last job of this nflow
     *
     * @return time, in millis, of the run time for the last job of this nflow
     */
    Long getRunTime();

    /**
     * Return a formatted string of the {@link #getRunTime()}
     *
     * @return a formatted string of the {@link #getRunTime()}
     */
    String getRunTimeString();

    /**
     * Return the average completion time, in millis, for the jobs on this nflow
     *
     * @return the average completion time, in millis, for the jobs on this nflow
     */
    Long getAvgCompleteTime();

    /**
     * Return a formatted string of the {@link #getAvgCompleteTime()}
     */
    String getAvgCompleteTimeString();

    /**
     * Return boolean if this nflow is healthy
     *
     * @return true if healthy, false if not
     */
    boolean isHealthy();

    /**
     * Return the last exit code on from the last job execution for this nflow
     *
     * @return the last exit code on from the last job execution for this nflow
     */
    String getLastExitCode();

    /**
     * Return the NflowHealth object for this nflow
     *
     * @return the NflowHealth object for this nflow
     */
    NflowHealth getNflowHealth();

    /**
     *
     * @return true if streaming nflow
     */
    boolean isStream();

}
