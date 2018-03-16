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

import com.onescorpin.cluster.ClusterService;
import com.onescorpin.nflowmgr.InvalidOperationException;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.UserField;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.nflowmgr.service.UserPropertyTransform;
import com.onescorpin.nflowmgr.service.security.SecurityService;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.MetadataCommand;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.event.category.CategoryChange;
import com.onescorpin.metadata.api.event.category.CategoryChangeEvent;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.event.MetadataChange;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.Action;

import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * An implementation of {@link NflowManagerCategoryService} backed by a {@link CategoryProvider}.
 */
public class DefaultNflowManagerCategoryService implements NflowManagerCategoryService {

    @Inject
    CategoryProvider categoryProvider;

    @Inject
    CategoryModelTransform categoryModelTransform;

    @Inject
    MetadataAccess metadataAccess;

    @Inject
    private SecurityService securityService;

    @Inject
    private AccessController accessController;

    @Inject
    private MetadataEventService metadataEventService;

    @Inject
    private ClusterService clusterService;


    @Override
    public boolean checkCategoryPermission(final String id, final Action action, final Action... more) {
        if (accessController.isEntityAccessControlled()) {
            return metadataAccess.read(() -> {
                final Category.ID domainId = categoryProvider.resolveId(id);
                final Category domainCategory = categoryProvider.findById(domainId);

                if (domainCategory != null) {
                    domainCategory.getAllowedActions().checkPermission(action, more);
                    return true;
                } else {
                    return false;
                }
            });
        } else {
            return true;
        }
    }

    @Override
    public Collection<NflowCategory> getCategories() {
        return getCategories(false);
    }

    public Collection<NflowCategory> getCategories(boolean includeNflowDetails) {
        return metadataAccess.read((MetadataCommand<Collection<NflowCategory>>) () -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_CATEGORIES);

            List<Category> domainCategories = categoryProvider.findAll();
            return categoryModelTransform.domainToNflowCategory(domainCategories, includeNflowDetails);
        });
    }

    @Override
    public NflowCategory getCategoryById(final String id) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_CATEGORIES);

            final Category.ID domainId = categoryProvider.resolveId(id);
            final Category domainCategory = categoryProvider.findById(domainId);
            return categoryModelTransform.domainToNflowCategory(domainCategory, true);
        });
    }

    @Override
    public NflowCategory getCategoryBySystemName(final String name) {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_CATEGORIES);

            final Category domainCategory = categoryProvider.findBySystemName(name);
            return categoryModelTransform.domainToNflowCategory(domainCategory, true);
        });
    }

    @Override
    public void saveCategory(final NflowCategory nflowCategory) {

        this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_CATEGORIES);
        boolean isNew = nflowCategory.getId() == null;

        // If the category exists and it is being renamed, perform the rename in a privileged transaction.
        // This is to get around the ModeShape problem of requiring admin privileges to do type manipulation.
        NflowCategory categoryUpdate = metadataAccess.commit(() -> {
            if (nflowCategory.getId() != null) {
                final NflowCategory oldCategory = getCategoryById(nflowCategory.getId());
                if (oldCategory != null && !oldCategory.getSystemName().equalsIgnoreCase(nflowCategory.getSystemName())) {
                    //system names have changed, only regenerate the system name if there are no related nflows
                    if (oldCategory.getRelatedNflows() == 0) {
                        Category.ID domainId = nflowCategory.getId() != null ? categoryProvider.resolveId(nflowCategory.getId()) : null;
                        categoryProvider.renameSystemName(domainId, nflowCategory.getSystemName());
                    }
                }
            }

            return nflowCategory;
        }, MetadataAccess.SERVICE);

        // Perform the rest of the updates as the current user.
        final Category.ID domainId = metadataAccess.commit(() -> {

            // Update the domain entity
            final Category domainCategory = categoryProvider.update(categoryModelTransform.nflowCategoryToDomain(categoryUpdate));

            // Repopulate identifier
            categoryUpdate.setId(domainCategory.getId().toString());

            ///update access control
            //TODO only do this when modifying the access control
            if (domainCategory.getAllowedActions().hasPermission(CategoryAccessControl.CHANGE_PERMS)) {
                categoryUpdate.toRoleMembershipChangeList().stream().forEach(roleMembershipChange -> securityService.changeCategoryRoleMemberships(categoryUpdate.getId(), roleMembershipChange));
                categoryUpdate.toNflowRoleMembershipChangeList().stream()
                    .forEach(roleMembershipChange -> securityService.changeCategoryNflowRoleMemberships(categoryUpdate.getId(), roleMembershipChange));
            }

            return domainCategory.getId();
        });

        // Update user-defined fields (must be outside metadataAccess)
        final Set<UserFieldDescriptor> userFields = (categoryUpdate.getUserFields() != null) ? UserPropertyTransform.toUserFieldDescriptors(categoryUpdate.getUserFields()) : Collections.emptySet();
        categoryProvider.setNflowUserFields(domainId, userFields);
        notifyCategoryChange(domainId, categoryUpdate.getSystemName(), isNew ? MetadataChange.ChangeType.CREATE : MetadataChange.ChangeType.UPDATE);
    }

    @Override
    public boolean deleteCategory(final String categoryId) throws InvalidOperationException {
        this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.EDIT_CATEGORIES);

        SimpleCategory simpleCategory = metadataAccess.read(() -> {
            Category.ID id = categoryProvider.resolveId(categoryId);
            final Category category = categoryProvider.findById(id);

            if (category != null) {

                if (accessController.isEntityAccessControlled()) {
                    //this check should throw a runtime exception
                    category.getAllowedActions().checkPermission(CategoryAccessControl.DELETE);
                }
                return new SimpleCategory(id, category.getSystemName());
            } else {
                //unable to read the category
                return null;
            }


        });
        if (simpleCategory != null) {
            metadataAccess.commit(() -> {
                categoryProvider.deleteById(simpleCategory.getCategoryId());
            }, MetadataAccess.SERVICE);
            notifyCategoryChange(simpleCategory.getCategoryId(), simpleCategory.getCategoryName(), MetadataChange.ChangeType.DELETE);
            return true;
        } else {
            return false;
        }
    }

    @Nonnull
    @Override
    public Set<UserField> getUserFields() {
        return metadataAccess.read(() -> {
            boolean hasPermission = this.accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_CATEGORIES);
            return hasPermission ? UserPropertyTransform.toUserFields(categoryProvider.getUserFields()) : Collections.emptySet();
        });
    }

    @Override
    public void setUserFields(@Nonnull Set<UserField> userFields) {
        boolean hasPermission = this.accessController.hasPermission(AccessController.SERVICES, NflowServicesAccessControl.ADMIN_CATEGORIES);
        if (hasPermission) {
            categoryProvider.setUserFields(UserPropertyTransform.toUserFieldDescriptors(userFields));
        }
    }

    @Nonnull
    @Override
    public Set<UserProperty> getUserProperties() {
        return metadataAccess.read(() -> {
            this.accessController.checkPermission(AccessController.SERVICES, NflowServicesAccessControl.ACCESS_CATEGORIES);

            return UserPropertyTransform.toUserProperties(Collections.emptyMap(), categoryProvider.getUserFields());
        });
    }

    private class SimpleCategory {

        private Category.ID categoryId;
        private String categoryName;

        public SimpleCategory(Category.ID categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        public Category.ID getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }
    }

    /**
     * update the audit information for nflow state changes
     *
     * @param categoryId   the category id
     * @param categoryName the categoryName
     * @param changeType   the event type
     */
    private void notifyCategoryChange(Category.ID categoryId, String categoryName, MetadataChange.ChangeType changeType) {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication() != null
                                    ? SecurityContextHolder.getContext().getAuthentication()
                                    : null;
        CategoryChange change = new CategoryChange(changeType, categoryName, categoryId);
        CategoryChangeEvent event = new CategoryChangeEvent(change, DateTime.now(), principal);
        metadataEventService.notify(event);
        clusterService.sendMessageToOthers(CategoryChange.CLUSTER_EVENT_TYPE, change);
    }
}
