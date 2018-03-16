package com.onescorpin.nflowmgr.service.datasource;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.google.common.collect.Collections2;
import com.onescorpin.app.ServicesApplicationStartup;
import com.onescorpin.nflowmgr.rest.Model;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.PostMetadataConfigAction;
import com.onescorpin.metadata.api.datasource.DatasourceCriteria;
import com.onescorpin.metadata.api.datasource.DatasourceDefinitionProvider;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.datasource.JdbcDatasourceDetails;
import com.onescorpin.metadata.api.datasource.UserDatasource;
import com.onescorpin.metadata.rest.model.data.DatasourceDefinition;
import com.onescorpin.metadata.rest.model.nflow.NflowLineageStyle;
import com.onescorpin.spring.FileResourceService;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.web.api.dto.ControllerServiceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Provides access to the {@link DatasourceDefinition} and {@link com.onescorpin.metadata.rest.model.nflow.NflowLineage}
 */
public class DatasourceService implements PostMetadataConfigAction {

    private static final Logger log = LoggerFactory.getLogger(DatasourceService.class);

    @Inject
    ServicesApplicationStartup startup;

    @Inject
    FileResourceService fileResourceService;
    @Inject
    MetadataAccess metadataAccess;
    @Inject
    private DatasourceDefinitionProvider datasourceDefinitionProvider;
    private Map<String, NflowLineageStyle> nflowLineageStyleMap;

    @Inject
    private DatasourceProvider datasourceProvider;

    @PostConstruct
    private void init() {
        loadNflowLineageStylesFromFile();
    }


    /**
     * Loads the styles from the json file
     */
    public void loadNflowLineageStylesFromFile() {

        String json = fileResourceService.getResourceAsString("classpath:/datasource-styles.json");
        Map<String, NflowLineageStyle> styles = null;
        try {
            if (StringUtils.isNotBlank(json)) {
                styles = (Map<String, NflowLineageStyle>) ObjectMapperSerializer.deserialize(json, Map.class);
                nflowLineageStyleMap = styles;
            }
        } catch (Exception e) {
            log.error("Unable to parse JSON for datasource-styles.json file.  Error: {}",
                      e.getMessage());
        }
    }


    /**
     * Refresh the styles
     */
    public void refreshNflowLineageStyles(Map<String, NflowLineageStyle> styles) {
        if (styles != null && !styles.isEmpty()) {
            nflowLineageStyleMap = styles;
        }
    }

    public Map<String, NflowLineageStyle> getNflowLineageStyleMap() {
        if (nflowLineageStyleMap == null) {
            nflowLineageStyleMap = new HashMap<>();
        }
        return nflowLineageStyleMap;
    }


    /**
     * Load the 'datasource-definitions.json' datasource definitions file this is called on startup of Nova
     */
    public void loadDefinitionsFromFile() {

        String json = fileResourceService.getResourceAsString("classpath:/datasource-definitions.json");
        List<DatasourceDefinition> definitions = null;
        if (StringUtils.isNotBlank(json)) {
            definitions = Arrays.asList(ObjectMapperSerializer.deserialize(json, DatasourceDefinition[].class));
        }
        updateDatasourceDefinitions(definitions);
    }

    /**
     * Update any definitions
     */
    public Set<DatasourceDefinition> updateDatasourceDefinitions(List<DatasourceDefinition> definitions) {
        if (definitions != null) {
            final List<DatasourceDefinition> datasourceDefinitions = definitions;
            metadataAccess.commit(() -> {
                datasourceDefinitions.stream().forEach(def ->
                                                       {
                                                           com.onescorpin.metadata.api.datasource.DatasourceDefinition
                                                               domainDef =
                                                               datasourceDefinitionProvider.ensureDatasourceDefinition(def.getProcessorType());
                                                           domainDef.setDatasourcePropertyKeys(def.getDatasourcePropertyKeys());
                                                           domainDef.setIdentityString(def.getIdentityString());
                                                           domainDef.setConnectionType(
                                                               com.onescorpin.metadata.api.datasource.DatasourceDefinition.ConnectionType.valueOf(def.getConnectionType().name()));
                                                           domainDef.setProcessorType(def.getProcessorType());
                                                           domainDef.setDatasourceType(def.getDatasourceType());
                                                           domainDef.setDescription(def.getDescription());
                                                           domainDef.setTile(def.getTitle());
                                                           datasourceDefinitionProvider.update(domainDef);
                                                       });
                return null;
            }, MetadataAccess.SERVICE);
        }
        return getDatasourceDefinitions();
    }


    /**
     * Return the saved datasource definitions
     *
     * @return a list of the configured datasource definitions
     */
    public Set<DatasourceDefinition> getDatasourceDefinitions() {
        return metadataAccess.read(() -> {
            Set<com.onescorpin.metadata.api.datasource.DatasourceDefinition> datasourceDefinitions = datasourceDefinitionProvider.getDatasourceDefinitions();
            if (datasourceDefinitions != null) {
                return new HashSet<>(Collections2.transform(datasourceDefinitions, Model.DOMAIN_TO_DS_DEFINITION));
            }
            return null;
        }, MetadataAccess.SERVICE);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        loadDefinitionsFromFile();
    }

    /**
     * Predicate function that filters controller services for those that aren't referenced by a data source or where the current user has access to the data source.
     */
    @Nonnull
    public Predicate<ControllerServiceDTO> getControllerServiceAccessControlFilter() {
        final Set<String> allControllerServiceIds = metadataAccess.read(this::getControllerServiceIds, MetadataAccess.SERVICE);
        final Set<String> allowedControllerServiceIds = metadataAccess.read(this::getControllerServiceIds);
        return controllerService -> !allControllerServiceIds.contains(controllerService.getId()) || allowedControllerServiceIds.contains(controllerService.getId());
    }

    /**
     * Returns the set of all controller service ids.
     */
    @Nonnull
    private Set<String> getControllerServiceIds() {
        final DatasourceCriteria criteria = datasourceProvider.datasetCriteria().type(UserDatasource.class);
        return datasourceProvider.getDatasources(criteria).stream()
            .map(UserDatasource.class::cast)
            .map(UserDatasource::getDetails).filter(Optional::isPresent).map(Optional::get)
            .filter(JdbcDatasourceDetails.class::isInstance).map(JdbcDatasourceDetails.class::cast)
            .map(JdbcDatasourceDetails::getControllerServiceId).filter(Optional::isPresent).map(Optional::get)
            .collect(Collectors.toSet());
    }
}
