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

import com.onescorpin.jpa.BaseJpaId;
import com.onescorpin.metadata.api.nflow.LatestNflowJobExecution;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.ExecutionConstants;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.jpa.jobrepo.job.JpaBatchJobExecution;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 */
@Entity
@Table(name = "LATEST_FINISHED_NFLOW_JOB_VW")
public class JpaLatestNflowJobExecution implements LatestNflowJobExecution {

    @ManyToOne(targetEntity = JpaOpsManagerNflow.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "NFLOW_ID", insertable = false, updatable = false)
    private OpsManagerNflow nflow;

    private LatestJobExecutionNflowId nflowId;

    @Column(name = "NFLOW_NAME", insertable = false, updatable = false)
    private String nflowName;

    @Column(name = "NFLOW_TYPE", insertable = false, updatable = false)
    String nflowType;

    @Id
    @Column(name = "JOB_EXECUTION_ID", insertable = false, updatable = false)
    private Long jobExecutionId;

    @Column(name = "JOB_INSTANCE_ID", insertable = false, updatable = false)
    private Long jobInstanceId;


    @ManyToOne(targetEntity = JpaBatchJobExecution.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "JOB_EXECUTION_ID", insertable = false, updatable = false)
    private BatchJobExecution jobExecution;


    @Type(type = "com.onescorpin.jpa.PersistentDateTimeAsMillisLong")
    @Column(name = "START_TIME")
    private DateTime startTime;

    @Type(type = "com.onescorpin.jpa.PersistentDateTimeAsMillisLong")
    @Column(name = "END_TIME")
    private DateTime endTime;


    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 10, nullable = false)
    private BatchJobExecution.JobStatus status = BatchJobExecution.JobStatus.UNKNOWN;


    @Enumerated(EnumType.STRING)
    @Column(name = "EXIT_CODE")
    private ExecutionConstants.ExitCode exitCode = ExecutionConstants.ExitCode.UNKNOWN;

    @Column(name = "EXIT_MESSAGE")
    @Type(type = "com.onescorpin.jpa.TruncateStringUserType", parameters = {@Parameter(name = "length", value = "2500")})
    private String exitMessage;

    @Column(name = "IS_STREAM", length = 1)
    @org.hibernate.annotations.Type(type = "yes_no")
    private boolean isStream;


    public JpaLatestNflowJobExecution() {

    }

    @Override
    public OpsManagerNflow getNflow() {
        return nflow;
    }

    public void setNflow(OpsManagerNflow nflow) {
        this.nflow = nflow;
    }

    @Override
    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    @Override
    public BatchJobExecution getJobExecution() {
        return jobExecution;
    }

    public void setJobExecution(BatchJobExecution jobExecution) {
        this.jobExecution = jobExecution;
    }

    @Override
    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    @Override
    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(DateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public BatchJobExecution.JobStatus getStatus() {
        return status;
    }

    public void setStatus(BatchJobExecution.JobStatus status) {
        this.status = status;
    }

    @Override
    public ExecutionConstants.ExitCode getExitCode() {
        return exitCode;
    }

    public void setExitCode(ExecutionConstants.ExitCode exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public String getExitMessage() {
        return exitMessage;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
    }

    @Override
    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public Long getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(Long jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public String getNflowType() {
        return nflowType;
    }

    public void setNflowType(String nflowType) {
        this.nflowType = nflowType;
    }

    @Override
    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean stream) {
        isStream = stream;
    }


    @Override
    public String getNflowId() {
        return nflowId != null ? nflowId.toString() : null;
    }

    public void setNflowId(LatestJobExecutionNflowId nflowId) {
        this.nflowId = nflowId;
    }


    @Embeddable
    public static class LatestJobExecutionNflowId extends BaseJpaId implements Serializable, OpsManagerNflow.ID {

        private static final long serialVersionUID = 6017751710414995750L;

        @Column(name = "NFLOW_ID",insertable = false, updatable = false)
        private UUID uuid;

        public LatestJobExecutionNflowId() {
        }

        public LatestJobExecutionNflowId(Serializable ser) {
            super(ser);
        }

        @Override
        public UUID getUuid() {
            return this.uuid;
        }

        @Override
        public void setUuid(UUID uuid) {
            this.uuid = uuid;
        }
    }
}