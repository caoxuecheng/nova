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


import com.onescorpin.metadata.api.jobrepo.job.BatchAndStreamingJobStatusCount;

/**
 * Hold counts of what is running for a given nflow including streaming nflow counts
 */
public class JpaBatchAndStreamingJobStatusCounts extends JpaBatchJobExecutionStatusCounts implements BatchAndStreamingJobStatusCount {

    private boolean isStream;
    private Long runningNflowFlows;

    public JpaBatchAndStreamingJobStatusCounts() {
        super();
    }


    public JpaBatchAndStreamingJobStatusCounts(String status, String nflowName, Integer year, Integer month, Integer day, Long count) {
        super(status, nflowName, year, month, day, count);
    }

    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean stream) {
        isStream = stream;
    }

    public Long getRunningNflowFlows() {
        return runningNflowFlows;
    }

    public void setRunningNflowFlows(Long runningNflowFlows) {
        this.runningNflowFlows = runningNflowFlows;
    }
}

