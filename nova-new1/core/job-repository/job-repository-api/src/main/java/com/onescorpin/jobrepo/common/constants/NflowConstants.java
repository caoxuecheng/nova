package com.onescorpin.jobrepo.common.constants;

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
 * Common Nflow Constants that are supplied as JobParameters
 *
 * @see com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionParameter for JobParameter key/value
 */
public interface NflowConstants {

    /**
     * job parameter name the identifies the nflow name associated with the job
     * {@link com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionParameter}
     **/
    String PARAM__NFLOW_NAME = "nflow"; // value is user defined String

    /**
     * job parameter name that identifies the type of job.  2 supported values for this parameter either "NFLOW", indicates a Nflow processing job, or "CHECK" indicates a data confidence check job for a
     * nflow {@link com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionParameter}
     */
    String PARAM__JOB_TYPE = "jobType"; // values defined below

    /**
     * the value for the "jobType" parameter above that indicates the Nflow processing job
     **/
    String PARAM_VALUE__JOB_TYPE_NFLOW = "NFLOW";

    /**
     * the value for the "jobType" parameter above that indicates the Data Confidence Check job
     **/
    String PARAM_VALUE__JOB_TYPE_CHECK = "CHECK";
}
