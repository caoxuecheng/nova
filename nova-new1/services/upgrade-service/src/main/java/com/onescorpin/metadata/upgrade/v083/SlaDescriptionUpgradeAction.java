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
import com.onescorpin.discovery.schema.TableSchema;
import com.onescorpin.nflowmgr.rest.model.schema.TableSetup;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerNflowService;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementService;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.datasource.DerivedDatasource;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementDescriptionProvider;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Ensures the SLA_DESCRIPTION table is populated with the SLA data from Modeshape
 */
@Component("slaUpgradeAction083")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class SlaDescriptionUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(SlaDescriptionUpgradeAction.class);

    @Inject
    private ServiceLevelAgreementService serviceLevelAgreementService;

    @Inject
    private ServiceLevelAgreementDescriptionProvider serviceLevelAgreementDescriptionProvider;

    @Inject
    private ServiceLevelAgreementProvider serviceLevelAgreementProvider;

    @Inject
    private NflowProvider nflowProvider;

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "3", "");
    }

    @Override
    public void upgradeTo(final NovaVersion startingVersion) {
        log.info("Adding SLA Descriptions.  Starting version: {}", startingVersion);
       serviceLevelAgreementService.getServiceLevelAgreements().stream().filter(sla -> !sla.getNflows().isEmpty())
           .forEach(nflowSla -> {
               Set<Nflow.ID> nflowIds = nflowSla.getNflows().stream().map(nflow -> nflowProvider.resolveId(nflow.getId())).collect(Collectors.toSet());
               serviceLevelAgreementDescriptionProvider.updateServiceLevelAgreement(serviceLevelAgreementProvider.resolve(nflowSla.getId()), nflowSla.getName(),nflowSla.getDescription(),nflowIds,null);
            });
    }
}
