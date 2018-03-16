/**
 * 
 */
package com.onescorpin.metadata.modeshape.security;

/*-
 * #%L
 * nova-metadata-modeshape
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

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.jcr.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.PostMetadataConfigAction;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.datasource.security.DatasourceAccessControl;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.security.RoleNotFoundException;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.api.template.security.TemplateAccessControl;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.common.SecurityPaths;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.metadata.modeshape.template.JcrNflowTemplate;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.Action;
import com.onescorpin.security.action.AllowableAction;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;

/**
 * An action invoked early post-metadata configuration that checks if access control has been enabled or disabled.
 * If it was enabled when previously disabled then roles are setup.  If it was disabled when previously enabled then
 * an exception is thrown to fail startup.
 */
@Order(PostMetadataConfigAction.EARLY_ORDER)
public class CheckEntityAccessControlAction implements PostMetadataConfigAction {

    private static final Logger log = LoggerFactory.getLogger(CheckEntityAccessControlAction.class);
    
    @Inject
    private MetadataAccess metadata;

    @Inject
    private AccessController accessController;
    
    @Inject
    private SecurityRoleProvider roleProvider;
    
    @Inject
    private AllowedEntityActionsProvider actionsProvider;

    @Inject
    private CategoryProvider categoryProvider;
    
    @Inject
    private NflowProvider nflowProvider;
    
    @Inject
    private NflowManagerTemplateProvider nflowManagerTemplateProvider;

    
    @Override
    public void run() {
        metadata.commit(() -> {
            Node securityNode = JcrUtil.getNode(JcrMetadataAccess.getActiveSession(), SecurityPaths.SECURITY.toString());
            boolean propertyEnabled = accessController.isEntityAccessControlled();
            boolean metadataEnabled = wasAccessControllEnabled(securityNode);
            
            if (metadataEnabled == true && propertyEnabled == false) {
                log.error(  "\n*************************************************************************************************************************************\n"
                            + "Nova has previously been started with entity access control enabled and the current configuration is attempting to set it as disabled\n"
                            + "*************************************************************************************************************************************");
                throw new IllegalStateException("Entity access control is configured as disabled when it was previously enabled");
            } else if (metadataEnabled == false && propertyEnabled == true) {
                ensureDefaultEntityRoles();
            }

            JcrPropertyUtil.setProperty(securityNode, SecurityPaths.ENTITY_ACCESS_CONTROL_ENABLED, propertyEnabled);
        }, MetadataAccess.SERVICE);
    }

    private boolean wasAccessControllEnabled(Node securityNode) {
        if (JcrPropertyUtil.hasProperty(securityNode, SecurityPaths.ENTITY_ACCESS_CONTROL_ENABLED)) {
            return JcrPropertyUtil.getBoolean(securityNode, SecurityPaths.ENTITY_ACCESS_CONTROL_ENABLED);
        } else {
            return roleProvider.getRoles().values().stream().anyMatch(roles -> roles.size() > 0);
        }
    }


    private void ensureDefaultEntityRoles() {
        createDefaultRoles();

        ensureTemplateAccessControl();
        ensureCategoryAccessControl();
        ensureNflowAccessControl();
    }

    private void createDefaultRoles() {
        // Create default roles
        SecurityRole nflowEditor = createDefaultRole(SecurityRole.NFLOW, "editor", "Editor", "Allows a user to edit, enable/disable, delete and export nflow. Allows access to job operations for nflow. If role inherited via a category, allows these operations for nflows under that category.",
                                                    NflowAccessControl.EDIT_DETAILS,
                                                    NflowAccessControl.DELETE,
                                                    NflowAccessControl.ACCESS_OPS,
                                                    NflowAccessControl.ENABLE_DISABLE,
                                                    NflowAccessControl.EXPORT);

        //admin can do everything the editor does + change perms
        createDefaultRole(SecurityRole.NFLOW, "admin", "Admin", "All capabilities defined in the 'Editor' role along with the ability to change the permissions", nflowEditor,
                          NflowAccessControl.CHANGE_PERMS);

        createDefaultRole(SecurityRole.NFLOW, "readOnly", "Read-Only", "Allows a user to view the nflow and access job operations",
                          NflowAccessControl.ACCESS_DETAILS,
                          NflowAccessControl.ACCESS_OPS);

        SecurityRole templateEditor = createDefaultRole(SecurityRole.TEMPLATE, "editor", "Editor", "Allows a user to edit,export a template",
                                                        TemplateAccessControl.ACCESS_TEMPLATE,
                                                        TemplateAccessControl.EDIT_TEMPLATE,
                                                        TemplateAccessControl.DELETE,
                                                        TemplateAccessControl.CREATE_NFLOW,
                                                        TemplateAccessControl.EXPORT);
        createDefaultRole(SecurityRole.TEMPLATE, "admin", "Admin", "All capabilities defined in the 'Editor' role along with the ability to change the permissions", templateEditor,
                          TemplateAccessControl.CHANGE_PERMS);

        createDefaultRole(SecurityRole.TEMPLATE, "readOnly", "Read-Only", "Allows a user to view the template", TemplateAccessControl.ACCESS_TEMPLATE);

        SecurityRole categoryEditor = createDefaultRole(SecurityRole.CATEGORY, "editor", "Editor", "Allows a user to edit, export and delete category. Allows creating nflows under the category",
                                                        CategoryAccessControl.ACCESS_CATEGORY,
                                                        CategoryAccessControl.EDIT_DETAILS,
                                                        CategoryAccessControl.EDIT_SUMMARY,
                                                        CategoryAccessControl.CREATE_NFLOW,
                                                        CategoryAccessControl.DELETE);

        createDefaultRole(SecurityRole.CATEGORY, "admin", "Admin", "All capabilities defined in the 'Editor' role along with the ability to change the permissions", categoryEditor,
                          CategoryAccessControl.CHANGE_PERMS);

        createDefaultRole(SecurityRole.CATEGORY, "readOnly", "Read-Only", "Allows a user to view the category", CategoryAccessControl.ACCESS_CATEGORY);

        createDefaultRole(SecurityRole.CATEGORY, "nflowCreator", "Nflow Creator", "Allows a user to create a new nflow using this category", CategoryAccessControl.ACCESS_DETAILS,
                          CategoryAccessControl.CREATE_NFLOW);

        final SecurityRole datasourceEditor = createDefaultRole(SecurityRole.DATASOURCE, "editor", "Editor", "Allows a user to edit,delete datasources",
                                                                DatasourceAccessControl.ACCESS_DATASOURCE,
                                                                DatasourceAccessControl.EDIT_DETAILS,
                                                                DatasourceAccessControl.EDIT_SUMMARY,
                                                                DatasourceAccessControl.DELETE);
        createDefaultRole(SecurityRole.DATASOURCE, "admin", "Admin", "All capabilities defined in the 'Editor' role along with the ability to change the permissions", datasourceEditor,
                          DatasourceAccessControl.CHANGE_PERMS);
        createDefaultRole(SecurityRole.DATASOURCE, "readOnly", "Read-Only", "Allows a user to view the datasource", DatasourceAccessControl.ACCESS_DATASOURCE);
    }
    
    protected SecurityRole createDefaultRole(@Nonnull final String entityName, @Nonnull final String roleName, @Nonnull final String title, final String desc, @Nonnull final SecurityRole baseRole,
                                             final Action... actions) {
        final Stream<Action> baseActions = baseRole.getAllowedActions().getAvailableActions().stream().flatMap(AllowableAction::stream);
        final Action[] allowedActions = Stream.concat(baseActions, Stream.of(actions)).toArray(Action[]::new);
        return createDefaultRole(entityName, roleName, title, desc, allowedActions);
    }

    protected SecurityRole createDefaultRole(String entityName, String roleName, String title, String desc, Action... actions) {
        Supplier<SecurityRole> createIfNotFound = () -> {
            SecurityRole role = roleProvider.createRole(entityName, roleName, title, desc);
            role.setPermissions(actions);
            return role;
        };

        Function<SecurityRole, SecurityRole> ensureActions = (role) -> {
            role.setDescription(desc);
            if (actions != null) {
                List<Action> actionsList = Arrays.asList(actions);
                boolean needsUpdate = actionsList.stream().anyMatch(action -> !role.getAllowedActions().hasPermission(action));
                if (needsUpdate) {
                    role.setPermissions(actions);
                }
            }
            return role;
        };

        try {
            return roleProvider.getRole(entityName, roleName).map(ensureActions).orElseGet(createIfNotFound);

        } catch (RoleNotFoundException e) {
            return createIfNotFound.get();
        }
    }

    private void ensureNflowAccessControl() {
        List<Nflow> nflows = nflowProvider.findAll();
        if (nflows != null) {
            List<SecurityRole> roles = this.roleProvider.getEntityRoles(SecurityRole.NFLOW);
            Optional<AllowedActions> allowedActions = this.actionsProvider.getAvailableActions(AllowedActions.NFLOW);
            nflows.stream().forEach(nflow -> {
                Principal owner = nflow.getOwner() != null ? nflow.getOwner() : JcrMetadataAccess.getActiveUser();
                allowedActions.ifPresent(actions -> ((JcrNflow) nflow).enableAccessControl((JcrAllowedActions) actions, owner, roles));
            });
        }
    }

    private void ensureCategoryAccessControl() {
        List<Category> categories = categoryProvider.findAll();
        if (categories != null) {
            List<SecurityRole> catRoles = this.roleProvider.getEntityRoles(SecurityRole.CATEGORY);
            List<SecurityRole> nflowRoles = this.roleProvider.getEntityRoles(SecurityRole.NFLOW);

            Optional<AllowedActions> allowedActions = this.actionsProvider.getAvailableActions(AllowedActions.CATEGORY);
            categories.stream().forEach(category -> {
                Principal owner = category.getOwner() != null ? category.getOwner() : JcrMetadataAccess.getActiveUser();
                allowedActions.ifPresent(actions -> ((JcrCategory) category).enableAccessControl((JcrAllowedActions) actions, owner, catRoles, nflowRoles));
            });
        }
    }

    private void ensureTemplateAccessControl() {
        List<NflowManagerTemplate> templates = nflowManagerTemplateProvider.findAll();
        if (templates != null) {
            List<SecurityRole> roles = this.roleProvider.getEntityRoles(SecurityRole.TEMPLATE);
            Optional<AllowedActions> allowedActions = this.actionsProvider.getAvailableActions(AllowedActions.TEMPLATE);
            templates.stream().forEach(template -> {
                Principal owner = template.getOwner() != null ? template.getOwner() : JcrMetadataAccess.getActiveUser();
                allowedActions.ifPresent(actions -> ((JcrNflowTemplate) template).enableAccessControl((JcrAllowedActions) actions, owner, roles));
            });
        }
    }

}
