package com.onescorpin.jobrepo.query.model;

/*-
 * #%L
 * onescorpin-job-repository-core
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.onescorpin.jobrepo.query.support.NflowHealthUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Nflow Health built from the transform class
 *
 * @see com.onescorpin.jobrepo.query.model.transform.NflowModelTransform
 */
public class DefaultNflowHealth implements NflowHealth {

    private String nflowId;
    private String nflow;
    private ExecutedNflow lastOpNflow;
    private Long avgRuntime;
    private Long healthyCount;
    private Long unhealthyCount;
    private Date lastUnhealthyTime;
    private boolean isStream;
    private Long runningCount;


    @Override
    public String getNflowId() {
        return nflowId;
    }

    @Override
    public void setNflowId(String nflowId) {
        this.nflowId = nflowId;
    }

    @JsonIgnore
    public static List<NflowHealth> parseToList(List<ExecutedNflow> latestOpNflows, Map<String, Long> avgRunTimes) {
        return NflowHealthUtil.parseToList(latestOpNflows, avgRunTimes);

    }

    @Override
    public Long getHealthyCount() {
        return healthyCount;
    }

    @Override
    public void setHealthyCount(Long healthyCount) {
        this.healthyCount = healthyCount;
    }

    @Override
    public Long getUnhealthyCount() {

        if (unhealthyCount == null) {
            unhealthyCount = 0L;
        }
        return unhealthyCount;
    }

    @Override
    public void setUnhealthyCount(Long unhealthyCount) {
        this.unhealthyCount = unhealthyCount;
    }

    @Override
    public String getNflow() {
        return nflow;
    }

    @Override
    public void setNflow(String nflow) {
        this.nflow = nflow;
    }

    @Override
    public ExecutedNflow getLastOpNflow() {
        return lastOpNflow;
    }

    @Override
    public void setLastOpNflow(ExecutedNflow lastOpNflow) {
        this.lastOpNflow = lastOpNflow;
    }

    @Override
    public Long getAvgRuntime() {
        return avgRuntime;
    }

    @Override
    public void setAvgRuntime(Long avgRuntime) {
        this.avgRuntime = avgRuntime;
    }

    @Override
    public Date getLastUnhealthyTime() {
        return lastUnhealthyTime;
    }

    @Override
    public void setLastUnhealthyTime(Date lastUnhealthyTime) {
        this.lastUnhealthyTime = lastUnhealthyTime;
    }

    /**
     * Checks the last Processed Data Nflow and if it is not in a FAILED State, mark it Healthy
     */
    @Override
    public boolean isHealthy() {
     return getUnhealthyCount() == 0L;
    }

    @Override
    public String getNflowState(ExecutedNflow nflow) {
        STATE state = STATE.WAITING;
        if (nflow != null) {
            ExecutionStatus status = nflow.getStatus();
            if (ExecutionStatus.STARTED.equals(status) || ExecutionStatus.STARTING.equals(status)) {
                state = STATE.RUNNING;
            }
        }
        return state.name();
    }

    @Override
    public String getLastOpNflowState() {
        String state = getNflowState(lastOpNflow);
        return state;
    }

    @Override
    public Long getRunningCount() {
        return runningCount;
    }

    @Override
    public void setRunningCount(Long runningCount) {
        this.runningCount = runningCount;
    }

    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean stream) {
        isStream = stream;
    }
}
