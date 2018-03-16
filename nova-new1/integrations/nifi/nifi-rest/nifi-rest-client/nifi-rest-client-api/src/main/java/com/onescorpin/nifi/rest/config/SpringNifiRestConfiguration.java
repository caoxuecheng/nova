package com.onescorpin.nifi.rest.config;

/*-
 * #%L
 * onescorpin-nifi-rest-client-api
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

import com.onescorpin.nifi.rest.NiFiObjectCache;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.nifi.rest.client.NifiRestClientConfig;
import com.onescorpin.nifi.rest.client.layout.AlignNiFiComponents;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

import java.net.MalformedURLException;
import java.net.URL;

/**
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class SpringNifiRestConfiguration {

    @Autowired
    private Environment env;

    @Bean(name = "nifiRestClient")
    public LegacyNifiRestClient nifiRestClient() {

        LegacyNifiRestClient restClient = new LegacyNifiRestClient();
        return restClient;

    }

    @Bean
    public NiFiObjectCache niFiObjectCache(){
        return new NiFiObjectCache();
    }

    /**
     * Gets the configuration for the NiFi REST client.
     *
     * <p>Looks for {@code onescorpin.nifi.rest} properties first then for {@code nifi.rest} properties.</p>
     *
     * @return the NiFi REST client configuration
     */
    @Bean(name = "nifiRestClientConfig")
    @ConfigurationProperties(prefix = "nifi.rest")
    public NifiRestClientConfig nifiRestClientConfig() {
        final NifiRestClientConfig config = new NifiRestClientConfig();
        config.setUsername(env.getProperty("onescorpin.nifi.rest.username"));
        config.setPassword(env.getProperty("onescorpin.nifi.rest.password"));
        config.setHttps(BooleanUtils.toBoolean(env.getProperty("onescorpin.nifi.rest.https")));
        config.setUseConnectionPooling(BooleanUtils.toBoolean(env.getProperty("onescorpin.nifi.rest.useConnectionPooling")));
        config.setTruststorePath(env.getProperty("onescorpin.nifi.rest.truststorePath"));
        config.setTruststorePassword(env.getProperty("onescorpin.nifi.rest.truststorePassword"));
        config.setKeystorePassword(env.getProperty("onescorpin.nifi.rest.keystorePassword"));
        config.setKeystorePath(env.getProperty("onescorpin.nifi.rest.keystorePath"));
        config.setTrustStoreType(env.getProperty("onescorpin.nifi.rest.truststoreType"));
        config.setKeystoreType(env.getProperty("onescorpin.nifi.rest.keystoreType"));
        final String host = env.getProperty("onescorpin.nifi.rest.host");
        if (host != null) {
            try {
                final URL url = new URL(host);
                config.setHost(url.getHost());
                config.setPort((url.getPort() > -1) ? url.getPort() : 8079);
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException("Invalid onescorpin.nifi.rest.host: " + host, e);
            }
        }

        return config;
    }

    @Bean
    public NifiRestClientAroundAspect nifiRestClientAroundAspect() {
        return new NifiRestClientAroundAspect();
    }

    @Bean
    public AlignNiFiComponents alignNiFiComponents(){
        return new AlignNiFiComponents();
    }


}
