package com.onescorpin.nifi.provenance.model.stats;

/*-
 * #%L
 * onescorpin-nifi-provenance-model
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

import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTO;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 */
public class AggregatedNflowProcessorStatisticsHolder implements Serializable {

    DateTime minTime;
    DateTime maxTime;
    String collectionId;
    AtomicLong eventCount = new AtomicLong(0L);
    /**
     * Map of Starting processorId and stats related to it
     */
    Map<String, AggregatedNflowProcessorStatistics> nflowStatistics = new ConcurrentHashMap<>();
    private Long minEventId = 0L;
    private Long maxEventId = 0L;

    public AggregatedNflowProcessorStatisticsHolder() {

        this.collectionId = UUID.randomUUID().toString();
    }



    public AtomicLong getEventCount() {
        return eventCount;
    }

    public Long getMinEventId() {
        return minEventId;
    }

    public Long getMaxEventId() {
        return maxEventId;
    }

    public Map<String, AggregatedNflowProcessorStatistics> getNflowStatistics() {
        return nflowStatistics;
    }

    public void setNflowStatistics(Map<String, AggregatedNflowProcessorStatistics> nflowStatistics) {
        this.nflowStatistics = nflowStatistics;
    }

    public void setNflowStatistics(List<AggregatedNflowProcessorStatistics> stats){
        if(stats != null){
            this.nflowStatistics =    stats.stream().collect(Collectors.toMap(AggregatedNflowProcessorStatistics::getStartingProcessorId, Function.identity()));
        }
    }

    public void clear() {
        this.collectionId = UUID.randomUUID().toString();
        nflowStatistics.entrySet().forEach(e -> e.getValue().clear(collectionId));
    }

    public boolean hasStats(){
        return nflowStatistics.values().stream().anyMatch(s -> s.hasStats());
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AggregatedNflowProcessorStatisticsHolder{");
        sb.append("minTime=").append(minTime);
        sb.append(", maxTime=").append(maxTime);
        sb.append(", collectionId='").append(collectionId).append('\'');
        sb.append(", eventCount=").append(eventCount.get());
        sb.append('}');
        return sb.toString();
    }
}
