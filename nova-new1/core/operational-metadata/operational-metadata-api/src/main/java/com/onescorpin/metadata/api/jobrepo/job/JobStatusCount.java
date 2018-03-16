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

import org.joda.time.DateTime;


/**
 * Represents summary data about a job as it pertains to a specific date
 */
public interface JobStatusCount {

    /**
     * Return the total records for the specific date/status/nflow/job
     */
    Long getCount();

    /**
     * set the total record count
     */
    void setCount(Long count);

    String getNflowId();


    void setNflowId(String nflowId);

    /* 
     * Return the nflow name
     * @return the nflow name
     */
    String getNflowName();

    /**
     * set the nflow name
     */
    void setNflowName(String nflowName);

    /**
     * return the name of the job
     *
     * @return the name of the job
     */
    String getJobName();

    /**
     * set the name of the job
     */
    void setJobName(String jobName);

    /**
     * Return the status for this grouping
     *
     * @return the job status
     */
    String getStatus();

    /**
     * set the status
     */
    void setStatus(String status);

    /**
     * Return the DateTime for this grouping
     *
     * @return the date time
     */
    DateTime getDate();

    /**
     * set the datetime
     */
    void setDate(DateTime date);

}
