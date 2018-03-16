/**
 *
 */
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

import com.onescorpin.jobrepo.query.model.ExecutedJob;
import com.onescorpin.metadata.api.op.NflowOperation;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
@SuppressWarnings("serial")
public class NflowOperationExecutedJobWrapper implements NflowOperation {

    private final OpId id;
    private final ExecutedJob executed;


    public NflowOperationExecutedJobWrapper(ExecutedJob job) {
        this.id = new OpId(job.getExecutionId());
        this.executed = job;
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
        return this.executed.getCreateTime();
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
        return this.executed.getDisplayStatus();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperation#getResults()
     */
    @Override
    public Map<String, Object> getResults() {
        return Stream.of(this.executed.getExecutionContext(), this.executed.getJobParameters())
            .flatMap(s -> s.entrySet().stream())
            .collect(Collectors.toMap(e -> e.getKey(),
                                      e -> e.getValue(),
                                      (v1, v2) -> v1));
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
