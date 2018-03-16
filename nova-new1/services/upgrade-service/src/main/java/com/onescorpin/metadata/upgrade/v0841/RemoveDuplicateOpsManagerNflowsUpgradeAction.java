package com.onescorpin.metadata.upgrade.v0841;

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
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.versioning.EntityVersion;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Remove any records nova.NFLOW entries that have the same name, but dont exist in Modeshape
 */
@Component("removeDuplicateOpsManagerNflowsUpgradeAction084")
@Order(1)
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class RemoveDuplicateOpsManagerNflowsUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(RemoveDuplicateOpsManagerNflowsUpgradeAction.class);

    @Inject
    private OpsManagerNflowProvider opsManagerNflowProvider;

    @Inject
    private NflowProvider nflowProvider;

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "4", "1");
    }

    @Override
    public void upgradeTo(final NovaVersion startingVersion) {
        log.info("remove duplicate ops manager nflows from version: {}", startingVersion);

        List<? extends OpsManagerNflow> nflows = opsManagerNflowProvider.findNflowsWithSameName();
        if(nflows != null){
            final List<OpsManagerNflow> nflowsToDelete = new ArrayList<>();
            final Map<String,OpsManagerNflow> nflowsToKeep = new HashMap<>();

            nflows.stream().forEach(nflow -> {
                log.info("Found duplicate nflow {} - {} ",nflow.getId(),nflow.getName());
                Nflow jcrNflow = nflowProvider.getNflow(nflow.getId());
                if(jcrNflow  == null){
                    nflowsToDelete.add(nflow);
                }
                else {
                    nflowsToKeep.put(nflow.getName(),nflow);
                }

            });

            nflowsToDelete.stream().forEach(nflow -> {
                OpsManagerNflow nflowToKeep = nflowsToKeep.get(nflow.getName());
                if(nflowToKeep != null) {
                    //remove it
                    log.info("Unable to find nflow {} - {} in JCR metadata.  A nflow with id of {} already exists with this same name {}.  Attempt to remove its data from Operations Manager", nflow.getId(), nflow.getName(), nflowToKeep.getId(),nflowToKeep.getName());
                    opsManagerNflowProvider.delete(nflow.getId());
                }
            });
        }

    }
}
