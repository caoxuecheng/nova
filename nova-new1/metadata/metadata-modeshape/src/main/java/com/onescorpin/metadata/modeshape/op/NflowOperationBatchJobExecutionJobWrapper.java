package com.onescorpin.metadata.modeshape.op;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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
import com.onescorpin.metadata.api.op.NflowOperation;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Wrapper object to overlay the BatchJobExecution object to allow interaction with the JobExecution data against other metadata operations like Precondition Assessments
 */
public class NflowOperationBatchJobExecutionJobWrapper implements NflowOperation {

    private final OpId id;
    private final BatchJobExecution executed;

    NflowOperationBatchJobExecutionJobWrapper(BatchJobExecution jobExecution) {
        this.id = new OpId(jobExecution.getJobExecutionId());
        this.executed = jobExecution;
    }


    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperation#getId()
     */
    @Override
    public ID getId() {
        return this.id;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperation#getStartTime()
     */
    @Override
    public DateTime getStartTime() {
        return this.executed.getStartTime();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperation#getStopTime()
     */
    @Override
    public DateTime getStopTime() {
        return this.executed.getEndTime();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperation#getState()
     */
    @Override
    public State getState() {
        return JobRepoNflowOperationsProvider.asOperationState(this.executed.getStatus());
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperation#getStatus()
     */
    @Override
    public String getStatus() {
        return this.executed.getStatus().name();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperation#getResults()
     */
    @Override
    public Map<String, Object> getResults() {
        Map<String, Object> results = new HashMap<>();
        results.putAll(this.executed.getJobExecutionContextAsMap());
        return results;
    }


    protected static class OpId implements NflowOperation.ID {

        private final String idValue;

        public OpId(Serializable value) {
            this.idValue = value.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass().isAssignableFrom(obj.getClass())) {
                OpId that = (OpId) obj;
                return Objects.equals(this.idValue, that.idValue);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), this.idValue);
        }

        @Override
        public String toString() {
            return this.idValue;
        }
    }

}
