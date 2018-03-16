package com.onescorpin.nflowmgr.nifi.cache;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.onescorpin.nifi.rest.model.flow.NifiFlowConnection;

import java.util.Collection;

/**
 * A simple object used for serialization to for the Nova cluster sync
 */
public class NifiFlowCacheSimpleNflowUpdate {

    private String nflowName;
    private boolean isStream;
    private String nflowProcessGroupId;
    private Collection<NifiFlowCacheClusterNifiFlowProcessor> processors;
    private Collection<NifiFlowConnection> connections;


    public NifiFlowCacheSimpleNflowUpdate(){

    }

    public NifiFlowCacheSimpleNflowUpdate(String nflowName, boolean isStream, String nflowProcessGroupId, Collection<NifiFlowCacheClusterNifiFlowProcessor> processors,
                                         Collection<NifiFlowConnection> connections) {
        this.nflowName = nflowName;
        this.isStream = isStream;
        this.nflowProcessGroupId = nflowProcessGroupId;
        this.processors = processors;


        this.connections = connections;
    }

    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    public boolean isStream() {
        return isStream;
    }

    public void setStream(boolean stream) {
        isStream = stream;
    }

    public String getNflowProcessGroupId() {
        return nflowProcessGroupId;
    }

    public void setNflowProcessGroupId(String nflowProcessGroupId) {
        this.nflowProcessGroupId = nflowProcessGroupId;
    }

    public Collection<NifiFlowCacheClusterNifiFlowProcessor> getProcessors() {
        return processors;
    }

    public void setProcessors(Collection<NifiFlowCacheClusterNifiFlowProcessor> processors) {
        this.processors = processors;
    }

    public Collection<NifiFlowConnection> getConnections() {
        return connections;
    }

    public void setConnections(Collection<NifiFlowConnection> connections) {
        this.connections = connections;
    }
}
