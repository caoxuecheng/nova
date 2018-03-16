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

import com.onescorpin.metadata.api.nflow.NflowSummary;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.ExecutionConstants;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;


@Entity
@Table(name = "NFLOW_SUMMARY_VIEW")
@Immutable
public class JpaNflowSummary implements NflowSummary {

    @EmbeddedId
    private JpaNflowSummaryId nflowSummaryId;

    @Column(name = "NFLOW_ID", insertable = false, updatable = false)
    private UUID nflowId;

    @Column(name = "NFLOW_NAME", insertable = false, updatable = false)
    private String nflowName;

    @Enumerated(EnumType.STRING)
    @Column(name = "NFLOW_TYPE")
    private OpsManagerNflow.NflowType nflowType = OpsManagerNflow.NflowType.NFLOW;

    @Column(name = "IS_STREAM", length = 1)
    @org.hibernate.annotations.Type(type = "yes_no")
    private boolean isStream;

    @Column(name = "JOB_EXECUTION_ID", insertable = false, updatable = false)
    private Long jobExecutionId;

    @Column(name = "JOB_INSTANCE_ID", insertable = false, updatable = false)
    private Long jobInstanceId;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "RUN_STATUS", insertable = false, updatable = false)
    private RunStatus runStatus;

    @Column(name = "ALL_COUNT")
    private Long allCount;

    @Column(name = "FAILED_COUNT")
    private Long failedCount;

    @Column(name = "COMPLETED_COUNT")
    private Long completedCount;

    @Column(name = "ABANDONED_COUNT")
    private Long abandonedCount;

    @Column(name = "RUNNING_COUNT", insertable = false, updatable = false)
    private Long runningCount;

    public JpaNflowSummaryId getNflowSummaryId() {
        return nflowSummaryId;
    }

    public void setNflowSummaryId(JpaNflowSummaryId nflowSummaryId) {
        this.nflowSummaryId = nflowSummaryId;
    }

    public void setNflowId(UUID nflowId) {
        this.nflowId = nflowId;
    }

    @Override
    public UUID getNflowId() {
        return nflowId;
    }

    @Override
    public String getNflowIdAsString() {
        return nflowId.toString();
    }

    public void setNflowId(String nflowId) {
        this.nflowId = UUID.fromString(nflowId);
    }

    @Override
    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    @Override
    public OpsManagerNflow.NflowType getNflowType() {
        return nflowType;
    }

    public void setNflowType(OpsManagerNflow.NflowType nflowType) {
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
    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public Long getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(Long jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
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
    public RunStatus getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(RunStatus runStatus) {
        this.runStatus = runStatus;
    }

    public Long getAllCount() {
        return allCount;
    }

    public void setAllCount(Long allCount) {
        this.allCount = allCount;
    }

    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }

    public Long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Long completedCount) {
        this.completedCount = completedCount;
    }

    public Long getAbandonedCount() {
        return abandonedCount;
    }

    public void setAbandonedCount(Long abandonedCount) {
        this.abandonedCount = abandonedCount;
    }

    public Long getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(Long runningCount) {
        this.runningCount = runningCount;
    }

    @Embeddable
    public static class JpaNflowSummaryId implements Serializable, NflowSummary.ID {

        private static final long serialVersionUID = 6017751710414995750L;

        @Column(name = "ID")
        private String id;


        public JpaNflowSummaryId() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            JpaNflowSummaryId that = (JpaNflowSummaryId) o;

            return id != null ? id.equals(that.id) : that.id == null;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }
}
