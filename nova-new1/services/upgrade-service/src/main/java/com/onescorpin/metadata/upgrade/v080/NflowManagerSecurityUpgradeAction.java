/**
 * 
 */
package com.onescorpin.metadata.upgrade.v080;

/*-
 * #%L
 * nova-upgrade-service
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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.onescorpin.NovaVersion;
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.datasource.security.DatasourceAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.template.security.TemplateAccessControl;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.config.ActionsModuleBuilder;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

/**
 * Adds the entity-level permissions for the nflow manager.
 */
@Component("nflowManagerSecurityUpgradeAction080")
@Order(800)  // Order only relevant during fresh installs
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class NflowManagerSecurityUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(NflowManagerSecurityUpgradeAction.class);

    @Inject
    private ActionsModuleBuilder builder;


    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "0", "");
    }
    
    @Override
    public boolean isTargetFreshInstall() {
        return true;
    }

    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Defining nflow manager entity permissions for version: {}", startingVersion);

        //@formatter:off
        builder
            .module(AllowedActions.SERVICES)
                .action(NflowServicesAccessControl.ACCESS_TABLES)
                .action(NflowServicesAccessControl.ACCESS_VISUAL_QUERY)
                .action(NflowServicesAccessControl.ACCESS_SERVICE_LEVEL_AGREEMENTS)
                .action(NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENTS)
                .add()
            .module(AllowedActions.NFLOW)
                .action(NflowAccessControl.ACCESS_NFLOW)
                .action(NflowAccessControl.EDIT_SUMMARY)
                .action(NflowAccessControl.ACCESS_DETAILS)
                .action(NflowAccessControl.EDIT_DETAILS)
                .action(NflowAccessControl.DELETE)
                .action(NflowAccessControl.ENABLE_DISABLE)
                .action(NflowAccessControl.EXPORT)
                .action(NflowAccessControl.ACCESS_OPS)
                .action(NflowAccessControl.CHANGE_PERMS)
                .add()
            .module(AllowedActions.CATEGORY)
                .action(CategoryAccessControl.ACCESS_CATEGORY)
                .action(CategoryAccessControl.EDIT_SUMMARY)
                .action(CategoryAccessControl.ACCESS_DETAILS)
                .action(CategoryAccessControl.EDIT_DETAILS)
                .action(CategoryAccessControl.DELETE)
                .action(CategoryAccessControl.CREATE_NFLOW)
                .action(CategoryAccessControl.CHANGE_PERMS)
                .add()
            .module(AllowedActions.TEMPLATE)
                .action(TemplateAccessControl.ACCESS_TEMPLATE)
                .action(TemplateAccessControl.EDIT_TEMPLATE)
                .action(TemplateAccessControl.DELETE)
                .action(TemplateAccessControl.EXPORT)
                .action(TemplateAccessControl.CREATE_NFLOW)
                .action(TemplateAccessControl.CHANGE_PERMS)
                .add()
            .module(AllowedActions.DATASOURCE)
                .action(DatasourceAccessControl.ACCESS_DATASOURCE)
                .action(DatasourceAccessControl.EDIT_SUMMARY)
                .action(DatasourceAccessControl.ACCESS_DETAILS)
                .action(DatasourceAccessControl.EDIT_DETAILS)
                .action(DatasourceAccessControl.DELETE)
                .action(DatasourceAccessControl.CHANGE_PERMS)
                .add()
            .build();
        //@formatter:on
    }

}
