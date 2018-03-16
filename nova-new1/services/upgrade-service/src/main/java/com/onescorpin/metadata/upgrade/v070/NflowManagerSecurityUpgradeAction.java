/**
 * 
 */
package com.onescorpin.metadata.upgrade.v070;

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
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.config.ActionsModuleBuilder;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

/**
 * Adds the services-level permissions for the nflow manager.
 */
@Component("nflowManagerSecurityUpgradeAction070")
@Order(700)  // Order only relevant during fresh installs
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class NflowManagerSecurityUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(NflowManagerSecurityUpgradeAction.class);

    @Inject
    private ActionsModuleBuilder builder;


    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.7", "0", "");
    }
    
    @Override
    public boolean isTargetFreshInstall() {
        return true;
    }

    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Defining nflow manager datasource permissions for version: {}", startingVersion);

        //@formatter:off
        builder
            .module(AllowedActions.SERVICES)
                .action(NflowServicesAccessControl.ACCESS_DATASOURCES)
                .action(NflowServicesAccessControl.EDIT_DATASOURCES)
                .action(NflowServicesAccessControl.ADMIN_DATASOURCES)
                .add()
            .build();
        //@formatter:on
    }

}
