package com.onescorpin.metadata.jpa.nflow;

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
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.onescorpin.DateTimeUtil;
import com.onescorpin.alerts.api.Alert;
import com.onescorpin.alerts.api.AlertCriteria;
import com.onescorpin.alerts.api.AlertProvider;
import com.onescorpin.alerts.spi.AlertManager;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.alerts.OperationalAlerts;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.nflow.NflowHealth;
import com.onescorpin.metadata.api.nflow.NflowSummary;
import com.onescorpin.metadata.api.nflow.LatestNflowJobExecution;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.ExecutionConstants;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.api.jobrepo.job.JobStatusCount;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStats;
import com.onescorpin.metadata.jpa.cache.AbstractCacheBackedProvider;
import com.onescorpin.metadata.jpa.common.EntityAccessControlled;
import com.onescorpin.metadata.jpa.jobrepo.job.JpaBatchJobExecutionStatusCounts;
import com.onescorpin.metadata.jpa.jobrepo.job.QJpaBatchJobExecution;
import com.onescorpin.metadata.jpa.jobrepo.job.QJpaBatchJobInstance;
import com.onescorpin.metadata.jpa.jobrepo.nifi.JpaNifiNflowStats;
import com.onescorpin.metadata.jpa.sla.JpaServiceLevelAgreementDescription;
import com.onescorpin.metadata.jpa.sla.JpaServiceLevelAgreementDescriptionRepository;
import com.onescorpin.metadata.jpa.support.GenericQueryDslFilter;
import com.onescorpin.metadata.jpa.support.JobStatusDslQueryExpressionBuilder;
import com.onescorpin.security.AccessController;
import com.onescorpin.support.NflowNameUtil;

import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Provider allowing access to nflows {@link OpsManagerNflow}
 */
@Service
public class OpsNflowManagerNflowProvider extends AbstractCacheBackedProvider<OpsManagerNflow, OpsManagerNflow.ID> implements OpsManagerNflowProvider {

    private static final Logger log = LoggerFactory.getLogger(OpsNflowManagerNflowProvider.class);
    @Inject
    BatchJobExecutionProvider batchJobExecutionProvider;

    @Inject
    NifiNflowStatisticsProvider nifiNflowStatisticsProvider;

    private OpsManagerNflowRepository repository;
    private NflowHealthRepository nflowHealthRepository;
    private LatestNflowJobExectionRepository latestNflowJobExectionRepository;
    private BatchNflowSummaryCountsRepository batchNflowSummaryCountsRepository;
    private JpaServiceLevelAgreementDescriptionRepository serviceLevelAgreementDescriptionRepository;

    @Autowired
    private JPAQueryFactory factory;

    @Inject
    private AccessController accessController;

    @Inject
    private AlertProvider alertProvider;

    @Inject
    @Named("novaAlertManager")
    private AlertManager alertManager;

    @Inject
    private NflowSummaryRepository nflowSummaryRepository;

    @Inject
    private MetadataEventService metadataEventService;

    @Inject
    private OpsManagerNflowCacheByName opsManagerNflowCacheByName;

    @Inject
    private OpsManagerNflowCacheById opsManagerNflowCacheById;

    @Inject
    private NifiNflowStatisticsProvider nflowStatisticsProvider;

    @Inject
    private MetadataAccess metadataAccess;

    @Value("${nova.ops.mgr.ensure-unique-nflow-name:true}")
    private boolean ensureUniqueNflowName = true;

    private static String CLUSTER_MESSAGE_KEY = "OPS_MANAGER_NFLOW_CACHE";

    @Override
    public String getClusterMessageKey() {
        return CLUSTER_MESSAGE_KEY;
    }

    @Override
    public OpsManagerNflow.ID getId(OpsManagerNflow value) {
        return value.getId();
    }


    @Override
    public String getProviderName() {
        return this.getClass().getName();
    }

    @Autowired
    public OpsNflowManagerNflowProvider(OpsManagerNflowRepository repository, BatchNflowSummaryCountsRepository batchNflowSummaryCountsRepository,
                                      NflowHealthRepository nflowHealthRepository,
                                      LatestNflowJobExectionRepository latestNflowJobExectionRepository,
                                      JpaServiceLevelAgreementDescriptionRepository serviceLevelAgreementDescriptionRepository) {
        super(repository);
        this.repository = repository;
        this.batchNflowSummaryCountsRepository = batchNflowSummaryCountsRepository;
        this.nflowHealthRepository = nflowHealthRepository;
        this.latestNflowJobExectionRepository = latestNflowJobExectionRepository;
        this.serviceLevelAgreementDescriptionRepository = serviceLevelAgreementDescriptionRepository;
    }


    @PostConstruct
    private void init() {
        subscribeListener(opsManagerNflowCacheByName);
        subscribeListener(opsManagerNflowCacheById);
        clusterService.subscribe(this, getClusterMessageKey());
        //initially populate
        populateCache();
    }

    @Override
    public OpsManagerNflow.ID resolveId(Serializable id) {
        if (id instanceof OpsManagerNflowId) {
            return (OpsManagerNflowId) id;
        } else {
            return new OpsManagerNflowId(id);
        }
    }

    @Override
    @EntityAccessControlled
    public OpsManagerNflow findByName(String name) {
        // return repository.findByName(name);
        return opsManagerNflowCacheByName.findById(name);
    }

    public OpsManagerNflow findByNameWithoutAcl(String name) {
        OpsManagerNflow nflow = opsManagerNflowCacheByName.findByIdWithoutAcl(name);
        if (nflow == null) {
            nflow = repository.findByNameWithoutAcl(name);
        }
        return nflow;
    }

    @EntityAccessControlled
    public OpsManagerNflow findById(OpsManagerNflow.ID id) {
        return opsManagerNflowCacheById.findById(id);
    }

    @EntityAccessControlled
    public List<? extends OpsManagerNflow> findByNflowIds(List<OpsManagerNflow.ID> ids) {
        return opsManagerNflowCacheById.findByIds(ids);
    }

    public List<? extends OpsManagerNflow> findByNflowIdsWithoutAcl(List<OpsManagerNflow.ID> ids) {
        if (ids != null) {
            return opsManagerNflowCacheById.findByIdsWithoutAcl(new HashSet<>(ids));
        } else {
            return Collections.emptyList();
        }
    }


    @EntityAccessControlled
    public List<? extends OpsManagerNflow> findByNflowNames(Set<String> nflowNames) {
        return opsManagerNflowCacheByName.findByIds(nflowNames, true);
    }

    @EntityAccessControlled
    public List<? extends OpsManagerNflow> findByNflowNames(Set<String> nflowNames, boolean addAclFilter) {
        return opsManagerNflowCacheByName.findByIds(nflowNames, addAclFilter);
    }

    @Override
    public void save(List<? extends OpsManagerNflow> nflows) {
        saveList(nflows);
    }

    /**
     *
     * @param systemName
     * @param nflowId
     */
    private void ensureAndRemoveDuplicateNflowsWithTheSameName(String systemName, OpsManagerNflow.ID nflowId) {
        List<JpaOpsManagerNflow> nflows = repository.findNflowsByNameWithoutAcl(systemName);
        if (nflows != null) {
            nflows.stream().filter(nflow -> !nflow.getId().toString().equalsIgnoreCase(nflowId.toString())).forEach(nflow -> {
                log.warn(
                    "Attempting to create a new Nflow for {} with id {}, but found an existing Nflow in the nova.NFLOW table with id {} that has the same name {}.  Nova will remove the previous nflow with id: {} ",
                    systemName, nflowId, nflow.getId(), nflow.getName(), nflow.getId());
                delete(nflow.getId());
            });
        }
    }

    @Override
    public OpsManagerNflow save(OpsManagerNflow.ID nflowId, String systemName, boolean isStream, Long timeBetweenBatchJobs) {
        OpsManagerNflow nflow = repository.findByIdWithoutAcl(nflowId);
        if (nflow == null) {
            if (ensureUniqueNflowName) {
                ensureAndRemoveDuplicateNflowsWithTheSameName(systemName, nflowId);
            }
            nflow = new JpaOpsManagerNflow();
            ((JpaOpsManagerNflow) nflow).setName(systemName);
            ((JpaOpsManagerNflow) nflow).setId((OpsManagerNflowId) nflowId);
            ((JpaOpsManagerNflow) nflow).setStream(isStream);
            ((JpaOpsManagerNflow) nflow).setTimeBetweenBatchJobs(timeBetweenBatchJobs);
            NifiNflowStats stats = nflowStatisticsProvider.findLatestStatsForNflowWithoutAccessControl(systemName);
            if (stats == null) {
                JpaNifiNflowStats newStats = new JpaNifiNflowStats(systemName, new JpaNifiNflowStats.OpsManagerNflowId(nflowId.toString()));
                newStats.setRunningNflowFlows(0L);
                nflowStatisticsProvider.saveLatestNflowStats(Lists.newArrayList(newStats));
            }

        } else {
            ((JpaOpsManagerNflow) nflow).setStream(isStream);
            ((JpaOpsManagerNflow) nflow).setTimeBetweenBatchJobs(timeBetweenBatchJobs);
        }
        nflow = save(nflow);
        return nflow;
    }

    @Override
    public void delete(OpsManagerNflow.ID id) {
        OpsManagerNflow nflow = repository.findByIdWithoutAcl(id);
        if (nflow != null) {
            log.info("Deleting nflow {} ({})  and all job executions. ", nflow.getName(), nflow.getId());
            //first delete all jobs for this nflow
            deleteNflowJobs(NflowNameUtil.category(nflow.getName()), NflowNameUtil.nflow(nflow.getName()));
            //remove an slas on this nflow
            List<JpaServiceLevelAgreementDescription> slas = serviceLevelAgreementDescriptionRepository.findForNflow(id);
            if (slas != null && !slas.isEmpty()) {
                serviceLevelAgreementDescriptionRepository.delete(slas);
            }
            nflowStatisticsProvider.deleteNflowStats(nflow.getName());
            delete(nflow);

            log.info("Successfully deleted the nflow {} ({})  and all job executions. ", nflow.getName(), nflow.getId());
        }
    }

    public boolean isNflowRunning(OpsManagerNflow.ID id) {
        OpsManagerNflow nflow = opsManagerNflowCacheById.findByIdWithoutAcl(id);
        if (nflow == null) {
            nflow = repository.findByIdWithoutAcl(id);
        }
        if (nflow != null) {
            return batchJobExecutionProvider.isNflowRunning(nflow.getName());
        }
        return false;
    }

    @EntityAccessControlled
    public List<OpsManagerNflow> findAll(String filter) {
        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        return Lists.newArrayList(repository.findAll(GenericQueryDslFilter.buildFilter(nflow, filter)));
    }

    @EntityAccessControlled
    public List<String> getNflowNames() {
        return opsManagerNflowCacheByName.findAll().stream().map(f -> f.getName()).collect(Collectors.toList());
    }

    public List<OpsManagerNflow> findAll() {
        return opsManagerNflowCacheByName.findAll();
    }

    public List<OpsManagerNflow> findAllWithoutAcl() {
        return findAll();
    }

    @EntityAccessControlled
    public Map<String, List<OpsManagerNflow>> getNflowsGroupedByCategory() {
        return opsManagerNflowCacheByName.findAll().stream().collect(Collectors.groupingBy(f -> NflowNameUtil.category(f.getName())));
    }

    public List<? extends NflowHealth> getNflowHealth() {
        return nflowHealthRepository.findAll();
    }

    private List<? extends NflowHealth> findNflowHealth(String nflowName) {
        if (accessController.isEntityAccessControlled()) {
            return nflowHealthRepository.findByNflowNameWithAcl(nflowName);
        } else {
            return nflowHealthRepository.findByNflowNameWithoutAcl(nflowName);
        }
    }

    public NflowHealth getNflowHealth(String nflowName) {
        List<? extends NflowHealth> nflowHealthList = findNflowHealth(nflowName);
        if (nflowHealthList != null && !nflowHealthList.isEmpty()) {
            return nflowHealthList.get(0);
        } else {
            return null;
        }
    }

    public List<? extends LatestNflowJobExecution> findLatestCheckDataJobs() {
        return latestNflowJobExectionRepository.findCheckDataJobs();
    }

    public List<JobStatusCount> getJobStatusCountByDateFromNow(String nflowName, ReadablePeriod period) {

        QJpaBatchJobExecution jobExecution = QJpaBatchJobExecution.jpaBatchJobExecution;

        QJpaBatchJobInstance jobInstance = QJpaBatchJobInstance.jpaBatchJobInstance;

        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;

        JPAQuery
            query = factory.select(
            Projections.constructor(JpaBatchJobExecutionStatusCounts.class,
                                    JobStatusDslQueryExpressionBuilder.jobState().as("status"),
                                    Expressions.constant(nflowName),
                                    jobExecution.startYear,
                                    jobExecution.startMonth,
                                    jobExecution.startDay,
                                    jobExecution.count().as("count")))
            .from(jobExecution)
            .innerJoin(jobInstance).on(jobExecution.jobInstance.jobInstanceId.eq(jobInstance.jobInstanceId))
            .innerJoin(nflow).on(jobInstance.nflow.id.eq(nflow.id))
            .where(jobExecution.startTime.goe(DateTime.now().minus(period))
                       .and(nflow.name.eq(nflowName))
                       .and(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, accessController.isEntityAccessControlled())))
            .groupBy(jobExecution.status,
                     jobExecution.startYear,
                     jobExecution.startMonth,
                     jobExecution.startDay);

        return (List<JobStatusCount>) query.fetch();

    }

    /**
     * This will call the stored procedure delete_nflow_jobs and remove all data, jobs, steps for the nflow.
     */
    public void deleteNflowJobs(String category, String nflow) {
        repository.deleteNflowJobs(category, nflow);
        alertManager.updateLastUpdatedTime();
    }

    /**
     * This will call the stored procedure abandon_nflow_jobs
     */
    public void abandonNflowJobs(String nflow) {

        String exitMessage = String.format("Job manually abandoned @ %s", DateTimeUtil.getNowFormattedWithTimeZone());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        repository.abandonNflowJobs(nflow, exitMessage, username);
        //TODO Notify the JobExecution Cache of updates

        //all the alerts manager to handle all job failures
        AlertCriteria criteria = alertProvider.criteria().type(OperationalAlerts.JOB_FALURE_ALERT_TYPE).subtype(nflow);
        Iterator<? extends Alert> alerts = alertProvider.getAlerts(criteria);
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(alerts, Spliterator.ORDERED), false)
            .forEach(alert -> alertProvider.respondTo(alert.getId(), (alert1, response) -> response.handle(exitMessage)));
        alertManager.updateLastUpdatedTime();
    }

    /**
     * Sets the stream flag for the list of nflows
     *
     * @param nflowNames the nflow names to update
     * @param isStream  true if stream/ false if not
     */
    public void updateStreamingFlag(Set<String> nflowNames, boolean isStream) {
        List<JpaOpsManagerNflow> nflows = (List<JpaOpsManagerNflow>) findByNflowNames(nflowNames, false);
        if (nflows != null) {
            for (JpaOpsManagerNflow nflow : nflows) {
                //if we move from a stream to a batch we need to stop/complete the running stream job
                if (nflow.isStream() && !isStream) {
                    BatchJobExecution jobExecution = batchJobExecutionProvider.findLatestJobForNflow(nflow.getName());
                    if (jobExecution != null && !jobExecution.isFinished()) {
                        jobExecution.setStatus(BatchJobExecution.JobStatus.STOPPED);
                        jobExecution.setExitCode(ExecutionConstants.ExitCode.COMPLETED);
                        jobExecution.setEndTime(DateTime.now());
                        jobExecution = batchJobExecutionProvider.save(jobExecution);
                        batchJobExecutionProvider.notifyStopped(jobExecution, nflow, null);
                        //notify stream to batch for nflow
                        batchJobExecutionProvider.notifyStreamToBatch(jobExecution, nflow);

                    }
                } else if (!nflow.isStream() && isStream) {
                    //if we move from a batch to a stream we need to complete any jobs that are running.
                    batchJobExecutionProvider.findRunningJobsForNflow(nflow.getName()).stream().forEach(jobExecution -> {
                        jobExecution.setExitCode(ExecutionConstants.ExitCode.STOPPED);
                        jobExecution.setEndTime(DateTime.now());
                        jobExecution.setExitMessage("Stopping and Abandoning the Job.  The job was running while the nflow/template changed from a batch to a stream");
                        jobExecution.setStatus(BatchJobExecution.JobStatus.ABANDONED);
                        batchJobExecutionProvider.save(jobExecution);
                        log.info("Stopping and Abandoning the Job {} for nflow {}.  The job was running while the nflow/template changed from a batch to a stream", jobExecution.getJobExecutionId(),
                                 nflow.getName());
                        batchJobExecutionProvider.notifyFailure(jobExecution, nflow, false, null);
                        //notify batch to stream for nflow
                        batchJobExecutionProvider.notifyBatchToStream(jobExecution, nflow);
                    });
                }
                nflow.setStream(isStream);
            }
            save(nflows);
        }
    }


    public List<OpsManagerNflow> findNflowsWithFilter(String filter) {
        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        BooleanBuilder where = GenericQueryDslFilter.buildFilter(nflow, filter);

        JPAQuery
            query = factory.select(nflow)
            .from(nflow)
            .where(where);
        return query.fetch();
    }

    /**
     * @param nflowNames            a set of category.nflow names
     * @param timeBetweenBatchJobs a time in millis to suppress new job creation
     */
    @Override
    public void updateTimeBetweenBatchJobs(Set<String> nflowNames, Long timeBetweenBatchJobs) {
        List<JpaOpsManagerNflow> nflows = (List<JpaOpsManagerNflow>) findByNflowNames(nflowNames, false);
        if (nflows != null) {
            for (JpaOpsManagerNflow nflow : nflows) {
                nflow.setTimeBetweenBatchJobs(timeBetweenBatchJobs);
            }
            save(nflows);
        }

    }


    public List<? extends OpsManagerNflow> findNflowsWithSameName() {
        List<JpaNflowNameCount> nflowNameCounts = repository.findNflowsWithSameName();
        if (nflowNameCounts != null && !nflowNameCounts.isEmpty()) {
            List<String> nflowNames = nflowNameCounts.stream().map(c -> c.getNflowName()).collect(Collectors.toList());
            if(nflowNames != null && !nflowNames.isEmpty()) {
                return repository.findNflowsByNameWithoutAcl(nflowNames);
            }
        }
        return Collections.emptyList();
    }

    public List<? extends NflowSummary> findNflowSummary() {
        return nflowSummaryRepository.findAllWithoutAcl();
    }

    @Override
    public DateTime getLastActiveTimeStamp(String nflowName) {
        DateTime lastNflowTime = null;
        OpsManagerNflow nflow = this.findByName(nflowName);
        if (nflow.isStream()) {
            NifiNflowStats nflowStats = metadataAccess.read(() -> nifiNflowStatisticsProvider.findLatestStatsForNflow(nflowName));
            if (nflowStats != null) {
                lastNflowTime = new DateTime(nflowStats.getLastActivityTimestamp());
            }
        } else {
            BatchJobExecution jobExecution = metadataAccess.read(() -> batchJobExecutionProvider.findLatestCompletedJobForNflow(nflowName));
            if (jobExecution != null) {
                lastNflowTime = jobExecution.getEndTime();
            }
        }
        return lastNflowTime;
    }


    @Override
    protected Collection<OpsManagerNflow> populateCache() {
        return metadataAccess.read(() -> super.populateCache(), MetadataAccess.SERVICE);
    }
}
