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

import com.fasterxml.jackson.core.type.TypeReference;
import com.onescorpin.cluster.ClusterService;
import com.onescorpin.cluster.ClusterServiceListener;
import com.onescorpin.cluster.NiFiFlowCacheUpdateType;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.service.MetadataService;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.cluster.NiFiFlowCacheClusterUpdateItem;
import com.onescorpin.metadata.jpa.cluster.NiFiFlowCacheClusterUpdateProvider;
import com.onescorpin.nifi.rest.model.flow.NifiFlowConnection;
import com.onescorpin.nifi.rest.model.flow.NifiFlowProcessGroup;
import com.onescorpin.nifi.rest.model.flow.NifiFlowProcessor;

import org.apache.nifi.web.api.dto.ConnectionDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Manage the Nifi flow cache when Nova is clustered
 */
public class NifiFlowCacheClusterManager implements ClusterServiceListener {

    private static final String LAST_MODIFIED_KEY_PREFIX = "NIFI_FLOW_CACHE";

    private static final Logger log = LoggerFactory.getLogger(NifiFlowCacheClusterManager.class);


    @Inject
    MetadataAccess metadataAccess;

    @Inject
    ClusterService clusterService;

    @Inject
    MetadataService metadataService;


    @Inject
    NiFiFlowCacheClusterUpdateProvider niFiFlowCacheProvider;

    @PostConstruct
    public void init() {
        clusterService.subscribe(this);
    }

    public NifiFlowCacheClusterUpdateMessage updateTemplate(String templateName) {
        NifiFlowCacheClusterUpdateMessage updateMessage = new NifiFlowCacheClusterUpdateMessage(NiFiFlowCacheUpdateType.TEMPLATE, templateName);
        updatedCache(updateMessage);
        return updateMessage;
    }


    public NifiFlowCacheClusterUpdateMessage updateConnections(Collection<ConnectionDTO> connections) {
        String json = ObjectMapperSerializer.serialize(connections);
        NifiFlowCacheClusterUpdateMessage updateMessage = new NifiFlowCacheClusterUpdateMessage(NiFiFlowCacheUpdateType.CONNECTION, json);
        updatedCache(updateMessage);
        return updateMessage;
    }

    public NifiFlowCacheClusterUpdateMessage updateProcessors(Collection<ProcessorDTO> processors) {
        //strip for serialization ... create new NifiFlowCacheSimpleProcessorDTO
        List<NifiFlowCacheSimpleProcessorDTO> processorsToCache = processors.stream()
            .map(p -> new NifiFlowCacheSimpleProcessorDTO(p.getId(), p.getName(), p.getType(), p.getParentGroupId()))
            .collect(Collectors.toList());
        String json = ObjectMapperSerializer.serialize(processorsToCache);
        NifiFlowCacheClusterUpdateMessage updateMessage = new NifiFlowCacheClusterUpdateMessage(NiFiFlowCacheUpdateType.PROCESSOR, json);
        updatedCache(updateMessage);
        return updateMessage;
    }

    public NifiFlowCacheClusterUpdateMessage updateNflow(String nflowName, boolean isStream, NifiFlowProcessGroup nflowFlow) {

        NifiFlowCacheNflowUpdate nflowUpdate = new NifiFlowCacheNflowUpdate(nflowName, isStream, nflowFlow.getId(), nflowFlow.getProcessorMap().values(), nflowFlow.getConnectionIdMap().values());
        String json = ObjectMapperSerializer.serialize(nflowUpdate);
        NifiFlowCacheClusterUpdateMessage updateMessage = new NifiFlowCacheClusterUpdateMessage(NiFiFlowCacheUpdateType.NFLOW, json);
        updatedCache(updateMessage);
        return updateMessage;
    }

    public NifiFlowCacheClusterUpdateMessage updateNflow(String nflowName, boolean isStream, String nflowProcessGroupId, Collection<NifiFlowProcessor> processors,
                                                        Collection<NifiFlowConnection> connections) {
        NifiFlowCacheSimpleNflowUpdate nflowUpdate = new NifiFlowCacheSimpleNflowUpdate(nflowName, isStream, nflowProcessGroupId, transformNifiFlowProcesors(processors), connections);
        String json = ObjectMapperSerializer.serialize(nflowUpdate);
        NifiFlowCacheClusterUpdateMessage updateMessage = new NifiFlowCacheClusterUpdateMessage(NiFiFlowCacheUpdateType.NFLOW, json);
        updatedCache(updateMessage);
        return updateMessage;
    }

    /**
     * This replaces the updateNflow() callback
     * starting with 0.8.3.1 cluster manager will callback using this method
     */
    public NifiFlowCacheClusterUpdateMessage updateNflow2(String nflowName, boolean isStream, String nflowProcessGroupId, Collection<ProcessorDTO> processors, Collection<ConnectionDTO> connections) {
        NifiFlowCacheSimpleNflowUpdate nflowUpdate = new NifiFlowCacheSimpleNflowUpdate(nflowName, isStream, nflowProcessGroupId, transformProcessors(processors), transformConnections(connections));
        String json = ObjectMapperSerializer.serialize(nflowUpdate);
        NifiFlowCacheClusterUpdateMessage updateMessage = new NifiFlowCacheClusterUpdateMessage(NiFiFlowCacheUpdateType.NFLOW2, json);
        updatedCache(updateMessage);
        return updateMessage;
    }


    public NifiFlowCacheNflowUpdate getNflowUpdate(String json) {
        NifiFlowCacheNflowUpdate update = ObjectMapperSerializer.deserialize(json, NifiFlowCacheNflowUpdate.class);
        return update;
    }

    public NifiFlowCacheNflowUpdate2 getNflowUpdate2(String json) {
        NifiFlowCacheNflowUpdate2 update = ObjectMapperSerializer.deserialize(json, NifiFlowCacheNflowUpdate2.class);
        return update;
    }


    public Collection<ProcessorDTO> getProcessorsUpdate(String json) {
        Set<ProcessorDTO> processors = ObjectMapperSerializer.deserialize(json, new TypeReference<Set<ProcessorDTO>>() {
        });
        return processors;
    }

    public Collection<ConnectionDTO> getConnectionsUpdate(String json) {
        Set<ConnectionDTO> connections = ObjectMapperSerializer.deserialize(json, new TypeReference<Set<ConnectionDTO>>() {
        });
        return connections;
    }

    public RegisteredTemplate getTemplate(String templateName) {
        return metadataService.findRegisteredTemplateByName(templateName);
    }

    public boolean isClustered() {
        return clusterService.isClustered();
    }

    private void updatedCache(NifiFlowCacheClusterUpdateMessage update) {
        metadataAccess.commit(() -> {
            niFiFlowCacheProvider.updatedCache(update.getType(), update.getMessage());
        }, MetadataAccess.SERVICE);
        //send it off to notify others its been updated?
    }


    public boolean needsUpdate() {
        return metadataAccess.commit(() -> {
            return niFiFlowCacheProvider.needsUpdate();
        }, MetadataAccess.SERVICE);
    }

    public List<NifiFlowCacheClusterUpdateMessage> findUpdates() {
        return metadataAccess.commit(() -> {
            List<NiFiFlowCacheClusterUpdateItem> updates = niFiFlowCacheProvider.findUpdates();
            return transformUpdates(updates);
        }, MetadataAccess.SERVICE);
    }


    public void appliedUpdates(List<NifiFlowCacheClusterUpdateMessage> updateMessages) {
        metadataAccess.commit(() -> {
            List<String> updateKeys = updateMessages.stream().map(m -> m.getUpdateKey()).collect(Collectors.toList());
            niFiFlowCacheProvider.appliedUpdates(updateKeys);
        }, MetadataAccess.SERVICE);
    }

    private List<NifiFlowCacheClusterUpdateMessage> transformUpdates(List<NiFiFlowCacheClusterUpdateItem> updates) {
        if (updates != null) {
            return updates.stream().map(update -> new NifiFlowCacheClusterUpdateMessage(update.getUpdateType(), update.getUpdateValue(), update.getUpdateKey())).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<NifiFlowCacheClusterNifiFlowProcessor> transformNifiFlowProcesors(Collection<NifiFlowProcessor> processors) {
        if (processors != null) {
            return processors.stream().map(p -> new NifiFlowCacheClusterNifiFlowProcessor(p.getId(), p.getName(), p.getType(), p.getFlowId())).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }


    private Collection<NifiFlowCacheClusterNifiFlowProcessor> transformProcessors(Collection<ProcessorDTO> processors) {
        if (processors != null) {
            return processors.stream().map(p -> new NifiFlowCacheClusterNifiFlowProcessor(p.getId(), p.getName(), p.getType(), null)).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<NifiFlowConnection> transformConnections(Collection<ConnectionDTO> connections) {
        if (connections != null) {
            return connections.stream()
                .map(c -> new NifiFlowConnection(c.getId(), c.getName(), c.getSource() != null ? c.getSource().getId() : null, c.getDestination() != null ? c.getDestination().getId() : null))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }


    @Override
    public void onClusterMembershipChanged(List<String> previousMembers, List<String> currentMembers) {

    }

    @Override
    public void onConnected(List<String> currentMembers) {
        log.info("Nova Cluster Node connected {} members exist.  {} ", currentMembers.size(), currentMembers);
        //on connected reset the previous db entries
        if (currentMembers.size() == 1) {
            try {
                metadataAccess.commit(() -> {
                    log.info("This is the First Member connecting to the cluster.  Resetting the previous Cluster cache updates  {} members exist.  {} ", currentMembers.size(), currentMembers);
                    niFiFlowCacheProvider.resetClusterSyncUpdates();
                }, MetadataAccess.SERVICE);
            } catch (Exception e) {
                //log the error and carry on
                log.error("Error attempting to reset the NiFi Flow Cache in the database when starting the Nova Cluster. {} ", e.getMessage(), e);
            }
        }

    }

    @Override
    public void onDisconnected(List<String> currentMembers) {

    }

    @Override
    public void onClosed(List<String> currentMembers) {

    }
}
