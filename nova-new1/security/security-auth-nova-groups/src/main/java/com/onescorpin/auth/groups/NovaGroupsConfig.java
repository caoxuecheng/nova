package com.onescorpin.auth.groups;

/*-
 * #%L
 * nova-security-auth-nova-groups
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

import com.onescorpin.auth.jaas.LoginConfiguration;
import com.onescorpin.auth.jaas.LoginConfigurationBuilder;
import com.onescorpin.auth.jaas.config.JaasAuthConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Spring configuration for the Nova Groups Login Module.
 */
@Configuration
@Profile("auth-nova-groups")
public class NovaGroupsConfig {

    @Value("${security.auth.nova.login.flag:required}")
    private String loginFlag;

    /*
     * This should be of the highest order, i.e. be the last one to commit, otherwise if a user has no
     * groups and this is committed first then all of nova groups would be assigned to the user
     */
    @Value("${security.auth.nova.login.order:#{T(com.onescorpin.auth.jaas.LoginConfiguration).HIGHEST_ORDER}}")
    private int loginOrder;

    @Bean(name = "novaGroupsLoginRestClientConfig")
    @ConfigurationProperties(prefix = "loginRestClientConfig")
    public LoginJerseyClientConfig loginRestClientConfig() {
        return new LoginJerseyClientConfig();
    }

    @Inject
    private LoginJerseyClientConfig loginRestClientConfig;

    @Value("${security.auth.nova.login.username:#{null}}")
    private String loginUser;

    @Value("${security.auth.nova.login.password:#{null}}")
    private String loginPassword;

    /**
     * Creates a new UI login configuration for the REST Login Module.
     *
     * @param builder the login configuration builder
     * @return the UI login configuration
     */
    @Bean(name = "novaGroupsConfiguration")
    @Nonnull
    public LoginConfiguration servicesRestLoginConfiguration(@Nonnull final LoginConfigurationBuilder builder) {
        // @formatter:off

        return builder
                .order(this.loginOrder)
                .loginModule(JaasAuthConfig.JAAS_UI)
                    .moduleClass(NovaGroupsLoginModule.class)
                    .controlFlag(this.loginFlag)
                    .option(NovaGroupsLoginModule.REST_CLIENT_CONFIG, loginRestClientConfig)
                    .add()
                .loginModule(JaasAuthConfig.JAAS_UI_TOKEN)
                    .moduleClass(NovaGroupsLoginModule.class)
                    .controlFlag(this.loginFlag)
                    .option(NovaGroupsLoginModule.REST_CLIENT_CONFIG, loginRestClientConfig)
                    .option(NovaGroupsLoginModule.LOGIN_USER_FIELD, loginUser)
                    .option(NovaGroupsLoginModule.LOGIN_PASSWORD_FIELD, loginPassword)
                    .add()
                .build();

        // @formatter:on
    }
}
