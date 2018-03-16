package com.onescorpin.metadata.upgrade.v084;

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
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.versioning.EntityVersion;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Ensures that all categories have the new, mandatory nflowRoleMemberships node.
 */
@Component("initialNflowVersionUpgradeAction084")
@Order(Ordered.LOWEST_PRECEDENCE)
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class InitialNflowVersionUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(InitialNflowVersionUpgradeAction.class);

    @Inject
    private NflowProvider nflowProvider;

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "4", "");
    }

    @Override
    public void upgradeTo(final NovaVersion startingVersion) {
        log.info("Upgrading nflows as versionable from version: {}", startingVersion);

        nflowProvider.getNflows().forEach(nflow -> {
            JcrNflow jcrNflow = (JcrNflow) nflow;
            Optional<List<EntityVersion<Nflow>>> versions = nflowProvider.findVersions(jcrNflow.getId(), false);
            
            versions.ifPresent(list -> {
                if (list.size() <= 1) {
                    jcrNflow.setDescription(jcrNflow.getDescription());
                    nflowProvider.update(jcrNflow);
                }
            });
        });
    }
}
