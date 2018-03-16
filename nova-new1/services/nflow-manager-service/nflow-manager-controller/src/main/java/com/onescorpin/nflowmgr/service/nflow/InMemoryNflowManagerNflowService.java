package com.onescorpin.nflowmgr.service.nflow;

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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.onescorpin.nflowmgr.rest.model.EntityVersion;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NflowVersions;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.UINflow;
import com.onescorpin.nflowmgr.rest.model.UserField;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.nflowmgr.service.FileObjectPersistence;
import com.onescorpin.nflowmgr.service.category.NflowManagerCategoryService;
import com.onescorpin.nflowmgr.service.template.NflowManagerTemplateService;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.policy.rest.model.FieldRuleProperty;
import com.onescorpin.rest.model.LabelValue;
import com.onescorpin.security.action.Action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * In memory implementation
 */
public class InMemoryNflowManagerNflowService implements NflowManagerNflowService {

    @Inject
    private LegacyNifiRestClient nifiRestClient;

    @Inject
    private NflowManagerCategoryService categoryProvider;

    @Inject
    private NflowManagerTemplateService templateProvider;

    private Map<String, NflowMetadata> nflows = new HashMap<>();

    @PostConstruct
    private void postConstruct() {
        Collection<NflowMetadata> savedNflows = FileObjectPersistence.getInstance().getNflowsFromFile();
        if (savedNflows != null) {
            Long maxId = 0L;
            for (NflowMetadata nflow : savedNflows) {

                //update the category mappings
                String categoryId = nflow.getCategory().getId();
                NflowCategory category = categoryProvider.getCategoryById(categoryId);
                nflow.setCategory(category);
                category.addRelatedNflow(new NflowSummary(nflow));
                //add it to the map
                nflows.put(nflow.getId(), nflow);
            }

            loadSavedNflowsToMetaClientStore();
        }
    }

    @Override
    public boolean checkNflowPermission(String id, Action action, Action... more) {
        // Permission checking not currently implemented for the in-memory implementation
        return true;
    }

    public Collection<NflowMetadata> getNflows() {
        return nflows.values();
    }

    public Collection<? extends UINflow> getNflows(boolean verbose) {
        if (verbose) {
            return getNflows();
        } else {
            return getNflowSummaryData();
        }
    }
    
    @Override
    public Page<UINflow> getNflows(boolean verbose, Pageable pageable, String filter) {
        Collection<? extends UINflow> allNflows = getNflows(verbose);
        List<UINflow> pagedNflows = getNflows(verbose).stream()
                        .skip(Math.max(pageable.getOffset() - 1, 0))
                        .limit(pageable.getPageSize())
                        .collect(Collectors.toList());
        return new PageImpl<>(pagedNflows, 
                             new PageRequest(pageable.getOffset() / pageable.getPageSize(), pageable.getPageSize()), 
                             allNflows.size());
    }

    public List<NflowSummary> getNflowSummaryData() {
        List<NflowSummary> summaryList = new ArrayList<>();
        if (nflows != null && !nflows.isEmpty()) {
            for (NflowMetadata nflow : nflows.values()) {
                summaryList.add(new NflowSummary(nflow));
            }
        }
        return summaryList;
    }


    public List<NflowSummary> getNflowSummaryForCategory(String categoryId) {
        List<NflowSummary> summaryList = new ArrayList<>();
        NflowCategory category = categoryProvider.getCategoryById(categoryId);
        if (category != null && category.getNflows() != null) {
            summaryList.addAll(category.getNflows());
        }
        return summaryList;
    }


    @Override
    public NflowMetadata getNflowByName(final String categoryName, final String nflowName) {

        if (nflows != null && !nflows.isEmpty()) {
            return Iterables.tryFind(nflows.values(), new Predicate<NflowMetadata>() {
                @Override
                public boolean apply(NflowMetadata metadata) {

                    return metadata.getNflowName().equalsIgnoreCase(nflowName) && metadata.getCategoryName().equalsIgnoreCase(categoryName);
                }
            }).orNull();
        }
        return nflows.get(nflowName);
    }

    @Override
    public NflowMetadata getNflowById(String id) {
        return getNflowById(id, false);
    }

    @Override
    public NflowMetadata getNflowById(String id, boolean refreshTargetTableSchema) {

        if (nflows != null && !nflows.isEmpty()) {
            NflowMetadata nflow = nflows.get(id);
            if (nflow != null) {
                //get the latest category data
                NflowCategory category = categoryProvider.getCategoryById(nflow.getCategory().getId());
                nflow.setCategory(category);

                //set the template to the nflow

                RegisteredTemplate registeredTemplate = templateProvider.getRegisteredTemplate(nflow.getTemplateId());

                if (registeredTemplate != null) {
                    RegisteredTemplate copy = new RegisteredTemplate(registeredTemplate);
                    copy.getProperties().clear();
                    nflow.setRegisteredTemplate(copy);
                    nflow.setTemplateId(copy.getNifiTemplateId());
                }

                return nflow;
            }
        }

        return null;
    }

    @Override
    public NflowVersions getNflowVersions(String nflowId, boolean includeContent) {
        NflowVersions versions = new NflowVersions(nflowId);
        NflowMetadata nflow = getNflowById(nflowId);
        
        if (nflow != null) {
            EntityVersion version = versions.addNewVersion(UUID.randomUUID().toString(), "v1.0", nflow.getCreateDate());
            if (includeContent) {
                version.setEntity(nflow);
            }
        }
        return versions;
    }
    
    @Override
    public Optional<EntityVersion> getNflowVersion(String nflowId, String versionId, boolean includeContent) {
        NflowMetadata nflow = getNflowById(nflowId);
        EntityVersion version = null;
        
        if (nflow != null) {
            version = new EntityVersion(UUID.randomUUID().toString(), "v1.0", nflow.getCreateDate());
            if (includeContent) {
                version.setEntity(nflow);
            }
        }
        
        return Optional.ofNullable(version);
    }

    @Override
    public List<NflowMetadata> getNflowsWithTemplate(final String registeredTemplateId) {
        return Lists.newArrayList(Iterables.filter(nflows.values(), new Predicate<NflowMetadata>() {
            @Override
            public boolean apply(NflowMetadata nflow) {
                return nflow.getTemplateId().equalsIgnoreCase(registeredTemplateId);
            }
        }));
    }

    @Override
    public Nflow.ID resolveNflow(@Nonnull Serializable fid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableNflowCleanup(@Nonnull String nflowId) {
        throw new UnsupportedOperationException();
    }

    /**
     * Needed to rewire nflows to ids in the server upon server start since the Nflow Metadata store is in memory now
     */
    private void loadSavedNflowsToMetaClientStore() {
        for (NflowMetadata nflowMetadata : nflows.values()) {
            nflowMetadata.setNflowId(null);
            //  saveToMetadataStore(nflowMetadata);
        }
    }


    public void saveNflow(NflowMetadata nflow) {
        if (nflow.getId() == null || !nflows.containsKey(nflow.getId())) {
            nflow.setId(UUID.randomUUID().toString());
            nflow.setVersion(new Long(1));
        } else {
            NflowMetadata previousNflow = nflows.get(nflow.getId());
            nflow.setId(previousNflow.getId());
            nflow.setVersion(previousNflow.getVersion() + 1L);
        }

        //match up the related category
        String categoryId = nflow.getCategory().getId();
        NflowCategory category = null;
        if (categoryId != null) {
            category = categoryProvider.getCategoryById(categoryId);
        }
        if (category == null) {
            final String categoryName = nflow.getCategory().getSystemName();
            category = categoryProvider.getCategoryBySystemName(categoryName);
            nflow.setCategory(category);
        }
        if (category != null) {
            category.addRelatedNflow(new NflowSummary(nflow));
        }

        //  saveToMetadataStore(nflow);
        nflows.put(nflow.getId(), nflow);
        FileObjectPersistence.getInstance().writeNflowsToFile(nflows.values());
    }

    @Override
    public void deleteNflow(@Nonnull String nflowId) {
        nflows.remove(nflowId);
    }

    @Override
    public NflowSummary enableNflow(String nflowId) {
        NflowMetadata nflowMetadata = getNflowById(nflowId);
        if (nflowMetadata != null) {
            nflowMetadata.setState("ENABLED");
            return new NflowSummary(nflowMetadata);
        }
        return null;
    }

    @Override
    public NflowSummary disableNflow(String nflowId) {
        NflowMetadata nflowMetadata = getNflowById(nflowId);
        if (nflowMetadata != null) {
            nflowMetadata.setState("DISABLED");
            return new NflowSummary(nflowMetadata);
        }
        return null;
    }

    @Override
    public void applyNflowSelectOptions(List<FieldRuleProperty> properties) {
        if (properties != null && !properties.isEmpty()) {
            List<NflowSummary> nflowSummaries = getNflowSummaryData();
            List<LabelValue> nflowSelection = new ArrayList<>();
            for (NflowSummary nflowSummary : nflowSummaries) {
                nflowSelection.add(new LabelValue(nflowSummary.getCategoryAndNflowDisplayName(), nflowSummary.getCategoryAndNflowSystemName()));
            }
            for (FieldRuleProperty property : properties) {
                property.setSelectableValues(nflowSelection);
                if (property.getValues() == null) {
                    property.setValues(new ArrayList<>()); // reset the intial values to be an empty arraylist
                }
            }
        }
    }

    @Nonnull
    @Override
    public Set<UserField> getUserFields() {
        return Collections.emptySet();
    }

    @Override
    public void setUserFields(@Nonnull Set<UserField> userFields) {
    }

    @Nonnull
    @Override
    public Optional<Set<UserProperty>> getUserFields(@Nonnull String categoryId) {
        return Optional.of(Collections.emptySet());
    }

    @Override
    public NifiNflow createNflow(NflowMetadata nflowMetadata) {
        saveNflow(nflowMetadata);
        NifiNflow nifiNflow = new NifiNflow();
        nifiNflow.setNflowMetadata(nflowMetadata);
        nifiNflow.setSuccess(true);
        return nifiNflow;
    }

    @Override
    public void updateNflowDatasources(String nflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAllNflowsDatasources() {
        throw new UnsupportedOperationException();
    }
}
