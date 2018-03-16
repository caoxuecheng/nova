package com.onescorpin.metadata.upgrade.v0811;

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
import com.onescorpin.metadata.api.user.UserGroup;
import com.onescorpin.metadata.api.user.UserProvider;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component("upgradeAction0811")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class GroupNamesUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(GroupNamesUpgradeAction.class);

    @Inject
    private UserProvider userProvider;
    @Inject
    private AllowedEntityActionsProvider actionsProvider;

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "1", "1");
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.upgrade.UpgradeState#upgradeFrom(com.onescorpin.metadata.api.app.NovaVersion)
     */
    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Upgrading from version: " + startingVersion);
        
        this.userProvider.findGroupByName("designer")
            .ifPresent(oldGrp -> {
                UserGroup designersGroup = createDefaultGroup("designers", "Designers");
    
                oldGrp.getUsers().forEach(user -> designersGroup.addUser(user));
    
                actionsProvider.getAllowedActions(AllowedActions.SERVICES)
                    .ifPresent((allowed) -> {
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
                    });
    
                this.userProvider.deleteGroup(oldGrp);
            });

        this.userProvider.findGroupByName("analyst")
            .ifPresent(oldGrp -> {
                UserGroup analystsGroup = createDefaultGroup("analysts", "Analysts");
    
                oldGrp.getUsers().forEach(user -> analystsGroup.addUser(user));
    
                actionsProvider.getAllowedActions(AllowedActions.SERVICES)
                    .ifPresent((allowed) -> {
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
    
                this.userProvider.deleteGroup(oldGrp);
            });
    }

    protected UserGroup createDefaultGroup(String groupName, String title) {
        UserGroup newGroup = userProvider.ensureGroup(groupName);
        newGroup.setTitle(title);
        return newGroup;
    }
}
