package com.onescorpin.metadata.upgrade.v040;

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
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.jpa.nflow.JpaOpsManagerNflow;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;
import com.onescorpin.support.NflowNameUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@Component("nflowOpspgradeAction040")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class NflowOpsUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(NflowOpsUpgradeAction.class);

    @Inject
    private NflowProvider nflowProvider;
    @Inject
    private CategoryProvider categoryProvider;
    @Inject
    private OpsManagerNflowProvider opsManagerNflowProvider;

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.4", "0", "");
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.upgrade.UpgradeState#upgradeFrom(com.onescorpin.metadata.api.app.NovaVersion)
     */
    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Upgrading from version: " + startingVersion);
        
        for (Category category : categoryProvider.findAll()) {
            // Ensure each category has an allowedActions (gets create if not present.)
            category.getAllowedActions();
        }

        // get all nflows defined in nflow manager
        List<Nflow> domainNflows = nflowProvider.findAll();
        Map<String, Nflow> nflowManagerNflowMap = new HashMap<>();
        if (domainNflows != null && !domainNflows.isEmpty()) {
            List<OpsManagerNflow.ID> opsManagerNflowIds = new ArrayList<OpsManagerNflow.ID>();
            for (Nflow nflowManagerNflow : domainNflows) {
                opsManagerNflowIds.add(opsManagerNflowProvider.resolveId(nflowManagerNflow.getId().toString()));
                nflowManagerNflowMap.put(nflowManagerNflow.getId().toString(), nflowManagerNflow);

                // Ensure each nflow has an allowedActions (gets create if not present.)
                nflowManagerNflow.getAllowedActions();
            }
            //find those that match
            List<? extends OpsManagerNflow> opsManagerNflows = opsManagerNflowProvider.findByNflowIds(opsManagerNflowIds);
            if (opsManagerNflows != null) {
                for (OpsManagerNflow opsManagerNflow : opsManagerNflows) {
                    nflowManagerNflowMap.remove(opsManagerNflow.getId().toString());
                }
            }

            List<OpsManagerNflow> nflowsToAdd = new ArrayList<>();
            for (Nflow nflow : nflowManagerNflowMap.values()) {
                String fullName = NflowNameUtil.fullName(nflow.getCategory().getSystemName(), nflow.getName());
                OpsManagerNflow.ID opsManagerNflowId = opsManagerNflowProvider.resolveId(nflow.getId().toString());
                OpsManagerNflow opsManagerNflow = new JpaOpsManagerNflow(opsManagerNflowId, fullName);
                nflowsToAdd.add(opsManagerNflow);
            }
            log.info("Synchronizing Nflows from Nflow Manager. About to insert {} nflow ids/names into Operations Manager", nflowsToAdd.size());
            opsManagerNflowProvider.save(nflowsToAdd);
        }
    }
}
