package com.onescorpin.nflowmgr.service.category;

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

import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.service.UserPropertyTransform;
import com.onescorpin.nflowmgr.service.nflow.NflowModelTransform;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryNotFoundException;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.security.HadoopSecurityGroupProvider;
import com.onescorpin.metadata.modeshape.security.JcrHadoopSecurityGroup;
import com.onescorpin.security.rest.controller.SecurityModelTransform;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Transforms categories between Nflow Manager and Metadata formats.
 */
public class CategoryModelTransform  extends SimpleCategoryModelTransform{

    @Inject
    private SecurityModelTransform securityTransform;

    /**
     * Transform functions for nflows
     */
    @Inject
    NflowModelTransform nflowModelTransform;


    /**
     * Transforms the specified Metadata category to a Nflow Manager category.
     *
     * @param domainCategory the Metadata category
     * @return the Nflow Manager category
     */
    @Nullable
    public NflowCategory domainToNflowCategory(@Nullable final Category domainCategory) {
        return domainToNflowCategory(domainCategory, categoryProvider.getUserFields());
    }

    public NflowCategory domainToNflowCategory(@Nullable final Category domainCategory, boolean includeNflowDetails) {
        return domainToNflowCategory(domainCategory, categoryProvider.getUserFields(),includeNflowDetails);
    }

    /**
     * Transforms the specified Metadata categories into Nflow Manager categories.
     *
     * @param domain the Metadata categories
     * @return the Nflow Manager categories
     */
    @Nonnull
    public List<NflowCategory> domainToNflowCategory(@Nonnull final Collection<Category> domain) {
        final Set<UserFieldDescriptor> userFields = categoryProvider.getUserFields();
        return domain.stream().map(c -> domainToNflowCategory(c, userFields)).collect(Collectors.toList());
    }
    public List<NflowCategory> domainToNflowCategory(@Nonnull final Collection<Category> domain, boolean includeNflowDetails) {
        final Set<UserFieldDescriptor> userFields = categoryProvider.getUserFields();
        return domain.stream().map(c -> domainToNflowCategory(c, userFields,includeNflowDetails)).collect(Collectors.toList());
    }

    private NflowCategory domainToNflowCategory(@Nullable final Category domainCategory, @Nonnull final Set<UserFieldDescriptor> userFields) {
        return domainToNflowCategory(domainCategory,userFields,false);
    }
    /**
     * Transforms the specified Metadata category into a Nflow Manager category.
     *
     * @param domainCategory the Metadata category
     * @param userFields     the user-defined fields
     * @return the Nflow Manager category
     */
    @Nullable
    private NflowCategory domainToNflowCategory(@Nullable final Category domainCategory, @Nonnull final Set<UserFieldDescriptor> userFields, boolean includeNflowDetails) {
        if (domainCategory != null) {
            NflowCategory category = new NflowCategory();
            category.setId(domainCategory.getId().toString());
            if (includeNflowDetails && domainCategory.getNflows() != null) {
                List<NflowSummary> summaries = nflowModelTransform.domainToNflowSummary(domainCategory.getNflows());
                category.setNflows(summaries);
                category.setRelatedNflows(summaries.size());
            }
            category.setIconColor(domainCategory.getIconColor());
            category.setIcon(domainCategory.getIcon());
            category.setName(domainCategory.getDisplayName());
            category.setSystemName(domainCategory.getSystemName() == null ? domainCategory.getDisplayName() : domainCategory.getSystemName()); //in pre-0.8.4 version of Nova there was no system name stored for domain categories
            category.setDescription(domainCategory.getDescription());
            category.setCreateDate(domainCategory.getCreatedTime() != null ? domainCategory.getCreatedTime().toDate() : null);
            category.setUpdateDate(domainCategory.getModifiedTime() != null ? domainCategory.getModifiedTime().toDate() : null);

            // Transform user-defined fields and properties
            category.setUserFields(UserPropertyTransform.toUserFields(categoryProvider.getNflowUserFields(domainCategory.getId()).orElse(Collections.emptySet())));
            category.setUserProperties(UserPropertyTransform.toUserProperties(domainCategory.getUserProperties(), userFields));

            // Convert JCR securitygroup to DTO
            List<com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup> restSecurityGroups = new ArrayList<>();
            if (domainCategory.getSecurityGroups() != null && domainCategory.getSecurityGroups().size() > 0) {
                for (Object group : domainCategory.getSecurityGroups()) {
                    HadoopSecurityGroup hadoopSecurityGroup = (HadoopSecurityGroup) group;
                    com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup restSecurityGroup = new com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup();
                    restSecurityGroup.setDescription(hadoopSecurityGroup.getDescription());
                    restSecurityGroup.setId(hadoopSecurityGroup.getGroupId());
                    restSecurityGroup.setName(hadoopSecurityGroup.getName());
                    restSecurityGroups.add(restSecurityGroup);
                }
            }
            category.setSecurityGroups(restSecurityGroups);

            securityTransform.applyAccessControl(domainCategory,category);

            return category;
        } else {
            return null;
        }
    }

    /**
     * Transforms the specified Metadata category to a simple Nflow Manager category.
     *
     * @param domainCategory the Metadata category
     * @return the Nflow Manager category
     */
    @Nullable
    public NflowCategory domainToNflowCategorySimple(@Nullable final Category domainCategory) {
       return super.domainToNflowCategorySimple(domainCategory,false,false);
    }

    /**
     * Transforms the specified Metadata categories to simple Nflow Manager categories.
     *
     * @param domain the Metadata categories
     * @return the Nflow Manager categories
     */
    @Nonnull
    public List<NflowCategory> domainToNflowCategorySimple(@Nonnull final Collection<Category> domain) {
        return super.domainToNflowCategorySimple(domain,false,false);
    }

    /**
     * Transforms the specified Nflow Manager category to a Metadata category.
     *
     * @param nflowCategory the Nflow Manager category
     * @return the Metadata category
     */
    @Nonnull
    public Category nflowCategoryToDomain(@Nonnull final NflowCategory nflowCategory) {
        final Set<UserFieldDescriptor> userFields = categoryProvider.getUserFields();
        return nflowCategoryToDomain(nflowCategory, userFields);
    }

    /**
     * Transforms the specified Nflow Manager categories to Metadata categories.
     *
     * @param nflowCategories the Nflow Manager categories
     * @return the Metadata categories
     */
    public List<Category> nflowCategoryToDomain(Collection<NflowCategory> nflowCategories) {
        final Set<UserFieldDescriptor> userFields = categoryProvider.getUserFields();
        return nflowCategories.stream().map(c -> nflowCategoryToDomain(c, userFields)).collect(Collectors.toList());
    }

    /**
     * Transforms the specified Nflow Manager category to a Metadata category.
     *
     * @param nflowCategory the Nflow Manager category
     * @param userFields   the user-defined fields
     * @return the Metadata category
     */
    @Nonnull
    private Category nflowCategoryToDomain(@Nonnull final NflowCategory nflowCategory, @Nonnull final Set<UserFieldDescriptor> userFields) {
        Category.ID domainId = nflowCategory.getId() != null ? categoryProvider.resolveId(nflowCategory.getId()) : null;
        Category category = null;
        if (domainId != null) {
            category = categoryProvider.findById(domainId);
        }

        if (category == null) {
            category = categoryProvider.ensureCategory(nflowCategory.getSystemName());
        }
        if (category == null) {
            throw new CategoryNotFoundException("Unable to find Category ", domainId);
        }
        domainId = category.getId();
        nflowCategory.setId(domainId.toString());
        category.setDisplayName(nflowCategory.getName());
        category.setSystemName(nflowCategory.getSystemName());
        category.setDescription(nflowCategory.getDescription());
        category.setIcon(nflowCategory.getIcon());
        category.setIconColor(nflowCategory.getIconColor());
        category.setCreatedTime(new DateTime(nflowCategory.getCreateDate()));
        category.setModifiedTime(new DateTime(nflowCategory.getUpdateDate()));

        // Transforms the Nflow Manager user-defined properties to domain user-defined properties
        if (nflowCategory.getUserProperties() != null) {
            category.setUserProperties(UserPropertyTransform.toMetadataProperties(nflowCategory.getUserProperties()), userFields);
        }

        // Set the hadoop security groups
        final List<HadoopSecurityGroup> securityGroups = new ArrayList<>();
        if (nflowCategory.getSecurityGroups() != null) {

            for (com.onescorpin.nflowmgr.rest.model.HadoopSecurityGroup securityGroup : nflowCategory.getSecurityGroups()) {
                JcrHadoopSecurityGroup hadoopSecurityGroup = (JcrHadoopSecurityGroup) hadoopSecurityGroupProvider.ensureSecurityGroup(securityGroup.getName());
                hadoopSecurityGroup.setGroupId(securityGroup.getId());
                hadoopSecurityGroup.setDescription(securityGroup.getDescription());
                securityGroups.add(hadoopSecurityGroup);
            }

        }
        category.setSecurityGroups(securityGroups);

        return category;
    }
}
