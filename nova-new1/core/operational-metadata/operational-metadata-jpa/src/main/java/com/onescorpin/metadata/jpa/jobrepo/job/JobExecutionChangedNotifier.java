package com.onescorpin.metadata.jpa.jobrepo.job;
/*-
 * #%L
 * nova-operational-metadata-jpa
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

import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowOperationStatusEvent;
import com.onescorpin.metadata.api.event.nflow.OperationStatus;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.op.NflowOperation;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;

import javax.inject.Inject;

/**
 * Notify other listeners when a Job Execution changes status
 */
public class JobExecutionChangedNotifier {


    @Inject
    private MetadataEventService eventService;



    protected static NflowOperation.State asOperationState(BatchJobExecution.JobStatus status) {
        switch (status) {
            case ABANDONED:
                return NflowOperation.State.ABANDONED;
            case COMPLETED:
                return NflowOperation.State.SUCCESS;
            case FAILED:
                return NflowOperation.State.FAILURE;
            case STARTED:
                return NflowOperation.State.STARTED;
            case STARTING:
                return NflowOperation.State.STARTED;
            case STOPPING:
                return NflowOperation.State.STARTED;
            case STOPPED:
                return NflowOperation.State.CANCELED;
            case UNKNOWN:
                return NflowOperation.State.STARTED;
            default:
                return NflowOperation.State.FAILURE;
        }
    }


    public void notifyStopped(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status) {

        notifyOperationStatusEvent(jobExecution, nflow, NflowOperation.State.CANCELED, status);

    }

    public void notifyDataConfidenceJob(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status) {
        if (nflow == null) {
            nflow = jobExecution.getJobInstance().getNflow();
        }
        NflowOperation.State state = asOperationState(jobExecution.getStatus());
        if (StringUtils.isBlank(status)) {
            status = "Job " + jobExecution.getJobExecutionId() + " " + state.name().toLowerCase() + " for nflow: " + (nflow != null ? nflow.getName() : null);
        }
        this.eventService.notify(newNflowOperationStatusEvent(jobExecution.getJobExecutionId(), nflow, state, status));
    }


    public void notifySuccess(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status) {

        notifyOperationStatusEvent(jobExecution, nflow, NflowOperation.State.SUCCESS, status);

    }

    public void notifyAbandoned(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status) {

        notifyOperationStatusEvent(jobExecution, nflow, NflowOperation.State.ABANDONED, status);

    }

    public void notifyStarted(BatchJobExecution jobExecution, OpsManagerNflow nflow, String status) {
        notifyOperationStatusEvent(jobExecution, nflow, NflowOperation.State.STARTED, status);

    }

    public void notifyOperationStatusEvent(BatchJobExecution jobExecution, OpsManagerNflow nflow, NflowOperation.State state, String status) {

        if (nflow == null) {
            nflow = jobExecution.getJobInstance().getNflow();
        }
        if (StringUtils.isBlank(status)) {
            status = "Job " + jobExecution.getJobExecutionId() + " " + state.name().toLowerCase() + " for nflow: " + (nflow != null ? nflow.getName() : null);
        }
        this.eventService.notify(newNflowOperationStatusEvent(jobExecution.getJobExecutionId(), nflow, state, status));

    }

    private NflowOperationStatusEvent newNflowOperationStatusEvent(Long jobExecutionId, OpsManagerNflow nflow, NflowOperation.State state, String status) {
        String nflowName = nflow != null ? nflow.getName() : null;
        Nflow.ID nflowId = nflow != null ? nflow.getId() : null;
        NflowOperation.NflowType nflowType = nflow != null ? NflowOperation.NflowType.valueOf(nflow.getNflowType().name()) : NflowOperation.NflowType.NFLOW;
        return new NflowOperationStatusEvent(new OperationStatus(nflowId, nflowName, nflowType, new OpId(jobExecutionId), state, status));
    }

    protected static class OpId implements NflowOperation.ID {

        private final String idValue;

        public OpId(Serializable value) {
            this.idValue = value.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass().isAssignableFrom(obj.getClass())) {
                OpId that = (OpId) obj;
                return Objects.equals(this.idValue, that.idValue);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), this.idValue);
        }

        @Override
        public String toString() {
            return this.idValue;
        }
    }


}
