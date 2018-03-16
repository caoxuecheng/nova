package com.onescorpin.metadata.jpa.jobrepo.nifi;

/*-
 * #%L
 * onescorpin-operational-metadata-jpa
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

import com.google.common.collect.Lists;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.onescorpin.metadata.api.common.ItemLastModifiedProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorErrors;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats;
import com.onescorpin.metadata.jpa.nflow.NflowAclIndexQueryAugmentor;
import com.onescorpin.metadata.jpa.nflow.QJpaOpsManagerNflow;
import com.onescorpin.security.AccessController;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;

/**
 * Provider for accessing the statistics for a nflow and processor
 */
@Service
public class NifiNflowProcessorStatisticsProvider implements com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStatisticsProvider {

    public static final String ITEM_LAST_MODIFIED_KEY = "NIFI_NFLOW_PROCESSOR_STATS";

    @Autowired
    private JPAQueryFactory factory;

    private NifiNflowProcessorStatisticsRepository statisticsRepository;

    private NifiEventRepository nifiEventRepository;

    @Inject
    private ItemLastModifiedProvider itemLastModifiedProvider;

    @Inject
    private NifiEventProvider nifiEventProvider;

    @Inject
    private AccessController accessController;

    @Autowired
    public NifiNflowProcessorStatisticsProvider(NifiNflowProcessorStatisticsRepository repository, NifiEventRepository nifiEventRepository) {
        this.statisticsRepository = repository;
        this.nifiEventRepository = nifiEventRepository;
    }

    private String getLastModifiedKey(String clusterId) {
        if (StringUtils.isBlank(clusterId) || "Node".equalsIgnoreCase(clusterId) || "non-clustered-node-id".equalsIgnoreCase(clusterId)) {
            return ITEM_LAST_MODIFIED_KEY;
        } else {
            return ITEM_LAST_MODIFIED_KEY + "-" + clusterId;
        }
    }

    @Override
    public NifiNflowProcessorStats create(NifiNflowProcessorStats t) {
        NifiNflowProcessorStats stats = statisticsRepository.save((JpaNifiNflowProcessorStats) t);
        return stats;
    }

    public List<? extends JpaNifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorId(String nflowName, TimeFrame timeFrame) {
        DateTime now = DateTime.now();
        return findNflowProcessorStatisticsByProcessorId(nflowName, timeFrame.startTimeRelativeTo(now), now);
    }

    public List<? extends JpaNifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorName(String nflowName, TimeFrame timeFrame) {
        DateTime now = DateTime.now();
        return findNflowProcessorStatisticsByProcessorName(nflowName, timeFrame.startTimeRelativeTo(now), now);
    }

    public List<? extends JpaNifiNflowProcessorStats> findForNflowStatisticsGroupedByTime(String nflowName, TimeFrame timeFrame) {
        DateTime now = DateTime.now();
        return findForNflowStatisticsGroupedByTime(nflowName, timeFrame.startTimeRelativeTo(now), now);
    }


    @Override
    public List<? extends JpaNifiNflowProcessorStats> findWithinTimeWindow(DateTime start, DateTime end) {
        return accessController.isEntityAccessControlled() ? statisticsRepository.findWithinTimeWindowWithAcl(start, end) : statisticsRepository.findWithinTimeWindowWithoutAcl(start, end);
    }


    private Predicate withinDateTime(DateTime start, DateTime end) {
        QJpaNifiNflowProcessorStats stats = QJpaNifiNflowProcessorStats.jpaNifiNflowProcessorStats;
        Predicate p = null;

        if (start == null && end == null) {
            return p;
        }
        if (start != null && end != null) {
            p = stats.minEventTime.goe(start).and(stats.maxEventTime.loe(end));
        } else if (start == null) {
            p = stats.maxEventTime.loe(end);
        } else if (end == null) {
            p = stats.minEventTime.goe(start);
        }
        return p;
    }


    /**
     * Find stats for a given nflow between the two dates not grouped
     */
    private List<? extends JpaNifiNflowProcessorStats> findForNflow(String nflowName, DateTime start, DateTime end) {
        QJpaNifiNflowProcessorStats stats = QJpaNifiNflowProcessorStats.jpaNifiNflowProcessorStats;
        Iterable<JpaNifiNflowProcessorStats> result = statisticsRepository.findAll(stats.nflowName.eq(nflowName).and(withinDateTime(start, end)));
        if (result != null) {
            return Lists.newArrayList(result);
        }
        return null;
    }


    @Override
    public List<? extends JpaNifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorId(String nflowName, DateTime start, DateTime end) {
        QJpaNifiNflowProcessorStats stats = QJpaNifiNflowProcessorStats.jpaNifiNflowProcessorStats;
        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        JPAQuery
            query = factory.select(
            Projections.bean(JpaNifiNflowProcessorStats.class,
                             stats.nflowName, stats.processorId, stats.processorName,
                             stats.bytesIn.sum().as("bytesIn"), stats.bytesOut.sum().as("bytesOut"), stats.duration.sum().as("duration"),
                             stats.jobsStarted.sum().as("jobsStarted"), stats.jobsFinished.sum().as("jobsFinished"), stats.jobDuration.sum().as("jobDuration"),
                             stats.flowFilesStarted.sum().as("flowFilesStarted"), stats.flowFilesFinished.sum().as("flowFilesFinished"), stats.totalCount.sum().as("totalCount"),
                             stats.maxEventTime.max().as("maxEventTime"), stats.minEventTime.min().as("minEventTime"), stats.jobsFailed.sum().as("jobsFailed"),
                             stats.failedCount.sum().as("failedCount"),
                             stats.count().as("resultSetCount"))
        )
            .from(stats)
            .innerJoin(nflow).on(nflow.name.eq(stats.nflowName))
            .where(stats.nflowName.eq(nflowName)
                       .and(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, accessController.isEntityAccessControlled()))
                       .and(stats.minEventTime.goe(start)
                                .and(stats.maxEventTime.loe(end))))
            .groupBy(stats.nflowName, stats.processorId, stats.processorName)
            .orderBy(stats.processorName.asc());

        return (List<JpaNifiNflowProcessorStats>) query.fetch();
    }


    @Override
    public List<? extends JpaNifiNflowProcessorStats> findNflowProcessorStatisticsByProcessorName(String nflowName, DateTime start, DateTime end) {
        QJpaNifiNflowProcessorStats stats = QJpaNifiNflowProcessorStats.jpaNifiNflowProcessorStats;

        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;

        JPAQuery
            query = factory.select(
            Projections.bean(JpaNifiNflowProcessorStats.class,
                             stats.nflowName, stats.processorName,
                             stats.bytesIn.sum().as("bytesIn"), stats.bytesOut.sum().as("bytesOut"), stats.duration.sum().as("duration"),
                             stats.jobsStarted.sum().as("jobsStarted"), stats.jobsFinished.sum().as("jobsFinished"), stats.jobDuration.sum().as("jobDuration"),
                             stats.flowFilesStarted.sum().as("flowFilesStarted"), stats.flowFilesFinished.sum().as("flowFilesFinished"), stats.totalCount.sum().as("totalCount"),
                             stats.maxEventTime.max().as("maxEventTime"), stats.minEventTime.min().as("minEventTime"), stats.jobsFailed.sum().as("jobsFailed"),
                             stats.failedCount.sum().as("failedCount"),
                             stats.count().as("resultSetCount"))
        )
            .from(stats)
            .innerJoin(nflow).on(nflow.name.eq(stats.nflowName))
            .where(stats.nflowName.eq(nflowName)
                       .and(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, accessController.isEntityAccessControlled()))
                       .and(stats.minEventTime.goe(start)
                                .and(stats.maxEventTime.loe(end))))
            .groupBy(stats.nflowName, stats.processorName)
            .orderBy(stats.processorName.asc());

        return (List<JpaNifiNflowProcessorStats>) query.fetch();
    }

    public List<? extends JpaNifiNflowProcessorStats> findForNflowStatisticsGroupedByTime(String nflowName, DateTime start, DateTime end) {
        QJpaNifiNflowProcessorStats stats = QJpaNifiNflowProcessorStats.jpaNifiNflowProcessorStats;

        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;

        JPAQuery
            query = factory.select(
            Projections.bean(JpaNifiNflowProcessorStats.class,
                             stats.nflowName,
                             stats.bytesIn.sum().as("bytesIn"), stats.bytesOut.sum().as("bytesOut"), stats.duration.sum().as("duration"),
                             stats.jobsStarted.sum().as("jobsStarted"), stats.jobsFinished.sum().as("jobsFinished"), stats.jobDuration.sum().as("jobDuration"),
                             stats.flowFilesStarted.sum().as("flowFilesStarted"), stats.flowFilesFinished.sum().as("flowFilesFinished"), stats.failedCount.sum().as("failedCount"),
                             stats.minEventTime,
                             stats.jobsStarted.sum().divide(stats.collectionIntervalSeconds).castToNum(BigDecimal.class).as("jobsStartedPerSecond"),
                             stats.jobsFinished.sum().divide(stats.collectionIntervalSeconds).castToNum(BigDecimal.class).as("jobsFinishedPerSecond"),
                             stats.collectionIntervalSeconds.as("collectionIntervalSeconds"),
                             stats.jobsFailed.sum().as("jobsFailed"), stats.totalCount.sum().as("totalCount"),
                             stats.count().as("resultSetCount"))
        )
            .from(stats)
            .innerJoin(nflow).on(nflow.name.eq(stats.nflowName))
            .where(stats.nflowName.eq(nflowName)
                       .and(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, accessController.isEntityAccessControlled()))
                       .and(stats.minEventTime.goe(start)
                                .and(stats.maxEventTime.loe(end))))

            .groupBy(stats.nflowName, stats.minEventTime, stats.collectionIntervalSeconds)
            .orderBy(stats.minEventTime.asc());

        return (List<JpaNifiNflowProcessorStats>) query.fetch();
    }

    public List<? extends NifiNflowProcessorErrors> findNflowProcessorErrors(String nflowName, DateTime start, DateTime end) {
        return accessController.isEntityAccessControlled() ? statisticsRepository.findWithErrorsWithinTimeWithAcl(nflowName, start, end)
                                                           : statisticsRepository.findWithErrorsWithinTimeWithoutAcl(nflowName, start, end);
    }


    public List<? extends NifiNflowProcessorErrors> findNflowProcessorErrorsAfter(String nflowName, DateTime after) {
        return accessController.isEntityAccessControlled() ? statisticsRepository.findWithErrorsAfterTimeWithAcl(nflowName, after)
                                                           : statisticsRepository.findWithErrorsAfterTimeWithoutAcl(nflowName, after);
    }

    @Override
    public List<NifiNflowProcessorStats> findLatestFinishedStats(String nflowName) {
        if (accessController.isEntityAccessControlled()) {
            DateTime latestTime = statisticsRepository.findLatestFinishedTimeWithAcl(nflowName).getDateProjection();
            return statisticsRepository.findLatestFinishedStatsWithAcl(nflowName, latestTime);
        } else {
            return findLatestFinishedStatsWithoutAcl(nflowName);
        }
    }

    @Override
    public List<NifiNflowProcessorStats> findLatestFinishedStatsWithoutAcl(String nflowName) {
        DateTime latestTime = statisticsRepository.findLatestFinishedTimeWithoutAcl(nflowName).getDateProjection();
        return statisticsRepository.findLatestFinishedStatsWithoutAcl(nflowName, latestTime);
    }


    @Override
    public List<? extends NifiNflowProcessorStats> save(List<? extends NifiNflowProcessorStats> stats) {
        if (stats != null && !stats.isEmpty()) {
            return statisticsRepository.save((List<JpaNifiNflowProcessorStats>) stats);
        }
        return stats;
    }

    /**
     * Call the procedure to compact the NIFI_NFLOW_PROCESSOR_STATS table
     * @return a summary of what was compacted
     */
    public String compactNflowProcessorStatistics(){
        return statisticsRepository.compactNflowProcessorStats();
    }
}
