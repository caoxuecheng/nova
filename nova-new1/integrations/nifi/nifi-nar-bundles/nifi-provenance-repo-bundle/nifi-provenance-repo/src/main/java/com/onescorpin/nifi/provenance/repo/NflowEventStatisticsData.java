package com.onescorpin.nifi.provenance.repo;

/*-
 * #%L
 * onescorpin-nifi-provenance-repo
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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Backup of the NflowEventStatistics.  This is the object that will be serialized to disk upon NiFi shutdown
 */
public class NflowEventStatisticsData implements Serializable {

    protected Map<String, String> nflowFlowFileIdToNflowProcessorId = new ConcurrentHashMap<>();

    ////Track nflowFlowFile relationships to parent/child flow files for lifetime nflow job execution
    protected Set<String> detailedTrackingNflowFlowFileId = new HashSet<>();

    /**
     * Map of all the flow files as they pertain to the starting nflow flow file
     * Used to expire EventStatistics
     */
    protected Map<String, String> allFlowFileToNflowFlowFile = new ConcurrentHashMap<>();

    /**
     *  nflow flow file to respective flow files that are detail tracked.
     */
    protected Map<String, Set<String>> detailedTrackingInverseInverseMap = new ConcurrentHashMap<>();

    ///Track Timing Information for each event


    /**
     * Map of the FlowFile Id to Event Time that is not a drop event
     */
    protected Map<String, Long> flowFileLastNonDropEventTime = new ConcurrentHashMap<>();

    /**
     * Map of the EventId to the duration in millis
     */
    protected Map<Long, Long> eventDuration = new ConcurrentHashMap<>();

    protected Map<Long, Long> eventStartTime = new ConcurrentHashMap<>();

    /**
     * nflow flowFile Id to startTime
     */
    protected Map<String, Long> nflowFlowFileStartTime = new ConcurrentHashMap<>();

    /**
     * nflowFlowFile Id to end time
     */
    protected Map<String, Long> nflowFlowFileEndTime = new ConcurrentHashMap<>();

    //Nflow Execution tracking

    /**
     * Set of Event Ids that are events that finish the nflow flow execution.  Last Job Event Ids
     */
    protected Set<Long> eventsThatCompleteNflowFlow = new HashSet<>();

    /**
     * Count of how many flow files are still processing for a given nflowFlowFile execution
     */
    protected Map<String, AtomicInteger> nflowFlowProcessing = new ConcurrentHashMap<>();

    /// Skipped detail tracking and failure counts

    /**
     * Events not capturing details
     */
    protected AtomicLong skippedEvents = new AtomicLong(0);


    /**
     * Count of how many failures have been detected for a nflowFlowFile execution
     */
    Map<String, AtomicInteger> nflowFlowFileFailureCount = new ConcurrentHashMap<>();

    public NflowEventStatisticsData() {

    }


    public NflowEventStatisticsData(NflowEventStatistics other) {
        this.nflowFlowFileIdToNflowProcessorId = other.nflowFlowFileIdToNflowProcessorId;
        this.detailedTrackingNflowFlowFileId = other.detailedTrackingNflowFlowFileId;
        this.allFlowFileToNflowFlowFile = other.allFlowFileToNflowFlowFile;
        this.flowFileLastNonDropEventTime = other.flowFileLastNonDropEventTime;
        this.eventDuration = other.eventDuration;
        this.eventStartTime = other.eventStartTime;
        this.nflowFlowFileStartTime = other.nflowFlowFileStartTime;
        this.nflowFlowFileEndTime = other.nflowFlowFileEndTime;
        this.eventsThatCompleteNflowFlow = other.eventsThatCompleteNflowFlow;
        this.nflowFlowProcessing = other.nflowFlowProcessing;
        this.skippedEvents = other.skippedEvents;
        this.nflowFlowFileFailureCount = other.nflowFlowFileFailureCount;
    }

    public void load() {

    }

}
