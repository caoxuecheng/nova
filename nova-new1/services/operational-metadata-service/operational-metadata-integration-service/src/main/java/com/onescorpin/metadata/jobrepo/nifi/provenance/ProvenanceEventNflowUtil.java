package com.onescorpin.metadata.jobrepo.nifi.provenance;

/*-
 * #%L
 * onescorpin-operational-metadata-integration-service
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

import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCache;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.jpa.jobrepo.nifi.NifiEventProvider;
import com.onescorpin.metadata.rest.model.nifi.NiFiFlowCacheConnectionData;
import com.onescorpin.metadata.rest.model.nifi.NifiFlowCacheSnapshot;
import com.onescorpin.nifi.provenance.NovaProcessorFlowType;
import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTO;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 *
 */
public class ProvenanceEventNflowUtil {

    private static final Logger log = LoggerFactory.getLogger(ProvenanceEventNflowUtil.class);

    @Inject
    private NifiFlowCache nifiFlowCache;

    @Inject
    NifiEventProvider nifiEventProvider;

    @Inject
    MetadataAccess metadataAccess;

    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;


    @PostConstruct
    private void init() {

    }


    public ProvenanceEventNflowUtil() {

    }

    /**
     * Ensure the event has all the necessary information needed to be processed from the NiFi Flow Cache
     *
     * @param event the provenance event
     * @return true if the data exists in the cache, false if not
     */
    public boolean validateNiFiNflowInformation(ProvenanceEventRecordDTO event) {
        String nflowName = getNflowName(event.getFirstEventProcessorId());
        if (StringUtils.isBlank(nflowName)) {
            nflowName = event.getNflowName();
        }
        String processGroupId = getNflowProcessGroupId(event.getFirstEventProcessorId());
        if (StringUtils.isBlank(processGroupId)) {
            processGroupId = event.getNflowProcessGroupId();
        }
        String processorName = getProcessorName(event.getComponentId());
        if (StringUtils.isBlank(processorName)) {
            processorName = event.getComponentName();
        }
        return StringUtils.isNotBlank(nflowName) && StringUtils.isNotBlank(processGroupId) && StringUtils.isNotBlank(processorName);
    }


    public ProvenanceEventRecordDTO enrichEventWithNflowInformation(ProvenanceEventRecordDTO event) {
        String nflowName = getNflowName(event.getFirstEventProcessorId());
        if (StringUtils.isBlank(nflowName) && StringUtils.isNotBlank(event.getNflowName())) {
            nflowName = event.getNflowName();
        }
        //if we cant get the nflow name check to see if the NiFi flow cache is updated... and wait for it to be updated before processing
        if(StringUtils.isBlank(nflowName) && needsUpdateFromCluster()){
            log.info("Unable to find the nflow for processorId: {}.  Changes were detected from the cluster.  Refreshing the cache ...",event.getFirstEventProcessorId());
            nifiFlowCache.applyClusterUpdates();
            nflowName = getNflowName(event.getFirstEventProcessorId());
            if (StringUtils.isNotBlank(nflowName)) {
                log.info("Cache Refreshed.  Found the nflow: {} ",nflowName);
            }
            else {
                log.info("Cache Refreshed, but still unable to find the nflow.  This event {} will not be processed ",event);
            }
        }


        String processGroupId = getNflowProcessGroupId(event.getFirstEventProcessorId());
        if (StringUtils.isBlank(processGroupId)) {
            processGroupId = event.getNflowProcessGroupId();
        }
        String processorName = getProcessorName(event.getComponentId());
        if (StringUtils.isBlank(processorName)) {
            processorName = event.getComponentName();
        }
        event.setNflowName(nflowName);
        event.setNflowProcessGroupId(processGroupId);
        event.setComponentName(processorName);
        setProcessorFlowType(event);

        if (StringUtils.isNotBlank(nflowName)) {
            OpsManagerNflow nflow = opsManagerNflowProvider.findByNameWithoutAcl(nflowName);
            if (nflow != null && !OpsManagerNflow.NULL_NFLOW.equals(nflow)) {
                event.setStream(nflow.isStream());
            }
        }

        return event;
    }

    public OpsManagerNflow getNflow(String nflowName) {
        if (StringUtils.isNotBlank(nflowName)) {
            OpsManagerNflow nflow = opsManagerNflowProvider.findByNameWithoutAcl(nflowName);
            if (nflow != null && !OpsManagerNflow.NULL_NFLOW.equals(nflow)) {
                return nflow;
            }
        }
        return null;
    }

    public OpsManagerNflow getNflow(ProvenanceEventRecordDTO event) {
        String nflowName = event.getNflowName();
        if (StringUtils.isBlank(nflowName)) {
            nflowName = getNflowName(event.getFirstEventProcessorId());
        }
        return getNflow(nflowName);
    }


    public NovaProcessorFlowType setProcessorFlowType(ProvenanceEventRecordDTO event) {
        if (event.getProcessorType() == null) {

            if (event.isTerminatedByFailureRelationship()) {
                event.setProcessorType(NovaProcessorFlowType.FAILURE);
                event.setIsFailure(true);
            }
            NovaProcessorFlowType flowType = getProcessorFlowType(event.getSourceConnectionIdentifier());
            event.setProcessorType(flowType);

            if (flowType.equals(NovaProcessorFlowType.FAILURE)) {
                event.setIsFailure(true);
            }
        }
        return event.getProcessorType();
    }


    public boolean isFailure(String sourceConnectionIdentifer) {
        return NovaProcessorFlowType.FAILURE.equals(getProcessorFlowType(sourceConnectionIdentifer));
    }

    private NovaProcessorFlowType getProcessorFlowType(String sourceConnectionIdentifer) {

        if (sourceConnectionIdentifer != null) {
            NiFiFlowCacheConnectionData connectionData = getFlowCache().getConnectionIdToConnection().get(sourceConnectionIdentifer);
            if (connectionData != null && connectionData.getName() != null) {
                if (connectionData.getName().toLowerCase().contains("failure")) {
                    return NovaProcessorFlowType.FAILURE;
                } else if (connectionData.getName().toLowerCase().contains("warn")) {
                    return NovaProcessorFlowType.WARNING;
                }
            }
        }
        return NovaProcessorFlowType.NORMAL_FLOW;
    }

    public boolean isReusableFlowProcessor(String processorId) {
        return getFlowCache().getReusableTemplateProcessorIds().contains(processorId);
    }

    /**
     * Check to see if the event has a relationship to Nflow Manager
     * In cases where a user is experimenting in NiFi and not using Nflow Manager the event would not be registered
     *
     * @param event a provenance event
     * @return {@code true} if the event has a nflow associaetd with it {@code false} if there is no nflow associated with it
     */
    public boolean isRegisteredWithNflowManager(ProvenanceEventRecordDTO event) {

        String nflowName = event.getNflowName();
        if (StringUtils.isNotBlank(nflowName)) {
            OpsManagerNflow nflow = opsManagerNflowProvider.findByNameWithoutAcl(nflowName);
            if (nflow == null || OpsManagerNflow.NULL_NFLOW.equals(nflow)) {
                log.debug("Not processing operational metadata for nflow {} , event {} because it is not registered in nflow manager ", nflowName, event);
                // opsManagerNflowCache.invalidateNflow(nflowName);
                return false;
            } else {
                return true;
            }
        }
        return false;
    }


    public String getNflowName(ProvenanceEventRecordDTO event) {
        return getNflowName(event.getFirstEventProcessorId());
    }

    public String getNflowName(String nflowProcessorId) {
        return getFlowCache().getProcessorIdToNflowNameMap().get(nflowProcessorId);
    }

    public String getNflowProcessGroupId(String nflowProcessorId) {
        return nflowProcessorId != null ? getFlowCache().getProcessorIdToNflowProcessGroupId().get(nflowProcessorId) : null;
    }

    public String getProcessorName(String processorId) {
        return processorId != null ? getFlowCache().getProcessorIdToProcessorName().get(processorId) : null;
    }

    public boolean needsUpdateFromCluster(){
        return nifiFlowCache.needsUpdateFromCluster();
    }

    private NifiFlowCacheSnapshot getFlowCache() {
        return nifiFlowCache.getLatest();
    }


    public boolean isNifiFlowCacheAvailable() {
        return nifiFlowCache.isAvailable();
    }

    public boolean isConnectedToNifi() {
        return nifiFlowCache.isConnectedToNiFi();
    }
}
