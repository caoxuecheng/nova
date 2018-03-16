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
import com.onescorpin.nflowmgr.security.NflowServicesAccessControl;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.config.ActionsModuleBuilder;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component("slaEmailTemplateUpgradeAction084")
@Order(Ordered.LOWEST_PRECEDENCE)
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class ServiceLevelAgreementEmailTemplateUpgradeAction implements UpgradeState {

    @Inject
    private ActionsModuleBuilder builder;

    private static final Logger log = LoggerFactory.getLogger(ServiceLevelAgreementEmailTemplateUpgradeAction.class);

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "4", "");
    }

    @Override
    public boolean isTargetFreshInstall() {
        return true;
    }


    public void upgradeTo(final NovaVersion startingVersion) {
           builder.module(AllowedActions.SERVICES)
            .action(NflowServicesAccessControl.EDIT_SERVICE_LEVEL_AGREEMENT_EMAIL_TEMPLATE)
            .add()
            .build();
    }

}
