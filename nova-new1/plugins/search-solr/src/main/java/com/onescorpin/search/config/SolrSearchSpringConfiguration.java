package com.onescorpin.search.config;

/*-
 * #%L
 * nova-search-solr
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

import com.onescorpin.search.SolrSearchModeShapeConfigurationService;
import com.onescorpin.search.SolrSearchService;
import com.onescorpin.search.api.RepositoryIndexConfiguration;
import com.onescorpin.search.api.Search;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Profile("search-solr")
@PropertySource("classpath:solrsearch.properties")
public class SolrSearchSpringConfiguration {

    @Bean
    @ConfigurationProperties("search")
    public SolrSearchClientConfiguration solrSearchClientConfiguration() {
        return new SolrSearchClientConfiguration();
    }

    @Bean
    @Profile("!novaUpgrade")
    public RepositoryIndexConfiguration solrModeShapeConfigurationService(SolrSearchClientConfiguration solrSearchClientConfiguration) {
        return new SolrSearchModeShapeConfigurationService(solrSearchClientConfiguration);
    }

    @Bean
    public Search search(SolrSearchClientConfiguration solrSearchClientConfiguration) {
        return new SolrSearchService(solrSearchClientConfiguration);
    }

}
