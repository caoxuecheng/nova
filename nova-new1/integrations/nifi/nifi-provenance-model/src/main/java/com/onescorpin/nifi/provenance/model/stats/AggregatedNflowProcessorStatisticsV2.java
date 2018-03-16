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

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Group Stats by Nflow and Processor
 */
public class AggregatedNflowProcessorStatisticsV2 extends AggregatedNflowProcessorStatistics implements  Serializable {

    public AggregatedNflowProcessorStatisticsV2(){
    super();
    }

    public AggregatedNflowProcessorStatisticsV2(String startingProcessorId, String collectionId, Long collectionIntervalMillis) {
        super(startingProcessorId, collectionId, collectionIntervalMillis);
    }

    public AggregatedNflowProcessorStatisticsV2(String startingProcessorId, String collectionId, Long collectionIntervalMillis, String nflowName) {
        super(startingProcessorId, collectionId, collectionIntervalMillis);
        this.nflowName = nflowName;
    }

    private String nflowName;

    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }
}
