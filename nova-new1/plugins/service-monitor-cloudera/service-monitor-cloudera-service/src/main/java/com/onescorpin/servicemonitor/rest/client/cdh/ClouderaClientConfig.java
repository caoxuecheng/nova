package com.onescorpin.servicemonitor.rest.client.cdh;

/*-
 * #%L
 * onescorpin-service-monitor-cloudera
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

import com.onescorpin.servicemonitor.rest.client.RestClientConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Cloudera client.
 */
@Configuration
public class ClouderaClientConfig {

    @Bean(name = "clouderaRestClientConfig")
    @ConfigurationProperties("clouderaRestClientConfig")
    public RestClientConfig getConfig() {
        return new RestClientConfig();
    }

}
