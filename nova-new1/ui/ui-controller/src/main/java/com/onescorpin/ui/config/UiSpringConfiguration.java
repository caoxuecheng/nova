package com.onescorpin.ui.config;

/*-
 * #%L
 * nova-ui-controller
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

import com.onescorpin.spring.FileResourceService;
import com.onescorpin.ui.service.StandardUiTemplateService;
import com.onescorpin.ui.service.TemplateTableOptionConfigurerAdapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 */
@Configuration
public class UiSpringConfiguration {

    @Bean
    public FileResourceService fileResourceLoaderService(){
        return new FileResourceService();
    }

    @Bean
    public StandardUiTemplateService uiTemplateService(){
        return new StandardUiTemplateService();
    }


    @Bean
    public TemplateTableOptionConfigurerAdapter templateTableOptionConfigurerAdapter(StandardUiTemplateService uiTemplateService) {
       return new TemplateTableOptionConfigurerAdapter(uiTemplateService);
    }

}