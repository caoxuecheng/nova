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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.nifi.provenance.util.ProvenanceEventUtil;

import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.provenance.ProvenanceEventType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Hold all data about running flows as they pertain to Nflows
 */
public class NflowEventStatistics implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(NflowEventStatistics.class);

    protected Map<String, String> nflowFlowFileIdToNflowProcessorId = new ConcurrentHashMap<>();

    ////Track nflowFlowFile relationships to parent/child flow files for lifetime nflow job execution
    protected Set<String> detailedTrackingNflowFlowFileId = new HashSet<>();

    /**
     * Map of all the flow files as they pertain to the starting nflow flow file
     * Used to expire EventStatistics
     */
    protected Map<String, String> allFlowFileToNflowFlowFile = new ConcurrentHashMap<>();


    /**
     * Removal Listener for those events that are tracked and sent to Nova Operations Manager.
     * Events with Detailed Tracking need to be added to the special cache to ensure step capture and timing information.
     * DROP events on Flowfiles indicate the end of the flow file.  Flows that split/merge will relate to other flow files.  Sometimes the DROP of the previous flow file will
     * be processed in a different order than the next flow file.  Beacuse of this the removal of data needs to be done after the fact to ensure the flows capture the correct data in ops manager
     */
    RemovalListener<Long, String> flowFileRemovalListener = new RemovalListener<Long, String>() {
        @Override
        public void onRemoval(RemovalNotification<Long, String> removalNotification) {
            Long eventId = removalNotification.getKey();
            String flowFileId = removalNotification.getValue();
            clearData(eventId, flowFileId);
        }
    };

    /**
     * An Expiring cache of the flowfile information that is tracked and Sent to Nova Ops Manager as ProvenanceEventDTO objects
     * Map<EventId, eventFlowFileId>.  The eventId is tied to the flowfile id that initiated the final DROP event type
     * Wait 1 minute before expiring and cleaning up these resources.
     **/
    protected Cache<Long, String> detailedTrackingFlowFilesToDelete = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .removalListener(flowFileRemovalListener)
        .build();

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

    private Map<String, long[]> flowRate = new HashMap<>();

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
    protected Map<String, AtomicInteger> nflowFlowFileFailureCount = new ConcurrentHashMap<>();


    /**
     * Count of the flows running by nflow processor
     */
    protected Map<String,AtomicLong> nflowProcessorRunningNflowFlows = new ConcurrentHashMap<>();

    /**
     * Count of the flows running by nflow processor
     */
    protected Set<String> changedNflowProcessorRunningNflowFlows = new HashSet<>();

    protected AtomicBoolean nflowProcessorRunningNflowFlowsChanged = new AtomicBoolean(false);

    /**
     * file location to persist this data if NiFi goes Down midstream
     * This value is set via the NovaPersistenetProvenanceEventRepository during initialization
     */
    private String backupLocation = "/opt/nifi/nflow-event-statistics.gz";

    /**
     * Map of the NiFi Event to Nifi Class that should be skipped
     */
    private Map<String, Set<String>> eventTypeProcessorTypeSkipChildren = new HashMap<>();


    private static final NflowEventStatistics instance = new NflowEventStatistics();

    private NflowEventStatistics() {

    }

    public static NflowEventStatistics getInstance() {
        return instance;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
    }

    private boolean shouldSkipChildren(ProvenanceEventType eventType, String componentType) {
        boolean
            skip =
            componentType != null && eventType != null && eventTypeProcessorTypeSkipChildren != null && eventTypeProcessorTypeSkipChildren.containsKey(eventType.name())
            && eventTypeProcessorTypeSkipChildren
                   .get(eventType.name()) != null && eventTypeProcessorTypeSkipChildren
                .get(eventType.name()).stream().anyMatch(type -> componentType.equalsIgnoreCase(type));
        if (skip) {
            //log it
            log.info("Skip processing children flow files {} for {} ", eventType, componentType);
        }
        return skip;
    }

    public void updateEventTypeProcessorTypeSkipChildren(String json) {
        try {
            Map<String, Set<String>> m = ObjectMapperSerializer.deserialize(json, new TypeReference<Map<String, Set<String>>>() {
            });
            this.eventTypeProcessorTypeSkipChildren = m;
            log.info("Reset the Orphan Flowfile processor Map with {} ", json);
        } catch (Exception e) {
            log.error("Unable to update the {}.  Invalid JSON supplied {} ", ConfigurationProperties.ORPHAN_CHILD_FLOW_FILE_PROCESSORS_KEY, e.getMessage(), e);
        }

    }

    public boolean backup() {
        return backup(getBackupLocation());
    }

    public boolean backup(String location) {

        try {
            //cleanup any files that should be removed before backup
            detailedTrackingFlowFilesToDelete.cleanUp();

            FileOutputStream fos = new FileOutputStream(location);
            GZIPOutputStream gz = new GZIPOutputStream(fos);

            ObjectOutputStream oos = new ObjectOutputStream(gz);

            oos.writeObject(new NflowEventStatisticsData(this));
            oos.close();
            return true;

        } catch (Exception ex) {
            log.error("Error backing up nflow event statistics to {}. {} ", location, ex.getMessage(), ex);
        }
        return false;
    }

    public boolean loadBackup() {
        return loadBackup(getBackupLocation());
    }

    public boolean loadBackup(String location) {
        NflowEventStatisticsData inStats = null;
        try {

            FileInputStream fin = new FileInputStream(location);
            GZIPInputStream gis = new GZIPInputStream(fin);
            ObjectInputStream ois = new ObjectInputStream(gis);
            inStats = (NflowEventStatisticsData) ois.readObject();
            ois.close();

        } catch (Exception ex) {
            if (!(ex instanceof FileNotFoundException)) {
                log.error("Unable to load nflow event statistics backup from {}. {} ", location, ex.getMessage(), ex);
            } else {
                log.info("Nova nflow event statistics backup file not found. Not loading backup from {}. ", location);
            }
        }
        if (inStats != null) {
            boolean success = this.load(inStats);
            //DELETE backup
            try {
                File f = new File(location);
                if (f.exists()) {
                    if(! f.delete()) {
                        throw new RuntimeException("Error deleting file " + f.getName());
                    }
                }
            } catch (Exception e) {

            }
            return success;
        }
        return false;
    }


    public void clear() {
        this.nflowFlowFileIdToNflowProcessorId.clear();
        this.detailedTrackingNflowFlowFileId.clear();
        this.allFlowFileToNflowFlowFile.clear();
        this.flowFileLastNonDropEventTime.clear();
        this.eventDuration.clear();
        this.eventStartTime.clear();
        this.nflowFlowFileStartTime.clear();
        this.nflowFlowFileEndTime.clear();
        this.eventsThatCompleteNflowFlow.clear();
        this.nflowFlowProcessing.clear();
        this.skippedEvents.set(0L);
        this.nflowFlowFileFailureCount.clear();
    }


    public boolean load(NflowEventStatisticsData other) {

        // clear();
        this.nflowFlowFileIdToNflowProcessorId.putAll(other.nflowFlowFileIdToNflowProcessorId);
        this.detailedTrackingNflowFlowFileId.addAll(other.detailedTrackingNflowFlowFileId);
        this.allFlowFileToNflowFlowFile.putAll(other.allFlowFileToNflowFlowFile);
        this.flowFileLastNonDropEventTime.putAll(other.flowFileLastNonDropEventTime);
        this.eventDuration.putAll(other.eventDuration);
        this.eventStartTime.putAll(other.eventStartTime);
        this.nflowFlowFileStartTime.putAll(other.nflowFlowFileStartTime);
        this.nflowFlowFileEndTime.putAll(other.nflowFlowFileEndTime);
        this.eventsThatCompleteNflowFlow.addAll(other.eventsThatCompleteNflowFlow);
        this.nflowFlowProcessing.putAll(other.nflowFlowProcessing);
        this.skippedEvents.set(other.skippedEvents.get());
        this.nflowFlowFileFailureCount.putAll(other.nflowFlowFileFailureCount);
        return true;


    }


    public void checkAndAssignStartingFlowFile(ProvenanceEventRecord event) {
        if (ProvenanceEventUtil.isStartingNflowFlow(event)) {
            //startingFlowFiles.add(event.getFlowFileUuid());
            allFlowFileToNflowFlowFile.put(event.getFlowFileUuid(), event.getFlowFileUuid());
            //add the flow to active processing
            nflowFlowProcessing.computeIfAbsent(event.getFlowFileUuid(), nflowFlowFileId -> new AtomicInteger(0)).incrementAndGet();
            nflowFlowFileIdToNflowProcessorId.put(event.getFlowFileUuid(), event.getComponentId());

            nflowProcessorRunningNflowFlows.computeIfAbsent(event.getComponentId(),processorId -> new AtomicLong(0)).incrementAndGet();
            nflowProcessorRunningNflowFlowsChanged.set(true);
            changedNflowProcessorRunningNflowFlows.add(event.getComponentId());
            //  nflowFlowToRelatedFlowFiles.computeIfAbsent(event.getFlowFileUuid(), nflowFlowFileId -> new HashSet<>()).add(event.getFlowFileUuid());
        }
    }

    public void markNflowProcessorRunningNflowFlowsUnchanged(){
        nflowProcessorRunningNflowFlowsChanged.set(false);
        changedNflowProcessorRunningNflowFlows.clear();
    }

    public boolean isNflowProcessorRunningNflowFlowsChanged(){
        return nflowProcessorRunningNflowFlowsChanged.get();
    }

    /**
     * attach the event that has parents/children to a tracking nflowflowfile (if possible)
     * This is for the Many to one case
     *
     * @param event the event
     * @return the parent event to track
     */
    private String determineParentNflowFlow(ProvenanceEventRecord event) {
        String nflowFlowFile = null;
        String parent = event.getParentUuids().stream().filter(parentFlowFileId -> isTrackingDetails(parentFlowFileId)).findFirst().orElse(null);
        if (parent == null) {
            parent = event.getParentUuids().get(0);
        }
        if (parent != null) {
            nflowFlowFile = getNflowFlowFileId(parent);
        }

        return nflowFlowFile;
    }


    public boolean assignParentsAndChildren(ProvenanceEventRecord event) {

        //Assign the Event to one of the Parents

        //  activeFlowFiles.add(event.getFlowFileUuid());
        String startingFlowFile = allFlowFileToNflowFlowFile.get(event.getFlowFileUuid());
        boolean trackingEventFlowFile = false;
        if (event.getParentUuids() != null && !event.getParentUuids().isEmpty()) {

            if (startingFlowFile == null) {
                startingFlowFile = determineParentNflowFlow(event);
                if (startingFlowFile != null) {
                    allFlowFileToNflowFlowFile.put(event.getFlowFileUuid(), startingFlowFile);
                    if (nflowFlowProcessing.containsKey(startingFlowFile)) {
                        nflowFlowProcessing.get(startingFlowFile).incrementAndGet();
                        trackingEventFlowFile = true;
                    }
                }
            }


        }
        if (startingFlowFile != null && event.getChildUuids() != null && !event.getChildUuids().isEmpty() && !shouldSkipChildren(event.getEventType(), event.getComponentType())) {
            for (String child : event.getChildUuids()) {
                allFlowFileToNflowFlowFile.put(child, startingFlowFile);
                //Add children flow files to active processing
                //skip this add if we already did it while iterating the parents.
                //NiFi will create a new Flow File for this event (event.getFlowFileId) and it will also be part of the children
                if (nflowFlowProcessing.containsKey(startingFlowFile) && (!trackingEventFlowFile || (trackingEventFlowFile && !child.equalsIgnoreCase(event.getFlowFileUuid())))) {
                    nflowFlowProcessing.get(startingFlowFile).incrementAndGet();
                }
                flowFileLastNonDropEventTime.put(child, event.getEventTime());
            }
        }

        return startingFlowFile != null;

    }


    public void calculateTimes(ProvenanceEventRecord event, Long eventId) {
        //  eventIdEventTime.put(eventId,event.getEventTime());
        Long startTime = lastEventTimeForFlowFile(event.getFlowFileUuid());
        if (startTime == null && hasParents(event)) {
            startTime = lastEventTimeForParent(event.getParentUuids());
        }
        if (startTime == null) {
            startTime = event.getFlowFileEntryDate();
        }
        DateTime st = new DateTime(startTime);
        if (ProvenanceEventUtil.isStartingNflowFlow(event)) {
            nflowFlowFileStartTime.put(event.getFlowFileUuid(), startTime);
        }

        Long duration = event.getEventTime() - startTime;
        eventDuration.put(eventId, duration);
        eventStartTime.put(eventId, startTime);

        if (!ProvenanceEventType.DROP.equals(event.getEventType())) {
            flowFileLastNonDropEventTime.put(event.getFlowFileUuid(), event.getEventTime());
        }

    }

    public void skip(ProvenanceEventRecord event, Long eventId) {
        skippedEvents.incrementAndGet();
    }


    private Long lastEventTimeForFlowFile(String flowFile) {
        return flowFileLastNonDropEventTime.get(flowFile);
    }


    private Long lastEventTimeForParent(Collection<String> parentIds) {
        return parentIds.stream().filter(flowFileId -> flowFileLastNonDropEventTime.containsKey(flowFileId)).findFirst().map(flowFileId -> flowFileLastNonDropEventTime.get(flowFileId)).orElse(null);
    }


    public boolean isEndingNflowFlow(Long eventId) {
        return eventsThatCompleteNflowFlow.contains(eventId);
    }


    /**
     * are we tracking details for this nflow
     */
    public boolean isTrackingDetails(String eventFlowFileId) {
        String nflowFlowFile = allFlowFileToNflowFlowFile.get(eventFlowFileId);
        if (nflowFlowFile != null) {
            return detailedTrackingNflowFlowFileId.contains(nflowFlowFile);
        }
        return false;
    }

    public void setTrackingDetails(ProvenanceEventRecord event) {
        detailedTrackingNflowFlowFileId.add(event.getFlowFileUuid());
    }

    private boolean hasParents(ProvenanceEventRecord event) {
        return event.getParentUuids() != null && !event.getParentUuids().isEmpty();
    }


    public Long getEventDuration(Long eventId) {
        return eventDuration.get(eventId);
    }

    public Long getEventStartTime(Long eventId) {
        return eventStartTime.get(eventId);
    }


    public boolean hasFailures(ProvenanceEventRecord event) {
        String nflowFlowFile = getNflowFlowFileId(event);
        if (nflowFlowFile != null) {
            return nflowFlowFileFailureCount.getOrDefault(nflowFlowFile, new AtomicInteger(0)).get() > 0;
        }
        return false;
    }

    public Long getSkippedEvents() {
        return skippedEvents.get();
    }

    public String getNflowFlowFileId(ProvenanceEventRecord event) {
        return allFlowFileToNflowFlowFile.get(event.getFlowFileUuid());
    }

    public String getNflowFlowFileId(String eventFlowFileId) {
        return allFlowFileToNflowFlowFile.get(eventFlowFileId);
    }


    public String getNflowProcessorId(ProvenanceEventRecord event) {
        String nflowFlowFileId = getNflowFlowFileId(event);
        return nflowFlowFileId != null ? nflowFlowFileIdToNflowProcessorId.get(nflowFlowFileId) : null;
    }

    public Long getNflowFlowStartTime(ProvenanceEventRecord event) {
        String nflowFlowFile = getNflowFlowFileId(event);
        if (nflowFlowFile != null) {
            return nflowFlowFileStartTime.getOrDefault(nflowFlowFile, null);
        }
        return null;
    }

    public Long getNflowFlowEndTime(ProvenanceEventRecord event) {
        String nflowFlowFile = getNflowFlowFileId(event);
        if (nflowFlowFile != null) {
            return nflowFlowFileEndTime.getOrDefault(nflowFlowFile, null);
        }
        return null;
    }

    public Long getNflowFlowFileDuration(ProvenanceEventRecord event) {
        Long start = getNflowFlowStartTime(event);
        Long end = getNflowFlowEndTime(event);
        if (start != null && end != null) {
            return end - start;
        }
        return null;
    }

    private void clearMapsForEventFlowFile(String eventFlowFileId) {
        flowFileLastNonDropEventTime.remove(eventFlowFileId);
        allFlowFileToNflowFlowFile.remove(eventFlowFileId);
    }

    /**
     * Return the count of running nflow flow files for a given starting processor id
     * @param nflowProcessorId the starting nflow processor id
     * @return the count of running nflow flows
     */
    public Long getRunningNflowFlows(String nflowProcessorId){
        return nflowProcessorRunningNflowFlows.getOrDefault(nflowProcessorId, new AtomicLong(0L)).get();
    }

    public Map<String,Long> getRunningNflowFlows(){
        return nflowProcessorRunningNflowFlows.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    public Map<String,Long> getRunningNflowFlowsChanged(){
        return changedNflowProcessorRunningNflowFlows.stream().collect(Collectors.toMap(processorId -> processorId,processorId -> nflowProcessorRunningNflowFlows.get(processorId).get()));
    }

    private void decrementRunningProcessorNflowFlows(String nflowFlowFile){
        String nflowProcessor = nflowFlowFileIdToNflowProcessorId.get(nflowFlowFile);
        if(nflowProcessor != null){
            AtomicLong runningCount = nflowProcessorRunningNflowFlows.get(nflowProcessor);
            if( runningCount != null && runningCount.get() >=1) {
                runningCount.decrementAndGet();
                nflowProcessorRunningNflowFlowsChanged.set(true);
                changedNflowProcessorRunningNflowFlows.add(nflowProcessor);
            }
        }
    }

    private void clearMapsForNflowFlowFile(String nflowFlowFile) {
        if (nflowFlowFile != null) {
            detailedTrackingNflowFlowFileId.remove(nflowFlowFile);
            nflowFlowFileFailureCount.remove(nflowFlowFile);
            nflowFlowFileEndTime.remove(nflowFlowFile);
            nflowFlowFileStartTime.remove(nflowFlowFile);
            nflowFlowProcessing.remove(nflowFlowFile);


            nflowFlowFileIdToNflowProcessorId.remove(nflowFlowFile);
        }
    }

    private void clearData(Long eventId, String eventFlowFileId) {
        String nflowFlowFile = getNflowFlowFileId(eventFlowFileId);
        clearMapsForEventFlowFile(eventFlowFileId);
        if (isEndingNflowFlow(eventId)) {
            clearMapsForNflowFlowFile(nflowFlowFile);
            eventsThatCompleteNflowFlow.remove(eventId);
        }
    }


    public void checkAndClear(String eventFlowFileId, String eventType, Long eventId) {
        if (ProvenanceEventType.DROP.name().equals(eventType)) {

            boolean isTrackingDetails = isTrackingDetails(eventFlowFileId);

            if (!isTrackingDetails) {
                //if we are not tracking ProvenanceEventDTO details then we can just expire all the flowfile data
                clearData(eventId, eventFlowFileId);
            } else {
                //if we are tracking details it needs to be added to an expiring map.
                //Sometimes the DROP event for the flowfile will come in before the next event causing us to loose the tracking information
                //this will happen in a very short time, so adding to an expiring cache to help manage the cleanup of these entries is needed.
                detailedTrackingFlowFilesToDelete.put(eventId, eventFlowFileId);
            }
        }

        eventDuration.remove(eventId);
        eventStartTime.remove(eventId);
    }


    /**
     * is this the last DROP event for a nflow that is being tracked to go to ops manager
     *
     * @param event   the event
     * @param eventId the id
     * @return true if last event, false if not
     */
    public boolean beforeProcessingIsLastEventForTrackedNflow(ProvenanceEventRecord event, Long eventId) {
        String nflowFlowFileId = allFlowFileToNflowFlowFile.get(event.getFlowFileUuid());
        if (isTrackingDetails(event.getFlowFileUuid()) && nflowFlowFileId != null && ProvenanceEventType.DROP.equals(event.getEventType())) {
            //get the nflow flow fileId for this event
            AtomicInteger activeCounts = nflowFlowProcessing.get(nflowFlowFileId);
            if (activeCounts != null) {
                return activeCounts.get() == 1;
            }
        }
        return false;
    }

    public void finishedEvent(ProvenanceEventRecord event, Long eventId) {

        String nflowFlowFileId = allFlowFileToNflowFlowFile.get(event.getFlowFileUuid());
        if (nflowFlowFileId != null && ProvenanceEventType.DROP.equals(event.getEventType())) {
            //get the nflow flow fileId for this event
            AtomicInteger activeCounts = nflowFlowProcessing.get(nflowFlowFileId);
            if (activeCounts != null) {
                nflowFlowProcessing.get(nflowFlowFileId).decrementAndGet();
                if (activeCounts.get() <= 0) {
                    //Nflow is finished
                    eventsThatCompleteNflowFlow.add(eventId);
                    nflowFlowFileEndTime.put(nflowFlowFileId, event.getEventTime());
                    decrementRunningProcessorNflowFlows(nflowFlowFileId);
                }

            }

        }

        if (nflowFlowFileId != null && ProvenanceEventUtil.isTerminatedByFailureRelationship(event)) {
            //add to failureMap
            nflowFlowFileFailureCount.computeIfAbsent(nflowFlowFileId, flowFileId -> new AtomicInteger(0)).incrementAndGet();
        }


    }

    public void cleanup(ProvenanceEventRecord event, Long eventId) {
        checkAndClear(event.getFlowFileUuid(), event.getEventType().name(), eventId);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NflowEventStatistics{");
        sb.append("detailedTrackingNflowFlowFileId=").append(detailedTrackingNflowFlowFileId.size());
        sb.append(", allFlowFileToNflowFlowFile=").append(allFlowFileToNflowFlowFile.size());
        sb.append(", nflowFlowProcessing=").append(nflowFlowProcessing.size());
        sb.append(", skippedEvents=").append(skippedEvents);
        sb.append('}');
        return sb.toString();
    }
}
