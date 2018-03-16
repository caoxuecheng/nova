package com.onescorpin.metadata.jobrepo.nifi.provenance;

/*-
 * #%L
 * onescorpin-operational-metadata-integration-service
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import com.onescorpin.cluster.ClusterMessage;
import com.onescorpin.cluster.ClusterService;
import com.onescorpin.cluster.ClusterServiceMessageReceiver;
import com.onescorpin.jms.JmsConstants;
import com.onescorpin.jms.Queues;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorErrors;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStats;
import com.onescorpin.metadata.jpa.jobrepo.nifi.JpaNifiNflowProcessorStats;
import com.onescorpin.metadata.jpa.jobrepo.nifi.JpaNifiNflowStats;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatistics;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatisticsHolder;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatisticsHolderV2;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatisticsHolderV3;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatisticsV2;
import com.onescorpin.nifi.provenance.model.stats.GroupedStats;
import com.onescorpin.nifi.provenance.model.stats.GroupedStatsV2;
import com.onescorpin.scheduler.JobIdentifier;
import com.onescorpin.scheduler.JobScheduler;
import com.onescorpin.scheduler.QuartzScheduler;
import com.onescorpin.scheduler.TriggerIdentifier;
import com.onescorpin.scheduler.model.DefaultJobIdentifier;
import com.onescorpin.scheduler.model.DefaultTriggerIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.web.api.dto.BulletinDTO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 */
public class NifiStatsJmsReceiver implements ClusterServiceMessageReceiver {

    private static final Logger log = LoggerFactory.getLogger(NifiStatsJmsReceiver.class);

    @Inject
    private NifiNflowProcessorStatisticsProvider nifiEventStatisticsProvider;

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private ProvenanceEventNflowUtil provenanceEventNflowUtil;

    @Inject
    private NifiNflowStatisticsProvider nifiNflowStatisticsProvider;

    @Inject
    private NifiBulletinExceptionExtractor nifiBulletinExceptionExtractor;

    @Inject
    private BatchJobExecutionProvider batchJobExecutionProvider;

    @Inject
    private JobScheduler jobScheduler;

    @Value("${nova.ops.mgr.stats.compact.cron:0 0 0 1/1 * ? *}")
    private String compactStatsCronSchedule;

    @Value("${nova.ops.mgr.stats.compact.enabled:true}")
    private boolean compactStatsEnabled;

    public static final String NIFI_NFLOW_PROCESSOR_ERROR_CLUSTER_TYPE = "NIFI_NFLOW_PROCESSOR_ERROR";

    @Inject
    private ClusterService clusterService;

    @Value("${nova.ops.mgr.stats.nifi.bulletins.mem.size:30}")
    private Integer errorsToStorePerNflow = 30;

    @Value("${nova.ops.mgr.stats.nifi.bulletins.persist:false}")
    private boolean persistErrors = false;

    private LoadingCache<String, Queue<NifiNflowProcessorErrors>> nflowProcessorErrors = CacheBuilder.newBuilder().build(new CacheLoader<String, Queue<NifiNflowProcessorErrors>>() {
        @Override
        public Queue<NifiNflowProcessorErrors> load(String nflowName) throws Exception {
            return EvictingQueue.create(errorsToStorePerNflow);
        }

    });

    private Long lastBulletinId = -1L;

    @Inject
    private RetryProvenanceEventWithDelay retryProvenanceEventWithDelay;


    private static final String JMS_LISTENER_ID = "nifiStatesJmsListener";

    @PostConstruct
    private void init() {
        retryProvenanceEventWithDelay.setStatsJmsReceiver(this);
        scheduleStatsCompaction();
    }

    /**
     * Map of the summary stats.
     * This is used to see if we need to update the nflow stats or not
     * to reduce the query on saving all the stats
     */
    private Map<String, JpaNifiNflowStats> latestStatsCache = new ConcurrentHashMap<>();


    /**
     * Schedule the compaction job in Quartz if the properties have this enabled with a Cron Expression
     */
    private void scheduleStatsCompaction() {
        if (compactStatsEnabled && StringUtils.isNotBlank(compactStatsCronSchedule)) {
            QuartzScheduler scheduler = (QuartzScheduler) jobScheduler;
            JobIdentifier jobIdentifier = new DefaultJobIdentifier("Compact NiFi Processor Stats", "NOVA");
            TriggerIdentifier triggerIdentifier = new DefaultTriggerIdentifier(jobIdentifier.getName(), jobIdentifier.getGroup());
            try {
                scheduler.scheduleJob(jobIdentifier, triggerIdentifier, NiFiStatsCompactionQuartzJobBean.class, compactStatsCronSchedule, null);
            } catch (ObjectAlreadyExistsException e) {
                log.info("Unable to schedule the job to compact the NiFi processor stats.  It already exists.  Most likely another Nova node has already schceduled this job. ");
            }
            catch (SchedulerException e) {
                throw new RuntimeException("Error scheduling job: Compact NiFi Processor Stats", e);
            }
        }
    }

    /**
     * get Errors in memory for a nflow
     *
     * @param nflowName       the nflow name
     * @param afterTimestamp and optional timestamp to look after
     */
    public List<NifiNflowProcessorErrors> getErrorsForNflow(String nflowName, Long afterTimestamp) {
        return getErrorsForNflow(nflowName, afterTimestamp, null);
    }

    public List<NifiNflowProcessorErrors> getErrorsForNflow(String nflowName, Long startTime, Long endTime) {
        List<NifiNflowProcessorErrors> errors = null;
        Queue<NifiNflowProcessorErrors> queue = nflowProcessorErrors.getUnchecked(nflowName);
        if (queue != null) {
            if (startTime != null && endTime != null) {

                if (queue != null) {
                    errors =
                        queue.stream().filter(error -> error.getErrorMessageTimestamp().getMillis() >= startTime && error.getErrorMessageTimestamp().getMillis() <= endTime)
                            .collect(Collectors.toList());
                }
            } else if (startTime == null && endTime != null) {
                errors = queue.stream().filter(error -> error.getErrorMessageTimestamp().getMillis() <= endTime).collect(Collectors.toList());
            } else if (startTime != null && endTime == null) {
                errors = queue.stream().filter(error -> error.getErrorMessageTimestamp().getMillis() >= startTime).collect(Collectors.toList());
            } else {
                errors = new ArrayList<>();
                errors.addAll(queue);
            }
        } else {
            errors = Collections.emptyList();
        }

        return errors;
    }

    private String getNflowName(AggregatedNflowProcessorStatistics nflowProcessorStatistics) {
        String nflowName = null;
        String nflowProcessorId = nflowProcessorStatistics.getStartingProcessorId();
        if (nflowProcessorStatistics instanceof AggregatedNflowProcessorStatisticsV2) {
            nflowName = ((AggregatedNflowProcessorStatisticsV2) nflowProcessorStatistics).getNflowName();
        }
        if (nflowProcessorId != null && nflowName == null) {
            nflowName = provenanceEventNflowUtil.getNflowName(nflowProcessorId);
        }
        return nflowName;
    }

    /**
     * Ensure the cache and NiFi are up, or if not ensure the data exists in the NiFi cache to be processed
     *
     * @param stats the stats to process
     */
    public boolean readyToProcess(AggregatedNflowProcessorStatisticsHolder stats) {
        return provenanceEventNflowUtil.isNifiFlowCacheAvailable() || (!provenanceEventNflowUtil.isNifiFlowCacheAvailable() && stats.getNflowStatistics().values().stream()
            .allMatch(nflowProcessorStats -> StringUtils.isNotBlank(getNflowName(nflowProcessorStats))));
    }

    /**
     * If the incoming object is not a Retry object or if the incoming object is of type of Retry and we have not maxed out the retry attempts
     *
     * @param stats event holder
     * @return true  If the incoming object is not a Retry object or if the incoming object is of type of Retry and we have not maxed out the retry attempts, otherwise false
     */
    private boolean ensureValidRetryAttempt(AggregatedNflowProcessorStatisticsHolder stats) {
        return !(stats instanceof RetryAggregatedNflowProcessorStatisticsHolder) || (stats instanceof RetryAggregatedNflowProcessorStatisticsHolder
                                                                                    && ((RetryAggregatedNflowProcessorStatisticsHolder) stats).shouldRetry());
    }

    @JmsListener(id = JMS_LISTENER_ID, destination = Queues.PROVENANCE_EVENT_STATS_QUEUE, containerFactory = JmsConstants.JMS_CONTAINER_FACTORY)
    public void receiveTopic(AggregatedNflowProcessorStatisticsHolder stats) {
        if (readyToProcess(stats)) {

            if (ensureValidRetryAttempt(stats)) {
                final List<AggregatedNflowProcessorStatistics> unregisteredEvents = new ArrayList<>();
                metadataAccess.commit(() -> {
                    List<NifiNflowProcessorStats> summaryStats = createSummaryStats(stats, unregisteredEvents);

                    List<JpaNifiNflowProcessorStats> failedStatsWithFlowFiles = new ArrayList<>();
                    for (NifiNflowProcessorStats stat : summaryStats) {
                        NifiNflowProcessorStats savedStats = nifiEventStatisticsProvider.create(stat);
                        if (savedStats.getFailedCount() > 0L && savedStats.getLatestFlowFileId() != null) {
                            //offload the query to nifi and merge back in
                            failedStatsWithFlowFiles.add((JpaNifiNflowProcessorStats) savedStats);
                        }
                    }
                    if (stats instanceof AggregatedNflowProcessorStatisticsHolderV2) {
                        saveNflowStats((AggregatedNflowProcessorStatisticsHolderV2) stats, summaryStats);
                    }
                    if (!failedStatsWithFlowFiles.isEmpty()) {
                        assignNiFiBulletinErrors(failedStatsWithFlowFiles);
                    }
                    return summaryStats;
                }, MetadataAccess.SERVICE);

                if (clusterService.isClustered() && !unregisteredEvents.isEmpty()) {
                    //reprocess with delay
                    if (retryProvenanceEventWithDelay != null) {
                        retryProvenanceEventWithDelay.delay(stats, unregisteredEvents);
                    }
                }
            } else {
                //stop processing the events
                log.info("Unable find the nflow in Ops Manager.  Not processing {} stats ", stats.getNflowStatistics().values().size());

            }
        } else {
            log.info("NiFi is not up yet.  Sending back to JMS for later dequeue ");
            throw new JmsProcessingException("Unable to process Statistics Events.  NiFi is either not up, or there is an error trying to populate the Nova NiFi Flow Cache. ");
        }

    }


    private void assignNiFiBulletinErrors(List<JpaNifiNflowProcessorStats> stats) {

        //might need to query with the 'after' parameter

        //group the NflowStats by processorId_flowfileId

        Map<String, Map<String, List<JpaNifiNflowProcessorStats>>>
            processorFlowFilesStats =
            stats.stream().filter(s -> s.getProcessorId() != null)
                .collect(Collectors.groupingBy(NifiNflowProcessorStats::getProcessorId, Collectors.groupingBy(NifiNflowProcessorStats::getLatestFlowFileId)));

        Set<String> processorIds = processorFlowFilesStats.keySet();
        //strip out those processorIds that are part of a reusable flow
        Set<String> nonReusableFlowProcessorIds = processorIds.stream().filter(processorId -> !provenanceEventNflowUtil.isReusableFlowProcessor(processorId)).collect(Collectors.toSet());

        //find all errors for the processors
        List<BulletinDTO> errors = nifiBulletinExceptionExtractor.getErrorBulletinsForProcessorId(processorIds, lastBulletinId);

        if (errors != null && !errors.isEmpty()) {
            Set<JpaNifiNflowProcessorStats> statsToUpdate = new HashSet<>();
            // first look for matching nflow flow and processor ids.  otherwise look for processor id matches that are not part of reusable flows
            errors.stream().forEach(b -> {
                stats.stream().forEach(stat -> {
                    if (stat.getLatestFlowFileId() != null && b.getSourceId().equalsIgnoreCase(stat.getProcessorId()) && b.getMessage().contains(stat.getLatestFlowFileId())) {
                        stat.setErrorMessageTimestamp(getAdjustBulletinDateTime(b));
                        stat.setErrorMessages(b.getMessage());
                        addNflowProcessorError(stat);
                        statsToUpdate.add(stat);
                    } else if (nonReusableFlowProcessorIds.contains(b.getSourceId()) && b.getSourceId().equalsIgnoreCase(stat.getProcessorId())) {
                        stat.setErrorMessageTimestamp(getAdjustBulletinDateTime(b));
                        stat.setErrorMessages(b.getMessage());
                        addNflowProcessorError(stat);
                        statsToUpdate.add(stat);
                    }
                });
            });
            lastBulletinId = errors.stream().mapToLong(b -> b.getId()).max().getAsLong();

            if (!statsToUpdate.isEmpty()) {
                notifyClusterOfNflowProcessorErrors(statsToUpdate);
                if (persistErrors) {
                    nifiEventStatisticsProvider.save(new ArrayList<>(statsToUpdate));
                }
            }
        }
    }

    /**
     * the BulletinDTO comes back from nifi as a Date object in the year 1970
     * We need to convert this to the current date and account for DST
     *
     * @param b the bulletin
     */
    private DateTime getAdjustBulletinDateTime(BulletinDTO b) {
        DateTimeZone defaultZone = DateTimeZone.getDefault();

        int currentOffsetMillis = defaultZone.getOffset(DateTime.now().getMillis());
        double currentOffsetHours = (double) currentOffsetMillis / 1000d / 60d / 60d;

        long bulletinOffsetMillis = DateTimeZone.getDefault().getOffset(b.getTimestamp().getTime());

        double bulletinOffsetHours = (double) bulletinOffsetMillis / 1000d / 60d / 60d;

        DateTime adjustedTime = new DateTime(b.getTimestamp()).withDayOfYear(DateTime.now().getDayOfYear()).withYear(DateTime.now().getYear());
        int adjustedHours = 0;
        if (currentOffsetHours != bulletinOffsetHours) {
            adjustedHours = new Double(bulletinOffsetHours - currentOffsetHours).intValue();
            adjustedTime = adjustedTime.plusHours(-adjustedHours);
        }
        return adjustedTime;
    }

    private void addNflowProcessorError(NifiNflowProcessorErrors error) {
        Queue<NifiNflowProcessorErrors> q = nflowProcessorErrors.getUnchecked(error.getNflowName());
        if (q != null) {
            q.add(error);
        }
    }


    /**
     * Save the running totals for the nflow
     */
    private Map<String, JpaNifiNflowStats> saveNflowStats(AggregatedNflowProcessorStatisticsHolderV2 holder, List<NifiNflowProcessorStats> summaryStats) {
        Map<String, JpaNifiNflowStats> nflowStatsMap = new HashMap<>();

        if (summaryStats != null) {
            Map<String, Long> nflowLatestTimestamp = summaryStats.stream().collect(Collectors.toMap(NifiNflowProcessorStats::getNflowName, stats -> stats.getMinEventTime().getMillis(), Long::max));
            nflowLatestTimestamp.entrySet().stream().forEach(e -> {
                String nflowName = e.getKey();
                Long timestamp = e.getValue();
                JpaNifiNflowStats stats = nflowStatsMap.computeIfAbsent(nflowName, name -> new JpaNifiNflowStats(nflowName));
                OpsManagerNflow opsManagerNflow = provenanceEventNflowUtil.getNflow(nflowName);
                if (opsManagerNflow != null) {
                    stats.setNflowId(new JpaNifiNflowStats.OpsManagerNflowId(opsManagerNflow.getId().toString()));
                }
                stats.setLastActivityTimestamp(timestamp);
            });
        }
        if (holder.getProcessorIdRunningFlows() != null) {
            holder.getProcessorIdRunningFlows().entrySet().stream().forEach(e -> {
                String nflowProcessorId = e.getKey();
                Long runningCount = e.getValue();
                String nflowName = provenanceEventNflowUtil.getNflowName(nflowProcessorId);  //ensure not null
                if (StringUtils.isNotBlank(nflowName)) {
                    JpaNifiNflowStats stats = nflowStatsMap.computeIfAbsent(nflowName, name -> new JpaNifiNflowStats(nflowName));
                    OpsManagerNflow opsManagerNflow = provenanceEventNflowUtil.getNflow(nflowName);
                    if (opsManagerNflow != null) {
                        stats.setNflowId(new JpaNifiNflowStats.OpsManagerNflowId(opsManagerNflow.getId().toString()));
                        stats.setStream(opsManagerNflow.isStream());
                    }
                    stats.addRunningNflowFlows(runningCount);
                    if(holder instanceof AggregatedNflowProcessorStatisticsHolderV3){
                        stats.setTime(((AggregatedNflowProcessorStatisticsHolderV3)holder).getTimestamp());
                        stats.setLastActivityTimestamp(((AggregatedNflowProcessorStatisticsHolderV3)holder).getTimestamp());
                    }
                    else {
                        stats.setTime(DateTime.now().getMillis());
                    }
                }
            });
        }

        //group stats to save together by nflow name
        if (!nflowStatsMap.isEmpty()) {
            //only save those that have changed
            List<NifiNflowStats> updatedStats = nflowStatsMap.entrySet().stream().filter(e -> {
                                                                                           String key = e.getKey();
                                                                                           JpaNifiNflowStats value = e.getValue();
                                                                                           JpaNifiNflowStats savedStats = latestStatsCache.computeIfAbsent(key, name -> value);
                                                                                           return ((value.getLastActivityTimestamp() != null && savedStats.getLastActivityTimestamp() != null && value.getLastActivityTimestamp() > savedStats.getLastActivityTimestamp()) ||
                                                                                                   (value.getLastActivityTimestamp() != null && value.getRunningNflowFlows() != savedStats.getRunningNflowFlows()));
                                                                                       }
            ).map(e -> e.getValue()).collect(Collectors.toList());

            //if the running flows are 0 and its streaming we should try back to see if this nflow is running or not
            updatedStats.stream().filter(s -> s.isStream()).forEach(stats -> {
                latestStatsCache.put(stats.getNflowName(), (JpaNifiNflowStats) stats);
                if (stats.getRunningNflowFlows() == 0L) {
                    batchJobExecutionProvider.markStreamingNflowAsStopped(stats.getNflowName());
                } else {
                    batchJobExecutionProvider.markStreamingNflowAsStarted(stats.getNflowName());
                }
            });
            nifiNflowStatisticsProvider.saveLatestNflowStats(updatedStats);
        }
        return nflowStatsMap;
    }

    private List<NifiNflowProcessorStats> createSummaryStats(AggregatedNflowProcessorStatisticsHolder holder, final List<AggregatedNflowProcessorStatistics> unregisteredEvents) {
        List<NifiNflowProcessorStats> nifiNflowProcessorStatsList = new ArrayList<>();

        holder.getNflowStatistics().values().stream().forEach(nflowProcessorStats -> {
            Long collectionIntervalMillis = nflowProcessorStats.getCollectionIntervalMillis();
            String nflowProcessorId = nflowProcessorStats.getStartingProcessorId();
            String nflowName = getNflowName(nflowProcessorStats);
            if (StringUtils.isNotBlank(nflowName)) {
                String nflowProcessGroupId = provenanceEventNflowUtil.getNflowProcessGroupId(nflowProcessorId);

                nflowProcessorStats.getProcessorStats().values().forEach(processorStats -> {
                    processorStats.getStats().values().stream().forEach(stats -> {
                        NifiNflowProcessorStats
                            nifiNflowProcessorStats =
                            toSummaryStats(stats);
                        nifiNflowProcessorStats.setNflowName(nflowName);
                        nifiNflowProcessorStats
                            .setProcessorId(processorStats.getProcessorId());
                        nifiNflowProcessorStats.setCollectionIntervalSeconds(
                            (collectionIntervalMillis / 1000));
                        if (holder instanceof AggregatedNflowProcessorStatisticsHolderV2) {
                            nifiNflowProcessorStats
                                .setCollectionId(((AggregatedNflowProcessorStatisticsHolderV2) holder).getCollectionId());
                        }
                        String
                            processorName =
                            provenanceEventNflowUtil
                                .getProcessorName(processorStats.getProcessorId());
                        if (processorName == null) {
                            processorName = processorStats.getProcessorName();
                        }
                        nifiNflowProcessorStats.setProcessorName(processorName);
                        nifiNflowProcessorStats
                            .setNflowProcessGroupId(nflowProcessGroupId);
                        nifiNflowProcessorStatsList.add(nifiNflowProcessorStats);
                    });
                });
            } else {
                unregisteredEvents.add(nflowProcessorStats);
            }


        });

        if (!unregisteredEvents.isEmpty()) {

            //reprocess

        }
        return nifiNflowProcessorStatsList;

    }


    private NifiNflowProcessorStats toSummaryStats(GroupedStats groupedStats) {
        NifiNflowProcessorStats nifiNflowProcessorStats = new JpaNifiNflowProcessorStats();
        nifiNflowProcessorStats.setTotalCount(groupedStats.getTotalCount());
        nifiNflowProcessorStats.setFlowFilesFinished(groupedStats.getFlowFilesFinished());
        nifiNflowProcessorStats.setFlowFilesStarted(groupedStats.getFlowFilesStarted());
        nifiNflowProcessorStats.setCollectionId(groupedStats.getGroupKey());
        nifiNflowProcessorStats.setBytesIn(groupedStats.getBytesIn());
        nifiNflowProcessorStats.setBytesOut(groupedStats.getBytesOut());
        nifiNflowProcessorStats.setDuration(groupedStats.getDuration());
        nifiNflowProcessorStats.setJobsFinished(groupedStats.getJobsFinished());
        nifiNflowProcessorStats.setJobsStarted(groupedStats.getJobsStarted());
        nifiNflowProcessorStats.setProcessorsFailed(groupedStats.getProcessorsFailed());
        nifiNflowProcessorStats.setCollectionTime(new DateTime(groupedStats.getTime()));
        nifiNflowProcessorStats.setMinEventTime(new DateTime(groupedStats.getMinTime()));
        nifiNflowProcessorStats.setMinEventTimeMillis(nifiNflowProcessorStats.getMinEventTime().getMillis());
        nifiNflowProcessorStats.setMaxEventTime(new DateTime(groupedStats.getMaxTime()));
        nifiNflowProcessorStats.setJobsFailed(groupedStats.getJobsFailed());
        nifiNflowProcessorStats.setSuccessfulJobDuration(groupedStats.getSuccessfulJobDuration());
        nifiNflowProcessorStats.setJobDuration(groupedStats.getJobDuration());
        nifiNflowProcessorStats.setMaxEventId(groupedStats.getMaxEventId());
        nifiNflowProcessorStats.setFailedCount(groupedStats.getProcessorsFailed());
        if (groupedStats instanceof GroupedStatsV2) {
            nifiNflowProcessorStats.setLatestFlowFileId(((GroupedStatsV2) groupedStats).getLatestFlowFileId());
        }
        if (provenanceEventNflowUtil.isFailure(groupedStats.getSourceConnectionIdentifier())) {
            nifiNflowProcessorStats.setFailedCount(groupedStats.getTotalCount() + groupedStats.getProcessorsFailed());
        }

        return nifiNflowProcessorStats;
    }


    public boolean isPersistErrors() {
        return persistErrors;
    }

    @Override
    public void onMessageReceived(String from, ClusterMessage message) {

        if (message != null && NIFI_NFLOW_PROCESSOR_ERROR_CLUSTER_TYPE.equalsIgnoreCase(message.getType())) {
            NifiNflowProcessorStatsErrorClusterMessage content = (NifiNflowProcessorStatsErrorClusterMessage) message.getMessage();
            if (content != null && content.getErrors() != null) {
                content.getErrors().stream().forEach(error -> addNflowProcessorError(error));
            }
        }
    }

    private void notifyClusterOfNflowProcessorErrors(Set<? extends NifiNflowProcessorErrors> errors) {
        if (clusterService.isClustered()) {
            clusterService.sendMessageToOthers(NIFI_NFLOW_PROCESSOR_ERROR_CLUSTER_TYPE, new NifiNflowProcessorStatsErrorClusterMessage(errors));
        }
    }

}
