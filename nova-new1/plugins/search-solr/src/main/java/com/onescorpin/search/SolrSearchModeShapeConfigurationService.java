package com.onescorpin.search;

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

import com.onescorpin.search.api.RepositoryIndexConfiguration;
import com.onescorpin.search.config.SolrSearchClientConfiguration;

import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Editor;
import org.modeshape.schematic.document.ParsingException;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * ModeShape configuration for Solr index (lucene) integration
 */
//TODO: Think about allowing uses to configure just what they want to index. I can build the JSON accordingly.
public class SolrSearchModeShapeConfigurationService implements RepositoryIndexConfiguration {

    private SolrSearchClientConfiguration clientConfig;
    private static final String LUCENE = "lucene";

    public SolrSearchModeShapeConfigurationService(SolrSearchClientConfiguration config) {
        this.clientConfig = config;
    }

    @Override
    public RepositoryConfiguration build() {
        RepositoryConfiguration repositoryConfiguration;

        final String EMPTY_CONFIG = "{}";
        final String NOVA_CATEGORIES_METADATA = "nova-categories-metadata";
        final String NOVA_NFLOWS_METADATA = "nova-nflows-metadata";
        final String INDEXES = "indexes";
        final String INDEX_PROVIDERS = "indexProviders";

        try {
            repositoryConfiguration = RepositoryConfiguration.read(EMPTY_CONFIG);
        } catch (ParsingException | FileNotFoundException e) {
            e.printStackTrace();
            repositoryConfiguration = new RepositoryConfiguration();
        }

        Editor editor = repositoryConfiguration.edit();
        EditableDocument indexesDocument = editor.getOrCreateDocument(INDEXES);

        EditableDocument categoriesIndexDocument = indexesDocument.getOrCreateDocument(NOVA_CATEGORIES_METADATA);
        EditableDocument nflowsIndexDocument = indexesDocument.getOrCreateDocument(NOVA_NFLOWS_METADATA);
        categoriesIndexDocument.putAll(getCategoriesIndexConfiguration());
        nflowsIndexDocument.putAll(getNflowsIndexConfiguration());

        EditableDocument indexProvidersDocument = editor.getOrCreateDocument(INDEX_PROVIDERS);
        EditableDocument localNamedIndexProviderDocument = indexProvidersDocument.getOrCreateDocument(LUCENE);
        localNamedIndexProviderDocument.putAll(getLuceneIndexProviderConfiguration());

        repositoryConfiguration = new RepositoryConfiguration(editor, repositoryConfiguration.getName());
        return repositoryConfiguration;
    }

    private Map<String, Object> getCategoriesIndexConfiguration() {
        final String VALUE = "value";
        final String TBA_CATEGORY = "tba:category";
        final String COLUMNS = "jcr:title (STRING), jcr:description (STRING)";

        Map<String, Object> categoriesIndexConfigurationMap = new HashMap<>();
        categoriesIndexConfigurationMap.put("kind", VALUE);
        categoriesIndexConfigurationMap.put("provider", LUCENE);
        categoriesIndexConfigurationMap.put("synchronous", false);
        categoriesIndexConfigurationMap.put("nodeType", TBA_CATEGORY);
        categoriesIndexConfigurationMap.put("columns", COLUMNS);
        return categoriesIndexConfigurationMap;
    }

    private Map<String, Object> getNflowsIndexConfiguration() {
        final String VALUE = "value";
        final String NFLOW_SUMMARY = "tba:nflowSummary";
        final String COLUMNS = "jcr:title (STRING), jcr:description (STRING), tba:tags (STRING)";

        Map<String, Object> nflowsIndexConfigurationMap = new HashMap<>();
        nflowsIndexConfigurationMap.put("kind", VALUE);
        nflowsIndexConfigurationMap.put("provider", LUCENE);
        nflowsIndexConfigurationMap.put("synchronous", false);
        nflowsIndexConfigurationMap.put("nodeType", NFLOW_SUMMARY);
        nflowsIndexConfigurationMap.put("columns", COLUMNS);
        return nflowsIndexConfigurationMap;
    }

    private Map<String, Object> getLuceneIndexProviderConfiguration() {
        Map<String, Object> luceneIndexProviderConfigurationMap = new HashMap<>();
        luceneIndexProviderConfigurationMap.put("classname", LUCENE);
        luceneIndexProviderConfigurationMap.put("directory", clientConfig.getIndexStorageDirectory());
        return luceneIndexProviderConfigurationMap;
    }
}
