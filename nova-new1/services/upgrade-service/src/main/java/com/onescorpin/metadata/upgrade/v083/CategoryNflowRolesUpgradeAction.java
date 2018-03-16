package com.onescorpin.metadata.upgrade.v083;

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

import com.onescorpin.NovaVersion;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.modeshape.category.CategoryDetails;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.inject.Inject;

/**
 * Ensures that all categories have the new, mandatory nflowRoleMemberships node.
 */
@Component("categoryNflowRolesUpgradeAction083")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class CategoryNflowRolesUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(CategoryNflowRolesUpgradeAction.class);

    @Inject
    private AccessController accessController;
    
    @Inject
    private CategoryProvider categoryProvider;

    @Inject
    private SecurityRoleProvider roleProvider;

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "3", "");
    }

    @Override
    public void upgradeTo(final NovaVersion startingVersion) {
        log.info("Upgrading category nflow roles from version: {}", startingVersion);

        if (this.accessController.isEntityAccessControlled()) {
            final List<SecurityRole> nflowRoles = this.roleProvider.getEntityRoles(SecurityRole.NFLOW);
            
            this.categoryProvider.findAll().stream()
                .map(JcrCategory.class::cast)
                .map(cat -> cat.getDetails().get())
                .filter(details -> ! JcrUtil.hasNode(details.getNode(), CategoryDetails.NFLOW_ROLE_MEMBERSHIPS))
                .forEach(details -> { 
                    log.info("Updating roles for category: {}", JcrUtil.getName(JcrUtil.getParent(details.getNode())));
                    details.enableNflowRoles(nflowRoles); 
                    });
        }
    }
}
