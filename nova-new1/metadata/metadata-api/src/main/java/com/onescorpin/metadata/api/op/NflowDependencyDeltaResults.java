package com.onescorpin.metadata.api.op;

/*-
 * #%L
 * onescorpin-metadata-api
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

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
public class NflowDependencyDeltaResults {

    Map<String, List<NflowJobExecutionData>> nflowJobExecutionContexts = new HashMap<>();
    /**
     * Map storing the dependent nflowName and the latest completed Execution Context
     */
    Map<String, NflowJobExecutionData> latestNflowJobExecutionContext = new HashMap<>();
    /**
     * internal map to store jobexecution data
     */
    @JsonIgnore
    Map<Long, NflowJobExecutionData> jobExecutionDataMap = new HashMap<>();
    private String nflowName;
    private String nflowId;
    /**
     * An array of the dependentNflow system Names
     */
    private List<String> dependentNflowNames = new ArrayList<>();

    public NflowDependencyDeltaResults() {

    }


    public NflowDependencyDeltaResults(String nflowId, String nflowName) {
        this.nflowId = nflowId;
        this.nflowName = nflowName;
    }

    public void addNflowExecutionContext(String depNflowSystemName, Long jobExecutionId, DateTime startTime, DateTime endTime, Map<String, Object> executionContext) {
        if (!dependentNflowNames.contains(depNflowSystemName)) {
            dependentNflowNames.add(depNflowSystemName);
        }
        NflowJobExecutionData nflowJobExecutionData = jobExecutionDataMap.get(jobExecutionId);
        if (nflowJobExecutionData == null) {
            nflowJobExecutionData = new NflowJobExecutionData(jobExecutionId, startTime, endTime, executionContext);
            nflowJobExecutionContexts.computeIfAbsent(depNflowSystemName, nflowName -> new ArrayList<>()).add(nflowJobExecutionData);
            NflowJobExecutionData latest = latestNflowJobExecutionContext.get(depNflowSystemName);
            //update the latest pointer
            if (latest == null || (latest != null && endTime.isAfter(latest.getEndTime()))) {
                latestNflowJobExecutionContext.put(depNflowSystemName, nflowJobExecutionData);
            }
        } else {
            nflowJobExecutionData.getExecutionContext().putAll(executionContext);
        }

    }

    private void reduceExecutionContextToMatchingKeys(NflowJobExecutionData executionData, List<String> validKeys) {
        if (executionData != null && executionData.getExecutionContext() != null) {
            Map<String, Object> reducedMap = executionData.getExecutionContext().entrySet().stream().filter(e ->
                                                                                                                validKeys.stream()
                                                                                                                    .anyMatch(validKey -> e.getKey().toLowerCase().startsWith(validKey.toLowerCase())))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
            executionData.setExecutionContext(reducedMap);

        }

    }

    /**
     * reduce the excecution Context data that is in the Map matching where any key starts with the passed in list of validkeys
     */
    public void reduceExecutionContextToMatchingKeys(List<String> validKeys) {
        nflowJobExecutionContexts.values().forEach(nflowJobExecutionDatas -> {
            if (nflowJobExecutionDatas != null) {
                nflowJobExecutionDatas.stream().forEach(executionData -> reduceExecutionContextToMatchingKeys(executionData, validKeys));
            }
        });

        latestNflowJobExecutionContext.values().forEach(executionData -> {
            if (executionData != null) {
                reduceExecutionContextToMatchingKeys(executionData, validKeys);
            }
        });
    }


    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    public String getNflowId() {
        return nflowId;
    }

    public void setNflowId(String nflowId) {
        this.nflowId = nflowId;
    }

    public List<String> getDependentNflowNames() {
        if (dependentNflowNames == null) {
            dependentNflowNames = new ArrayList<>();
        }
        return dependentNflowNames;
    }

    public void setDependentNflowNames(List<String> dependentNflowNames) {
        this.dependentNflowNames = dependentNflowNames;
    }

    public Map<String, List<NflowJobExecutionData>> getNflowJobExecutionContexts() {
        return nflowJobExecutionContexts;
    }

    public Map<String, NflowJobExecutionData> getLatestNflowJobExecutionContext() {
        return latestNflowJobExecutionContext;
    }


    public static class NflowJobExecutionData {

        private Long jobExecutionId;
        private DateTime startTime;
        private DateTime endTime;
        private Map<String, Object> executionContext;

        public NflowJobExecutionData() {

        }

        public NflowJobExecutionData(Long jobExecutionId, DateTime startTime, DateTime endTime, Map<String, Object> executionContext) {
            this.jobExecutionId = jobExecutionId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.executionContext = executionContext;
        }

        public Long getJobExecutionId() {
            return jobExecutionId;
        }

        public void setJobExecutionId(Long jobExecutionId) {
            this.jobExecutionId = jobExecutionId;
        }

        public DateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(DateTime startTime) {
            this.startTime = startTime;
        }

        public DateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(DateTime endTime) {
            this.endTime = endTime;
        }

        public Map<String, Object> getExecutionContext() {
            return executionContext;
        }

        public void setExecutionContext(Map<String, Object> executionContext) {
            this.executionContext = executionContext;
        }
    }
}
