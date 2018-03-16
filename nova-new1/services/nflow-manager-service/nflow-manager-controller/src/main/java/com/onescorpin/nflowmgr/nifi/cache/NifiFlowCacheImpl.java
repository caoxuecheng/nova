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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.onescorpin.app.ServicesApplicationStartup;
import com.onescorpin.app.ServicesApplicationStartupListener;
import com.onescorpin.nflowmgr.nifi.NifiConnectionListener;
import com.onescorpin.nflowmgr.nifi.NifiConnectionService;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.app.NovaVersionProvider;
import com.onescorpin.metadata.rest.model.nifi.NiFiFlowCacheConnectionData;
import com.onescorpin.metadata.rest.model.nifi.NiFiFlowCacheSync;
import com.onescorpin.metadata.rest.model.nifi.NifiFlowCacheSnapshot;
import com.onescorpin.nifi.nflowmgr.TemplateCreationHelper;
import com.onescorpin.nifi.provenance.NiFiProvenanceConstants;
import com.onescorpin.nifi.rest.NiFiObjectCache;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.nifi.rest.model.flow.NiFiFlowConnectionConverter;
import com.onescorpin.nifi.rest.model.flow.NifiFlowConnection;
import com.onescorpin.nifi.rest.model.flow.NifiFlowProcessGroup;
import com.onescorpin.nifi.rest.model.flow.NifiFlowProcessor;
import com.onescorpin.nifi.rest.support.NifiConnectionUtil;
import com.onescorpin.nifi.rest.support.NifiProcessUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.web.api.dto.ConnectionDTO;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Cache Connections and Processors in NiFi
 */
public class NifiFlowCacheImpl implements ServicesApplicationStartupListener, NifiConnectionListener, NiFiProvenanceConstants, NifiFlowCache {

    private static final Logger log = LoggerFactory.getLogger(NifiFlowCacheImpl.class);

    public static final String ITEM_LAST_MODIFIED_KEY = "NIFI_FLOW_CACHE";


    @Inject
    LegacyNifiRestClient nifiRestClient;

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private NifiConnectionService nifiConnectionService;

    @Inject
    private NovaVersionProvider novaVersionProvider;

    @Inject
    private NifiFlowCacheClusterManager nifiFlowCacheClusterManager;

    @Inject
    private NiFiObjectCache niFiObjectCache;

    @Inject
    ServicesApplicationStartup startup;

    @Value("${nifi.flow.inspector.threads:1}")
    private Integer nififlowInspectorThreads = 1;


    @Value("${nifi.flow.max.retries:100}")
    private Integer nifiFlowMaxRetries = 100;


    @Value("${nifi.flow.retry.wait.time.seconds:5}")
    private Integer nifiFlowWaitTime = 5;

    @Deprecated
    private Map<String, Map<String, List<NifiFlowProcessor>>> nflowProcessorIdProcessorMap = new ConcurrentHashMap<>();

    @Deprecated
    private Map<String, NifiFlowProcessor> processorIdMap = new ConcurrentHashMap<>();

    private Set<String> reuseableTemplateProcessorIds = new HashSet<>();

    private String reusableTemplateProcessGroupId = null;

    private NifiFlowCacheSnapshot latest;

    private List<NiFiFlowCacheListener> listeners = new ArrayList<>();


    public void subscribe(NiFiFlowCacheListener listener) {
        this.listeners.add(listener);
    }

    private AtomicLong reloadCount = new AtomicLong(0);

    /**
     * Flag to mark if the cache is loaded or not This is used to determine if the cache is ready to be used
     */
    private boolean loaded = false;

    /**
     * Flag to indicate we are connected to NiFi
     */
    private boolean nifiConnected = false;

    private AtomicBoolean rebuildWithRetryInProgress = new AtomicBoolean(false);


    private Map<String, String> processorIdToNflowProcessGroupId = new ConcurrentHashMap<>();

    private Map<String, String> processorIdToNflowNameMap = new ConcurrentHashMap<>();
    private Map<String, String> processorIdToProcessorName = new ConcurrentHashMap<>();
    private Map<String, NiFiFlowCacheConnectionData> connectionIdToConnectionMap = new ConcurrentHashMap<>();
    private Map<String, String> connectionIdCacheNameMap = new ConcurrentHashMap<>();

    /**
     * Set of the category.nflow names for those that are just streaming nflows
     */
    // private Set<String> streamingNflows = new HashSet();

    /**
     * Set of the category.nflow names
     */
    // private Set<String> allNflows = new HashSet<>();

    /**
     * Map of the sync id to cache
     * This is the cache of the items out there that others have built and will check/update themseleves based upon the base maps in the object
     */
    private Map<String, NiFiFlowCacheSync> syncMap = new ConcurrentHashMap<>();


    /**
     * Map with the sync Id and the last time that item was sync'd with the system
     * This is used to expire the stale non used caches
     */
    private Map<String, DateTime> lastSyncTimeMap = new ConcurrentHashMap<>();

    private DateTime lastUpdated = null;

    @PostConstruct
    private void init() {
        nifiConnectionService.subscribeConnectionListener(this);
        startup.subscribe(this);
        initExpireTimerThread();
        initializeLatestSnapshot();
    }


    @Override
    public void onStartup(DateTime startTime) {
        checkAndInitializeCache();
    }

    /**
     * NiFi has made a connection
     */
    @Override
    public void onNiFiConnected() {
        this.nifiConnected = true;
        checkAndInitializeCache();
    }

    @Override
    public void onNiFiDisconnected() {
        this.nifiConnected = false;
        //reset the flag to force cache initialization on nifi availability
        this.loaded = false;
        notifyCacheUnavailable();
    }

    public boolean isConnectedToNiFi() {
        return this.nifiConnected;
    }

    /**
     * When Nova is updated and nifi are connected and ready attempt to initialize the cache
     */
    private void checkAndInitializeCache() {
        boolean isLatest = metadataAccess.read(() -> {
            return novaVersionProvider.isUpToDate();
        }, MetadataAccess.SERVICE);

        if (!loaded && rebuildWithRetryInProgress.get() == false) {
            log.info("Check and Initialize NiFi Flow Cache. Nova up to date:{}, NiFi Connected:{}, Cache needs loading:{} ", isLatest, nifiConnected, !loaded);
            if (isLatest && nifiConnected && !loaded) {
                rebuildCacheWithRetry(nifiFlowMaxRetries, nifiFlowWaitTime);
            }
        }
    }

    /**
     * rebuild a given cache resetting the cache with the given sync id to the latest data in the cache
     *
     * @param syncId a cache id
     * @return the latest cache
     */
    public NiFiFlowCacheSync refreshAll(String syncId) {
        NiFiFlowCacheSync sync = getSync(syncId);
        if (!sync.isUnavailable()) {
            sync.reset();
            return syncAndReturnUpdates(sync, false);
        } else {
            return NiFiFlowCacheSync.UNAVAILABLE;
        }
    }

    /**
     * Check to see if the cache is loaded
     *
     * @return {@code true} if the cache is populated, {@code false} if the cache is not populated
     */
    @Override
    public boolean isAvailable() {
        return loaded;
    }


    /**
     * If nova is clustered it needs to do an additional check to ensure the flow cache is synchronized across all nova instances
     *
     * @return true if nova is clustered, false if not.
     */
    @Override
    public boolean isNovaClustered() {
        return nifiFlowCacheClusterManager.isClustered();
    }


    /**
     * Return only the records that were updated since the last sync
     *
     * @param syncId a cache id
     * @return updates that have been applied to the cache.
     */
    @Override
    public NiFiFlowCacheSync syncAndReturnUpdates(String syncId) {
        NiFiFlowCacheSync sync = getSync(syncId);
        if (!sync.isUnavailable()) {
            return syncAndReturnUpdates(sync);
        }
        return sync;
    }

    /**
     * Return the data in the cache for a given cache id
     *
     * @param syncId a cache id
     * @return the data in the cache for a given cache id
     */
    @Override
    public NiFiFlowCacheSync getCache(String syncId) {
        NiFiFlowCacheSync sync = getSync(syncId);
        return sync;
    }

    /**
     * Preview any new updates that will be applied to a given cache
     *
     * @param syncId a cache id
     * @return any new updates that will be applied to a given cache
     */
    @Override
    public NiFiFlowCacheSync previewUpdates(String syncId) {
        NiFiFlowCacheSync sync = getSync(syncId, true);
        if (!sync.isUnavailable()) {
            return previewUpdates(sync);
        }
        return sync;
    }

    /**
     * Rebuild the base cache that others will update from.
     */
    @Override
    public boolean rebuildAll() {
        if (rebuildWithRetryInProgress.get() == false) {
            return rebuildCacheWithRetry(1, 5);
        }
        return false;
    }


    private void rebuildAllCache() {
        log.info("Rebuilding the NiFi Flow Cache. Starting NiFi Flow Inspection with {} threads ...", nififlowInspectorThreads);
        boolean notify = reloadCount.get() == 0;
        loaded = false;

        DefaultNiFiFlowCompletionCallback completionCallback = new DefaultNiFiFlowCompletionCallback();
        NiFiFlowInspectorManager flowInspectorManager = new NiFiFlowInspectorManager.NiFiFlowInspectorManagerBuilder(nifiRestClient.getNiFiRestClient())
            .startingProcessGroupId("root")
            .completionCallback(completionCallback)
            .threads(nififlowInspectorThreads)
            .waitUntilComplete(true)
            .buildAndInspect();

        connectionIdCacheNameMap.putAll(completionCallback.getConnectionIdCacheNameMap());
        connectionIdToConnectionMap.putAll(completionCallback.getConnectionIdToConnectionMap());
        processorIdToNflowProcessGroupId.putAll(completionCallback.getProcessorIdToNflowProcessGroupId());
        processorIdToNflowNameMap.putAll(completionCallback.getProcessorIdToNflowNameMap());
        processorIdToProcessorName.putAll(completionCallback.getProcessorIdToProcessorName());
        reuseableTemplateProcessorIds.addAll(completionCallback.getReusableTemplateProcessorIds());
        reusableTemplateProcessGroupId = completionCallback.getReusableTemplateProcessGroupId();

        if (!flowInspectorManager.hasErrors()) {
            log.info("NiFi Flow Inspection took {} ms with {} threads for {} nflows, {} processors and {} connections ", flowInspectorManager.getTotalTime(), flowInspectorManager.getThreadCount(),
                     completionCallback.getNflowNames().size(), processorIdToProcessorName.size(), connectionIdCacheNameMap.size());
            if (completionCallback.getRootConnections() != null) {
                log.info("Adding {} Root Connections to the niFiObjectCache ", completionCallback.getRootConnections().size());
                niFiObjectCache.addProcessGroupConnections(completionCallback.getRootConnections());
            }
            if (completionCallback.getReusableTemplateProcessGroupId() != null) {
                niFiObjectCache.setReusableTemplateProcessGroupId(completionCallback.getReusableTemplateProcessGroupId());
            }
            lastUpdated = DateTime.now();
            loaded = true;
            reloadCount.incrementAndGet();
            log.info("Successfully built NiFi Flow Cache");
            if (notify) {
                notifyCacheAvailable();
            }
        } else {
            throw new NiFiFlowCacheException("Error inspecting and building the NiFi flow cache.");
        }

    }

    private void notifyCacheAvailable() {
        this.listeners.stream().forEach(listener -> {
            try {
                listener.onCacheAvailable();
            } catch (Exception e) {
                log.error("Error processing listener onCacheAvailable {}", e.getMessage(), e);
            }
        });

    }

    private void notifyCacheUnavailable() {
        this.listeners.stream().forEach(listener -> {
            try {
                listener.onCacheUnavailable();
            } catch (Exception e) {
                log.error("Error processing listener onCacheUnavailable {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Rebuilds the cache.
     * If an exception occurs during the rebuild it will attempt to retry to build it up to 10 times before aborting
     */
    public boolean rebuildCacheWithRetry(int retries, int waitTime) {
        boolean updated = false;
        if (rebuildWithRetryInProgress.compareAndSet(false, true)) {
            Exception lastError = null;

            for (int count = 1; count <= retries; ++count) {
                try {
                    log.info("Attempting to build the NiFiFlowCache");
                    rebuildAllCache();
                    if (loaded) {
                        log.info("Successfully built the NiFiFlowCache");
                        updated = true;
                        break;
                    }
                } catch (final Exception e) {
                    log.error("Error attempting to build cache.  The system will attempt to retry {} more times.  Next attempt to rebuild in {} seconds.  The error was: {}. ", (retries - count),
                              waitTime,
                              e.getMessage());
                    lastError = e;
                    Uninterruptibles.sleepUninterruptibly(waitTime, TimeUnit.SECONDS);
                }
            }
            if (!loaded) {
                log.error(
                    "Unable to build the NiFi Flow Cache!  You will need to manually rebuild the cache using the following url:  http://NOVA_HOST:PORT/proxy/v1/metadata/nifi-provenance/nifi-flow-cache/reset-cache ",
                    lastError);
            }
            rebuildWithRetryInProgress.set(false);
        }
        return updated;
    }


    private NiFiFlowCacheSync previewUpdates(NiFiFlowCacheSync sync) {
        return syncAndReturnUpdates(sync, true);
    }

    private NiFiFlowCacheSync syncAndReturnUpdates(NiFiFlowCacheSync sync) {
        return syncAndReturnUpdates(sync, false);
    }

    private NiFiFlowCacheSync getSync(String syncId) {
        return getSync(syncId, false);
    }

    private NiFiFlowCacheSync getSync(String syncId, boolean forPreview) {
        if (isAvailable()) {
            NiFiFlowCacheSync sync = null;
            if (syncId == null || !syncMap.containsKey(syncId)) {
                sync = new NiFiFlowCacheSync();
                if (StringUtils.isNotBlank(syncId)) {
                    sync.setSyncId(syncId);
                }
                if (!forPreview) {
                    syncMap.put(sync.getSyncId(), sync);
                }
            } else {
                sync = syncMap.get(syncId);
            }
            return sync;
        } else {
            return NiFiFlowCacheSync.UNAVAILABLE;
        }
    }

    public boolean needsUpdateFromCluster() {
        return isNovaClustered() && nifiFlowCacheClusterManager.needsUpdate();
    }

    /**
     * if Nova is clustered it needs to sync any updates from the other Nova instances before proceeding
     */
    public synchronized void applyClusterUpdates() {
        List<NifiFlowCacheClusterUpdateMessage> updates = nifiFlowCacheClusterManager.findUpdates();
        Set<String> templateUpdates = new HashSet<>();
        boolean needsUpdates = !updates.isEmpty();
        if (needsUpdates) {
            log.info("Nova Cluster Update: Detected changes.  About to apply {} updates ", updates.size());
        }
        updates.stream().forEach(update -> {
            switch (update.getType()) {
                case NFLOW:
                    NifiFlowCacheNflowUpdate nflowUpdate = nifiFlowCacheClusterManager.getNflowUpdate(update.getMessage());
                    log.info("Nova Cluster Update:  Applying Nflow Change update for {}", nflowUpdate.getNflowName());
                    updateFlow(nflowUpdate);
                    break;
                case NFLOW2:
                    NifiFlowCacheNflowUpdate2 nflowUpdate2 = nifiFlowCacheClusterManager.getNflowUpdate2(update.getMessage());
                    log.info("Nova Cluster Update:  Applying Nflow Change update for {}", nflowUpdate2.getNflowName());
                    updateFlow(nflowUpdate2);
                    break;
                case CONNECTION:
                    Collection<ConnectionDTO> connectionDTOS = nifiFlowCacheClusterManager.getConnectionsUpdate(update.getMessage());
                    log.info("Nova Cluster Update:  Applying Connection list update");
                    updateConnectionMap(connectionDTOS, false);
                    if (connectionDTOS != null) {
                        connectionDTOS.stream().forEach(c -> {
                            niFiObjectCache.addConnection(c.getParentGroupId(), c);
                        });
                    }
                    break;
                case PROCESSOR:
                    Collection<ProcessorDTO> processorDTOS = nifiFlowCacheClusterManager.getProcessorsUpdate(update.getMessage());
                    log.info("Nova Cluster Update:  Applying Processor list update");
                    updateProcessorIdNames(processorDTOS, false);
                    break;
                case TEMPLATE:
                    if (!templateUpdates.contains(update.getMessage())) {
                        RegisteredTemplate template = nifiFlowCacheClusterManager.getTemplate(update.getMessage());
                        log.info("Nova Cluster Update:  Applying Template update for {} ", template.getTemplateName());
                        updateRegisteredTemplate(template, false);
                        templateUpdates.add(update.getMessage());
                    }
                    break;
                default:
                    break;
            }
        });

        if (needsUpdates) {
            nifiFlowCacheClusterManager.appliedUpdates(updates);
            lastUpdated = DateTime.now();
            log.info("Nova Cluster Update: NiFi Flow File Cache is in sync. All {} updates have been applied to the cache. ", updates.size());
        }

    }

    public NifiFlowCacheSnapshot getLatest() {
     return latest;
    }

    private void initializeLatestSnapshot() {
        latest =
            new NifiFlowCacheSnapshot(processorIdToNflowNameMap, processorIdToNflowProcessGroupId, processorIdToProcessorName, null, null);
        latest.setConnectionIdToConnection(connectionIdToConnectionMap);
        latest.setConnectionIdToConnectionName(connectionIdCacheNameMap);
        latest.setReusableTemplateProcessorIds(reuseableTemplateProcessorIds);

    }


    private NiFiFlowCacheSync syncAndReturnUpdates(NiFiFlowCacheSync sync, boolean preview) {
        if (!preview) {
            lastSyncTimeMap.put(sync.getSyncId(), DateTime.now());
        }
        if (isNovaClustered()) {
            applyClusterUpdates();
        }

        if (sync.needsUpdate(lastUpdated)) {
            Map<String, String> processorIdToNflowNameMapCopy = ImmutableMap.copyOf(processorIdToNflowNameMap);
            Map<String, String> processorIdToNflowProcessGroupIdCopy = ImmutableMap.copyOf(processorIdToNflowProcessGroupId);
            Map<String, String> processorIdToProcessorNameCopy = ImmutableMap.copyOf(processorIdToProcessorName);
            Map<String, NiFiFlowCacheConnectionData> connectionDataMapCopy = ImmutableMap.copyOf(connectionIdToConnectionMap);

            //get nflows updated since last sync
            NifiFlowCacheSnapshot latest = new NifiFlowCacheSnapshot.Builder()
                .withProcessorIdToNflowNameMap(processorIdToNflowNameMapCopy)
                .withProcessorIdToNflowProcessGroupId(processorIdToNflowProcessGroupIdCopy)
                .withProcessorIdToProcessorName(processorIdToProcessorNameCopy)
                .withConnections(connectionDataMapCopy)
                .withSnapshotDate(lastUpdated).build();
            return syncAndReturnUpdates(sync, latest, preview);
        } else {
            return NiFiFlowCacheSync.EMPTY(sync.getSyncId());
        }
    }


    private NiFiFlowCacheSync syncAndReturnUpdates(NiFiFlowCacheSync sync, NifiFlowCacheSnapshot latest, boolean preview) {
        if (latest != null && sync.needsUpdate(latest.getSnapshotDate())) {

            NifiFlowCacheSnapshot updated = new NifiFlowCacheSnapshot.Builder()
                .withProcessorIdToNflowNameMap(sync.getProcessorIdToNflowNameMapUpdatedSinceLastSync(latest.getProcessorIdToNflowNameMap()))
                .withProcessorIdToNflowProcessGroupId(sync.getProcessorIdToProcessGroupIdUpdatedSinceLastSync(latest.getProcessorIdToNflowProcessGroupId()))
                .withProcessorIdToProcessorName(sync.getProcessorIdToProcessorNameUpdatedSinceLastSync(latest.getProcessorIdToProcessorName()))
                .withConnections(sync.getConnectionIdToConnectionUpdatedSinceLastSync(latest.getConnectionIdToConnectionName(), latest.getConnectionIdToConnection()))
                .withReusableTemplateProcessorIds(latest.getReusableTemplateProcessorIds())
                .build();
            //reset the pointers on this sync to be the latest
            if (!preview) {
                sync.setSnapshot(latest);
                sync.setLastSync(latest.getSnapshotDate());

            }
            NiFiFlowCacheSync updatedSync = new NiFiFlowCacheSync(sync.getSyncId(), updated);
            updatedSync.setUpdated(true);
            if (!preview) {
                updatedSync.setLastSync(latest.getSnapshotDate());
            }
            return updatedSync;

        }

        return NiFiFlowCacheSync.EMPTY(sync.getSyncId());
    }


    /**
     * clears the current cache
     ***/
    private void clearAll() {
        processorIdToNflowProcessGroupId.clear();
        processorIdToNflowProcessGroupId.clear();
        processorIdToProcessorName.clear();
        connectionIdToConnectionMap.clear();
        connectionIdCacheNameMap.clear();
        reuseableTemplateProcessorIds.clear();
    }


    /**
     * Called after someone updates/Registers a template in the UI using the template stepper
     * This is used to update the nflow marker for streaming/batch nflows
     */
    public synchronized void updateRegisteredTemplate(RegisteredTemplate template, boolean notifyClusterMembers) {

        if (notifyClusterMembers) {
            //mark the persistent table that this was updated
            if (nifiFlowCacheClusterManager.isClustered()) {
                nifiFlowCacheClusterManager.updateTemplate(template.getTemplateName());
            }
            lastUpdated = DateTime.now();
        }

    }

    /**
     * Update the cache of processorIds and connections when a reusable template is updated
     *
     * @param templateName    the name of the template
     * @param processGroupDTO the process group that stores the flow of the reusable template
     */
    public void updateCacheForReusableTemplate(String templateName, ProcessGroupDTO processGroupDTO) {
        Collection<ProcessorDTO> processors = NifiProcessUtil.getProcessors(processGroupDTO);
        updateProcessorIdNames(templateName, processors);
        Set<ConnectionDTO> connections = NifiConnectionUtil.getAllConnections(processGroupDTO);
        updateConnectionMap(templateName, connections);
        processGroupDTO.getContents().getProcessors().stream().forEach(processorDTO -> reuseableTemplateProcessorIds.add(processorDTO.getId()));
        lastUpdated = DateTime.now();
    }


    /**
     * add processors to the cache
     *
     * @param templateName a template name
     * @param processors   processors to add to the cache
     */
    public void updateProcessorIdNames(String templateName, Collection<ProcessorDTO> processors) {
        updateProcessorIdNames(processors, true);
    }

    private void updateProcessorIdNames(Collection<ProcessorDTO> processors, boolean notifyClusterMembers) {

        Map<String, String> processorIdToProcessorName = new HashMap<>();
        processors.stream().forEach(flowProcessor -> {
            processorIdToProcessorName.put(flowProcessor.getId(), flowProcessor.getName());
        });

        this.processorIdToProcessorName.putAll(processorIdToProcessorName);

        if (notifyClusterMembers) {
            if (nifiFlowCacheClusterManager.isClustered()) {
                nifiFlowCacheClusterManager.updateProcessors(processors);
            }
            lastUpdated = DateTime.now();
        }
    }

    /**
     * Add connections to the cache
     *
     * @param templateName a template name
     * @param connections  connections to add to the cache
     */
    public void updateConnectionMap(String templateName, Collection<ConnectionDTO> connections) {
        updateConnectionMap(connections, true);
    }

    private void updateConnectionMap(Collection<ConnectionDTO> connections, boolean notifyClusterMembers) {
        Map<String, NifiFlowConnection> connectionIdToConnectionMap = new HashMap<>();
        if (connections != null) {
            connections.stream().forEach(connectionDTO -> {
                NifiFlowConnection nifiFlowConnection = NiFiFlowConnectionConverter.toNiFiFlowConnection(connectionDTO);
                if (nifiFlowConnection != null) {
                    connectionIdToConnectionMap.put(nifiFlowConnection.getConnectionIdentifier(), nifiFlowConnection);
                }

            });
        }
        this.connectionIdToConnectionMap.putAll(toConnectionIdMap(connectionIdToConnectionMap.values()));

        if (connections != null) {
            Map<String, String> connectionIdToNameMap = connections.stream().collect(Collectors.toMap(conn -> conn.getId(), conn -> conn.getName()));
            connectionIdCacheNameMap.putAll(connectionIdToNameMap);
        }

        if (notifyClusterMembers) {
            if (nifiFlowCacheClusterManager.isClustered()) {
                nifiFlowCacheClusterManager.updateConnections(connections);
            }
            lastUpdated = DateTime.now();
        }
    }


    /**
     * Update cache for a nflows flow
     * Used by CreateNflow builder
     *
     * @param nflow             a nflow
     * @param nflowProcessGroup the process group created with this nflow
     */
    public void updateFlow(NflowMetadata nflow, NifiFlowProcessGroup nflowProcessGroup) {
        String nflowName = nflow.getCategoryAndNflowName();
        this.updateFlow(nflowName, nflow.getRegisteredTemplate().isStream(), nflowProcessGroup.getId(), nflowProcessGroup.getProcessorMap().values(), nflowProcessGroup.getConnectionIdMap().values(), true);
    }

    public void updateFlowForNflow(NflowMetadata nflow, String nflowProcessGroupId, Collection<ProcessorDTO> processorDTOs, Collection<ConnectionDTO> connectionDTOs) {
        String nflowName = nflow.getCategoryAndNflowName();
        this.updateFlowForNflow(nflowName, nflow.getRegisteredTemplate().isStream(), nflowProcessGroupId, processorDTOs, connectionDTOs, true);
    }

    /**
     * Update  cache for a nflow
     *
     * @param nflowName         the name of the nflow
     * @param isStream         {@code true} if its a streaming nflow, {@code false} if its a batch nflow
     * @param nflowProcessGroup the process group created with this nflow
     */
    public void updateFlow(String nflowName, boolean isStream, NifiFlowProcessGroup nflowProcessGroup) {
        //  nflowProcessGroup.calculateCriticalPathProcessors();
        this.updateFlow(nflowName, isStream, nflowProcessGroup.getId(), nflowProcessGroup.getProcessorMap().values(), nflowProcessGroup.getConnectionIdMap().values(), true);
    }

    /**
     * update for clustered nova
     */
    public void updateFlow(NifiFlowCacheNflowUpdate2 flowCacheNflowUpdate) {
        updateFlowForNflow(flowCacheNflowUpdate.getNflowName(), flowCacheNflowUpdate.isStream(), flowCacheNflowUpdate.getNflowProcessGroupId(), flowCacheNflowUpdate.getProcessors(),
                          flowCacheNflowUpdate.getConnections(), false);
    }


    /**
     * update for clustered nova
     */
    public void updateFlow(NifiFlowCacheNflowUpdate flowCacheNflowUpdate) {
        updateFlow(flowCacheNflowUpdate.getNflowName(), flowCacheNflowUpdate.isStream(), flowCacheNflowUpdate.getNflowProcessGroupId(), flowCacheNflowUpdate.getProcessors(),
                   flowCacheNflowUpdate.getConnections(), false);
    }

    private void updateFlowForNflow(String nflowName, boolean isStream, String nflowProcessGroupId, Collection<ProcessorDTO> processors, Collection<ConnectionDTO> connections,
                                   boolean notifyClusterMembers) {
        Map<String, String> processorIdToProcessorName = processors.stream().collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        Map<String, String> processorIdToNflowProcessGroupId = processors.stream().collect(Collectors.toMap(p -> p.getId(), p -> nflowProcessGroupId));
        Map<String, String> processorIdToNflowName = processors.stream().collect(Collectors.toMap(p -> p.getId(), p -> nflowName));
        this.processorIdToNflowProcessGroupId.putAll(processorIdToNflowProcessGroupId);
        this.processorIdToProcessorName.putAll(processorIdToProcessorName);
        processorIdToNflowNameMap.putAll(processorIdToNflowName);

        updateConnectionMap(connections, false);

        //notify others of the cache update only if we are not doing a full refresh
        if (loaded && notifyClusterMembers) {
            if (nifiFlowCacheClusterManager.isClustered()) {
                nifiFlowCacheClusterManager.updateNflow2(nflowName, isStream, nflowProcessGroupId, processors, connections);
            }
            lastUpdated = DateTime.now();
        }


    }

    /**
     * updateFlowForNflow is now being used
     */
    @Deprecated
    private void updateFlow(String nflowName, boolean isStream, String nflowProcessGroupId, Collection<NifiFlowProcessor> processors, Collection<NifiFlowConnection> connections,
                            boolean notifyClusterMembers) {

        nflowProcessorIdProcessorMap.put(nflowName, toProcessorIdProcessorMap(processors));

        updateProcessorIdMaps(nflowProcessGroupId, processors);

        connectionIdToConnectionMap.putAll(toConnectionIdMap(connections));

        if (connections != null) {
            Map<String, String> connectionIdToNameMap = connections.stream().collect(Collectors.toMap(conn -> conn.getConnectionIdentifier(), conn -> conn.getName()));
            connectionIdCacheNameMap.putAll(connectionIdToNameMap);
        }

        processorIdMap.putAll(toProcessorIdMap(processors));
        processorIdToNflowNameMap.putAll(toProcessorIdNflowNameMap(processors, nflowName));

        //notify others of the cache update only if we are not doing a full refresh
        if (loaded && notifyClusterMembers) {
            if (nifiFlowCacheClusterManager.isClustered()) {
                nifiFlowCacheClusterManager.updateNflow(nflowName, isStream, nflowProcessGroupId, processors, connections);
            }
            lastUpdated = DateTime.now();
        }


    }

    private void updateProcessorIdMaps(String processGroupId, Collection<NifiFlowProcessor> processors) {
        Map<String, String> processorIdToProcessGroupId = new HashMap<>();
        Map<String, String> processorIdToProcessorName = new HashMap<>();
        processors.stream().forEach(flowProcessor -> {
            processorIdToProcessGroupId.put(flowProcessor.getId(), processGroupId);
            processorIdToProcessorName.put(flowProcessor.getId(), flowProcessor.getName());

            if (flowProcessor.getProcessGroup() != null && flowProcessor.getProcessGroup().getParentGroupName() != null && TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME
                .equalsIgnoreCase(flowProcessor.getProcessGroup().getParentGroupName())) {
                reuseableTemplateProcessorIds.add(flowProcessor.getId());
                if (reusableTemplateProcessGroupId == null) {
                    reusableTemplateProcessGroupId = flowProcessor.getProcessGroup().getId();
                }
            }
        });
        this.processorIdToNflowProcessGroupId.putAll(processorIdToProcessGroupId);
        this.processorIdToProcessorName.putAll(processorIdToProcessorName);

    }

    private Map<String, NiFiFlowCacheConnectionData> toConnectionIdMap(Collection<NifiFlowConnection> connections) {
        Map<String, NiFiFlowCacheConnectionData> connectionMap = new HashMap<>();
        connections.stream().forEach(conn -> {
            connectionMap
                .put(conn.getConnectionIdentifier(), new NiFiFlowCacheConnectionData(conn.getConnectionIdentifier(), conn.getName(), conn.getSourceIdentifier(), conn.getDestinationIdentifier()));
        });
        return connectionMap;
    }

    private Map<String, NiFiFlowCacheConnectionData> connectionDTOtoConnectionIdMap(Collection<ConnectionDTO> connections) {
        Map<String, NiFiFlowCacheConnectionData> connectionMap = new HashMap<>();
        connections.stream().forEach(conn -> {
            connectionMap
                .put(conn.getId(), new NiFiFlowCacheConnectionData(conn.getId(), conn.getName(), conn.getSource() != null ? conn.getSource().getId() : null,
                                                                   conn.getDestination() != null ? conn.getDestination().getId() : null));
        });
        return connectionMap;
    }

    private Map<String, NifiFlowProcessor> toProcessorIdMap(Collection<NifiFlowProcessor> processors) {
        return processors.stream().collect(Collectors.toMap(NifiFlowProcessor::getId, Function.identity()));
    }

    private Map<String, String> toProcessorIdNflowNameMap(Collection<NifiFlowProcessor> processors, String nflowName) {
        return processors.stream().collect(Collectors.toMap(NifiFlowProcessor::getId, name -> nflowName));
    }


    private Map<String, List<NifiFlowProcessor>> toFlowIdProcessorMap(Collection<NifiFlowProcessor> processors) {
        if (processors != null && !processors.isEmpty()) {
            return processors.stream().filter(nifiFlowProcessor -> nifiFlowProcessor.getFlowId() != null).collect(Collectors.groupingBy(NifiFlowProcessor::getFlowId));
        }
        return Collections.emptyMap();
    }


    private Map<String, List<NifiFlowProcessor>> toProcessorIdProcessorMap(Collection<NifiFlowProcessor> processors) {
        if (processors != null && !processors.isEmpty()) {
            return processors.stream().collect(Collectors.groupingBy(NifiFlowProcessor::getId));
        }
        return new HashMap<>();
    }

    public CacheSummary cacheSummary() {
        return CacheSummary.build(syncMap);
    }

    private void initExpireTimerThread() {
        long timer = 30; // run ever 30 sec to check and expire
        ScheduledExecutorService service = Executors
            .newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(() -> {
            checkAndExpireUnusedCache();
        }, timer, timer, TimeUnit.SECONDS);

    }

    /**
     * Expire any cache entries that havent been touched in 60 minutes
     */
    public void checkAndExpireUnusedCache() {
        int minutes = 60;
        try {

            long expireAfter = minutes * 1000 * 60; //60 min
            Set<String> itemsRemoved = new HashSet<>();
            //find cache items that havent been synced in allotted time
            lastSyncTimeMap.entrySet().stream().filter(entry -> ((DateTime.now().getMillis() - entry.getValue().getMillis()) > expireAfter)).forEach(entry -> {
                syncMap.remove(entry.getKey());
                itemsRemoved.add(entry.getKey());
                log.info("Expiring Cache {}.  This cache has not been used in over {} minutes", entry.getKey(), minutes);
            });
            itemsRemoved.stream().forEach(item -> lastSyncTimeMap.remove(item));

        } catch (Exception e) {
            log.error("Error attempting to invalidate flow cache for items not touched in {} or more minutes", minutes, e);
        }
    }

    public void addConnectionToCache(ConnectionDTO connectionDTO) {
        Collection<ConnectionDTO> connectionList = Lists.newArrayList(connectionDTO);
        updateConnectionMap(connectionList, true);
    }


}
