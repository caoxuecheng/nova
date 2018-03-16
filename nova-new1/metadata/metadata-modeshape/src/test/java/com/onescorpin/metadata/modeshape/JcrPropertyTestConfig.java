package com.onescorpin.metadata.modeshape;

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

import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.modeshape.category.JcrCategoryProvider;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasourceProvider;
import com.onescorpin.metadata.modeshape.nflow.JcrNflowProvider;
import com.onescorpin.metadata.modeshape.tag.TagProvider;
import com.onescorpin.metadata.modeshape.template.JcrNflowTemplateProvider;

import org.springframework.context.annotation.Bean;

/**
 * Defines the beans used by JcrPropertyTest which override the mocks of JcrTestConfig.
 */
public class JcrPropertyTestConfig {

    @Bean
    public CategoryProvider categoryProvider() {
        return new JcrCategoryProvider();
    }

    @Bean
    public DatasourceProvider datasourceProvider() {
        return new JcrDatasourceProvider();
    }

    @Bean
    public NflowProvider nflowProvider() {
        return new JcrNflowProvider();
    }

    @Bean
    public NflowManagerTemplateProvider nflowManagerTemplateProvider() {
        return new JcrNflowTemplateProvider();
    }

    @Bean
    public TagProvider tagProvider() {
        return new TagProvider();
    }

    @Bean
    public JcrMetadataAccess metadata() {
        return new JcrMetadataAccess();
    }
}
