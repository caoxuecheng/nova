package com.onescorpin.nifi.v2.core.precondition;

/*-
 * #%L
 * onescorpin-nifi-core-service
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

import com.onescorpin.nifi.core.api.precondition.PreconditionEventConsumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;

/**
 * The spring configuration class for the precondition beans
 */
@Configuration
public class PreconditionJmsConfiguration {

    @Bean
    public ConfigurationClassPostProcessor configurationClassPostProcessor() {
        return new ConfigurationClassPostProcessor();
    }

    @Bean
    public PreconditionEventConsumer preconditionEventJmsConsumer() {
        return new JmsPreconditionEventConsumer();
    }

}
