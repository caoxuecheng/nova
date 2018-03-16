/**
 *
 */
package com.onescorpin.metadata.modeshape.security.role;

/*-
 * #%L
 * nova-metadata-modeshape
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

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.PostMetadataConfigAction;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.modeshape.security.CheckEntityAccessControlAction;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.config.ActionsModuleBuilder;
import com.onescorpin.security.role.SecurityRoleProvider;

/**
 *
 */
@Configuration
public class JcrSecurityRoleProviderTestConfig {

    @Inject
    private ActionsModuleBuilder builder;
    
    @Inject
    private MetadataAccess metadataAccess;

    @Bean
    public PostMetadataConfigAction configAuthorization() {
        return () -> metadataAccess.commit(() -> {
            //@formatter:off

            return builder
                            .module(AllowedActions.NFLOW)
                                .action(NflowAccessControl.ACCESS_NFLOW)
                                .action(NflowAccessControl.EDIT_SUMMARY)
                                .action(NflowAccessControl.ACCESS_DETAILS)
                                .action(NflowAccessControl.EDIT_DETAILS)
                                .action(NflowAccessControl.DELETE)
                                .action(NflowAccessControl.ENABLE_DISABLE)
                                .action(NflowAccessControl.EXPORT)
    //                            .action(NflowAccessControl.SCHEDULE_NFLOW)
                                .action(NflowAccessControl.ACCESS_OPS)
                                .action(NflowAccessControl.CHANGE_PERMS)
                                .add()
                            .module(AllowedActions.CATEGORY)
                                .action(CategoryAccessControl.ACCESS_CATEGORY)
                                .action(CategoryAccessControl.EDIT_SUMMARY)
                                .action(CategoryAccessControl.ACCESS_DETAILS)
                                .action(CategoryAccessControl.EDIT_DETAILS)
                                .action(CategoryAccessControl.DELETE)
                                .action(CategoryAccessControl.CREATE_NFLOW)
                                .action(CategoryAccessControl.CHANGE_PERMS)
                                .add()
                            .build();

            //@formatter:on
        }, MetadataAccess.SERVICE);
    }
    

    @Bean
    @Primary
    public SecurityRoleProvider roleProvider() {
        return new JcrSecurityRoleProvider();
    }
    
    @Bean
    @Primary
    @Order(PostMetadataConfigAction.EARLY_ORDER)
    public PostMetadataConfigAction checkEntityAccessControl() {
        // Mock this action so that the default roles do not get created.
        return Mockito.mock(PostMetadataConfigAction.class);
    }

}
