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

import com.onescorpin.metadata.rest.model.nifi.NiFiFlowCacheConnectionData;
import com.onescorpin.nifi.nflowmgr.TemplateCreationHelper;
import com.onescorpin.nifi.rest.model.flow.NiFiFlowConnectionConverter;
import com.onescorpin.support.NflowNameUtil;

import org.apache.nifi.web.api.dto.ConnectionDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by sr186054 on 9/13/17.
 */
public class DefaultNiFiFlowCompletionCallback implements NiFiFlowInspectionCallback {

    private static final Logger log = LoggerFactory.getLogger(DefaultNiFiFlowCompletionCallback.class);

    private Map<String, String> processorIdToNflowProcessGroupId = new ConcurrentHashMap<>();
    private Map<String, String> processorIdToNflowNameMap = new ConcurrentHashMap<>();
    private Map<String, String> processorIdToProcessorName = new ConcurrentHashMap<>();
    private Map<String, NiFiFlowCacheConnectionData> connectionIdToConnectionMap = new ConcurrentHashMap<>();
    private Map<String, String> connectionIdCacheNameMap = new ConcurrentHashMap<>();
    private Set<String> reusableTemplateProcessorIds = new HashSet<>();
    private Set<ConnectionDTO> rootConnections = new HashSet<>();
    private String reusableTemplateProcessGroupId;
    private Set<String> nflowNames = new HashSet<>();

    private String rootProcessGroupId;

    @Override
    public void execute(NiFiFlowInspectorManager nifiFlowInspectorManager) {
        NiFiFlowInspection root = nifiFlowInspectorManager.getFlowsInspected().values().stream().filter(f -> f.isRoot()).findFirst().orElse(null);
        if (root != null) {
            rootProcessGroupId = root.getProcessGroupId();
            this.rootConnections = root.getProcessGroupFlow().getFlow().getConnections().stream().map(e -> e.getComponent()).collect(Collectors.toSet());
            if ("root".equalsIgnoreCase(rootProcessGroupId) && !rootConnections.isEmpty()) {
                rootProcessGroupId = rootConnections.stream().findFirst().map(c -> c.getParentGroupId()).orElse("root");
            }
        }

        this.reusableTemplateProcessGroupId = nifiFlowInspectorManager.getFlowsInspected()
            .values().stream()
            .filter(f -> f.getLevel() == 2 && TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME.equalsIgnoreCase(f.getProcessGroupName())).findFirst()
            .map(f -> f.getProcessGroupId()).orElse(null);

        reusableTemplateProcessorIds = nifiFlowInspectorManager.getFlowsInspected()
            .values().stream()
            .filter(f -> f.getLevel() == 3 && TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME.equalsIgnoreCase(f.getParent().getProcessGroupName()))
            .flatMap(f -> f.getAllProcessors().stream())
            .map(p -> p.getId())
            .collect(Collectors.toSet());

        List<NiFiFlowInspection> nflowProcessGroupInspections = nifiFlowInspectorManager.getFlowsInspected()
            .values().stream()
            .filter(f -> f.getLevel() == 3 && !TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME.equalsIgnoreCase(f.getParent().getProcessGroupName()))
            .collect(Collectors.toList());

        nflowProcessGroupInspections.stream().forEach(f ->
                                                     {

                                                         String nflowName = NflowNameUtil.fullName(f.getParent().getProcessGroupName(), f.getProcessGroupName());
                                                         nflowNames.add(nflowName);
                                                         // log.info("process nflow {} ",nflowName);
                                                         List<ProcessorDTO> nflowChildren = f.getAllProcessors();
                                                         Map<String, String> nflowNameMap = nflowChildren.stream()
                                                             .collect(Collectors.toMap(x -> x.getId(), x -> nflowName));
                                                         processorIdToNflowNameMap.putAll(nflowNameMap);

                                                         Map<String, String> processGroupIdMap =
                                                             nflowChildren.stream()
                                                                 .collect(Collectors.toMap(p -> p.getId(), p -> f.getProcessGroupId()));
                                                         processorIdToNflowProcessGroupId.putAll(processGroupIdMap);

                                                     });

        nifiFlowInspectorManager.getFlowsInspected().values().stream().forEach(inspection -> {

            Map<String, String>
                processorIdToNameMap =
                inspection.getProcessGroupFlow().getFlow().getProcessors().stream().map(e -> e.getComponent()).collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));

            Map<String, String>
                connectionIdMap =
                inspection.getProcessGroupFlow().getFlow().getConnections().stream().map(e -> e.getComponent()).collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));
            Map<String, NiFiFlowCacheConnectionData>
                connectionMap =
                inspection.getProcessGroupFlow().getFlow().getConnections().stream().map(e -> e.getComponent())
                    .map(c -> NiFiFlowConnectionConverter.toNiFiFlowConnection(c))
                    .collect(Collectors.toMap(c -> c.getConnectionIdentifier(),
                                              conn -> new NiFiFlowCacheConnectionData(conn.getConnectionIdentifier(), conn.getName(), conn.getSourceIdentifier(),
                                                                                      conn.getDestinationIdentifier())));

            connectionIdCacheNameMap.putAll(connectionIdMap);
            connectionIdToConnectionMap.putAll(connectionMap);
            processorIdToProcessorName.putAll(processorIdToNameMap);


        });


    }

    public Map<String, String> getProcessorIdToNflowProcessGroupId() {
        return processorIdToNflowProcessGroupId;
    }

    public Map<String, String> getProcessorIdToNflowNameMap() {
        return processorIdToNflowNameMap;
    }

    public Map<String, String> getProcessorIdToProcessorName() {
        return processorIdToProcessorName;
    }

    public Map<String, NiFiFlowCacheConnectionData> getConnectionIdToConnectionMap() {
        return connectionIdToConnectionMap;
    }

    public Map<String, String> getConnectionIdCacheNameMap() {
        return connectionIdCacheNameMap;
    }

    public Set<String> getReusableTemplateProcessorIds() {
        return reusableTemplateProcessorIds;
    }

    public String getReusableTemplateProcessGroupId() {
        return reusableTemplateProcessGroupId;
    }

    public Set<ConnectionDTO> getRootConnections() {
        return rootConnections;
    }

    public Set<String> getNflowNames() {
        return nflowNames;
    }

    public String getRootProcessGroupId() {
        return rootProcessGroupId;
    }
}
