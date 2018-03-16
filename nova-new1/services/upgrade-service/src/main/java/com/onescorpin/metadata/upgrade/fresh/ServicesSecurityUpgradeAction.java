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
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.common.SecurityPaths;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedEntityActionsProvider;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.jcr.Node;

@Component("servicesSecurityUpgradeActionFreshInstall")
@Order(Ordered.LOWEST_PRECEDENCE - 1)  // Must execute before CreateDefaultUsersGroupsAction
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class ServicesSecurityUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(ServicesSecurityUpgradeAction.class);

    @Inject
    private JcrAllowedEntityActionsProvider actionsProvider;

    @Override
    public boolean isTargetFreshInstall() {
        return true;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.upgrade.UpgradeState#upgradeFrom(com.onescorpin.metadata.api.app.NovaVersion)
     */
    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Setting up Services permissions for version: " + startingVersion);
        
        Node securityNode = JcrUtil.getNode(JcrMetadataAccess.getActiveSession(), SecurityPaths.SECURITY.toString());
        Node svcAllowedNode = JcrUtil.getOrCreateNode(securityNode, AllowedActions.SERVICES, JcrAllowedActions.NODE_TYPE);

        actionsProvider.createEntityAllowedActions(AllowedActions.SERVICES, svcAllowedNode);
    }
}
