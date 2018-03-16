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

import java.util.List;

/**
 * Represents summary data about nflows and their health in the system
 */
public interface NflowStatus {

    /**
     * set fields on this object
     */
    void populate();

    /**
     * Return the list of NflowHealth objects
     */
    List<NflowHealth> getNflows();

    /**
     * set the list of nflow health objects to summarize
     */
    void setNflows(List<NflowHealth> nflows);

    /**
     * Return a count of the healthy nflows
     *
     * @return a count of the healthy nflows
     */
    Integer getHealthyCount();

    /**
     * set the healthy nflow count
     */
    void setHealthyCount(Integer healthyCount);

    /**
     * Return a count of the failed nflows
     *
     * @return a count of the nflows that failed
     */
    Integer getFailedCount();

    /**
     * set the failed count
     */
    void setFailedCount(Integer failedCount);

    /**
     * Return a percent of the nflows that are healthy
     *
     * @return a precent of the nflows that are healthy
     */
    Float getPercent();

    /**
     * set the percent of healthy nflows
     */
    void setPercent(Integer percent);

    /**
     * Return a list of all the Healthy nflows
     *
     * @return a list of healthy nflows
     */
    List<NflowHealth> getHealthyNflows();

    /**
     * set the list of healthy nflows
     */
    void setHealthyNflows(List<NflowHealth> healthyNflows);

    /**
     * Return a list of unhealthy nflows
     *
     * @return a list of unhealthy nflows
     */
    List<NflowHealth> getFailedNflows();

    /**
     * set the unhealthy nflows
     */
    void setFailedNflows(List<NflowHealth> failedNflows);

    /**
     * Return a summary of the nflows
     */
    List<NflowSummary> getNflowSummary();

    /**
     * set the summar of the nflows
     */
    void setNflowSummary(List<NflowSummary> nflowSummary);
}
