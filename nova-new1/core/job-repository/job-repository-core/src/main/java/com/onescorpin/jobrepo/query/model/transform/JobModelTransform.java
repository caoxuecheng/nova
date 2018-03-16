package com.onescorpin.jobrepo.query.model.transform;

/*-
 * #%L
 * onescorpin-job-repository-core
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

import com.onescorpin.jobrepo.common.constants.CheckDataStepConstants;
import com.onescorpin.jobrepo.query.model.CheckDataJob;
import com.onescorpin.jobrepo.query.model.DefaultCheckDataJob;
import com.onescorpin.jobrepo.query.model.DefaultExecutedJob;
import com.onescorpin.jobrepo.query.model.DefaultExecutedStep;
import com.onescorpin.jobrepo.query.model.ExecutedJob;
import com.onescorpin.jobrepo.query.model.ExecutedStep;
import com.onescorpin.jobrepo.query.model.ExecutionStatus;
import com.onescorpin.jobrepo.query.model.ExitStatus;
import com.onescorpin.metadata.api.nflow.LatestNflowJobExecution;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiEventStepExecution;
import com.onescorpin.metadata.api.jobrepo.step.BatchStepExecution;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 */
public class JobModelTransform {


    public static ExecutedJob executedJob(BatchJobExecution jobExecution) {
        DefaultExecutedJob job = (DefaultExecutedJob) executedJobSimple(jobExecution);
        Map<String, String> jobExecutionContext = jobExecution.getJobExecutionContextAsMap();
        if (jobExecutionContext != null) {
            job.setExecutionContext(new HashMap<>(jobExecutionContext));
        }
        OpsManagerNflow nflow = jobExecution.getJobInstance().getNflow();
        if (nflow != null) {
            job.setJobType(nflow.getNflowType().name());
        }
        Map<String, String> jobParams = jobExecution.getJobParametersAsMap();
        if (jobParams != null) {
            job.setJobParameters(new HashMap<>(jobParams));
        }

        job.setExecutedSteps(executedSteps(jobExecution.getStepExecutions()));
        return job;
    }

    public static ExecutedJob executedJobSimple(BatchJobExecution jobExecution) {
        DefaultExecutedJob job = new DefaultExecutedJob();
        job.setExecutionId(jobExecution.getJobExecutionId());
        job.setStartTime(jobExecution.getStartTime());
        job.setEndTime(jobExecution.getEndTime());
        job.setCreateTime(jobExecution.getCreateTime());
        job.setExitCode(jobExecution.getExitCode().name());
        job.setExitStatus(jobExecution.getExitMessage());
        job.setStatus(ExecutionStatus.valueOf(jobExecution.getStatus().name()));
        job.setJobName(jobExecution.getJobInstance().getJobName());
        job.setRunTime(ModelUtils.runTime(jobExecution.getStartTime(), jobExecution.getEndTime()));
        job.setTimeSinceEndTime(ModelUtils.timeSince(jobExecution.getStartTime(), jobExecution.getEndTime()));
        job.setInstanceId(jobExecution.getJobInstance().getJobInstanceId());
        job.setStream(jobExecution.isStream());
        if (jobExecution.getJobInstance() != null && jobExecution.getJobInstance().getNflow() != null) {
            job.setNflowName(jobExecution.getJobInstance().getNflow().getName());
            job.setNflowId(jobExecution.getJobInstance().getNflow().getId().toString());
        }
        return job;
    }

    public static ExecutedStep executedStep(BatchStepExecution stepExecution) {
        DefaultExecutedStep step = new DefaultExecutedStep();
        NifiEventStepExecution nifiEventStepExecution = stepExecution.getNifiEventStepExecution();
        if (nifiEventStepExecution != null) {
            step.setNifiEventId(nifiEventStepExecution.getEventId());
        }
        step.setRunning(!stepExecution.isFinished());
        step.setStartTime(stepExecution.getStartTime());
        step.setEndTime(stepExecution.getEndTime());
        step.setLastUpdateTime(stepExecution.getLastUpdated());
        step.setVersion(stepExecution.getVersion().intValue());
        step.setStepName(stepExecution.getStepName());
        step.setExitDescription(stepExecution.getExitMessage());
        step.setExitCode(stepExecution.getExitCode().name());
        step.setId(stepExecution.getStepExecutionId());
        step.setTimeSinceEndTime(ModelUtils.timeSince(stepExecution.getStartTime(), stepExecution.getEndTime()));
        step.setRunTime(ModelUtils.runTime(stepExecution.getStartTime(), stepExecution.getEndTime()));
        Map<String, String> stepExecutionContext = stepExecution.getStepExecutionContextAsMap();
        if (stepExecutionContext != null) {
            step.setExecutionContext(new HashMap<>(stepExecutionContext));
        }
        return step;

    }

    public static ExecutedJob executedJob(LatestNflowJobExecution jobExecution) {

        DefaultExecutedJob executedJob = new DefaultExecutedJob();
        executedJob.setExecutionId(jobExecution.getJobExecutionId());
        executedJob.setStartTime(jobExecution.getStartTime());
        executedJob.setEndTime(jobExecution.getEndTime());
        executedJob.setExitCode(jobExecution.getExitCode().name());
        executedJob.setExitStatus(jobExecution.getExitMessage());
        executedJob.setStatus(ExecutionStatus.valueOf(jobExecution.getStatus().name()));
        executedJob.setJobName(jobExecution.getNflowName());
        executedJob.setRunTime(ModelUtils.runTime(jobExecution.getStartTime(), jobExecution.getEndTime()));
        executedJob.setTimeSinceEndTime(ModelUtils.timeSince(jobExecution.getStartTime(), jobExecution.getEndTime()));
        executedJob.setInstanceId(jobExecution.getJobInstanceId());
        executedJob.setStream(jobExecution.isStream());
        executedJob.setNflowId(jobExecution.getNflowId());
        executedJob.setNflowName(jobExecution.getNflowName());
        return executedJob;

    }

    public static List<ExecutedStep> executedSteps(Collection<? extends BatchStepExecution> steps) {
        if (steps != null && !steps.isEmpty()) {
            return steps.stream().map(stepExecution -> executedStep(stepExecution)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static List<ExecutedJob> executedJobs(Collection<? extends BatchJobExecution> jobs) {
        if (jobs != null && !jobs.isEmpty()) {
            return jobs.stream().map(jobExecution -> executedJob(jobExecution)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static List<ExecutedJob> executedJobsSimple(Collection<? extends BatchJobExecution> jobs) {
        if (jobs != null && !jobs.isEmpty()) {
            return jobs.stream().map(jobExecution -> executedJobSimple(jobExecution)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static CheckDataJob checkDataJob(LatestNflowJobExecution latestNflowJobExecution) {
        ExecutedJob job = executedJob(latestNflowJobExecution);
        CheckDataJob checkDataJob = new DefaultCheckDataJob(job);
        boolean valid = false;
        String msg = "Unknown";
        Map<String, String> jobExecutionContext = latestNflowJobExecution.getJobExecution().getJobExecutionContextAsMap();
        if (BatchJobExecution.JobStatus.ABANDONED.equals(latestNflowJobExecution.getStatus())) {
            valid = true;
            msg = latestNflowJobExecution.getExitMessage();
        } else if (jobExecutionContext != null) {
            msg = jobExecutionContext.get(CheckDataStepConstants.VALIDATION_MESSAGE_KEY);
            String isValid = jobExecutionContext.get(CheckDataStepConstants.VALIDATION_KEY);
            if (msg == null) {
                msg = job.getExitStatus();
            }
            if (StringUtils.isBlank(isValid)) {
                valid = ExitStatus.COMPLETED.getExitCode().equals(job.getExitStatus());
            } else {
                valid = BooleanUtils.toBoolean(isValid);
            }
        }
        checkDataJob.setValidationMessage(msg);
        checkDataJob.setIsValid(valid);
        return checkDataJob;
    }

}
