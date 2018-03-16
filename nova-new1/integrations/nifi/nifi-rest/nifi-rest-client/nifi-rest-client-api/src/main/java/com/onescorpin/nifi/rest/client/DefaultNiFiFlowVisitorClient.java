package com.onescorpin.nifi.rest.client;

/*-
 * #%L
 * onescorpin-nifi-rest-client-api
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

import com.onescorpin.nifi.nflowmgr.TemplateCreationHelper;
import com.onescorpin.nifi.rest.model.flow.NifiFlowProcessGroup;
import com.onescorpin.nifi.rest.model.visitor.NifiFlowBuilder;
import com.onescorpin.nifi.rest.model.visitor.NifiVisitableProcessGroup;
import com.onescorpin.nifi.rest.model.visitor.NifiVisitableProcessor;
import com.onescorpin.nifi.rest.support.NifiConstants;
import com.onescorpin.nifi.rest.support.NifiProcessUtil;
import com.onescorpin.nifi.rest.visitor.NifiConnectionOrderVisitor;
import com.onescorpin.nifi.rest.visitor.NifiConnectionOrderVisitorCache;
import com.onescorpin.support.NflowNameUtil;

import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.ProcessorDTO;
import org.apache.nifi.web.api.dto.TemplateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 */
public class DefaultNiFiFlowVisitorClient implements NiFiFlowVisitorClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultNiFiFlowVisitorClient.class);


    private NiFiRestClient restClient;

    public DefaultNiFiFlowVisitorClient(NiFiRestClient restClient) {
        this.restClient = restClient;
    }


    public NifiVisitableProcessGroup getFlowOrder(String processGroupId, NifiConnectionOrderVisitorCache cache) throws NifiComponentNotFoundException {
        ProcessGroupDTO processGroupEntity = restClient.processGroups().findById(processGroupId, true, true).orElse(null);
        return getFlowOrder(processGroupEntity, cache);
    }


    public NifiVisitableProcessGroup getFlowOrder(ProcessGroupDTO processGroupEntity, NifiConnectionOrderVisitorCache cache) throws NifiComponentNotFoundException {
        return getFlowOrder(processGroupEntity, cache, true);
    }

    /**
     *
     * @param processGroupEntity
     * @param cache
     * @param logRestAccessErrors
     * @return
     * @throws NifiComponentNotFoundException
     */
    public NifiVisitableProcessGroup getFlowOrder(ProcessGroupDTO processGroupEntity, NifiConnectionOrderVisitorCache cache, boolean logRestAccessErrors) throws NifiComponentNotFoundException {
        if (cache == null) {
            cache = new NifiConnectionOrderVisitorCache();
        }

        final NifiConnectionOrderVisitorCache finalCache = cache;
        Optional<ProcessGroupDTO> cachedProcessGroup = cache.getProcessGroup(processGroupEntity.getId());
        if (!cachedProcessGroup.isPresent() && processGroupEntity.getContents() != null) {
            NifiProcessUtil.getProcessGroups(processGroupEntity).stream().forEach(processGroupDTO -> finalCache.add(processGroupDTO));
        }

        NifiVisitableProcessGroup group = null;
        if (processGroupEntity != null) {
            group = new NifiVisitableProcessGroup(processGroupEntity);
            NifiConnectionOrderVisitor orderVisitor = new NifiConnectionOrderVisitor(restClient, group, finalCache);
            try {
                Optional<ProcessGroupDTO> parent = cache.getProcessGroup(processGroupEntity.getParentGroupId());
                if (!parent.isPresent()) {
                    parent = restClient.processGroups().findById(processGroupEntity.getParentGroupId(), false, false, logRestAccessErrors);
                }
                if (parent.isPresent()) {
                    group.setParentProcessGroup(parent.get());
                }
            } catch (NifiComponentNotFoundException e) {
                //cant find the parent
            }
            group.accept(orderVisitor);
            finalCache.add(orderVisitor.toCachedItem());
        }
        return group;
    }


    public NifiFlowProcessGroup getNflowFlow(String processGroupId) {
        return getNflowFlow(processGroupId, null);
    }

    public NifiFlowProcessGroup getNflowFlow(String processGroupId, NifiConnectionOrderVisitorCache cache) {
        NifiVisitableProcessGroup visitableGroup = getFlowOrder(processGroupId, cache);
        NifiFlowProcessGroup flow = new NifiFlowBuilder().build(visitableGroup);
        String categoryName = flow.getParentGroupName();
        String nflowName = flow.getName();
        nflowName = NflowNameUtil.fullName(categoryName, nflowName);
        //if it is a versioned nflow then strip the version to get the correct nflow name
        nflowName = TemplateCreationHelper.parseVersionedProcessGroupName(nflowName);
        flow.setNflowName(nflowName);
        return flow;
    }

    public Set<ProcessorDTO> getProcessorsForFlow(String processGroupId) throws NifiComponentNotFoundException {
        NifiVisitableProcessGroup group = getFlowOrder(processGroupId, null);
        Set<ProcessorDTO> processors = new HashSet<>();
        for (NifiVisitableProcessor p : group.getStartingProcessors()) {
            processors.addAll(p.getProcessors());
        }
        return processors;
    }


    public NifiFlowProcessGroup getNflowFlowForCategoryAndNflow(String categoryAndNflowName) {
        NifiFlowProcessGroup flow = null;
        String category = NflowNameUtil.category(categoryAndNflowName);
        String nflow = NflowNameUtil.nflow(categoryAndNflowName);
        //1 find the ProcessGroup under "root" matching the name category
        ProcessGroupDTO processGroupEntity = restClient.processGroups().findRoot();
        ProcessGroupDTO root = processGroupEntity;
        ProcessGroupDTO categoryGroup = root.getContents().getProcessGroups().stream().filter(group -> category.equalsIgnoreCase(group.getName())).findAny().orElse(null);
        if (categoryGroup != null) {
            ProcessGroupDTO nflowGroup = categoryGroup.getContents().getProcessGroups().stream().filter(group -> nflow.equalsIgnoreCase(group.getName())).findAny().orElse(null);
            if (nflowGroup != null) {
                flow = getNflowFlow(nflowGroup.getId());
            }
        }
        return flow;
    }

    public List<NifiFlowProcessGroup> getNflowFlows(Collection<String> nflowNames) {
        return getNflowFlows(nflowNames, new NifiConnectionOrderVisitorCache());
    }

    @Override
    public List<NifiFlowProcessGroup> getNflowFlowsWithCache(NifiConnectionOrderVisitorCache cache) {
        if (cache == null) {
            cache = new NifiConnectionOrderVisitorCache();
        }
        return getNflowFlows(null, cache);
    }

    public List<NifiFlowProcessGroup> getNflowFlows(Collection<String> nflowNames, final NifiConnectionOrderVisitorCache cache) {

        log.info("get Graph of Nifi Flows looking for {} ", nflowNames == null ? "ALL Nflows " : nflowNames);
        long start = System.currentTimeMillis();

        List<NifiFlowProcessGroup> nflowFlows = new ArrayList<>();
        ProcessGroupDTO processGroupEntity = restClient.processGroups().findRoot();
        ProcessGroupDTO root = processGroupEntity;
        //first level is the category
        root.getContents().getProcessGroups().stream().sorted(new Comparator<ProcessGroupDTO>() {
            @Override
            public int compare(ProcessGroupDTO o1, ProcessGroupDTO o2) {
                if (TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME.equalsIgnoreCase(o1.getName())) {
                    return -1;
                }
                if (TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME.equalsIgnoreCase(o2.getName())) {
                    return -1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        }).forEach(category -> {
            for (ProcessGroupDTO nflowProcessGroup : category.getContents().getProcessGroups()) {

                //second level is the nflow
                String nflowName = NflowNameUtil.fullName(category.getName(), nflowProcessGroup.getName());
                //if it is a versioned nflow then strip the version to get the correct nflow name
                nflowName = TemplateCreationHelper.parseVersionedProcessGroupName(nflowName);
                //if nflownames are sent in, only add those that match or those in the reusable group
                if ((nflowNames == null || nflowNames.isEmpty()) || (nflowNames != null && (nflowNames.contains(nflowName) || TemplateCreationHelper.REUSABLE_TEMPLATES_PROCESS_GROUP_NAME
                    .equalsIgnoreCase(category.getName())))) {
                    NifiFlowProcessGroup nflowFlow = getNflowFlow(nflowProcessGroup.getId(), cache);
                    nflowFlow.setNflowName(nflowName);
                    nflowFlows.add(nflowFlow);
                }
            }
        });
        long end = System.currentTimeMillis();
        log.info("finished Graph of Nifi Flows.  Returning {} flows, {} ", nflowFlows.size(), (end - start) + " ms");
        return nflowFlows;
    }


    //walk entire graph
    public List<NifiFlowProcessGroup> getNflowFlows() {
        return getNflowFlows(null, new NifiConnectionOrderVisitorCache());
    }


    public NifiFlowProcessGroup getTemplateNflowFlow(TemplateDTO template) {
        ProcessGroupDTO parentProcessGroup = new ProcessGroupDTO();
        parentProcessGroup.setId(UUID.randomUUID().toString());
        parentProcessGroup.setParentGroupId(UUID.randomUUID().toString());
        parentProcessGroup.setName(template.getName());
        parentProcessGroup.setContents(template.getSnippet());
        NifiConnectionOrderVisitorCache cache = new NifiConnectionOrderVisitorCache();
        Collection<ProcessGroupDTO> groups = NifiProcessUtil.getProcessGroups(parentProcessGroup);
        cache.add(parentProcessGroup);
        //add the snippet as its own process group
        if (template.getSnippet().getProcessors() != null) {
            //find the first processor and get its parent group id
            Optional<ProcessorDTO> firstProcessor = template.getSnippet().getProcessors().stream().findFirst();
            if (firstProcessor.isPresent()) {
                String groupId = firstProcessor.get().getParentGroupId();
                ProcessGroupDTO snippetGroup = new ProcessGroupDTO();
                snippetGroup.setId(groupId);
                snippetGroup.setParentGroupId(template.getGroupId());
                snippetGroup.setContents(template.getSnippet());
                cache.add(snippetGroup);
            }

        }
        if (groups != null) {
            groups.stream().forEach(group -> cache.add(group));
        }
        //add any remote ProcessGroups
        if (template.getSnippet().getRemoteProcessGroups() != null) {
            template.getSnippet().getRemoteProcessGroups().stream().forEach(remoteProcessGroupDTO -> cache.add(remoteProcessGroupDTO));
        }
        NifiVisitableProcessGroup visitableGroup = getFlowOrder(parentProcessGroup, cache, false);
        NifiFlowProcessGroup flow = new NifiFlowBuilder().build(visitableGroup);
        return flow;
    }

    public NifiFlowProcessGroup getTemplateNflowFlow(String templateId) {
        TemplateDTO template = restClient.templates().findById(templateId).orElseThrow(() -> new NifiComponentNotFoundException(templateId, NifiConstants.NIFI_COMPONENT_TYPE.TEMPLATE, null));
        return getTemplateNflowFlow(template);
    }


}
