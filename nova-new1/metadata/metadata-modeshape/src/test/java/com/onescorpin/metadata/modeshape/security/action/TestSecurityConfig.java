/**
 *
 */
package com.onescorpin.metadata.modeshape.security.action;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.PostMetadataConfigAction;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.common.SecurityPaths;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.GroupPrincipal;
import com.onescorpin.security.action.Action;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.config.ActionsModuleBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Principal;

import javax.inject.Inject;
import javax.jcr.Node;

/**
 *
 */
@Configuration
public class TestSecurityConfig {

    public static final Action MANAGE_AUTH = Action.create("manageAuthorization");
    public static final Action MANAGE_OPS = Action.create("manageOperations");
    public static final Action ADMIN_OPS = MANAGE_OPS.subAction("adminOperations");
    public static final Action NFLOW_SUPPORT = Action.create("accessNflowSupport");
    public static final Action ACCESS_CATEGORIES = NFLOW_SUPPORT.subAction("categoryAccess");
    public static final Action CREATE_CATEGORIES = ACCESS_CATEGORIES.subAction("createCategories");
    public static final Action ADMIN_CATEGORIES = ACCESS_CATEGORIES.subAction("adminCategories");
    public static final Action ACCESS_NFLOWS = NFLOW_SUPPORT.subAction("accessNflows");
    public static final Action CREATE_NFLOWS = ACCESS_NFLOWS.subAction("createNflows");
    public static final Action IMPORT_NFLOWS = ACCESS_NFLOWS.subAction("importNflows");
    public static final Action EXPORT_NFLOWS = ACCESS_NFLOWS.subAction("exportNflows");
    public static final Action ADMIN_NFLOWS = ACCESS_NFLOWS.subAction("adminNflows");
    public static final Action ACCESS_TEMPLATES = NFLOW_SUPPORT.subAction("accessTemplates");
    public static final Action CREATE_TEMPLATESS = ACCESS_TEMPLATES.subAction("adminTemplates");
    public static final Action ADMIN_TEMPLATES = ACCESS_TEMPLATES.subAction("adminCategories");

    public static final Principal ADMIN = new GroupPrincipal("admin");
    public static final Principal TEST = new GroupPrincipal("test");

    @Inject
    private MetadataAccess metadata;

    @Inject
    private ActionsModuleBuilder builder;
    
    
    @Bean
    public JcrAllowedEntityActionsProvider allowedEntityActionsProvider() {
        return new JcrAllowedEntityActionsProvider();
    }    

    @Bean
    public PostMetadataConfigAction configAuthorization() {
        return () -> metadata.commit(() -> {
            // JcrTool tool = new JcrTool(true);
            // tool.printSubgraph(JcrMetadataAccess.getActiveSession(), "/metadata");

            //@formatter:off
            builder
                .module(AllowedActions.SERVICES)
                    .action(MANAGE_AUTH)
                    .action(MANAGE_OPS)
                    .action(ADMIN_OPS)
                    .action(NFLOW_SUPPORT)
                    .action(ACCESS_CATEGORIES)
                    .action(CREATE_CATEGORIES)
                    .action(ADMIN_CATEGORIES)
                    .action(ACCESS_NFLOWS)
                    .action(CREATE_NFLOWS)
                    .action(ADMIN_NFLOWS)
                    .action(IMPORT_NFLOWS)
                    .action(EXPORT_NFLOWS)
                    .action(ACCESS_TEMPLATES)
                    .action(CREATE_TEMPLATESS)
                    .action(ADMIN_TEMPLATES)
                    .add()
                .build();
            //@formatter:on
            
            Node securityNode = JcrUtil.getNode(JcrMetadataAccess.getActiveSession(), SecurityPaths.SECURITY.toString());
            Node svcAllowedNode = JcrUtil.getOrCreateNode(securityNode, AllowedActions.SERVICES, JcrAllowedActions.NODE_TYPE);

            allowedEntityActionsProvider().createEntityAllowedActions(AllowedActions.SERVICES, svcAllowedNode);

        }, MetadataAccess.SERVICE);
    }
}
