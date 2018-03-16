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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.onescorpin.discovery.model.DefaultTag;
import com.onescorpin.discovery.schema.Tag;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NflowVersions;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.UINflow;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.nflowmgr.service.UserPropertyTransform;
import com.onescorpin.nflowmgr.service.category.CategoryModelTransform;
import com.onescorpin.nflowmgr.service.template.TemplateModelTransform;
import com.onescorpin.hive.service.HiveService;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.security.HadoopSecurityGroupProvider;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.api.versioning.EntityVersion;
import com.onescorpin.metadata.modeshape.security.JcrHadoopSecurityGroup;
import com.onescorpin.rest.model.search.SearchResult;
import com.onescorpin.rest.model.search.SearchResultImpl;
import com.onescorpin.security.core.encrypt.EncryptionService;
import com.onescorpin.security.rest.controller.SecurityModelTransform;
import com.onescorpin.security.rest.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Transforms nflows between Nflow Manager and Metadata formats.
 */
public class NflowModelTransform {

    @Inject
    CategoryProvider categoryProvider;

    @Inject
    NflowManagerTemplateProvider templateProvider;

    @Inject
    private NflowProvider nflowProvider;
    
    @Inject
    private SecurityModelTransform securityTransform;

    @Inject
    private TemplateModelTransform templateModelTransform;

    @Inject
    private CategoryModelTransform categoryModelTransform;

    @Inject
    private HiveService hiveService;

    @Inject
    private HadoopSecurityGroupProvider hadoopSecurityGroupProvider;

    @Inject
    private EncryptionService encryptionService;

    /**
     *
     * @param nflowMetadata
     */
    private void prepareForSave(NflowMetadata nflowMetadata) {

        if (nflowMetadata.getTable() != null) {
            nflowMetadata.getTable().simplifyFieldPoliciesForSerialization();
        }
        nflowMetadata.setRegisteredTemplate(null);

        //reset all those properties that contain config variables back to the string with the config options
        nflowMetadata.getProperties().stream().filter(property -> property.isContainsConfigurationVariables()).forEach(property -> property.setValue(property.getTemplateValue()));
        //reset all sensitive properties
        //ensure its encrypted
        encryptSensitivePropertyValues(nflowMetadata);


    }

    private void clearSensitivePropertyValues(NflowMetadata nflowMetadata) {
        nflowMetadata.getProperties().stream().filter(property -> property.isSensitive()).forEach(nifiProperty -> nifiProperty.setValue(""));
    }

    public void encryptSensitivePropertyValues(NflowMetadata nflowMetadata) {
        List<String> encrypted = new ArrayList<>();
        nflowMetadata.getSensitiveProperties().stream().forEach(nifiProperty -> {
            nifiProperty.setValue(encryptionService.encrypt(nifiProperty.getValue()));
            encrypted.add(nifiProperty.getValue());
        });
        int i = 0;
    }

    public void decryptSensitivePropertyValues(NflowMetadata nflowMetadata) {
        List<String> decrypted = new ArrayList<>();
        nflowMetadata.getProperties().stream().filter(property -> property.isSensitive()).forEach(nifiProperty -> {
            try {
                String decryptedValue = encryptionService.decrypt(nifiProperty.getValue());
                nifiProperty.setValue(decryptedValue);
                decrypted.add(decryptedValue);
            } catch (Exception e) {

            }
        });
        int i = 0;
    }

    /**
     * Convert a spring-data Page to a SearchResult UI object
     */
    public SearchResult toSearchResult(Page<UINflow> page) {
        SearchResult searchResult = new SearchResultImpl();
        searchResult.setData(page.getContent());
        searchResult.setRecordsTotal(page.getTotalElements());
        searchResult.setRecordsFiltered(page.getTotalElements());
        return searchResult;
    }

    /**
     * Transforms the specified Nflow Manager nflow to a Metadata nflow.
     *
     * @param nflowMetadata the Nflow Manager nflow
     * @return the Metadata nflow
     */
    @Nonnull
    public Nflow nflowToDomain(@Nonnull final NflowMetadata nflowMetadata) {
        //resolve the id
        Nflow.ID domainId = nflowMetadata.getId() != null ? nflowProvider.resolveId(nflowMetadata.getId()) : null;
        Nflow domain = domainId != null ? nflowProvider.findById(domainId) : null;

        NflowCategory restCategoryModel = nflowMetadata.getCategory();
        Category category = null;

        if (restCategoryModel != null && (domain == null || domain.getCategory() == null)) {
            category = categoryProvider.findById(categoryProvider.resolveId(restCategoryModel.getId()));
        }

        if (domain == null) {
            //ensure the Category exists
            if (category == null) {
                final String categoryId = (restCategoryModel != null) ? restCategoryModel.getId() : "(null)";
                throw new RuntimeException("Category cannot be found while creating nflow " + nflowMetadata.getSystemNflowName() + ".  Category Id is " + categoryId);
            }
            domain = nflowProvider.ensureNflow(category.getId(), nflowMetadata.getSystemNflowName());
            domainId = domain.getId();
            Nflow.State state = Nflow.State.valueOf(nflowMetadata.getState());
            domain.setState(state);
            //reassign the domain data back to the ui model....
            nflowMetadata.setNflowId(domainId.toString());
            nflowMetadata.setState(state.name());

        }
        domain.setDisplayName(nflowMetadata.getNflowName());
        if(nflowMetadata.getDescription() == null){
            nflowMetadata.setDescription("");
        }
        domain.setDescription(nflowMetadata.getDescription());

        nflowMetadata.setId(domain.getId().toString());

        if (StringUtils.isNotBlank(nflowMetadata.getState())) {
            Nflow.State state = Nflow.State.valueOf(nflowMetadata.getState().toUpperCase());
            domain.setState(state);
        }
        domain.setNifiProcessGroupId(nflowMetadata.getNifiProcessGroupId());

        //clear out the state as that
        RegisteredTemplate template = nflowMetadata.getRegisteredTemplate();
        prepareForSave(nflowMetadata);

        nflowMetadata.setRegisteredTemplate(template);
        if (domain.getTemplate() == null) {
            NflowManagerTemplate.ID templateId = templateProvider.resolveId(nflowMetadata.getTemplateId());
            NflowManagerTemplate domainTemplate = templateProvider.findById(templateId);
            domain.setTemplate(domainTemplate);
        }

        // Set user-defined properties
        if (nflowMetadata.getUserProperties() != null) {
            final Set<UserFieldDescriptor> userFields = getUserFields(category);
            domain.setUserProperties(UserPropertyTransform.toMetadataProperties(nflowMetadata.getUserProperties()), userFields);
        }

        // Set the hadoop security groups
        final List<HadoopSecurityGroup> securityGroups = new ArrayList<>();
        if (nflowMetadata.getSecurityGroups() != null) {

            for (com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup securityGroup : nflowMetadata.getSecurityGroups()) {
                JcrHadoopSecurityGroup hadoopSecurityGroup = (JcrHadoopSecurityGroup) hadoopSecurityGroupProvider.ensureSecurityGroup(securityGroup.getName());
                hadoopSecurityGroup.setGroupId(securityGroup.getId());
                hadoopSecurityGroup.setDescription(securityGroup.getDescription());
                securityGroups.add(hadoopSecurityGroup);
            }

        }
        domain.setSecurityGroups(securityGroups);
        domain.setVersionName(domain.getVersionName());
        
        if (nflowMetadata.getTags() != null) {
            domain.setTags(nflowMetadata.getTags().stream().map(Tag::getName).collect(Collectors.toSet()));
        }

        // Create a new nflow metadata stripped of any excess data that does 
        // not need to be serialized and stored in the nflow domain entity.
        NflowMetadata stripped = stripMetadata(nflowMetadata);
        domain.setJson(ObjectMapperSerializer.serialize(stripped));
        return domain;
    }
    
    
    /**
     * Clean out any excess or redundant data that should not be serialized and stored
     * with the nflow domain entity as JSON.
     * @param source the source metadata
     * @return a new metadata instance without the excess data
     */
    private NflowMetadata stripMetadata(NflowMetadata source) {
        NflowMetadata result = new NflowMetadata();
        
        result.setDataTransformation(source.getDataTransformation());
        result.setHadoopAuthorizationType(source.getHadoopAuthorizationType());
        result.setInputProcessorType(source.getInputProcessorType());
        result.setInputProcessorName(source.getInputProcessorName());
        result.setIsReusableNflow(source.isReusableNflow());
        result.setNifiProcessGroupId(source.getNifiProcessGroupId());
        result.setOptions(source.getOptions());
        result.setProperties(source.getProperties());
        result.setSchedule(source.getSchedule());
        result.setTable(source.getTable());
        result.setTableOption(source.getTableOption());
        return result;
    }

    /**
     * Transforms the specified domain nflow versions to a NflowVersions.
     *
     * @param domain the Metadata nflow
     * @return the Nflow Manager nflow
     */
    @Nonnull
    public NflowVersions domainToNflowVersions(@Nonnull final List<EntityVersion<Nflow>> versions, @Nonnull final Nflow.ID nflowId) {
        NflowVersions nflowVersions = new NflowVersions(nflowId.toString());
        versions.forEach(domainVer -> nflowVersions.getVersions().add(domainToNflowVersion(domainVer)));
        return nflowVersions;
    }
    
    @Nonnull
    public com.onescorpin.nflowmgr.rest.model.EntityVersion domainToNflowVersion(EntityVersion<Nflow> domainVer) {
        com.onescorpin.nflowmgr.rest.model.EntityVersion version
            = new com.onescorpin.nflowmgr.rest.model.EntityVersion(domainVer.getId().toString(), 
                                                                         domainVer.getName(), 
                                                                         domainVer.getCreatedDate().toDate());
        domainVer.getEntity().ifPresent(nflow -> version.setEntity(domainToNflowMetadata(nflow)));
        return version;
    }

    /**
     * Transforms the specified Metadata nflow to a Nflow Manager nflow.
     *
     * @param domain the Metadata nflow
     * @return the Nflow Manager nflow
     */
    @Nonnull
    public NflowMetadata domainToNflowMetadata(@Nonnull final Nflow domain) {
        return domainToNflowMetadata(domain, null);
    }

    /**
     * Transforms the specified Metadata nflows to Nflow Manager nflows.
     *
     * @param domain the Metadata nflows
     * @return the Nflow Manager nflows
     */
    @Nonnull
    public List<NflowMetadata> domainToNflowMetadata(@Nonnull final Collection<? extends Nflow> domain) {
        final Map<Category, Set<UserFieldDescriptor>> userFieldMap = Maps.newHashMap();
        return domain.stream().map(f -> domainToNflowMetadata(f, userFieldMap)).collect(Collectors.toList());
    }

    public NflowMetadata deserializeNflowMetadata(Nflow domain, boolean clearSensitiveProperties) {
        String json = domain.getJson();
        NflowMetadata nflowMetadata = ObjectMapperSerializer.deserialize(json, NflowMetadata.class);
        
        populate(nflowMetadata, domain);
        
        if (clearSensitiveProperties) {
            clearSensitivePropertyValues(nflowMetadata);
        }
        return nflowMetadata;
    }

    /**
     * @param nflowMetadata
     * @param domain
     */
    private void populate(NflowMetadata nflowMetadata, Nflow domain) {
    }

    public NflowMetadata deserializeNflowMetadata(Nflow domain) {
        return deserializeNflowMetadata(domain, true);
    }


    /**
     * Transforms the specified Metadata nflow to a Nflow Manager nflow.
     *
     * @param domain       the Metadata nflow
     * @param userFieldMap cache map from category to user-defined fields, or {@code null}
     * @return the Nflow Manager nflow
     */
    @Nonnull
    private NflowMetadata domainToNflowMetadata(@Nonnull final Nflow domain, @Nullable final Map<Category, Set<UserFieldDescriptor>> userFieldMap) {

        NflowMetadata nflow = deserializeNflowMetadata(domain, false);
        nflow.setId(domain.getId().toString());
        nflow.setNflowId(domain.getId().toString());
        nflow.setNflowName(domain.getDisplayName());
        nflow.setSystemNflowName(domain.getName());
        nflow.setDescription(domain.getDescription());
        nflow.setOwner(domain.getOwner() != null ? new User(domain.getOwner().getName()) : null);
        
        if (domain.getCreatedTime() != null) {
            nflow.setCreateDate(domain.getCreatedTime().toDate());
        }
        if (domain.getModifiedTime() != null) {
            nflow.setUpdateDate(domain.getModifiedTime().toDate());
        }

        NflowManagerTemplate template = domain.getTemplate();
        if (template != null) {
            RegisteredTemplate registeredTemplate = templateModelTransform.DOMAIN_TO_REGISTERED_TEMPLATE.apply(template);
            nflow.setRegisteredTemplate(registeredTemplate);
            nflow.setTemplateId(registeredTemplate.getId());
            nflow.setTemplateName(registeredTemplate.getTemplateName());
        }
        Category category = domain.getCategory();
        if (category != null) {
            nflow.setCategory(categoryModelTransform.domainToNflowCategorySimple(category));
        }
        nflow.setState(domain.getState() != null ? domain.getState().name() : null);
        nflow.setVersionName(domain.getVersionName() != null ? domain.getVersionName() : null);

        // Set user-defined properties
        final Set<UserFieldDescriptor> userFields;
        if (userFieldMap == null) {
            userFields = getUserFields(category);
        } else if (userFieldMap.containsKey(category)) {
            userFields = userFieldMap.get(category);
        } else {
            userFields = getUserFields(category);
            userFieldMap.put(category, userFields);
        }

        @SuppressWarnings("unchecked") final Set<UserProperty> userProperties = UserPropertyTransform.toUserProperties(domain.getUserProperties(), userFields);
        nflow.setUserProperties(userProperties);

        // Convert JCR securitygroup to DTO
        List<com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup> restSecurityGroups = new ArrayList<>();
        if (domain.getSecurityGroups() != null && domain.getSecurityGroups().size() > 0) {
            for (Object group : domain.getSecurityGroups()) {
                HadoopSecurityGroup hadoopSecurityGroup = (HadoopSecurityGroup) group;
                com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup restSecurityGroup = new com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup();
                restSecurityGroup.setDescription(hadoopSecurityGroup.getDescription());
                restSecurityGroup.setId(hadoopSecurityGroup.getGroupId());
                restSecurityGroup.setName(hadoopSecurityGroup.getName());
                restSecurityGroups.add(restSecurityGroup);
            }
        }
        nflow.setSecurityGroups(restSecurityGroups);
        
        nflow.setTags(domain.getTags().stream().map(name -> new DefaultTag(name)).collect(Collectors.toList()));

        if (domain.getUsedByNflows() != null) {
            final List<NflowSummary> usedByNflows = domain.getUsedByNflows().stream()
                .map(this::domainToNflowSummary)
                .collect(Collectors.toList());
            nflow.setUsedByNflows(usedByNflows);
        }

        //add in access control items
        securityTransform.applyAccessControl(domain, nflow);

        return nflow;
    }

    /**
     * Transforms the specified Metadata nflow to a Nflow Manager nflow summary.
     *
     * @param nflowManagerNflow the Metadata nflow
     * @return the Nflow Manager nflow summary
     */
    public NflowSummary domainToNflowSummary(@Nonnull final Nflow nflowManagerNflow) {
        Category category = nflowManagerNflow.getCategory();
        if (category == null) {
            return null;
        }

        NflowSummary nflowSummary = new NflowSummary();
        nflowSummary.setId(nflowManagerNflow.getId().toString());
        nflowSummary.setNflowId(nflowManagerNflow.getId().toString());

        nflowSummary.setCategoryId(category.getId().toString());
        if (category instanceof Category) {
            nflowSummary.setCategoryIcon(category.getIcon());
            nflowSummary.setCategoryIconColor(category.getIconColor());
        }
        nflowSummary.setCategoryName(category.getDisplayName());
        nflowSummary.setSystemCategoryName(category.getSystemName());
        nflowSummary.setUpdateDate(nflowManagerNflow.getModifiedTime() != null ? nflowManagerNflow.getModifiedTime().toDate() : null);
        nflowSummary.setNflowName(nflowManagerNflow.getDisplayName());
        nflowSummary.setSystemNflowName(nflowManagerNflow.getName());
        nflowSummary.setActive(nflowManagerNflow.getState() != null && nflowManagerNflow.getState().equals(Nflow.State.ENABLED));

        nflowSummary.setState(nflowManagerNflow.getState() != null ? nflowManagerNflow.getState().name() : null);

        if (nflowManagerNflow instanceof Nflow) {

            Nflow fmf = (Nflow) nflowManagerNflow;
            if (fmf.getTemplate() != null) {
                nflowSummary.setTemplateId(fmf.getTemplate().getId().toString());
                nflowSummary.setTemplateName(fmf.getTemplate().getName());
            }
        }
        //add in access control items
        securityTransform.applyAccessControl(nflowManagerNflow, nflowSummary);

        return nflowSummary;

    }

    /**
     * Transforms the specified Metadata nflows to Nflow Manager nflow summaries.
     *
     * @param domain the Metadata nflow
     * @return the Nflow Manager nflow summaries
     */
    @Nonnull
    public List<NflowSummary> domainToNflowSummary(@Nonnull final Collection<? extends Nflow> domain) {
        return domain.stream().map(this::domainToNflowSummary).filter(nflowSummary -> nflowSummary != null).collect(Collectors.toList());
    }
    
    /**
     * Transforms the specified Metadata nflows to Nflow Manager nflow summaries.
     *
     * @param domain the Metadata nflow
     * @return the Nflow Manager nflow summaries
     */
    @Nonnull
    public Page<UINflow> domainToNflowSummary(@Nonnull final Page<Nflow> domain, @Nonnull final Pageable pageable) {
        List<UINflow> summaries = domain.getContent().stream()
                        .map(this::domainToNflowSummary)
                        .filter(nflowSummary -> nflowSummary != null)
                        .collect(Collectors.toList());
        return new PageImpl<>(summaries, pageable, domain.getTotalElements());
    }

    /**
     * Gets the user-defined fields including those for the specified category.
     *
     * @param category the domain category
     * @return the user-defined fields
     */
    @Nonnull
    private Set<UserFieldDescriptor> getUserFields(@Nullable final Category category) {
        final Set<UserFieldDescriptor> userFields = nflowProvider.getUserFields();
        return (category != null) ? Sets.union(userFields, categoryProvider.getNflowUserFields(category.getId()).orElse(Collections.emptySet())) : userFields;
    }
}
