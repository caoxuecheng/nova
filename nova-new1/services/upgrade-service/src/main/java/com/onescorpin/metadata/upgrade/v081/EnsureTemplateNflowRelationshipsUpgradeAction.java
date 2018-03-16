package com.onescorpin.metadata.upgrade.v081;

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
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.datasource.security.DatasourceAccessControl;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.security.RoleNotFoundException;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.api.template.security.TemplateAccessControl;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.template.JcrNflowTemplate;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.Action;
import com.onescorpin.security.action.AllowableAction;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.jcr.ItemNotFoundException;

/**
 * This action is upgraded both on upgrade to v0.8.1 and during a fresh install.
 */
@Component("ensureTemplateNflowRelationshipsUpgradeAction081")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class EnsureTemplateNflowRelationshipsUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(EnsureTemplateNflowRelationshipsUpgradeAction.class);

    @Inject
    private CategoryProvider categoryProvider;
    @Inject
    private NflowProvider nflowProvider;
    @Inject
    private NflowManagerTemplateProvider nflowManagerTemplateProvider;
    @Inject
    private SecurityRoleProvider roleProvider;
    @Inject
    private AllowedEntityActionsProvider actionsProvider;
    @Inject
    private AccessController accessController;
    

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "1", "");
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.upgrade.UpgradeState#upgradeFrom(com.onescorpin.metadata.api.app.NovaVersion)
     */
    @Override
    public void upgradeTo(NovaVersion startingVersion) {
        log.info("Upgrading template nflow relationships from version: " + startingVersion);
        
        ensureNflowTemplateNflowRelationships();
    }
    
    private void ensureNflowTemplateNflowRelationships() {
        //ensure the templates have the nflow relationships
        List<Nflow> nflows = nflowProvider.findAll();
        if (nflows != null) {
            nflows.stream().forEach(nflow -> {
                NflowManagerTemplate template = nflow.getTemplate();
                if (template != null) {
                    //ensure the template has nflows.
                    List<Nflow> templateNflows = null;
                    try {
                        templateNflows = template.getNflows();
                    } catch (MetadataRepositoryException e) {
                        //templateNflows are weak references.
                        //if the template nflows return itemNotExists we need to reset it
                        Throwable rootCause = ExceptionUtils.getRootCause(e);
                        if (rootCause != null && rootCause instanceof ItemNotFoundException) {
                            //reset the reference collection.  It will be rebuilt in the subsequent call
                            JcrPropertyUtil.removeAllFromSetProperty(((JcrNflowTemplate) template).getNode(), JcrNflowTemplate.NFLOWS);
                        }
                    }
                    if (templateNflows == null || !templateNflows.contains(nflow)) {
                        log.info("Updating relationship temlate: {} -> nflow: {}", template.getName(), nflow.getName());
                        template.addNflow(nflow);
                        nflowManagerTemplateProvider.update(template);
                    }
                }
            });

        }

        nflowProvider.populateInverseNflowDependencies();
    }
}
