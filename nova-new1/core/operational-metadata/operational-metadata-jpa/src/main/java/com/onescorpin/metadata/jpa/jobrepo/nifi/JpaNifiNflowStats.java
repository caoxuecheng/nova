package com.onescorpin.metadata.jpa.jobrepo.nifi;

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
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStats;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "NIFI_NFLOW_STATS")
public class JpaNifiNflowStats implements NifiNflowStats {

    @Id
    @Column(name = "NFLOW_NAME")
    private String nflowName;

    @Column(name = "NFLOW_ID")
    private OpsManagerNflowId nflowId;

    @Column(name = "RUNNING_NFLOW_FLOWS")
    private Long runningNflowFlows;

    @Column(name = "TIME")
    private Long time;

    @Column(name = "LAST_ACTIVITY_TIMESTAMP")
    private Long lastActivityTimestamp;

    @Transient
    private boolean isStream;

    public JpaNifiNflowStats() {

    }

    public JpaNifiNflowStats(String nflowName) {
        this.nflowName = nflowName;
    }

    public JpaNifiNflowStats(String nflowName, OpsManagerNflowId nflowId) {
        this(nflowName);
        this.nflowId = nflowId;
    }

    @Override
    public String getNflowName() {
        return nflowName;
    }

    @Override
    public OpsManagerNflowId getNflowId() {
        return nflowId;
    }

    public void setNflowId(OpsManagerNflowId nflowId) {
        this.nflowId = nflowId;
    }

    @Override
    public Long getRunningNflowFlows() {
        if(runningNflowFlows == null){
            runningNflowFlows = 0L;
        }
        return runningNflowFlows;
    }

    public void setRunningNflowFlows(Long runningNflowFlows) {
        this.runningNflowFlows = runningNflowFlows;
    }

    public void addRunningNflowFlows(Long runningNflowFlows) {
        if (runningNflowFlows != null) {
            if (this.runningNflowFlows == null) {
                this.runningNflowFlows = 0L;
            }
            this.runningNflowFlows += runningNflowFlows;
        }
    }

    @Override
    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    public Long getLastActivityTimestamp() {
        return lastActivityTimestamp;
    }

    public void setLastActivityTimestamp(Long lastActivityTimestamp) {
        this.lastActivityTimestamp = lastActivityTimestamp;
    }

    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean stream) {
        isStream = stream;
    }

    @Embeddable
    public static class OpsManagerNflowId extends BaseJpaId implements Serializable, OpsManagerNflow.ID {

        private static final long serialVersionUID = 6017751710414995750L;

        @Column(name = "NFLOW_ID")
        private UUID uuid;

        public OpsManagerNflowId() {
        }


        public OpsManagerNflowId(Serializable ser) {
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
