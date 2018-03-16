package com.onescorpin.metadata.jpa.jobrepo.job;

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

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.onescorpin.DateTimeUtil;
import com.onescorpin.alerts.api.Alert;
import com.onescorpin.alerts.api.AlertProvider;
import com.onescorpin.alerts.spi.AlertManager;
import com.onescorpin.alerts.spi.DefaultAlertChangeEventContent;
import com.onescorpin.cluster.ClusterMessage;
import com.onescorpin.cluster.ClusterService;
import com.onescorpin.cluster.ClusterServiceMessageReceiver;
import com.onescorpin.jobrepo.common.constants.CheckDataStepConstants;
import com.onescorpin.jobrepo.common.constants.NflowConstants;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.SearchCriteria;
import com.onescorpin.metadata.api.alerts.OperationalAlerts;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowOperationBatchStatusChange;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.ExecutionConstants;
import com.onescorpin.metadata.api.jobrepo.job.BatchAndStreamingJobStatusCount;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobInstance;
import com.onescorpin.metadata.api.jobrepo.job.BatchRelatedFlowFile;
import com.onescorpin.metadata.api.jobrepo.job.JobStatusCount;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStats;
import com.onescorpin.metadata.api.jobrepo.step.BatchStepExecutionProvider;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.nflow.NflowAclIndexQueryAugmentor;
import com.onescorpin.metadata.jpa.nflow.JpaOpsManagerNflow;
import com.onescorpin.metadata.jpa.nflow.OpsManagerNflowRepository;
import com.onescorpin.metadata.jpa.nflow.QJpaOpsManagerNflow;
import com.onescorpin.metadata.jpa.nflow.QOpsManagerNflowId;
import com.onescorpin.metadata.jpa.jobrepo.nifi.JpaNifiEventJobExecution;
import com.onescorpin.metadata.jpa.jobrepo.nifi.NifiRelatedRootFlowFilesRepository;
import com.onescorpin.metadata.jpa.jobrepo.nifi.QJpaNifiNflowStats;
import com.onescorpin.metadata.jpa.support.CommonFilterTranslations;
import com.onescorpin.metadata.jpa.support.GenericQueryDslFilter;
import com.onescorpin.metadata.jpa.support.JobStatusDslQueryExpressionBuilder;
import com.onescorpin.metadata.jpa.support.QueryDslFetchJoin;
import com.onescorpin.metadata.jpa.support.QueryDslPagingSupport;
import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTO;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.support.NflowNameUtil;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.OptimisticLockException;


/**
 * Provider for the {@link JpaBatchJobExecution}
 */
@Service
public class JpaBatchJobExecutionProvider extends QueryDslPagingSupport<JpaBatchJobExecution> implements BatchJobExecutionProvider {

    private static final Logger log = LoggerFactory.getLogger(JpaBatchJobExecutionProvider.class);

    private static String PARAM_TB_JOB_TYPE = "tb.jobType";

    @Autowired
    private JPAQueryFactory factory;

    private BatchJobExecutionRepository jobExecutionRepository;

    private BatchJobInstanceRepository jobInstanceRepository;

    private BatchJobParametersRepository jobParametersRepository;

    private OpsManagerNflowRepository opsManagerNflowRepository;

    private NifiRelatedRootFlowFilesRepository relatedRootFlowFilesRepository;

    @Inject
    private BatchStepExecutionProvider batchStepExecutionProvider;

    private BatchRelatedFlowFileRepository batchRelatedFlowFileRepository;

    @Inject
    private AccessController controller;

    @Inject
    private MetadataEventService eventService;

    @Inject
    private NifiNflowStatisticsProvider nflowStatisticsProvider;


    @Inject
    @Named("novaAlertManager")
    protected AlertManager alertManager;

    @Inject
    private AlertProvider provider;

    @Inject
    private JobExecutionChangedNotifier jobExecutionChangedNotifier;


    @Inject
    private MetadataAccess metadataAccess;

    private Map<String, BatchJobExecution> latestStreamingJobByNflowName = new ConcurrentHashMap<>();

    @Inject
    private ClusterService clusterService;

    private BatchStatusChangeReceiver batchStatusChangeReceiver = new BatchStatusChangeReceiver();


    /**
     * Latest start time for nflow.
     * This is used to speed up the findLatestJobForNflow query limiting the result set by the last known start time
     */
    private Map<String, Long> latestStartTimeByNflowName = new ConcurrentHashMap<>();


    @Autowired
    public JpaBatchJobExecutionProvider(BatchJobExecutionRepository jobExecutionRepository, BatchJobInstanceRepository jobInstanceRepository,
                                        NifiRelatedRootFlowFilesRepository relatedRootFlowFilesRepository,
                                        BatchJobParametersRepository jobParametersRepository,
                                        OpsManagerNflowRepository opsManagerNflowRepository,
                                        BatchRelatedFlowFileRepository batchRelatedFlowFileRepository
    ) {
        super(JpaBatchJobExecution.class);
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobInstanceRepository = jobInstanceRepository;
        this.relatedRootFlowFilesRepository = relatedRootFlowFilesRepository;
        this.jobParametersRepository = jobParametersRepository;
        this.opsManagerNflowRepository = opsManagerNflowRepository;
        this.batchRelatedFlowFileRepository = batchRelatedFlowFileRepository;

    }

    @PostConstruct
    private void init() {
        clusterService.subscribe(batchStatusChangeReceiver, NflowOperationBatchStatusChange.CLUSTER_MESSAGE_TYPE);
    }

    @Override
    public BatchJobInstance createJobInstance(ProvenanceEventRecordDTO event) {

        JpaBatchJobInstance jobInstance = new JpaBatchJobInstance();
        jobInstance.setJobKey(jobKeyGenerator(event));
        jobInstance.setJobName(event.getNflowName());
        //wire this instance to the Nflow
        OpsManagerNflow nflow = opsManagerNflowRepository.findByName(event.getNflowName());
        jobInstance.setNflow(nflow);
        BatchJobInstance batchJobInstance = this.jobInstanceRepository.save(jobInstance);
        return batchJobInstance;
    }


    /**
     * Generate a Unique key for the Job Instance table This code is similar to what was used by Spring Batch
     */
    private String jobKeyGenerator(ProvenanceEventRecordDTO event) {

        StringBuffer stringBuffer = new StringBuffer(event.getEventTime() + "").append(event.getFlowFileUuid());
        MessageDigest digest1;
        try {
            digest1 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException var10) {
            throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
        }

        try {
            byte[] e1 = digest1.digest(stringBuffer.toString().getBytes("UTF-8"));
            return String.format("%032x", new Object[]{new BigInteger(1, e1)});
        } catch (UnsupportedEncodingException var9) {
            throw new IllegalStateException("UTF-8 encoding not available.  Fatal (should be in the JDK).");
        }

    }

    /**
     * Crate a new job exection from a provenance event
     *
     * @param event the provenance event indicating it is the start of a job
     * @return the job execution
     */
    private JpaBatchJobExecution createNewJobExecution(ProvenanceEventRecordDTO event, OpsManagerNflow nflow) {
        BatchJobInstance jobInstance = createJobInstance(event);
        JpaBatchJobExecution jobExecution = createJobExecution(jobInstance, event, nflow);

        return jobExecution;
    }


    /**
     * Create a new Job Execution record from a given Provenance Event
     *
     * @param jobInstance the job instance to relate this job execution to
     * @param event       the event that started this job execution
     * @return the job execution
     */
    private JpaBatchJobExecution createJobExecution(BatchJobInstance jobInstance, ProvenanceEventRecordDTO event, OpsManagerNflow nflow) {

        JpaBatchJobExecution jobExecution = new JpaBatchJobExecution();
        jobExecution.setJobInstance(jobInstance);
        //add in the parameters from the attributes
        jobExecution.setCreateTime(DateTimeUtil.getNowUTCTime());
        jobExecution.setStartTime(DateTimeUtil.convertToUTC(event.getEventTime()));
        jobExecution.setStatus(BatchJobExecution.JobStatus.STARTED);
        jobExecution.setExitCode(ExecutionConstants.ExitCode.EXECUTING);
        jobExecution.setLastUpdated(DateTimeUtil.getNowUTCTime());

        //create the job params
        Map<String, Object> jobParameters = new HashMap<>();
        if (event.isStartOfJob() && event.getAttributeMap() != null) {
            jobParameters = new HashMap<>(event.getAttributeMap());
        } else {
            jobParameters = new HashMap<>();
        }

        //save the params
        JpaNifiEventJobExecution eventJobExecution = new JpaNifiEventJobExecution(jobExecution, event.getEventId(), event.getJobFlowFileId());
        jobExecution.setNifiEventJobExecution(eventJobExecution);
        jobExecution = (JpaBatchJobExecution) save(jobExecution);

        jobExecutionChangedNotifier.notifyStarted(jobExecution, nflow, null);
        //bootstrap the nflow parameters
        jobParameters.put(NflowConstants.PARAM__NFLOW_NAME, event.getNflowName());
        jobParameters.put(NflowConstants.PARAM__JOB_TYPE, NflowConstants.PARAM_VALUE__JOB_TYPE_NFLOW);
        Set<JpaBatchJobExecutionParameter> jpaJobParameters = addJobParameters(jobExecution, jobParameters);
        this.jobParametersRepository.save(jpaJobParameters);
        return jobExecution;
    }

    /**
     * add parameters to a job execution
     *
     * @param jobExecution  the job execution
     * @param jobParameters the parameters to add to the {@code jobExecution}
     * @return the newly created job parameters
     */
    private Set<JpaBatchJobExecutionParameter> addJobParameters(JpaBatchJobExecution jobExecution, Map<String, Object> jobParameters) {
        Set<JpaBatchJobExecutionParameter> jobExecutionParametersList = new HashSet<>();
        for (Map.Entry<String, Object> entry : jobParameters.entrySet()) {
            JpaBatchJobExecutionParameter jobExecutionParameters = jobExecution.addParameter(entry.getKey(), entry.getValue());
            jobExecutionParametersList.add(jobExecutionParameters);
        }
        return jobExecutionParametersList;
    }


    /**
     * Check to see if the NifiEvent has the attributes indicating it is a Check Data Job
     */
    private boolean isCheckDataJob(ProvenanceEventRecordDTO event) {
        if (event.getAttributeMap() != null) {
            String jobType = event.getAttributeMap().get(NIFI_JOB_TYPE_PROPERTY);
            if (StringUtils.isBlank(jobType)) {
                jobType = event.getAttributeMap().get(NIFI_NOVA_JOB_TYPE_PROPERTY);
            }
            return StringUtils.isNotBlank(jobType) && NflowConstants.PARAM_VALUE__JOB_TYPE_CHECK.equalsIgnoreCase(jobType);
        }
        return false;
    }

    /**
     * Sets the Job Execution params to either Check Data or Nflow Jobs
     */
    private boolean updateJobType(BatchJobExecution jobExecution, ProvenanceEventRecordDTO event) {

        if (event.getUpdatedAttributes() != null && (event.getUpdatedAttributes().containsKey(NIFI_JOB_TYPE_PROPERTY) || event.getUpdatedAttributes().containsKey(NIFI_NOVA_JOB_TYPE_PROPERTY))) {
            String jobType = event.getUpdatedAttributes().get(NIFI_JOB_TYPE_PROPERTY);
            if (StringUtils.isBlank(jobType)) {
                jobType = event.getUpdatedAttributes().get(NIFI_NOVA_JOB_TYPE_PROPERTY);
            }
            String nifiCategory = event.getAttributeMap().get(NIFI_CATEGORY_PROPERTY);
            String nifiNflowName = event.getAttributeMap().get(NIFI_NFLOW_PROPERTY);
            String nflowName = NflowNameUtil.fullName(nifiCategory, nifiNflowName);
            if (NflowConstants.PARAM_VALUE__JOB_TYPE_CHECK.equalsIgnoreCase(jobType)) {
                Set<JpaBatchJobExecutionParameter> updatedParams = ((JpaBatchJobExecution) jobExecution).setAsCheckDataJob(nflowName);
                jobParametersRepository.save(updatedParams);

                //update nflow type
                JpaOpsManagerNflow checkDataNflow = (JpaOpsManagerNflow) opsManagerNflowRepository.findByName(event.getNflowName());
                checkDataNflow.setNflowType(OpsManagerNflow.NflowType.CHECK);
                //relate to this nflow
                JpaOpsManagerNflow nflowToCheck = (JpaOpsManagerNflow) opsManagerNflowRepository.findByName(nflowName);
                nflowToCheck.getCheckDataNflows().add(checkDataNflow);

                return true;
            }
        }
        return false;
    }

    /**
     * When the job is complete determine its status, write out execution context, and determine if all related jobs are complete
     */
    private void finishJob(ProvenanceEventRecordDTO event, JpaBatchJobExecution jobExecution) {

        if (jobExecution.getJobExecutionId() == null) {
            log.error("Warning execution id is null for ending event {} ", event);
        }
        if (event.isStream()) {
            jobExecution.finishStreamingJob();
        } else {
            if (event.isFailure()) {  //event.hasFailureEvents
                jobExecution.failJob();
            } else {
                jobExecution.completeOrFailJob();
            }
        }

        //ensure check data jobs are property failed if they dont pass
        if (isCheckDataJob(event)) {
            String valid = event.getAttributeMap().get(CheckDataStepConstants.VALIDATION_KEY);
            String msg = event.getAttributeMap().get(CheckDataStepConstants.VALIDATION_MESSAGE_KEY);
            if (StringUtils.isNotBlank(valid)) {
                if (!BooleanUtils.toBoolean(valid)) {
                    jobExecution.failJob();
                }
            }
            if (StringUtils.isNotBlank(msg)) {
                jobExecution.setExitMessage(msg);
            }
        }

        jobExecution.setEndTime(DateTimeUtil.convertToUTC(event.getEventTime()));
        log.info("Finishing Job: {} with a status of: {} for event: {} ", jobExecution.getJobExecutionId(), jobExecution.getStatus(), event.getEventId());
        //add in execution contexts
        Map<String, String> allAttrs = event.getAttributeMap();
        if (allAttrs != null && !allAttrs.isEmpty()) {
            for (Map.Entry<String, String> entry : allAttrs.entrySet()) {
                JpaBatchJobExecutionContextValue executionContext = new JpaBatchJobExecutionContextValue(jobExecution, entry.getKey());
                executionContext.setStringVal(entry.getValue());
                jobExecution.addJobExecutionContext(executionContext);

                if (entry.getKey().equals(BatchJobExecutionProvider.NIFI_JOB_EXIT_DESCRIPTION_PROPERTY)) {
                    String msg = jobExecution.getExitMessage() != null ? jobExecution.getExitMessage() + "\n" : "";
                    msg += entry.getValue();
                    jobExecution.setExitMessage(msg);
                }
            }
        }

    }

    public JpaBatchJobExecution findJobExecution(ProvenanceEventRecordDTO event) {
        return jobExecutionRepository.findByFlowFile(event.getJobFlowFileId());
    }


    /**
     * Get or Create the JobExecution for a given ProvenanceEvent
     */
    @Override
    public synchronized JpaBatchJobExecution getOrCreateJobExecution(ProvenanceEventRecordDTO event, OpsManagerNflow nflow) {
        JpaBatchJobExecution jobExecution = null;
        if (event.isStream()) {
            //Streams only care about start/stop events to track.. otherwise we can disregard the events)
            if (event.isStartOfJob() || event.isFinalJobEvent()) {
                jobExecution = getOrCreateStreamJobExecution(event, nflow);
            }
        } else {
            if (nflow == null) {
                nflow = opsManagerNflowRepository.findByName(event.getNflowName());
            }
            if (isProcessBatchEvent(event, nflow)) {
                jobExecution = getOrCreateBatchJobExecution(event, nflow);
            }
        }

        return jobExecution;

    }

    @Override
    public void updateNflowJobStartTime(BatchJobExecution jobExecution,OpsManagerNflow nflow){
        if(jobExecution != null){
            //add the starttime to the map
            Long startTime = jobExecution.getStartTime().getMillis();
            if(startTime != null) {
                if (!latestStartTimeByNflowName.containsKey(startTime)) {
                    latestStartTimeByNflowName.put(nflow.getName(), startTime);
                } else {
                    Long previousStartTime = latestStartTimeByNflowName.get(startTime);
                    if (startTime > previousStartTime) {
                        latestStartTimeByNflowName.put(nflow.getName(), startTime);
                    }
                }
            }
        }
    }

    private BatchRelatedFlowFile getOtherBatchJobFlowFile(ProvenanceEventRecordDTO event) {

        BatchRelatedFlowFile relatedFlowFile = batchRelatedFlowFileRepository.findOne(event.getJobFlowFileId());
        return relatedFlowFile;
    }

    private Long timeBetweenStartingJobs(OpsManagerNflow nflow) {
        Long time = null;
        if (nflow != null) {
            time = nflow.getTimeBetweenBatchJobs();
        }
        if (time == null) {
            time = 1000L;
        }
        return time;
    }


    private BatchRelatedFlowFile relateFlowFiles(String eventFlowFileId, String batchJobExecutionFlowFile, Long batchJobExecutionId) {
        JpaBatchRelatedFlowFile relatedFlowFile = new JpaBatchRelatedFlowFile(eventFlowFileId, batchJobExecutionFlowFile, batchJobExecutionId);
        return batchRelatedFlowFileRepository.save(relatedFlowFile);
    }


    private boolean isProcessBatchEvent(ProvenanceEventRecordDTO event, OpsManagerNflow nflow) {

        //if we have a job already for this event then let it pass
        JpaBatchJobExecution jobExecution = jobExecutionRepository.findByFlowFile(event.getJobFlowFileId());
        if (jobExecution != null) {
            return true;
        } else {
            jobExecution = (JpaBatchJobExecution) findLatestJobForNflow(event.getNflowName());

            if (jobExecution != null) {
                String jobFlowFile = jobExecution.getNifiEventJobExecution().getFlowFileId();
                if (jobFlowFile.equals(event.getJobFlowFileId())) {
                    return true;
                } else {
                    boolean isSkipped = getOtherBatchJobFlowFile(event) != null;
                    Long diff = event.getEventTime() - jobExecution.getStartTime().getMillis();
                    Long threshold = timeBetweenStartingJobs(nflow);
                    if (!isSkipped && threshold != -1 && jobExecution != null && diff >= 0 && diff < threshold) {

                        //relate this to that and return
                        BatchRelatedFlowFile related = getOtherBatchJobFlowFile(event);
                        if (related == null) {
                            relateFlowFiles(event.getJobFlowFileId(), jobFlowFile, jobExecution.getJobExecutionId());
                            event.setJobFlowFileId(jobFlowFile);
                            log.debug("Relating {} to {}, {} ", event.getJobFlowFileId(), jobFlowFile, jobExecution.getJobExecutionId());
                        }
                        return false;
                    } else {
                        return !isSkipped;
                    }
                }


            }
        }
        return true;

    }


    private JpaBatchJobExecution getOrCreateBatchJobExecution(ProvenanceEventRecordDTO event, OpsManagerNflow nflow) {
        JpaBatchJobExecution jobExecution = null;
        boolean isNew = false;
        try {
            jobExecution = jobExecutionRepository.findByFlowFile(event.getJobFlowFileId());
            if (jobExecution == null) {
                jobExecution = createNewJobExecution(event, nflow);
                isNew = true;
            }
        } catch (OptimisticLockException e) {
            //read
            jobExecution = jobExecutionRepository.findByFlowFile(event.getJobFlowFileId());
        }

        //if the attrs coming in change the type to a CHECK job then update the entity
        boolean updatedJobType = updateJobType(jobExecution, event);

        boolean save = isNew || updatedJobType;
        if (event.isFinalJobEvent()) {
            finishJob(event, jobExecution);
            save = true;
        }

        //if the event is the start of the Job, but the job execution was created from another downstream event, ensure the start time and event are related correctly
        if (event.isStartOfJob() && !isNew) {
            jobExecution.getNifiEventJobExecution().setEventId(event.getEventId());
            jobExecution.setStartTime(DateTimeUtil.convertToUTC(event.getEventTime()));
            //create the job params
            Map<String, Object> jobParameters = new HashMap<>();
            if (event.isStartOfJob() && event.getAttributeMap() != null) {
                jobParameters = new HashMap<>(event.getAttributeMap());
            } else {
                jobParameters = new HashMap<>();
            }

            this.jobParametersRepository.save(addJobParameters(jobExecution, jobParameters));
            save = true;
        }
        if (save) {

            jobExecution = (JpaBatchJobExecution) save(jobExecution);
            if (isNew) {
                log.info("Created new Job Execution with id of {} and starting event {} ", jobExecution.getJobExecutionId(), event);
            }
            if (updatedJobType) {
                //notify operations status
                jobExecutionChangedNotifier.notifyDataConfidenceJob(jobExecution, nflow, "Data Confidence Job detected ");
            }

        }
        return jobExecution;
    }

    public void markStreamingNflowAsStopped(String nflow) {
        BatchJobExecution jobExecution = findLatestJobForNflow(nflow);
        if (jobExecution != null && !jobExecution.getStatus().equals(BatchJobExecution.JobStatus.STOPPED)) {
            log.info("Stopping Streaming nflow job {} for Nflow {} ", jobExecution.getJobExecutionId(), nflow);
            jobExecution.setStatus(BatchJobExecution.JobStatus.STOPPED);
            jobExecution.setExitCode(ExecutionConstants.ExitCode.COMPLETED);
            ((JpaBatchJobExecution)jobExecution).setLastUpdated(DateTimeUtil.getNowUTCTime());
            jobExecution.setEndTime(DateTimeUtil.getNowUTCTime());
            save(jobExecution);
            //update the cache
            latestStreamingJobByNflowName.put(nflow, jobExecution);
        }
    }

    public void markStreamingNflowAsStarted(String nflow) {
        BatchJobExecution jobExecution = findLatestJobForNflow(nflow);
        //ensure its Running
        if (!jobExecution.getStatus().equals(BatchJobExecution.JobStatus.STARTED)) {
            log.info("Starting Streaming nflow job {} for Nflow {} ", jobExecution.getJobExecutionId(), nflow);
            jobExecution.setStatus(BatchJobExecution.JobStatus.STARTED);
            jobExecution.setExitCode(ExecutionConstants.ExitCode.EXECUTING);
            ((JpaBatchJobExecution)jobExecution).setLastUpdated(DateTimeUtil.getNowUTCTime());
            jobExecution.setStartTime(DateTimeUtil.getNowUTCTime());
            save(jobExecution);
            latestStreamingJobByNflowName.put(nflow, jobExecution);
        }
    }

    private JpaBatchJobExecution getOrCreateStreamJobExecution(ProvenanceEventRecordDTO event, OpsManagerNflow nflow) {
        JpaBatchJobExecution jobExecution = null;
        boolean isNew = false;
        try {
            BatchJobExecution latestJobExecution = latestStreamingJobByNflowName.get(event.getNflowName());
            if (latestJobExecution == null) {
                latestJobExecution = findLatestJobForNflow(event.getNflowName());
            } else {
                if (clusterService.isClustered()) {
                    latestJobExecution = jobExecutionRepository.findOne(latestJobExecution.getJobExecutionId());
                }
            }
            if (latestJobExecution == null || (latestJobExecution != null && !latestJobExecution.isStream())) {
                //If the latest Job is not set to be a Stream and its still running we need to fail it and create the new streaming job.
                if (latestJobExecution != null && !latestJobExecution.isFinished()) {
                    ProvenanceEventRecordDTO tempFailedEvent = new ProvenanceEventRecordDTO();
                    tempFailedEvent.setNflowName(event.getNflowName());
                    tempFailedEvent.setAttributeMap(new HashMap<>());
                    tempFailedEvent.setIsFailure(true);
                    tempFailedEvent.setDetails("Failed Running Batch event as this Nflow has now become a Stream");
                    finishJob(tempFailedEvent, (JpaBatchJobExecution) latestJobExecution);
                    latestJobExecution.setExitMessage("Failed Running Batch event as this Nflow has now become a Stream");
                    save(latestJobExecution);

                }
                jobExecution = createNewJobExecution(event, nflow);
                jobExecution.setStream(true);
                latestStreamingJobByNflowName.put(event.getNflowName(), jobExecution);
                log.info("Created new Streaming Job Execution with id of {} and starting event {} ", jobExecution.getJobExecutionId(), event);
            } else {
                jobExecution = (JpaBatchJobExecution) latestJobExecution;
            }
            if (jobExecution != null) {
                latestStreamingJobByNflowName.put(event.getNflowName(), jobExecution);
            }
        } catch (OptimisticLockException e) {
            //read
            jobExecution = (JpaBatchJobExecution) findLatestJobForNflow(event.getNflowName());
        }

        boolean save = isNew;

        if (!jobExecution.isStream()) {
            jobExecution.setStream(true);
            save = true;
        }

        if (save) {
            save(jobExecution);
        }
        return jobExecution;
    }


    @Override
    public BatchJobExecution save(BatchJobExecution jobExecution, ProvenanceEventRecordDTO event) {
        if (jobExecution == null) {
            return null;
        }
        batchStepExecutionProvider.createStepExecution(jobExecution, event);
        return jobExecution;
    }

    /**
     * Save the job execution in the database
     *
     * @return the saved job execution
     */
    @Override
    public BatchJobExecution save(ProvenanceEventRecordDTO event, OpsManagerNflow nflow) {
        JpaBatchJobExecution jobExecution = getOrCreateJobExecution(event, nflow);
        if (jobExecution != null) {
            return save(jobExecution, event);
        }
        return null;
    }


    public BatchJobExecution save(BatchJobExecution jobExecution) {
        return jobExecutionRepository.save((JpaBatchJobExecution) jobExecution);
    }


    @Override
    public BatchJobExecution findByJobExecutionId(Long jobExecutionId) {
        return jobExecutionRepository.findOne(jobExecutionId);
    }


    @Override
    public List<? extends BatchJobExecution> findRunningJobsForNflow(String nflowName) {
        List<? extends BatchJobExecution> jobs = jobExecutionRepository.findJobsForNflowMatchingStatus(nflowName, BatchJobExecution.JobStatus.STARTED, BatchJobExecution.JobStatus.STARTING);
        return jobs != null ? jobs : Collections.emptyList();
    }

    /**
     * Find all the job executions for a nflow that have been completed since a given date
     *
     * @param nflowName  the nflow to check
     * @param sinceDate the min end date for the jobs on the {@code nflowName}
     * @return the job executions for a nflow that have been completed since a given date
     */
    @Override
    public Set<? extends BatchJobExecution> findJobsForNflowCompletedSince(String nflowName, DateTime sinceDate) {
        return jobExecutionRepository.findJobsForNflowCompletedSince(nflowName, sinceDate);
    }

    @Override
    public BatchJobExecution findLatestCompletedJobForNflow(String nflowName) {
        List<JpaBatchJobExecution> jobExecutions = jobExecutionRepository.findLatestCompletedJobForNflow(nflowName);
        if (jobExecutions != null && !jobExecutions.isEmpty()) {
            return jobExecutions.get(0);
        } else {
            return null;
        }
    }

    public BatchJobExecution findLatestFinishedJobForNflow(String nflowName) {
        List<JpaBatchJobExecution> jobExecutions = jobExecutionRepository.findLatestFinishedJobForNflow(nflowName);
        if (jobExecutions != null && !jobExecutions.isEmpty()) {
            return jobExecutions.get(0);
        } else {
            return null;
        }
    }

    @Override
    public BatchJobExecution findLatestJobForNflow(String nflowName) {
        List<JpaBatchJobExecution> jobExecutions = null;
        Long latestStartTime = latestStartTimeByNflowName.get(nflowName);
        if(latestStartTime != null) {
            jobExecutions = jobExecutionRepository.findLatestJobForNflowWithStartTimeLimit(nflowName,latestStartTime);
        }
        if(jobExecutions == null) {
            jobExecutions = jobExecutionRepository.findLatestJobForNflow(nflowName);
        }
        if (jobExecutions != null && !jobExecutions.isEmpty()) {
            return jobExecutions.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Boolean isNflowRunning(String nflowName) {
        return jobExecutionRepository.isNflowRunning(nflowName);
    }


    /**
     * Find all BatchJobExecution objects with the provided filter. the filter needs to match
     *
     * @return a paged result set of all the job executions matching the incoming filter
     */
    @Override
    public Page<? extends BatchJobExecution> findAll(String filter, Pageable pageable) {
        QJpaBatchJobExecution jobExecution = QJpaBatchJobExecution.jpaBatchJobExecution;
        //if the filter contains a filter on the nflow then delegate to the findAllForNflow method to include any check data jobs
        List<SearchCriteria> searchCriterias = GenericQueryDslFilter.parseFilterString(filter);
        SearchCriteria nflowFilter = searchCriterias.stream().map(searchCriteria -> searchCriteria.withKey(CommonFilterTranslations.resolvedFilter(jobExecution, searchCriteria.getKey()))).filter(
            sc -> sc.getKey().equalsIgnoreCase(CommonFilterTranslations.jobExecutionNflowNameFilterKey)).findFirst().orElse(null);
        if (nflowFilter != null && nflowFilter.getPreviousSearchCriteria() != null && !nflowFilter.isValueCollection()) {
            //remove the nflow filter from the list and filter by this nflow
            searchCriterias.remove(nflowFilter.getPreviousSearchCriteria());
            String nflowValue = nflowFilter.getValue().toString();
            //remove any quotes around the nflowValue
            nflowValue = nflowValue.replaceAll("^\"|\"$", "");
            return findAllForNflow(nflowValue, searchCriterias, pageable);
        } else {
            pageable = CommonFilterTranslations.resolveSortFilters(jobExecution, pageable);
            QJpaBatchJobInstance jobInstancePath = new QJpaBatchJobInstance("jobInstance");
            QJpaOpsManagerNflow nflowPath = new QJpaOpsManagerNflow("nflow");

            return findAllWithFetch(jobExecution,
                                    GenericQueryDslFilter.buildFilter(jobExecution, filter).and(augment(nflowPath.id)),
                                    pageable,
                                    QueryDslFetchJoin.innerJoin(jobExecution.nifiEventJobExecution),
                                    QueryDslFetchJoin.innerJoin(jobExecution.jobInstance, jobInstancePath),
                                    QueryDslFetchJoin.innerJoin(jobInstancePath.nflow, nflowPath)
            );
        }

    }

    private Predicate augment(QOpsManagerNflowId id) {
        return NflowAclIndexQueryAugmentor.generateExistsExpression(id, controller.isEntityAccessControlled());
    }


    private RoleSetExposingSecurityExpressionRoot getUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new RoleSetExposingSecurityExpressionRoot(authentication);
    }

    private Page<? extends BatchJobExecution> findAllForNflow(String nflowName, List<SearchCriteria> filters, Pageable pageable) {
        QJpaBatchJobExecution jobExecution = QJpaBatchJobExecution.jpaBatchJobExecution;
        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        QJpaOpsManagerNflow checkDataNflow = new QJpaOpsManagerNflow("checkDataNflow");
        QJpaBatchJobInstance jobInstance = QJpaBatchJobInstance.jpaBatchJobInstance;
        JPQLQuery checkNflowQuery = JPAExpressions.select(checkDataNflow.id).from(nflow).join(nflow.checkDataNflows, checkDataNflow).where(nflow.name.eq(nflowName));

        JPAQuery
            query = factory.select(jobExecution)
            .from(jobExecution)
            .join(jobExecution.jobInstance, jobInstance)
            .join(jobInstance.nflow, nflow)
            .where((nflow.name.eq(nflowName).or(nflow.id.in(checkNflowQuery)))
                       .and(GenericQueryDslFilter.buildFilter(jobExecution, filters)
                                .and(augment(nflow.id))))
            .fetchAll();

        pageable = CommonFilterTranslations.resolveSortFilters(jobExecution, pageable);
        return findAll(query, pageable);
    }


    /**
     * Get count of Jobs grouped by Status
     * Streaming Nflows are given a count of 1 if they are running, regardless of the number of active running flows
     */
    public List<BatchAndStreamingJobStatusCount> getBatchAndStreamingJobCounts(String filter) {

        QJpaBatchJobExecution jobExecution = QJpaBatchJobExecution.jpaBatchJobExecution;

        QJpaBatchJobInstance jobInstance = QJpaBatchJobInstance.jpaBatchJobInstance;

        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;

        QJpaNifiNflowStats nflowStats = QJpaNifiNflowStats.jpaNifiNflowStats;

        BooleanBuilder whereBuilder = new BooleanBuilder();
        if (StringUtils.isNotBlank(filter)) {
            whereBuilder.and(GenericQueryDslFilter.buildFilter(jobExecution, filter));
        }

        Expression<JpaBatchAndStreamingJobStatusCounts> expr =
            Projections.bean(JpaBatchAndStreamingJobStatusCounts.class,
                             JobStatusDslQueryExpressionBuilder.jobState().as("status"),
                             nflow.id.as("opsManagerNflowId"),
                             nflow.name.as("nflowName"),
                             nflow.isStream.as("isStream"),
                             nflowStats.runningNflowFlows.as("runningNflowFlows"),
                             jobExecution.jobExecutionId.count().as("count"),
                             nflowStats.lastActivityTimestamp.max().as("lastActivityTimestamp"));

        JPAQuery<?> query = factory.select(expr)
            .from(nflow)
            .innerJoin(jobInstance).on(jobInstance.nflow.id.eq(nflow.id))
            .innerJoin(jobExecution).on(jobExecution.jobInstance.jobInstanceId.eq(jobInstance.jobInstanceId))
            .leftJoin(nflowStats).on(nflow.id.uuid.eq(nflowStats.nflowId.uuid))
            .where(whereBuilder)
            .groupBy(jobExecution.status, nflow.id, nflow.name, nflow.isStream, nflowStats.runningNflowFlows);
        List<BatchAndStreamingJobStatusCount> stats = (List<BatchAndStreamingJobStatusCount>) query.fetch();

        return stats.stream().map(s -> {
            if (s.isStream()
                && (BatchJobExecution.RUNNING_DISPLAY_STATUS.equalsIgnoreCase(s.getStatus())
                    || BatchJobExecution.JobStatus.STARTING.name().equalsIgnoreCase(s.getStatus())
                    || BatchJobExecution.JobStatus.STARTED.name().equalsIgnoreCase(s.getStatus())) && s.getRunningNflowFlows() == 0L) {
                ((JpaBatchAndStreamingJobStatusCounts) s).setStatus(BatchJobExecution.JobStatus.STOPPED.name());
            }
            return s;
        }).collect(Collectors.toList());
        //  return stats;

    }


    /**
     * Get count of Jobs grouped by Status
     * Streaming Nflows are given a count of 1 if they are running, regardless of the number of active running flows
     */
    @Override
    public List<JobStatusCount> getJobStatusCount(String filter) {

        QJpaBatchJobExecution jobExecution = QJpaBatchJobExecution.jpaBatchJobExecution;

        QJpaBatchJobInstance jobInstance = QJpaBatchJobInstance.jpaBatchJobInstance;

        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;

        BooleanBuilder whereBuilder = new BooleanBuilder();
        if (StringUtils.isNotBlank(filter)) {
            whereBuilder.and(GenericQueryDslFilter.buildFilter(jobExecution, filter));
        }

        ConstructorExpression<JpaBatchJobExecutionStatusCounts> expr =
            Projections.constructor(JpaBatchJobExecutionStatusCounts.class,
                                    JobStatusDslQueryExpressionBuilder.jobState().as("status"),
                                    jobExecution.jobExecutionId.count().as("count"));

        JPAQuery<?> query = factory.select(expr)
            .from(jobExecution)
            .innerJoin(jobInstance).on(jobExecution.jobInstance.jobInstanceId.eq(jobInstance.jobInstanceId))
            .innerJoin(nflow).on(jobInstance.nflow.id.eq(nflow.id))
            .where(whereBuilder.and(nflow.isStream.eq(false))
                       .and(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, controller.isEntityAccessControlled())))
            .groupBy(jobExecution.status);
        List<JobStatusCount> stats = (List<JobStatusCount>) query.fetch();

        //merge in streaming nflow stats
        List<? extends NifiNflowStats> streamingNflowStats = nflowStatisticsProvider.findNflowStats(true);
        if (streamingNflowStats != null) {
            if (stats == null) {
                stats = new ArrayList<>();
            }
            Long runningCount = streamingNflowStats.stream().filter(s -> s.getRunningNflowFlows() > 0L).count();
            if (runningCount > 0) {
                JobStatusCount runningStatusCount = stats.stream().filter(s -> s.getStatus().equalsIgnoreCase(BatchJobExecution.RUNNING_DISPLAY_STATUS)).findFirst().orElse(null);
                if (runningStatusCount != null) {
                    runningCount = runningStatusCount.getCount() + runningCount;
                    runningStatusCount.setCount(runningCount);
                } else {
                    JpaBatchJobExecutionStatusCounts runningStreamingNflowCounts = new JpaBatchJobExecutionStatusCounts();
                    runningStreamingNflowCounts.setCount(runningCount);
                    runningStreamingNflowCounts.setStatus(BatchJobExecution.RUNNING_DISPLAY_STATUS);
                    stats.add(runningStreamingNflowCounts);
                }
            }
        }
        return stats;


    }

    @Override
    public List<JobStatusCount> getJobStatusCountByDate() {

        QJpaBatchJobExecution jobExecution = QJpaBatchJobExecution.jpaBatchJobExecution;

        QJpaBatchJobInstance jobInstance = QJpaBatchJobInstance.jpaBatchJobInstance;

        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;

        JPAQuery
            query = factory.select(
            Projections.constructor(JpaBatchJobExecutionStatusCounts.class,
                                    JobStatusDslQueryExpressionBuilder.jobState().as("status"),
                                    jobExecution.startYear,
                                    jobExecution.startMonth,
                                    jobExecution.startDay,
                                    jobExecution.count().as("count")))
            .from(jobExecution)
            .innerJoin(jobInstance).on(jobExecution.jobInstance.jobInstanceId.eq(jobInstance.jobInstanceId))
            .innerJoin(nflow).on(jobInstance.nflow.id.eq(nflow.id))
            .where(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, controller.isEntityAccessControlled()))
            .groupBy(jobExecution.status, jobExecution.startYear, jobExecution.startMonth, jobExecution.startDay);

        return (List<JobStatusCount>) query.fetch();

    }

    /**
     * gets job executions grouped by status and Day looking back from Now - the supplied {@code period}
     *
     * @param period period to look back from the current time to get job execution status
     */
    @Override
    public List<JobStatusCount> getJobStatusCountByDateFromNow(ReadablePeriod period, String filter) {

        QJpaBatchJobExecution jobExecution = QJpaBatchJobExecution.jpaBatchJobExecution;

        QJpaBatchJobInstance jobInstance = QJpaBatchJobInstance.jpaBatchJobInstance;

        QJpaOpsManagerNflow nflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;

        BooleanBuilder whereBuilder = new BooleanBuilder();
        whereBuilder.and(jobExecution.startTime.goe(DateTimeUtil.getNowUTCTime().minus(period)));
        if (StringUtils.isNotBlank(filter)) {
            whereBuilder.and(GenericQueryDslFilter.buildFilter(jobExecution, filter));
        }

        JPAQuery
            query = factory.select(
            Projections.constructor(JpaBatchJobExecutionStatusCounts.class,
                                    JobStatusDslQueryExpressionBuilder.jobState().as("status"),
                                    jobExecution.startYear,
                                    jobExecution.startMonth,
                                    jobExecution.startDay,
                                    jobExecution.count().as("count")))
            .from(jobExecution)
            .innerJoin(jobInstance).on(jobExecution.jobInstance.jobInstanceId.eq(jobInstance.jobInstanceId))
            .innerJoin(nflow).on(jobInstance.nflow.id.eq(nflow.id))
            .where(whereBuilder
                       .and(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, controller.isEntityAccessControlled())))
            .groupBy(jobExecution.status, jobExecution.startYear, jobExecution.startMonth, jobExecution.startDay);

        return (List<JobStatusCount>) query.fetch();

    }


    public List<String> findRelatedFlowFiles(String flowFileId) {
        return relatedRootFlowFilesRepository.findRelatedFlowFiles(flowFileId);
    }


    @Override
    public BatchJobExecution abandonJob(Long executionId) {
        BatchJobExecution execution = findByJobExecutionId(executionId);
        if (execution != null && !execution.getStatus().equals(BatchJobExecution.JobStatus.ABANDONED)) {
            if (execution.getStartTime() == null) {
                execution.setStartTime(DateTimeUtil.getNowUTCTime());
            }
            execution.setStatus(BatchJobExecution.JobStatus.ABANDONED);
            if (execution.getEndTime() == null) {
                execution.setEndTime(DateTimeUtil.getNowUTCTime());
            }
            String abandonMessage = "Job manually abandoned @ " + DateTimeUtil.getNowFormattedWithTimeZone();
            String msg = execution.getExitMessage() != null ? execution.getExitMessage() + "\n" : "";
            msg += abandonMessage;
            execution.setExitMessage(msg);
            //also stop any running steps??
            //find the nflow associated with the job
            OpsManagerNflow nflow = execution.getJobInstance().getNflow();

            save(execution);

            jobExecutionChangedNotifier.notifyAbandoned(execution, nflow, null);

            //clear the associated alert
            String alertId = execution.getJobExecutionContextAsMap().get(BatchJobExecutionProvider.NOVA_ALERT_ID_PROPERTY);
            if (StringUtils.isNotBlank(alertId)) {
                provider.respondTo(provider.resolve(alertId), (alert1, response) -> response.handle(abandonMessage));
            }

        }
        return execution;
    }

    public void notifyFailure(BatchJobExecution jobExecution, String nflowName, boolean isStream, String status) {
        OpsManagerNflow nflow = jobExecution.getJobInstance().getNflow();
        notifyFailure(jobExecution, nflow, isStream, status);
    }

    @Override
    public void notifySuccess(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status) {
        jobExecutionChangedNotifier.notifySuccess(jobExecution, nflow, status);
    }

    @Override
    public void notifyStopped(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status) {
        jobExecutionChangedNotifier.notifySuccess(jobExecution, nflow, status);
    }

    @Override
    public void notifyFailure(BatchJobExecution jobExecution, OpsManagerNflow nflow, boolean isStream, String status) {

        jobExecutionChangedNotifier.notifyOperationStatusEvent(jobExecution, nflow, NflowOperation.State.FAILURE, status);

        Alert alert = null;

        //see if the nflow has an unhandled alert already.
        String nflowId = nflow.getId().toString();
        String alertId = jobExecution.getJobExecutionContextAsMap().get(BatchJobExecutionProvider.NOVA_ALERT_ID_PROPERTY);
        String message = "Failed Job " + jobExecution.getJobExecutionId() + " for nflow " + nflow != null ? nflow.getName() : null;
        if (StringUtils.isNotBlank(alertId)) {
            alert = provider.getAlertAsServiceAccount(provider.resolve(alertId)).orElse(null);
        }
        if (alert == null) {
            alert = alertManager.createEntityAlert(OperationalAlerts.JOB_FALURE_ALERT_TYPE,
                                                   Alert.Level.FATAL,
                                                   message, alertManager.createEntityIdentificationAlertContent(nflowId,
                                                                                                                SecurityRole.ENTITY_TYPE.NFLOW, jobExecution.getJobExecutionId()));
            Alert.ID providerAlertId = provider.resolve(alert.getId(), alert.getSource());

            JpaBatchJobExecutionContextValue executionContext = new JpaBatchJobExecutionContextValue(jobExecution, NOVA_ALERT_ID_PROPERTY);
            executionContext.setStringVal(providerAlertId.toString());
            ((JpaBatchJobExecution) jobExecution).addJobExecutionContext(executionContext);
            save(jobExecution);


        } else {
            //if streaming nflow with unhandled alerts attempt to update alert content
            DefaultAlertChangeEventContent alertContent = null;

            if (isStream && alert.getState().equals(Alert.State.UNHANDLED)) {
                if (alert.getEvents() != null && alert.getEvents().get(0) != null) {

                    alertContent = alert.getEvents().get(0).getContent();

                    if (alertContent == null) {
                        alertContent = new DefaultAlertChangeEventContent();
                        alertContent.getContent().put("failedCount", 1);
                        alertContent.getContent().put("stream", true);
                    } else {
                        Integer count = (Integer) alertContent.getContent().putIfAbsent("failedCount", 0);
                        count++;
                        alertContent.getContent().put("failedCount", count);
                    }
                    final DefaultAlertChangeEventContent content = alertContent;
                    provider.respondTo(alert.getId(), (alert1, response) -> response.updateAlertChange(message, content));
                } else {
                    if (alertContent == null) {
                        alertContent = new DefaultAlertChangeEventContent();
                        alertContent.getContent().put("failedCount", 1);
                        alertContent.getContent().put("stream", true);
                    }

                    final DefaultAlertChangeEventContent content = alertContent;
                    provider.respondTo(alert.getId(), (alert1, response) -> response.unhandle(message, content));
                }
            } else {
                alertContent = new DefaultAlertChangeEventContent();
                alertContent.getContent().put("failedCount", 1);
                if (isStream) {
                    alertContent.getContent().put("stream", true);
                }
                final DefaultAlertChangeEventContent content = alertContent;
                provider.respondTo(alert.getId(), (alert1, response) -> response.unhandle(message, content));
            }

        }
    }



    /*
    public Page<? extends BatchJobExecution> findAllByExample(String filter, Pageable pageable){

        //construct new instance of JpaBatchExecution for searching
        JpaBatchJobExecution example = new JpaBatchJobExecution();
        example.setStatus(BatchJobExecution.JobStatus.valueOf("status"));
        ExampleMatcher matcher = ExampleMatcher.matching()
            .withIgnoreCase().withIgnoreCase(true);

      return   jobExecutionRepository.findAll(Example.of(example,matcher),pageable);

    }
    */


    @Override
    public void notifyBatchToStream(BatchJobExecution jobExecution, OpsManagerNflow nflow) {
        NflowOperationBatchStatusChange change = new NflowOperationBatchStatusChange(nflow.getId(), nflow.getName(), jobExecution.getJobExecutionId(), NflowOperationBatchStatusChange.BatchType.STREAM);
        clusterService.sendMessageToOthers(NflowOperationBatchStatusChange.CLUSTER_MESSAGE_TYPE, change);
    }

    @Override
    public void notifyStreamToBatch(BatchJobExecution jobExecution, OpsManagerNflow nflow) {
        NflowOperationBatchStatusChange change = new NflowOperationBatchStatusChange(nflow.getId(), nflow.getName(), jobExecution.getJobExecutionId(), NflowOperationBatchStatusChange.BatchType.BATCH);
        clusterService.sendMessageToOthers(NflowOperationBatchStatusChange.CLUSTER_MESSAGE_TYPE, change);
    }

    private class BatchStatusChangeReceiver implements ClusterServiceMessageReceiver {

        @Override
        public void onMessageReceived(String from, ClusterMessage message) {
            if (NflowOperationBatchStatusChange.CLUSTER_MESSAGE_TYPE.equalsIgnoreCase(message.getType())) {
                NflowOperationBatchStatusChange change = (NflowOperationBatchStatusChange) message.getMessage();
                latestStreamingJobByNflowName.remove(change.getNflowName());
            }

        }
    }
}
