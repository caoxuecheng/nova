package com.onescorpin.metadata.rest.model.nifi;

/*-
 * #%L
 * onescorpin-metadata-rest-model
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 */
public class NifiFlowCacheSnapshot {

    public static final NifiFlowCacheSnapshot EMPTY = new Builder()
        .withProcessorIdToNflowNameMap(ImmutableMap.of())
        .withProcessorIdToNflowProcessGroupId(ImmutableMap.of())
        .withProcessorIdToProcessorName(ImmutableMap.of())
        .withStreamingNflows(ImmutableSet.<String>of())
        .withNflows(ImmutableSet.<String>of())
        .build();
    private DateTime snapshotDate;
    //items to add
    private Map<String, String> processorIdToNflowNameMap = new ConcurrentHashMap<>();
    private Map<String, String> processorIdToNflowProcessGroupId = new ConcurrentHashMap<>();
    private Map<String, String> processorIdToProcessorName = new ConcurrentHashMap<>();
    private Map<String, String> connectionIdToConnectionName = new ConcurrentHashMap<>();
    private Map<String, NiFiFlowCacheConnectionData> connectionIdToConnection = new ConcurrentHashMap<>();
    private Set<String> allStreamingNflows = new HashSet<>();
    private Set<String> reusableTemplateProcessorIds = new HashSet<>();
    /**
     * Set of the category.nflow names
     */
    private Set<String> allNflows = new HashSet<>();

    public NifiFlowCacheSnapshot() {

    }

    public NifiFlowCacheSnapshot(Map<String, String> processorIdToNflowNameMap,
                                 Map<String, String> processorIdToNflowProcessGroupId, Map<String, String> processorIdToProcessorName, Set<String> allStreamingNflows, Set<String> allNflows) {
        this.processorIdToNflowNameMap = processorIdToNflowNameMap;
        this.processorIdToNflowProcessGroupId = processorIdToNflowProcessGroupId;
        this.processorIdToProcessorName = processorIdToProcessorName;
        this.allStreamingNflows = allStreamingNflows;
        this.allNflows = allNflows;
    }

    public Map<String, String> getProcessorIdToNflowNameMap() {
        if (processorIdToNflowNameMap == null) {
            this.processorIdToNflowNameMap = new HashMap<>();
        }
        return processorIdToNflowNameMap;
    }

    public void setProcessorIdToNflowNameMap(Map<String, String> processorIdToNflowNameMap) {
        this.processorIdToNflowNameMap = processorIdToNflowNameMap;
    }

    public Map<String, String> getProcessorIdToNflowProcessGroupId() {
        if (processorIdToNflowProcessGroupId == null) {
            this.processorIdToNflowProcessGroupId = new HashMap<>();
        }
        return processorIdToNflowProcessGroupId;
    }

    public void setProcessorIdToNflowProcessGroupId(Map<String, String> processorIdToNflowProcessGroupId) {
        this.processorIdToNflowProcessGroupId = processorIdToNflowProcessGroupId;
    }

    public Map<String, String> getProcessorIdToProcessorName() {
        if (processorIdToProcessorName == null) {
            this.processorIdToProcessorName = new HashMap<>();
        }
        return processorIdToProcessorName;
    }

    public void setProcessorIdToProcessorName(Map<String, String> processorIdToProcessorName) {
        this.processorIdToProcessorName = processorIdToProcessorName;
    }

    public Set<String> getAllStreamingNflows() {
        if (allStreamingNflows == null) {
            return new HashSet<>();
        }
        return allStreamingNflows;
    }

    public void setAllStreamingNflows(Set<String> allStreamingNflows) {
        this.allStreamingNflows = allStreamingNflows;
    }

    public DateTime getSnapshotDate() {
        return snapshotDate;
    }

    public void setSnapshotDate(DateTime snapshotDate) {
        this.snapshotDate = snapshotDate;
    }

    public Set<String> getAllNflows() {
        return allNflows;
    }

    public void setAllNflows(Set<String> allNflows) {
        this.allNflows = allNflows;
    }

    public Map<String, String> getConnectionIdToConnectionName() {
        return connectionIdToConnectionName;
    }

    public void setConnectionIdToConnectionName(Map<String, String> connectionIdToConnectionName) {
        this.connectionIdToConnectionName = connectionIdToConnectionName;
    }

    public Map<String, NiFiFlowCacheConnectionData> getConnectionIdToConnection() {
        return connectionIdToConnection;
    }

    public void setConnectionIdToConnection(Map<String, NiFiFlowCacheConnectionData> connectionIdToConnection) {
        this.connectionIdToConnection = connectionIdToConnection;
    }

    public Set<String> getReusableTemplateProcessorIds() {
        return reusableTemplateProcessorIds;
    }

    public void setReusableTemplateProcessorIds(Set<String> reusableTemplateProcessorIds) {
        this.reusableTemplateProcessorIds = reusableTemplateProcessorIds;
    }

    public void update(NifiFlowCacheSnapshot syncSnapshot) {
        processorIdToNflowNameMap.putAll(syncSnapshot.getProcessorIdToNflowNameMap());
        processorIdToNflowProcessGroupId.putAll(syncSnapshot.getProcessorIdToNflowProcessGroupId());
        processorIdToProcessorName.putAll(syncSnapshot.getProcessorIdToProcessorName());
        allStreamingNflows = new HashSet<>(syncSnapshot.getAllStreamingNflows());
        allNflows.addAll(syncSnapshot.getAllNflows());
        snapshotDate = syncSnapshot.getSnapshotDate();
        connectionIdToConnection.putAll(syncSnapshot.getConnectionIdToConnection());
        connectionIdToConnectionName.putAll(syncSnapshot.getConnectionIdToConnectionName());
        reusableTemplateProcessorIds.addAll(syncSnapshot.getReusableTemplateProcessorIds());
    }

    public static class Builder {

        //items to add
        private Map<String, String> processorIdToNflowNameMap;

        private Map<String, String> processorIdToNflowProcessGroupId;

        private Map<String, String> processorIdToProcessorName;

        private Set<String> streamingNflows;

        private Map<String, NiFiFlowCacheConnectionData> connectionIdToConnection;

        /**
         * Set of the category.nflow names
         */
        private Set<String> allNflows = new HashSet<>();

        private Set<String> reusableTemplateProcessorIds = new HashSet<>();
        private DateTime snapshotDate;


        public Builder withConnections(Map<String, NiFiFlowCacheConnectionData> connections) {
            this.connectionIdToConnection = connections;
            return this;
        }

        public Builder withProcessorIdToNflowNameMap(Map<String, String> addProcessorIdToNflowNameMap) {
            this.processorIdToNflowNameMap = addProcessorIdToNflowNameMap;
            return this;
        }


        public Builder withProcessorIdToNflowProcessGroupId(Map<String, String> addProcessorIdToNflowProcessGroupId) {
            this.processorIdToNflowProcessGroupId = addProcessorIdToNflowProcessGroupId;
            return this;
        }

        public Builder withProcessorIdToProcessorName(Map<String, String> addProcessorIdToProcessorName) {
            this.processorIdToProcessorName = addProcessorIdToProcessorName;
            return this;
        }


        public Builder withStreamingNflows(Set<String> addStreamingNflows) {
            this.streamingNflows = addStreamingNflows;
            return this;
        }

        public Builder withNflows(Set<String> nflows) {
            this.allNflows = nflows;
            return this;
        }

        public Builder withReusableTemplateProcessorIds(Set<String> processorIds) {
            this.reusableTemplateProcessorIds = processorIds;
            return this;
        }

        public Builder withSnapshotDate(DateTime snapshotDate) {
            this.snapshotDate = snapshotDate;
            return this;
        }


        public NifiFlowCacheSnapshot build() {
            NifiFlowCacheSnapshot
                snapshot =
                new NifiFlowCacheSnapshot(processorIdToNflowNameMap, processorIdToNflowProcessGroupId, processorIdToProcessorName, streamingNflows, allNflows);
            snapshot.setSnapshotDate(this.snapshotDate);
            snapshot.setConnectionIdToConnection(connectionIdToConnection);
            if (connectionIdToConnection != null) {
                Map<String, String> connectionIdNameMap = connectionIdToConnection.values().stream().collect(Collectors.toMap(conn -> conn.getConnectionIdentifier(), conn -> conn.getName()));
                snapshot.setConnectionIdToConnectionName(connectionIdNameMap);
            }
            return snapshot;
        }


    }


}
