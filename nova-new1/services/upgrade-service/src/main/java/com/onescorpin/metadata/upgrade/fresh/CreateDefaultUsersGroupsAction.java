package com.onescorpin.metadata.upgrade.fresh;

/*-
 * #%L
 * nova-operational-metadata-upgrade-service
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

import com.onescorpin.NovaVersion;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.jobrepo.security.OperationsAccessControl;
import com.onescorpin.metadata.api.user.User;
import com.onescorpin.metadata.api.user.UserGroup;
import com.onescorpin.metadata.api.user.UserProvider;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeException;
import com.onescorpin.server.upgrade.UpgradeState;

import org.modeshape.jcr.api.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

import javax.inject.Inject;
import javax.jcr.RepositoryException;

@Component("UsersGroupsUpgradeActionFreshInstall")
@Order(Ordered.LOWEST_PRECEDENCE)
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class CreateDefaultUsersGroupsAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(CreateDefaultUsersGroupsAction.class);

    @Inject
    private UserProvider userProvider;
    @Inject
    private AllowedEntityActionsProvider actionsProvider;
    @Inject
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean isTargetFreshInstall() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.upgrade.UpgradeState#upgradeFrom(com.onescorpin.metadata.api.app.NovaVersion)
     */
    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Creating default users/groups for version: " + startingVersion);
        
        User dladmin = createDefaultUser("dladmin", "Data Lake Administrator", null);
        User analyst = createDefaultUser("analyst", "Analyst", null);
        User designer = createDefaultUser("designer", "Designer", null);
        User operator = createDefaultUser("operator", "Operator", null);

        // Create default groups if they don't exist.
        UserGroup adminsGroup = createDefaultGroup("admin", "Administrators");
        UserGroup opsGroup = createDefaultGroup("operations", "Operations");
        UserGroup designersGroup = createDefaultGroup("designers", "Designers");
        UserGroup analystsGroup = createDefaultGroup("analysts", "Analysts");
        UserGroup usersGroup = createDefaultGroup("user", "Users");

        // Add default users to their respective groups
        adminsGroup.addUser(dladmin);
        designersGroup.addUser(designer);
        analystsGroup.addUser(analyst);
        opsGroup.addUser(operator);
        usersGroup.addUser(dladmin);
        usersGroup.addUser(analyst);
        usersGroup.addUser(designer);
        usersGroup.addUser(operator);

        // Setup initial group access control.  Administrators group already has all rights.
        actionsProvider.getAllowedActions(AllowedActions.SERVICES)
            .ifPresent((allowed) -> {
                allowed.enable(opsGroup.getRootPrincial(),
                               OperationsAccessControl.ADMIN_OPS,
                               NflowServicesAccessControl.ACCESS_CATEGORIES,
                               NflowServicesAccessControl.ACCESS_NFLOWS,
                               NflowServicesAccessControl.ACCESS_TEMPLATES,
                               NflowServicesAccessControl.ACCESS_TABLES,
                               NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS);
                allowed.enable(designersGroup.getRootPrincial(),
                               OperationsAccessControl.ACCESS_OPS,
                               NflowServicesAccessControl.EDIT_NFLOWS,
                               NflowServicesAccessControl.ACCESS_TABLES,
                               NflowServicesAccessControl.IMPORT_NFLOWS,
                               NflowServicesAccessControl.EXPORT_NFLOWS,
                               NflowServicesAccessControl.EDIT_CATEGORIES,
                               NflowServicesAccessControl.EDIT_DATASOURCES,
                               NflowServicesAccessControl.EDIT_TEMPLATES,
                               NflowServicesAccessControl.IMPORT_TEMPLATES,
                               NflowServicesAccessControl.EXPORT_TEMPLATES,
                               NflowServicesAccessControl.ADMIN_TEMPLATES,
                               NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS,
                               NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENTS,
                               NflowServicesAccessControl.ACCESS_GLOBAL_SEARCH);
                allowed.enable(analystsGroup.getRootPrincial(),
                               OperationsAccessControl.ACCESS_OPS,
                               NflowServicesAccessControl.EDIT_NFLOWS,
                               NflowServicesAccessControl.ACCESS_TABLES,
                               NflowServicesAccessControl.IMPORT_NFLOWS,
                               NflowServicesAccessControl.EXPORT_NFLOWS,
                               NflowServicesAccessControl.EDIT_CATEGORIES,
                               NflowServicesAccessControl.ACCESS_TEMPLATES,
                               NflowServicesAccessControl.ACCESS_DATASOURCES,
                               NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS,
                               NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENTS,
                               NflowServicesAccessControl.ACCESS_GLOBAL_SEARCH);
            });
        
        try {
            Workspace workspace = (Workspace) JcrMetadataAccess.getActiveSession().getWorkspace();
            workspace.reindex("/users");
            workspace.reindex("/groups");
        } catch (RepositoryException e) {
            log.error("Failed to re-index metadata", e);
            throw new UpgradeException("Failed to re-index metadata", e);
        }
    }
    
    protected User createDefaultUser(String username, String displayName, String password) {
        Optional<User> userOption = userProvider.findUserBySystemName(username);
        User user = null;

        // Create the user if it doesn't exists.
        if (userOption.isPresent()) {
            user = userOption.get();
        } else {
            user = userProvider.ensureUser(username);
            if (password != null) {
                user.setPassword(passwordEncoder.encode(password));
            }
            user.setDisplayName(displayName);
        }

        return user;
    }

    protected UserGroup createDefaultGroup(String groupName, String title) {
        UserGroup newGroup = userProvider.ensureGroup(groupName);
        newGroup.setTitle(title);
        return newGroup;
    }
}
