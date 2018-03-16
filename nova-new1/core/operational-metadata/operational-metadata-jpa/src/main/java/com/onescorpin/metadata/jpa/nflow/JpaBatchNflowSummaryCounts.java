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
import com.onescorpin.metadata.api.nflow.BatchNflowSummaryCounts;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * View entity summarizing a nflow and its {@link com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution} execution counts
 */
@Entity
@Table(name = "BATCH_NFLOW_SUMMARY_COUNTS_VW")
public class JpaBatchNflowSummaryCounts implements BatchNflowSummaryCounts {

    @OneToOne(targetEntity = JpaOpsManagerNflow.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "NFLOW_ID")
    OpsManagerNflow nflow;

    @EmbeddedId
    BatchNflowSummaryCountsNflowId nflowId;

    @Column(name = "NFLOW_NAME", insertable = false, updatable = false)
    String nflowName;

    @Column(name = "ALL_COUNT")
    Long allCount;

    @Column(name = "FAILED_COUNT")
    Long failedCount;

    @Column(name = "COMPLETED_COUNT")
    Long completedCount;

    @Column(name = "ABANDONED_COUNT")
    Long abandonedCount;

    public JpaBatchNflowSummaryCounts() {

    }


    @Override
    public OpsManagerNflow getNflow() {
        return nflow;
    }

    public void setNflow(OpsManagerNflow nflow) {
        this.nflow = nflow;
    }

    @Override
    public OpsManagerNflow.ID getNflowId() {
        return nflowId;
    }

    public void setNflowId(OpsManagerNflow.ID nflowId) {
        this.nflowId = (BatchNflowSummaryCountsNflowId) nflowId;
    }

    @Override
    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    @Override
    public Long getAllCount() {
        return allCount;
    }

    public void setAllCount(Long allCount) {
        this.allCount = allCount;
    }

    @Override
    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }

    @Override
    public Long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Long completedCount) {
        this.completedCount = completedCount;
    }

    @Override
    public Long getAbandonedCount() {
        return abandonedCount;
    }

    public void setAbandonedCount(Long abandonedCount) {
        this.abandonedCount = abandonedCount;
    }

    @Embeddable
    public static class BatchNflowSummaryCountsNflowId extends BaseJpaId implements Serializable, OpsManagerNflow.ID {

        private static final long serialVersionUID = 6017751710414995750L;

        @Column(name = "nflow_id")
        private UUID uuid;


        public BatchNflowSummaryCountsNflowId() {
        }

        public BatchNflowSummaryCountsNflowId(Serializable ser) {
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
