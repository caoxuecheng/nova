package com.onescorpin.metadata.modeshape.category;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.extension.ExtensibleType;
import com.onescorpin.metadata.api.extension.ExtensibleTypeProvider;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.modeshape.BaseJcrProvider;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.common.EntityUtil;
import com.onescorpin.metadata.modeshape.common.JcrEntity;
import com.onescorpin.metadata.modeshape.common.JcrObject;
import com.onescorpin.metadata.modeshape.extension.ExtensionsConstants;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedEntityActionsProvider;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.support.JcrQueryUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * A JCR provider for {@link Category} objects.
 */
public class JcrCategoryProvider extends BaseJcrProvider<Category, Category.ID> implements CategoryProvider {

    /**
     * JCR node type manager
     */
    @Inject
    ExtensibleTypeProvider extensibleTypeProvider;

    @Inject
    private SecurityRoleProvider roleProvider;

    @Inject
    private JcrAllowedEntityActionsProvider actionsProvider;

    @Inject
    private AccessController accessController;

    @Inject
    private NflowOpsAccessControlProvider opsAccessProvider;

    /**
     * Transaction support
     */
    @Inject
    MetadataAccess metadataAccess;

    @Override
    protected <T extends JcrObject> T constructEntity(Node node, Class<T> entityClass) {
        return JcrUtil.createJcrObject(node, entityClass, this.opsAccessProvider);
    }


    @Override
    protected String getEntityQueryStartingPath() {
        return  EntityUtil.pathForCategory();
    }

    @Override
    public Category update(Category category) {
        if (accessController.isEntityAccessControlled()) {
            category.getAllowedActions().checkPermission(CategoryAccessControl.EDIT_DETAILS);
        }
        return super.update(category);
    }

    @Override
    public Category findBySystemName(String systemName) {
        String query = "SELECT * FROM [" + getNodeType(getJcrEntityClass()) + "] as cat WHERE cat.[" + JcrCategory.SYSTEM_NAME + "] = $systemName ";
        query = applyFindAllFilter(query, EntityUtil.pathForCategory());
        return JcrQueryUtil.findFirst(getSession(), query, ImmutableMap.of("systemName", systemName), getEntityClass());
    }

    @Override
    public String getNodeType(Class<? extends JcrEntity> jcrEntityType) {
        return JcrCategory.NODE_TYPE;
    }

    @Override
    public Class<? extends Category> getEntityClass() {
        return JcrCategory.class;
    }

    @Override
    public Class<? extends JcrEntity> getJcrEntityClass() {
        return JcrCategory.class;
    }

    @Override
    public Category ensureCategory(String systemName) {
        String path = EntityUtil.pathForCategory();
        Map<String, Object> props = new HashMap<>();
        props.put(JcrCategory.SYSTEM_NAME, systemName);
        boolean isNew = !hasEntityNode(path, systemName);
        JcrCategory category = (JcrCategory) findOrCreateEntity(path, systemName, props);

        if (isNew) {
            if (this.accessController.isEntityAccessControlled()) {
                List<SecurityRole> catRoles = this.roleProvider.getEntityRoles(SecurityRole.CATEGORY);
                List<SecurityRole> nflowRoles = this.roleProvider.getEntityRoles(SecurityRole.NFLOW);

                this.actionsProvider.getAvailableActions(AllowedActions.CATEGORY)
                    .ifPresent(actions -> category.enableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser(), catRoles, nflowRoles));
            } else {
                this.actionsProvider.getAvailableActions(AllowedActions.CATEGORY)
                    .ifPresent(actions -> category.disableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser()));
            }
        }

        return category;
    }

    @Override
    public Category.ID resolveId(Serializable fid) {
        return new JcrCategory.CategoryId(fid);
    }

    @Override
    public void delete(final Category category) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteById(final Category.ID id) {
        // Get category
        final Category category = findById(id);

        if (category != null) {

            // Delete user type
            final ExtensibleType type = extensibleTypeProvider.getType(ExtensionsConstants.getUserCategoryNflow(category.getSystemName()));
            if (type != null) {
                extensibleTypeProvider.deleteType(type.getId());
            }

            // Delete category
            super.delete(category);
        }
    }

    @Nonnull
    @Override
    public Set<UserFieldDescriptor> getUserFields() {
        return JcrPropertyUtil.getUserFields(ExtensionsConstants.USER_CATEGORY, extensibleTypeProvider);
    }

    @Override
    public void setUserFields(@Nonnull final Set<UserFieldDescriptor> userFields) {
        metadataAccess.commit(() -> {
            JcrPropertyUtil.setUserFields(ExtensionsConstants.USER_CATEGORY, userFields, extensibleTypeProvider);
            return userFields;
        }, MetadataAccess.SERVICE);
    }

    @Nonnull
    @Override
    public Optional<Set<UserFieldDescriptor>> getNflowUserFields(@Nonnull final Category.ID categoryId) {
        return Optional.ofNullable(findById(categoryId))
            .map(category -> JcrPropertyUtil.getUserFields(ExtensionsConstants.getUserCategoryNflow(category.getSystemName()), extensibleTypeProvider));
    }

    @Override
    public void setNflowUserFields(@Nonnull final Category.ID categoryId, @Nonnull final Set<UserFieldDescriptor> userFields) {
        metadataAccess.commit(() -> {
            final Category category = findById(categoryId);
            setNflowUserFields(category.getSystemName(), userFields);
        }, MetadataAccess.SERVICE);
    }
    
    @Override
    public void renameSystemName(@Nonnull final Category.ID categoryId, @Nonnull final String newSystemName) {
        // Move the node to the new path
        JcrCategory category = (JcrCategory) findById(categoryId);
        String currentName = category.getSystemName();
        final Node node = category.getNode();

        // Update properties
        category.setSystemName(newSystemName);
        
        // Move user fields
        final Optional<Set<UserFieldDescriptor>> nflowUserFields = getNflowUserFields(category.getId());

        if (nflowUserFields.isPresent()) {
            final ExtensibleType type = extensibleTypeProvider.getType(ExtensionsConstants.getUserCategoryNflow(currentName));
            if (type != null) {
                extensibleTypeProvider.deleteType(type.getId());
            }

            setNflowUserFields(newSystemName, nflowUserFields.get());
        }

        try {
            final String newPath = JcrUtil.path(node.getParent().getPath(), newSystemName).toString();
            JcrMetadataAccess.getActiveSession().move(node.getPath(), newPath);
        } catch (final RepositoryException e) {
            throw new IllegalStateException("Unable to rename system name for category: " + node, e);
        }
    }

    private void setNflowUserFields(@Nonnull final String categorySystemName, @Nonnull final Set<UserFieldDescriptor> userFields) {
        JcrPropertyUtil.setUserFields(ExtensionsConstants.getUserCategoryNflow(categorySystemName), userFields, extensibleTypeProvider);
    }
}
