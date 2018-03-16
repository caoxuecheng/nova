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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTO;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatistics;
import com.onescorpin.nifi.provenance.model.stats.AggregatedProcessorStatistics;
import com.onescorpin.nifi.provenance.model.stats.AggregatedProcessorStatisticsV2;
import com.onescorpin.nifi.provenance.util.ProvenanceEventUtil;

import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Manage the Nflow Stats calculation and sending of events to Ops Manager
 */
public class NflowStatisticsManager {

    private static final Logger log = LoggerFactory.getLogger(NflowStatisticsManager.class);

    private Long sendJmsTimeMillis = ConfigurationProperties.DEFAULT_RUN_INTERVAL_MILLIS; //every 3 seconds

    private Lock lock = new ReentrantLock();

    private Map<String, NflowStatistics> nflowStatisticsMap = new ConcurrentHashMap<>();

    private static final NflowStatisticsManager instance = new NflowStatisticsManager();

    private NflowStatisticsManager() {
        initTimerThread();
    }

    public static NflowStatisticsManager getInstance() {
        return instance;
    }


    private ThreadFactory gatherStatsThreadFactory = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("NflowStatisticsManager-GatherStats-%d").build();

    private ThreadFactory sendJmsThreadFactory = new ThreadFactoryBuilder().setDaemon(true)
        .setNameFormat("NflowStatisticsManager-SendStats-%d").build();

    /**
     * Scheduled task to gather stats and send to JMS
     */
    private ScheduledFuture gatherStatsScheduledFuture;

    /**
     * Service to schedule the sending of events to activemq
     */
    private ExecutorService jmsService = Executors.newFixedThreadPool(3, sendJmsThreadFactory);

    private ScheduledExecutorService jmsGatherEventsToSendService = Executors.newSingleThreadScheduledExecutor(gatherStatsThreadFactory);



    public void addEvent(ProvenanceEventRecord event, Long eventId) {
        lock.lock();
        try {
            //build up nflow flow file map relationships
            boolean isStartingNflowFlow = ProvenanceEventUtil.isStartingNflowFlow(event);
            if (isStartingNflowFlow) {
                NflowEventStatistics.getInstance().checkAndAssignStartingFlowFile(event);
            }
            NflowEventStatistics.getInstance().assignParentsAndChildren(event);

            //generate statistics and process the event
            String nflowProcessorId = NflowEventStatistics.getInstance().getNflowProcessorId(event);
            if (nflowProcessorId != null) {
                String key = nflowProcessorId + event.getComponentId();
                nflowStatisticsMap.computeIfAbsent(key, nflowStatisticsKey -> new NflowStatistics(nflowProcessorId, event.getComponentId())).addEvent(event, eventId);
            } else {
                //UNABLE TO FIND data in maps
            }
        } finally {
            lock.unlock();
        }
    }

    public void gatherStatistics() {
        lock.lock();
        List<ProvenanceEventRecordDTO> eventsToSend = null;
        Map<String, AggregatedNflowProcessorStatistics> statsToSend = null;
        try {
            //Gather Events and Stats to send Ops Manager
            eventsToSend = nflowStatisticsMap.values().stream().flatMap(stats -> stats.getEventsToSend().stream()).collect(Collectors.toList());

            final String collectionId = UUID.randomUUID().toString();
            Map<String,Long> runningFlowsCount = new HashMap<>();

            for (NflowStatistics nflowStatistics : nflowStatisticsMap.values()) {
                if (nflowStatistics.hasStats()) {
                    if (statsToSend == null) {
                        statsToSend = new ConcurrentHashMap<>();
                    }
                    AggregatedNflowProcessorStatistics
                        nflowProcessorStatistics =
                        statsToSend.computeIfAbsent(nflowStatistics.getNflowProcessorId(),
                                                    nflowProcessorId -> new AggregatedNflowProcessorStatistics(nflowStatistics.getNflowProcessorId(), collectionId, sendJmsTimeMillis));

                    AggregatedProcessorStatistics
                        processorStatistics =
                        nflowProcessorStatistics.getProcessorStats()
                            .computeIfAbsent(nflowStatistics.getProcessorId(), processorId -> new AggregatedProcessorStatisticsV2(nflowStatistics.getProcessorId(), null, collectionId));

                    //accumulate the stats together into the processorStatistics object grouped by source connection id
                    nflowStatistics.getStats().stream().forEach(stats -> {
                        NflowProcessorStatisticsAggregator.getInstance().addStats1(processorStatistics.getStats(stats.getSourceConnectionIdentifier()), stats);
                    });
                }
            }

            if ((eventsToSend != null && !eventsToSend.isEmpty()) || (statsToSend != null && !statsToSend.isEmpty())) {
                //send it off to jms on a different thread
                JmsSender jmsSender = new JmsSender(eventsToSend, statsToSend.values(),NflowEventStatistics.getInstance().getRunningNflowFlowsChanged());
                this.jmsService.submit(new JmsSenderConsumer(jmsSender));
            }
            else {
                //if we are empty but the runningFlows have changed, then send off as well
                if(NflowEventStatistics.getInstance().isNflowProcessorRunningNflowFlowsChanged()){
                    JmsSender jmsSender = new JmsSender(null, null,NflowEventStatistics.getInstance().getRunningNflowFlowsChanged());
                    this.jmsService.submit(new JmsSenderConsumer(jmsSender));
                }

            }


        } finally {
            NflowEventStatistics.getInstance().markNflowProcessorRunningNflowFlowsUnchanged();

            nflowStatisticsMap.values().stream().forEach(stats -> stats.clear());
            lock.unlock();
        }


    }

    private Runnable gatherStatisticsTask = new Runnable() {
        @Override
        public void run() {
            gatherStatistics();
        }
    };

    public void resetStatisticsInterval(Long interval) {
        lock.lock();
        sendJmsTimeMillis = interval;
        try {
            if(gatherStatsScheduledFuture != null){
                gatherStatsScheduledFuture.cancel(true);
            }
            initGatherStatisticsTimerThread(interval);

        } finally {
            lock.unlock();
        }
    }

    public void resetMaxEvents(Integer limit) {
        lock.lock();
        try {
            nflowStatisticsMap.values().forEach(stats -> stats.setLimit(limit));
        } finally {
            lock.unlock();
        }
    }


    private void initTimerThread() {
        Long runInterval = ConfigurationProperties.getInstance().getNflowProcessingRunInterval();
        this.sendJmsTimeMillis = runInterval;
        initGatherStatisticsTimerThread(runInterval);
        log.info("Initialized Timer Thread to gather statistics and send events to JMS running every {} ms ",sendJmsTimeMillis);
    }

    //jms thread

    /**
     * Start the timer thread
     */
    private ScheduledFuture initGatherStatisticsTimerThread(Long time) {
        gatherStatsScheduledFuture = jmsGatherEventsToSendService.scheduleAtFixedRate(gatherStatisticsTask, time, time, TimeUnit.MILLISECONDS);
        return gatherStatsScheduledFuture;

    }

}
