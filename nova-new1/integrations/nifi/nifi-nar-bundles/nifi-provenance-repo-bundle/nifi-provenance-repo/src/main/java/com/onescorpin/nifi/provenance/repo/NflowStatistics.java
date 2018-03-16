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

import com.google.common.collect.EvictingQueue;
import com.onescorpin.nifi.provenance.ProvenanceEventRecordConverter;
import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTO;
import com.onescorpin.nifi.provenance.model.stats.GroupedStats;
import com.onescorpin.nifi.provenance.model.stats.GroupedStatsV2;
import com.onescorpin.nifi.provenance.util.ProvenanceEventUtil;

import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.provenance.ProvenanceEventType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds Statistics about a Nflow and Processor updated during Nifi execution
 */
public class NflowStatistics {

    private static final Logger log = LoggerFactory.getLogger(NflowStatistics.class);

    private static int limit = ConfigurationProperties.DEFAULT_MAX_EVENTS;

    /**
     * The originating processor id that started this entire execution.  This will mark the nflow identity
     */
    private String nflowProcessorId;

    /**
     * The Processor ID
     */
    private String processorId;


    /**
     * Records to send off to JMS
     */
    private Map<String, ProvenanceEventRecordDTO> lastRecords = new ConcurrentHashMap<>(limit);


    /**
     * SourceQueueIdentifier to Grouped Stats
     * Stats are grouped by their SourceQueueId so Nova can detect if it came off a "failure" path
     */
    private Map<String, GroupedStats> stats;

    /**
     * Flag to indicate we are throttling the start Job events that get sent to ops manager
     */
    private AtomicBoolean isThrottled = new AtomicBoolean(false);

    /**
     * The
     */
    private Integer throttleStartingNflowFlowsThreshold = 15;

    /**
     * Rolling queue of the last {throttleStartingNflowFlowsThreshold} items based upon time
     */
    Queue<Long> startingNflowFlowQueue = null;


    /**
     * Time to before the throttle key will rest
     * Rapid events need to be slow for this amount of time before resetting the key
     */
    private Integer throttleStartingNflowFlowsTimePeriod = 1000;


    private String batchKey(ProvenanceEventRecord event, String nflowFlowFileId, boolean isStartingNflowFlow) {
        String key = event.getComponentId() + ":" + event.getEventType().name();

        if (isStartingNflowFlow) {
            if(startingNflowFlowQueue == null){
                startingNflowFlowQueue =EvictingQueue.create(throttleStartingNflowFlowsThreshold);
            }

            startingNflowFlowQueue.add(event.getEventTime());
            if(startingNflowFlowQueue.size() >= throttleStartingNflowFlowsThreshold) {
                Long diff = event.getEventTime() - startingNflowFlowQueue.peek();
                if (diff < throttleStartingNflowFlowsTimePeriod) {
                    //we got more than x events within the threshold... throttle
                    key += eventTimeNearestSecond(event);
                    if(isThrottled.compareAndSet(false,true)) {
                        log.info("Detected over {} flows/sec starting within the given window, throttling back starting events ", throttleStartingNflowFlowsThreshold);
                    }
                } else {
                    key +=":" + nflowFlowFileId;
                    startingNflowFlowQueue.clear();
                    if(isThrottled.compareAndSet(true,false)) {
                        log.info("Resetting throttle flow rate is slower than threshold {} flows/se for flows starting within the given window.", throttleStartingNflowFlowsThreshold);
                    }
                }

            }
            else {
                key = key + ":" + nflowFlowFileId;
            }
        }
        else {
            key +=":" + nflowFlowFileId;
        }


        return key;


    }


    private Long eventTimeNearestSecond(ProvenanceEventRecord event) {
        return new DateTime(event.getEventTime()).withMillisOfSecond(0).getMillis();
    }

    private Long nowToNearestSecond(ProvenanceEventRecord event) {
        return DateTime.now().withMillis(0).getMillis();
    }



    public NflowStatistics(String nflowProcessorId, String processorId) {
        this.nflowProcessorId = nflowProcessorId;
        this.processorId = processorId;
        stats = new ConcurrentHashMap<>();
        this.limit = ConfigurationProperties.getInstance().getNflowProcessorMaxEvents();
        this.throttleStartingNflowFlowsThreshold = ConfigurationProperties.getInstance().getThrottleStartingNflowFlowsThreshold();
        this.throttleStartingNflowFlowsTimePeriod = ConfigurationProperties.getInstance().getDefaultThrottleStartingNflowFlowsTimePeriodMillis();
    }

    public GroupedStats getStats(ProvenanceEventRecord event) {
        String key = event.getSourceQueueIdentifier();
        if (key == null) {
            key = GroupedStats.DEFAULT_SOURCE_CONNECTION_ID;
        }
        return stats.computeIfAbsent(key, sourceConnectionIdentifier -> new GroupedStatsV2(sourceConnectionIdentifier));
    }


    public void addEvent(ProvenanceEventRecord event, Long eventId) {

        NflowEventStatistics.getInstance().calculateTimes(event, eventId);

        ProvenanceEventRecordDTO eventRecordDTO = null;

        String nflowFlowFileId = NflowEventStatistics.getInstance().getNflowFlowFileId(event);

        boolean isStartingNflowFlow = ProvenanceEventUtil.isStartingNflowFlow(event);
        String batchKey = batchKey(event, nflowFlowFileId, isStartingNflowFlow);

        //always track drop events if its on a tracked nflow
        boolean isDropEvent = ProvenanceEventUtil.isEndingFlowFileEvent(event);
        if (isDropEvent && NflowEventStatistics.getInstance().beforeProcessingIsLastEventForTrackedNflow(event, eventId)) {
            batchKey += UUID.randomUUID().toString();
        }

        if (((!isStartingNflowFlow && NflowEventStatistics.getInstance().isTrackingDetails(event.getFlowFileUuid())) || (isStartingNflowFlow && lastRecords.size() <= limit)) && !lastRecords
            .containsKey(batchKey)) {
            // if we are tracking details send the event off for jms
            if (isStartingNflowFlow) {
                NflowEventStatistics.getInstance().setTrackingDetails(event);
            }

            eventRecordDTO = ProvenanceEventRecordConverter.convert(event);
            eventRecordDTO.setEventId(eventId);
            eventRecordDTO.setIsStartOfJob(ProvenanceEventUtil.isStartingNflowFlow(event));

            eventRecordDTO.setJobFlowFileId(nflowFlowFileId);
            eventRecordDTO.setFirstEventProcessorId(nflowProcessorId);
            eventRecordDTO.setStartTime(NflowEventStatistics.getInstance().getEventStartTime(eventId));
            eventRecordDTO.setEventDuration(NflowEventStatistics.getInstance().getEventDuration(eventId));

            if (ProvenanceEventUtil.isFlowFileQueueEmptied(event)) {
                // a Drop event component id will be the connection, not the processor id. we will set the name of the component
                eventRecordDTO.setComponentName("FlowFile Queue emptied");
                eventRecordDTO.setIsFailure(true);
            }

            if (ProvenanceEventUtil.isTerminatedByFailureRelationship(event)) {
                eventRecordDTO.setIsFailure(true);
            }

            lastRecords.put(batchKey, eventRecordDTO);

        } else {
            NflowEventStatistics.getInstance().skip(event, eventId);
        }
        NflowEventStatistics.getInstance().finishedEvent(event, eventId);

        boolean isEndingEvent = NflowEventStatistics.getInstance().isEndingNflowFlow(eventId);
        if (eventRecordDTO != null && isEndingEvent) {
            eventRecordDTO.setIsFinalJobEvent(isEndingEvent);
        }
        NflowProcessorStatisticsAggregator.getInstance().add(getStats(event), event, eventId);

        NflowEventStatistics.getInstance().cleanup(event, eventId);


    }

    public boolean hasStats() {
        return getStats().stream().anyMatch(s -> s.getTotalCount() > 0);
    }

    public String getNflowProcessorId() {
        return nflowProcessorId;
    }

    public String getProcessorId() {
        return processorId;
    }

    public Collection<ProvenanceEventRecordDTO> getEventsToSend() {
        return lastRecords.values();
    }

    //  public AggregatedProcessorStatistics getNflowProcessorStatistics(){
    //      return nflowProcessorStatistics;
    //  }

    public Collection<GroupedStats> getStats() {
        return stats.values();
    }

    public void clear() {
        lastRecords.clear();
        stats.clear();
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

}
