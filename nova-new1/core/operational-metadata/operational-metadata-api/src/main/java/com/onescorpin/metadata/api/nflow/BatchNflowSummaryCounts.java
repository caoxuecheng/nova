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


/**
 * Represents summary data about Batch related jobs for a nflow
 */
public interface BatchNflowSummaryCounts {

    /**
     * Return the Nflow
     *
     * @return the nflow
     */
    OpsManagerNflow getNflow();

    /**
     * Return the nflow id0
     *
     * @return the nflowid
     */
    OpsManagerNflow.ID getNflowId();

    /**
     * Return the nflow name
     *
     * @return the nflow name
     */
    String getNflowName();

    /**
     * Return a count of all the batch jobs executed for this nflow
     *
     * @return a count of all the batch jobs executed for this nflow
     */
    Long getAllCount();

    /**
     * Return a count of all the batch jobs that have failed for this nflow
     *
     * @return a count of all the batch jobs that have failed for this nflow
     */
    Long getFailedCount();

    /**
     * Return a count of all the batch jobs that have completed for this nflow
     *
     * @return a count of all the batch jobs that have completed for this nflow
     */
    Long getCompletedCount();

    /**
     * Return a count of all the batch jobs that have been abandoned for this nflow
     *
     * @return a count of all the batch jobs that have been abandoned for this nflow
     */
    Long getAbandonedCount();
}
