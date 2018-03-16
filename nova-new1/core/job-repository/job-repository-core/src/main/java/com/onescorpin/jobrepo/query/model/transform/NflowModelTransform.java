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

import com.onescorpin.jobrepo.query.model.DefaultExecutedNflow;
import com.onescorpin.jobrepo.query.model.DefaultNflowHealth;
import com.onescorpin.jobrepo.query.model.DefaultNflowStatus;
import com.onescorpin.jobrepo.query.model.DefaultNflowSummary;
import com.onescorpin.jobrepo.query.model.ExecutedNflow;
import com.onescorpin.jobrepo.query.model.ExecutionStatus;
import com.onescorpin.jobrepo.query.model.NflowHealth;
import com.onescorpin.jobrepo.query.model.NflowStatus;
import com.onescorpin.metadata.api.nflow.NflowSummary;
import com.onescorpin.metadata.api.nflow.LatestNflowJobExecution;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.ExecutionConstants;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Transform Nflow domain model objects to REST friendly objects
 */
public class NflowModelTransform {

    /**
     * Transform the BatchJobExectution into an ExecutedNflow object
     *
     * @param jobExecution the job to transform
     * @param nflow         the nflow this job relates to
     * @return the ExecutedNflow transformed from the BatchJobExecution
     */
    public static ExecutedNflow executedNflow(BatchJobExecution jobExecution, OpsManagerNflow nflow) {
        return executedNflow(jobExecution, nflow.getName());
    }

    /**
     * Transform the BatchJobExectution into an ExecutedNflow object
     *
     * @param jobExecution the job to transform
     * @param nflowName     the name of the nflow the job relates to
     * @return the ExecutedNflow transformed from the BatchJobExecution
     */
    public static ExecutedNflow executedNflow(BatchJobExecution jobExecution, String nflowName) {

        DefaultExecutedNflow executedNflow = new DefaultExecutedNflow();
        executedNflow.setNflowExecutionId(jobExecution.getJobExecutionId());
        executedNflow.setStartTime(jobExecution.getStartTime());
        executedNflow.setEndTime(jobExecution.getEndTime());
        executedNflow.setExitCode(jobExecution.getExitCode().name());
        executedNflow.setExitStatus(jobExecution.getExitMessage());
        executedNflow.setStatus(ExecutionStatus.valueOf(jobExecution.getStatus().name()));
        executedNflow.setName(nflowName);
        executedNflow.setRunTime(ModelUtils.runTime(jobExecution.getStartTime(), jobExecution.getEndTime()));
        executedNflow.setTimeSinceEndTime(ModelUtils.timeSince(jobExecution.getStartTime(), jobExecution.getEndTime()));
        executedNflow.setNflowInstanceId(jobExecution.getJobInstance().getJobInstanceId());
        return executedNflow;

    }


    /**
     * Transform the LatestJobExecution into an ExecutedNflow object
     *
     * @return the ExecutedNflow from the LatestNflowJobExecution
     */
    public static ExecutedNflow executedNflow(LatestNflowJobExecution jobExecution) {

        DefaultExecutedNflow executedNflow = new DefaultExecutedNflow();
        executedNflow.setNflowExecutionId(jobExecution.getJobExecutionId());
        executedNflow.setStartTime(jobExecution.getStartTime());
        executedNflow.setEndTime(jobExecution.getEndTime());
        executedNflow.setExitCode(jobExecution.getExitCode().name());
        executedNflow.setExitStatus(jobExecution.getExitMessage());
        executedNflow.setStatus(ExecutionStatus.valueOf(jobExecution.getStatus().name()));
        executedNflow.setName(jobExecution.getNflowName());
        executedNflow.setRunTime(ModelUtils.runTime(jobExecution.getStartTime(), jobExecution.getEndTime()));
        executedNflow.setTimeSinceEndTime(ModelUtils.timeSince(jobExecution.getStartTime(), jobExecution.getEndTime()));
        executedNflow.setNflowInstanceId(jobExecution.getJobInstanceId());
        return executedNflow;

    }

    /**
     * Transform the NflowHealth object into an Executed nflow
     *
     * @return the ExecutedNflow from the NflowHealth object
     */
    public static ExecutedNflow executedNflow(com.onescorpin.metadata.api.nflow.NflowHealth nflowHealth) {

        DefaultExecutedNflow executedNflow = new DefaultExecutedNflow();
        executedNflow.setNflowExecutionId(nflowHealth.getJobExecutionId());
        executedNflow.setStartTime(nflowHealth.getStartTime());
        executedNflow.setEndTime(nflowHealth.getEndTime());
        executedNflow.setExitCode(nflowHealth.getExitCode().name());
        executedNflow.setExitStatus(nflowHealth.getExitMessage());
        executedNflow.setStatus(ExecutionStatus.valueOf(nflowHealth.getStatus().name()));
        executedNflow.setName(nflowHealth.getNflowName());
        executedNflow.setRunTime(ModelUtils.runTime(nflowHealth.getStartTime(), nflowHealth.getEndTime()));
        executedNflow.setTimeSinceEndTime(ModelUtils.timeSince(nflowHealth.getStartTime(), nflowHealth.getEndTime()));
        executedNflow.setNflowInstanceId(nflowHealth.getJobInstanceId());
        return executedNflow;

    }

    /**
     * Transform the NflowHealth object into an Executed nflow
     *
     * @return the ExecutedNflow from the NflowHealth object
     */
    public static ExecutedNflow executedNflow(NflowSummary nflowSummary) {

        DefaultExecutedNflow executedNflow = new DefaultExecutedNflow();
        executedNflow.setNflowExecutionId(nflowSummary.getJobExecutionId());
        executedNflow.setStartTime(nflowSummary.getStartTime());
        executedNflow.setEndTime(nflowSummary.getEndTime());
        if(nflowSummary.getExitCode() != null) {
            executedNflow.setExitCode(nflowSummary.getExitCode().name());
        }
        else {
            executedNflow.setExitCode(ExecutionConstants.ExitCode.UNKNOWN.name());
        }
        executedNflow.setExitStatus(nflowSummary.getExitMessage());
        if(nflowSummary.getStatus() != null) {
            executedNflow.setStatus(ExecutionStatus.valueOf(nflowSummary.getStatus().name()));
        }
        else {
            executedNflow.setStatus(ExecutionStatus.UNKNOWN);
        }
        executedNflow.setName(nflowSummary.getNflowName());
        executedNflow.setRunTime(ModelUtils.runTime(nflowSummary.getStartTime(), nflowSummary.getEndTime()));
        executedNflow.setTimeSinceEndTime(ModelUtils.timeSince(nflowSummary.getStartTime(), nflowSummary.getEndTime()));
        executedNflow.setNflowInstanceId(nflowSummary.getJobInstanceId());
        return executedNflow;

    }

    /**
     * Transforms the NflowHealth domain object to the Rest model object
     */
    public static List<NflowHealth> nflowHealth(List<? extends com.onescorpin.metadata.api.nflow.NflowHealth> domain) {
        if (domain != null && !domain.isEmpty()) {
            return domain.stream().map(d -> nflowHealth(d)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Transform the NflowHealth domain object to the REST friendly NflowHealth object
     *
     * @return the transformed NflowHealth object
     */
    public static NflowHealth nflowHealth(com.onescorpin.metadata.api.nflow.NflowHealth domain) {
        NflowHealth nflowHealth = new DefaultNflowHealth();
        nflowHealth.setUnhealthyCount(domain.getFailedCount());
        nflowHealth.setHealthyCount(domain.getCompletedCount());
        nflowHealth.setNflow(domain.getNflowName());
        nflowHealth.setNflowId(domain.getNflowId() != null ? domain.getNflowId().toString() : null);
        nflowHealth.setLastOpNflow(executedNflow(domain));
        nflowHealth.setStream(domain.isStream());
        nflowHealth.setRunningCount(domain.getRunningCount());
        return nflowHealth;
    }


    /**
     * Transform the NflowHealth domain object to the REST friendly NflowHealth object
     *
     * @return the transformed NflowHealth object
     */
    public static NflowHealth nflowHealth(NflowSummary domain) {
        NflowHealth nflowHealth = new DefaultNflowHealth();
        nflowHealth.setUnhealthyCount(domain.getFailedCount());
        nflowHealth.setHealthyCount(domain.getCompletedCount());
        nflowHealth.setNflow(domain.getNflowName());
        nflowHealth.setNflowId(domain.getNflowId() != null ? domain.getNflowId().toString() : null);
        nflowHealth.setLastOpNflow(executedNflow(domain));
        nflowHealth.setStream(domain.isStream());
        nflowHealth.setRunningCount(domain.getRunningCount());
        return nflowHealth;
    }


    /**
     * Transforms the nflow object to the REST friendly NflowHealth object, assuming the nflow has not yet executed.
     *
     * @param domain the nflow object
     * @return the transformed NflowHealth object
     */
    @Nonnull
    public static NflowHealth nflowHealth(@Nonnull final OpsManagerNflow domain) {
        final NflowHealth nflowHealth = new DefaultNflowHealth();
        nflowHealth.setUnhealthyCount(0L);
        nflowHealth.setHealthyCount(0L);
        nflowHealth.setNflow(domain.getName());
        nflowHealth.setNflowId(domain.getId() != null ? domain.getId().toString() : null);
        nflowHealth.setStream(domain.isStream());
        return nflowHealth;
    }

    /**
     * Transform the list of NflowHealth objects to a NflowStatus object summarizing the nflows.
     *
     * @return the NflowStatus summarizing the Nflows for the list of NflowHealth objects
     */
    public static NflowStatus nflowStatus(List<NflowHealth> nflowHealth) {

        DefaultNflowStatus status = new DefaultNflowStatus(nflowHealth);
        return status;

    }


    public static NflowStatus nflowStatusFromNflowSummary(List<com.onescorpin.jobrepo.query.model.NflowSummary> nflowSummaryList) {

        DefaultNflowStatus status = new DefaultNflowStatus();
        status.populate(nflowSummaryList);
        return status;

    }

    /**
     * Transforms the nflow object to a NflowStatus object summarizing the nflow, assuming the nflow has not yet executed.
     *
     * @param domain the nflow object
     * @return the NflowStatus summarizing the Nflow
     */
    @Nonnull
    public static NflowStatus nflowStatus(@Nonnull final OpsManagerNflow domain) {
        final DefaultNflowStatus status = new DefaultNflowStatus(null);
        status.getNflowSummary().add(new DefaultNflowSummary(nflowHealth(domain)));
        return status;
    }
}
