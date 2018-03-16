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

import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobInstance;
import com.onescorpin.metadata.jpa.nflow.JpaOpsManagerNflow;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

/**
 * Entity to store the Batch Job Instances
 */
@Entity
@Table(name = "BATCH_JOB_INSTANCE")
public class JpaBatchJobInstance implements BatchJobInstance {


    @TableGenerator(
        name = "JOB_INSTANCE_KEY_GENERATOR",
        table = "GENERATED_KEYS",
        pkColumnName = "PK_COLUMN",
        valueColumnName = "VALUE_COLUMN",
        pkColumnValue = "JOB_INSTANCE_ID",
        allocationSize = 1)
    @Id
    @Column(name = "JOB_INSTANCE_ID")
    @GeneratedValue(generator = "JOB_INSTANCE_KEY_GENERATOR", strategy = GenerationType.TABLE)
    private Long jobInstanceId;
    @Version
    @Column(name = "VERSION")
    private Long version = 0L;
    @Column(name = "JOB_NAME")
    @Type(type = "com.onescorpin.jpa.TruncateStringUserType", parameters = {@Parameter(name = "length", value = "255")})
    private String jobName;
    @Column(name = "JOB_KEY")
    private String jobKey;

    @OneToMany(targetEntity = JpaBatchJobExecution.class, mappedBy = "jobInstance", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BatchJobExecution> jobExecutions = new ArrayList<>();

    @ManyToOne(targetEntity = JpaOpsManagerNflow.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "NFLOW_ID", nullable = true, insertable = true, updatable = true)
    private OpsManagerNflow nflow;


    public JpaBatchJobInstance() {

    }

    @Override
    public Long getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(Long jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    @Override
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getJobKey() {
        return jobKey;
    }

    public void setJobKey(String jobKey) {
        this.jobKey = jobKey;
    }

    @Override
    public List<BatchJobExecution> getJobExecutions() {
        return jobExecutions;
    }

    public void setJobExecutions(List<BatchJobExecution> jobExecutions) {
        this.jobExecutions = jobExecutions;
    }

    public OpsManagerNflow getNflow() {
        return nflow;
    }

    public void setNflow(OpsManagerNflow nflow) {
        this.nflow = nflow;
    }
}
