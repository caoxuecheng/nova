package com.onescorpin.metadata.modeshape;

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

import com.onescorpin.alerts.api.AlertProvider;
import com.onescorpin.alerts.spi.AlertManager;
import com.onescorpin.auth.jaas.LoginConfiguration;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.PostMetadataConfigAction;
import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.datasource.security.DatasourceAccessControl;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.op.NflowOperationsProvider;
import com.onescorpin.metadata.api.template.security.TemplateAccessControl;
import com.onescorpin.metadata.modeshape.security.DefaultAccessController;
import com.onescorpin.scheduler.JobScheduler;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.config.ActionsModuleBuilder;

import org.mockito.Mockito;
import org.modeshape.jcr.RepositoryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.Nonnull;

/**
 * Defines mocks for most JCR-based providers and other components, and configures a ModeShape test repository.
 */
@Configuration
public class JcrTestConfig {

    @Bean
    public RepositoryConfiguration metadataRepoConfig() throws IOException {
        ClassPathResource res = new ClassPathResource("/test-metadata-repository.json");
        return RepositoryConfiguration.read(res.getURL());
    }

    @Bean(name = "servicesModeShapeLoginConfiguration")
    @Primary
    public LoginConfiguration restModeShapeLoginConfiguration() {
        return Mockito.mock(LoginConfiguration.class);
    }

    @Bean
    public NflowOpsAccessControlProvider opsAccessProvider() {
        return Mockito.mock(NflowOpsAccessControlProvider.class);
    }

    @Bean
    public NflowOperationsProvider nflowOperationsProvider() {
        return Mockito.mock(NflowOperationsProvider.class);
    }

    @Bean
    public JobScheduler jobSchedule() {
        return Mockito.mock(JobScheduler.class);
    }

    @Bean
    public AlertManager alertManager() {
        return Mockito.mock(AlertManager.class);
    }

    @Bean
    public AlertProvider alertProvider() {
        return Mockito.mock(AlertProvider.class);
    }

    @Bean
    public MetadataEventService metadataEventService() {
        return Mockito.mock(MetadataEventService.class);
    }

    @Bean
    public PostMetadataConfigAction nflowManagerSecurityConfigAction(@Nonnull final MetadataAccess metadata, @Nonnull final ActionsModuleBuilder builder) {
        //@formatter:off

        return () -> metadata.commit(() -> {
            return builder
                            .module(AllowedActions.NFLOW)
                                .action(NflowAccessControl.ACCESS_NFLOW)
                                .action(NflowAccessControl.EDIT_SUMMARY)
                                .action(NflowAccessControl.ACCESS_DETAILS)
                                .action(NflowAccessControl.EDIT_DETAILS)
                                .action(NflowAccessControl.DELETE)
                                .action(NflowAccessControl.ENABLE_DISABLE)
                                .action(NflowAccessControl.EXPORT)
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
                            .module(AllowedActions.TEMPLATE)
                                .action(TemplateAccessControl.ACCESS_TEMPLATE)
                                .action(TemplateAccessControl.EDIT_TEMPLATE)
                                .action(TemplateAccessControl.DELETE)
                                .action(TemplateAccessControl.EXPORT)
                                .action(TemplateAccessControl.CREATE_NFLOW)
                                .action(TemplateAccessControl.CHANGE_PERMS)
                                .add()
                            .module(AllowedActions.DATASOURCE)
                                .action(DatasourceAccessControl.ACCESS_DATASOURCE)
                                .action(DatasourceAccessControl.EDIT_SUMMARY)
                                .action(DatasourceAccessControl.ACCESS_DETAILS)
                                .action(DatasourceAccessControl.EDIT_DETAILS)
                                .action(DatasourceAccessControl.DELETE)
                                .action(DatasourceAccessControl.CHANGE_PERMS)
                                .add()
                            .build();
            }, MetadataAccess.SERVICE);

        // @formatter:on
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
        final Properties properties = new Properties();
        properties.setProperty("security.entity.access.controlled", "true");

        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(properties);
        return configurer;
    }
}
