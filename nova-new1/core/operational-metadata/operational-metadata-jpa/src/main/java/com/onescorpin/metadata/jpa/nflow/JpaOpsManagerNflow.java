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

import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobInstance;
import com.onescorpin.metadata.jpa.jobrepo.job.JpaBatchJobInstance;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.OneToMany;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureParameter;
import javax.persistence.Table;

/**
 * This entity is used to Map the Operational Nflow Data with the Modeshape JCR nflow data. The ID here maps directly to the JCR Modeshape Nflow.ID
 * Two stored procedures are mapped here to delete jobs for a given nflow, and abandon all failed jobs for a nflow.
 */
@Entity
@Table(name = "NFLOW")
@NamedStoredProcedureQueries({
                                 @NamedStoredProcedureQuery(name = "OpsManagerNflow.deleteNflowJobs", procedureName = "delete_nflow_jobs", parameters = {
                                     @StoredProcedureParameter(mode = ParameterMode.IN, name = "category", type = String.class),
                                     @StoredProcedureParameter(mode = ParameterMode.IN, name = "nflow", type = String.class),
                                     @StoredProcedureParameter(mode = ParameterMode.OUT, name = "result", type = Integer.class)
                                 }),
                                 @NamedStoredProcedureQuery(name = "OpsManagerNflow.abandonNflowJobs", procedureName = "abandon_nflow_jobs", parameters = {
                                     @StoredProcedureParameter(mode = ParameterMode.IN, name = "nflow", type = String.class),
                                     @StoredProcedureParameter(mode = ParameterMode.IN, name = "exitMessage", type = String.class),
                                     @StoredProcedureParameter(mode = ParameterMode.IN, name = "username", type = String.class),
                                     @StoredProcedureParameter(mode = ParameterMode.OUT, name = "res", type = Integer.class)
                                 })
                             })
public class JpaOpsManagerNflow implements OpsManagerNflow {

    @EmbeddedId
    private OpsManagerNflowId id;

    @Column(name = "name", length = 100, unique = true, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "NFLOW_TYPE")
    private NflowType nflowType = NflowType.NFLOW;

    @Column(name = "IS_STREAM", length = 1)
    @org.hibernate.annotations.Type(type = "yes_no")
    private boolean isStream;

    @Column(name =" TIME_BTWN_BATCH_JOBS")
    private Long timeBetweenBatchJobs;

    @OneToMany(targetEntity = JpaBatchJobInstance.class, mappedBy = "nflow", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<BatchJobInstance> jobInstances = new HashSet<>();


    /**
     * The NFLOW_CHECK_DATA_NFLOWS is a many to many table linking a Nflow to any other nflows which are registered to check the data of the related nflow.
     */
    @ManyToMany(targetEntity = JpaOpsManagerNflow.class, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "NFLOW_CHECK_DATA_NFLOWS",
               joinColumns = {@JoinColumn(name = "NFLOW_ID")},
               inverseJoinColumns = {@JoinColumn(name = "CHECK_DATA_NFLOW_ID")})
    private Set<OpsManagerNflow> checkDataNflows = new HashSet<OpsManagerNflow>();

    @ManyToMany(targetEntity = JpaOpsManagerNflow.class, mappedBy = "checkDataNflows")
    private Set<OpsManagerNflow> nflowsToCheck = new HashSet<OpsManagerNflow>();

    public JpaOpsManagerNflow(OpsManagerNflow.ID id, String name) {
        this.id = (OpsManagerNflowId) id;
        this.name = name;
    }

    public JpaOpsManagerNflow() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OpsManagerNflowId getId() {
        return id;
    }

    public void setId(OpsManagerNflowId id) {
        this.id = id;
    }

    public NflowType getNflowType() {
        return nflowType;
    }

    public void setNflowType(NflowType nflowType) {
        this.nflowType = nflowType;
    }

    public Set<BatchJobInstance> getJobInstances() {
        return jobInstances;
    }

    public void setJobInstances(Set<BatchJobInstance> jobInstances) {
        this.jobInstances = jobInstances;
    }

    public Set<OpsManagerNflow> getCheckDataNflows() {
        if (checkDataNflows == null) {
            checkDataNflows = new HashSet<>();
        }
        return checkDataNflows;
    }

    public void setCheckDataNflows(Set<OpsManagerNflow> checkDataNflows) {
        this.checkDataNflows = checkDataNflows;
    }

    public Set<OpsManagerNflow> getNflowsToCheck() {
        if (nflowsToCheck == null) {
            nflowsToCheck = new HashSet<>();
        }
        return nflowsToCheck;
    }

    public void setNflowsToCheck(Set<OpsManagerNflow> nflowsToCheck) {
        this.nflowsToCheck = nflowsToCheck;
    }

    @Override
    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean stream) {
        isStream = stream;
    }

    public Long getTimeBetweenBatchJobs() {
        return timeBetweenBatchJobs;
    }

    public void setTimeBetweenBatchJobs(Long timeBetweenBatchJobs) {
        this.timeBetweenBatchJobs = timeBetweenBatchJobs;
    }
}
