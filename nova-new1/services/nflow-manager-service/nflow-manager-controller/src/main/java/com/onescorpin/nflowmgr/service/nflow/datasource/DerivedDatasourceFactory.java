package com.onescorpin.nflowmgr.service.nflow.datasource;

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

import com.onescorpin.discovery.schema.TableSchema;
import com.onescorpin.nflowmgr.nifi.NifiControllerServiceProperties;
import com.onescorpin.nflowmgr.nifi.PropertyExpressionResolver;
import com.onescorpin.nflowmgr.rest.model.NflowDataTransformation;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.TemplateProcessorDatasourceDefinition;
import com.onescorpin.nflowmgr.rest.model.schema.TableSetup;
import com.onescorpin.nflowmgr.service.template.NflowManagerTemplateService;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateCache;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.datasource.DatasourceDefinition;
import com.onescorpin.metadata.api.datasource.DatasourceDefinitionProvider;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.datasource.DerivedDatasource;
import com.onescorpin.nifi.rest.model.NifiProperty;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.web.api.dto.ControllerServiceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Create and assign {@link DerivedDatasource} based upon a template or nflow
 */
public class DerivedDatasourceFactory {

    private static final Logger log = LoggerFactory.getLogger(DerivedDatasourceFactory.class);

    /**
     * Processor type for a Hive datasource in a data transformation nflow.
     */
    private static String DATA_TRANSFORMATION_HIVE_DEFINITION = "datatransformation.hive.template";

    /**
     * Processor type for a JDBC datasource in a data transformation nflow.
     */
    private static String DATA_TRANSFORMATION_JDBC_DEFINITION = "datatransformation.jdbc.template";

    /**
     * Property for the table name of a HiveDatasource.
     */
    private static String HIVE_TABLE_KEY = "table";

    /**
     * Property for the schema name of a HiveDatasource.
     */
    private static String HIVE_SCHEMA_KEY = "schema";

    /**
     * Property for the database connection name of a DatabaseDatasource.
     */
    private static String JDBC_CONNECTION_KEY = "Database Connection";

    /**
     * Property for the schema and table name of a DatabaseDatasource.
     */
    private static String JDBC_TABLE_KEY = "Table";

    @Inject
    DatasourceDefinitionProvider datasourceDefinitionProvider;

    @Inject
    DatasourceProvider datasourceProvider;

    @Inject
    PropertyExpressionResolver propertyExpressionResolver;

    @Inject
    NflowManagerTemplateService nflowManagerTemplateService;

    @Inject
    MetadataAccess metadataAccess;

    @Inject
    RegisteredTemplateCache registeredTemplateCache;

    @Inject
    private NifiControllerServiceProperties nifiControllerServiceProperties;

    public void populateDatasources(NflowMetadata nflowMetadata, RegisteredTemplate template, Set<com.onescorpin.metadata.api.datasource.Datasource.ID> sources,
                                    Set<com.onescorpin.metadata.api.datasource.Datasource.ID> dest) {
        // Extract source and destination datasources from data transformation nflows
        if (isDataTransformation(nflowMetadata)) {
            if (noneMatch(template.getRegisteredDatasourceDefinitions(), DatasourceDefinition.ConnectionType.SOURCE)) {
                sources.addAll(ensureDataTransformationSourceDatasources(nflowMetadata));
            }
            if (noneMatch(template.getRegisteredDatasourceDefinitions(), DatasourceDefinition.ConnectionType.DESTINATION)) {
                dest.addAll(ensureDataTransformationDestinationDatasources(nflowMetadata));
            }
        }

        //see if its in the cache first
        List<RegisteredTemplate.Processor> processors = registeredTemplateCache.getProcessors(nflowMetadata.getTemplateId());
        //if not add it
        if (processors == null) {
            processors = nflowManagerTemplateService.getRegisteredTemplateProcessors(nflowMetadata.getTemplateId(), true);
            registeredTemplateCache.putProcessors(nflowMetadata.getTemplateId(), processors);
        }
        //COPY the properties since they will be replaced when evaluated
        List<NifiProperty> allProperties = processors.stream().flatMap(processor -> processor.getProperties().stream())
            .map(property -> new NifiProperty(property)).collect(Collectors.toList());

        template.getRegisteredDatasourceDefinitions().stream().forEach(definition -> {
            Datasource.ID id = ensureDatasource(definition, nflowMetadata, allProperties);
            if (id != null) {
                if (com.onescorpin.metadata.rest.model.data.DatasourceDefinition.ConnectionType.SOURCE.equals(definition.getDatasourceDefinition().getConnectionType())) {
                    //ensure this is the selected one for the nflow
                    if (template != null && template.getInputProcessors() != null && getNflowInputProcessorTypes(nflowMetadata).contains(definition.getProcessorType())) {
                        sources.add(id);
                    }
                } else {
                    dest.add(id);
                }
            }
        });
    }

    public boolean matchesDefinition(TemplateProcessorDatasourceDefinition definition, NifiProperty nifiProperty) {
        return nifiProperty.getProcessorType().equals(definition.getProcessorType()) && (nifiProperty.getProcessorId().equals(definition.getProcessorId()) || nifiProperty.getProcessorName()
            .equalsIgnoreCase(definition.getProcessorName()));
    }

    /**
     * Builds the list of destinations for the specified data transformation nflow.
     *
     * <p>The data source type is determined based on the sources used in the transformation. If only one source is used then it is assumed that the source and destination are the same. Otherwise it
     * is assumed that the destination is Hive.</p>
     *
     * @param nflow the nflow
     * @return the list of destinations
     * @throws NullPointerException if the nflow has no data transformation
     */
    @Nonnull
    private Set<Datasource.ID> ensureDataTransformationDestinationDatasources(@Nonnull final NflowMetadata nflow) {
        // Set properties based on data source type
        final String processorType;
        final Map<String, String> properties = new HashMap<>();

        if (nflow.getDataTransformation().getDatasourceIds() != null && nflow.getDataTransformation().getDatasourceIds().size() == 1) {
            final Datasource datasource = datasourceProvider.getDatasource(datasourceProvider.resolve(nflow.getDataTransformation().getDatasourceIds().get(0)));

            processorType = DATA_TRANSFORMATION_JDBC_DEFINITION;
            properties.put(JDBC_CONNECTION_KEY, datasource.getName());
            properties.put(JDBC_TABLE_KEY, nflow.getSystemCategoryName() + "." + nflow.getSystemNflowName());
        } else {
            processorType = DATA_TRANSFORMATION_HIVE_DEFINITION;
            properties.put(HIVE_SCHEMA_KEY, nflow.getSystemCategoryName());
            properties.put(HIVE_TABLE_KEY, nflow.getSystemNflowName());
        }

        // Create datasource
        final DatasourceDefinition datasourceDefinition = datasourceDefinitionProvider.findByProcessorType(processorType);
        if(datasourceDefinition != null) {
            final String identityString = propertyExpressionResolver.resolveVariables(datasourceDefinition.getIdentityString(), properties);
            final String title = datasourceDefinition.getTitle() != null ? propertyExpressionResolver.resolveVariables(datasourceDefinition.getTitle(), properties) : identityString;
            final String desc = propertyExpressionResolver.resolveVariables(datasourceDefinition.getDescription(), properties);

            if (processorType.equals(DATA_TRANSFORMATION_JDBC_DEFINITION)) {
                properties.putAll(parseDataTransformControllerServiceProperties(datasourceDefinition, properties.get(JDBC_CONNECTION_KEY)));
            }

            final DerivedDatasource datasource = datasourceProvider.ensureDerivedDatasource(datasourceDefinition.getDatasourceType(), identityString, title, desc, new HashMap<>(properties));
            return Collections.singleton(datasource.getId());
        }
        else {
            return Collections.emptySet();
        }
    }

    /**
     * Builds the list of data sources for the specified data transformation nflow.
     *
     * @param nflow the nflow
     * @return the list of data sources
     * @throws NullPointerException if the nflow has no data transformation
     */
    @Nonnull
    private Set<Datasource.ID> ensureDataTransformationSourceDatasources(@Nonnull final NflowMetadata nflow) {
        final Set<Datasource.ID> datasources = new HashSet<>();

        // Extract nodes in chart view model
        @SuppressWarnings("unchecked") final Stream<Map<String, Object>> nodes = Optional.ofNullable(nflow.getDataTransformation().getChartViewModel())
            .map(model -> (List<Map<String, Object>>) model.get("nodes"))
            .map(Collection::stream)
            .orElse(Stream.empty());

        // Create a data source for each node
        final DatasourceDefinition hiveDefinition = datasourceDefinitionProvider.findByProcessorType(DATA_TRANSFORMATION_HIVE_DEFINITION);
        final DatasourceDefinition jdbcDefinition = datasourceDefinitionProvider.findByProcessorType(DATA_TRANSFORMATION_JDBC_DEFINITION);

        nodes.forEach(node -> {
            // Extract properties from node
            final DatasourceDefinition datasourceDefinition;
            final Map<String, String> properties = new HashMap<>();

            if (node.get("datasourceId") == null || node.get("datasourceId").equals("HIVE")) {
                final String name = (String) node.get("name");
                datasourceDefinition = hiveDefinition;
                properties.put(HIVE_SCHEMA_KEY, StringUtils.trim(StringUtils.substringBefore(name, ".")));
                properties.put(HIVE_TABLE_KEY, StringUtils.trim(StringUtils.substringAfterLast(name, ".")));
            } else {
                final Datasource datasource = datasourceProvider.getDatasource(datasourceProvider.resolve((String) node.get("datasourceId")));
                datasourceDefinition = jdbcDefinition;
                properties.put(JDBC_CONNECTION_KEY, datasource.getName());
                properties.put(JDBC_TABLE_KEY, (String) node.get("name"));
                properties.putAll(parseDataTransformControllerServiceProperties(datasourceDefinition,datasource.getName()));

            }
            if(datasourceDefinition != null) {
                // Create the derived data source
                final String identityString = propertyExpressionResolver.resolveVariables(datasourceDefinition.getIdentityString(), properties);
                final String title = datasourceDefinition.getTitle() != null ? propertyExpressionResolver.resolveVariables(datasourceDefinition.getTitle(), properties) : identityString;
                final String desc = propertyExpressionResolver.resolveVariables(datasourceDefinition.getDescription(), properties);

                final DerivedDatasource datasource = datasourceProvider.ensureDerivedDatasource(datasourceDefinition.getDatasourceType(), identityString, title, desc, new HashMap<>(properties));
                datasources.add(datasource.getId());
            }
        });

        // Build the data sources from the data source ids
        final List<String> datasourceIds = Optional.ofNullable(nflow.getDataTransformation()).map(NflowDataTransformation::getDatasourceIds).orElse(Collections.emptyList());
        datasourceIds.stream()
            .map(datasourceProvider::resolve)
            .forEach(datasources::add);

        return datasources;
    }

    /**
     * Indicates if the nflow contains a data transformation.
     */
    private boolean isDataTransformation(@Nonnull final NflowMetadata nflowMetadata) {
        return nflowMetadata.getDataTransformation() != null && StringUtils.isNotEmpty(nflowMetadata.getDataTransformation().getDataTransformScript());
    }


    public Datasource.ID ensureDatasource(TemplateProcessorDatasourceDefinition definition, NflowMetadata nflowMetadata, List<NifiProperty> allProperties) {
        return metadataAccess.commit(() -> {

            List<NifiProperty> propertiesToEvalulate = new ArrayList<NifiProperty>();

            //fetch the def
            DatasourceDefinition datasourceDefinition = datasourceDefinitionProvider.findByProcessorType(definition.getProcessorType());
            if (datasourceDefinition != null) {

                //find out if there are any saved properties on the Nflow that match the datasourceDef
                List<NifiProperty> nflowProperties = nflowMetadata.getProperties().stream().filter(
                    property -> matchesDefinition(definition, property) && datasourceDefinition.getDatasourcePropertyKeys().contains(property.getKey())).collect(
                    Collectors.toList());

                //resolve any ${metadata.} properties
                List<NifiProperty> resolvedNflowProperties = propertyExpressionResolver.resolvePropertyExpressions(nflowProperties, nflowMetadata);

                List<NifiProperty> resolvedAllProperties = propertyExpressionResolver.resolvePropertyExpressions(allProperties, nflowMetadata);

                //propetyHash
                propertiesToEvalulate.addAll(nflowProperties);
                propertiesToEvalulate.addAll(allProperties);

                propertyExpressionResolver.resolveStaticProperties(propertiesToEvalulate);

                String identityString = datasourceDefinition.getIdentityString();
                String desc = datasourceDefinition.getDescription();
                String title = datasourceDefinition.getTitle();

                PropertyExpressionResolver.ResolvedVariables identityStringPropertyResolution = propertyExpressionResolver.resolveVariables(identityString, propertiesToEvalulate);
                identityString = identityStringPropertyResolution.getResolvedString();

                PropertyExpressionResolver.ResolvedVariables titlePropertyResolution = propertyExpressionResolver.resolveVariables(title, propertiesToEvalulate);
                title = titlePropertyResolution.getResolvedString();

                if (desc != null) {
                    PropertyExpressionResolver.ResolvedVariables descriptionPropertyResolution = propertyExpressionResolver.resolveVariables(desc, propertiesToEvalulate);
                    desc = descriptionPropertyResolution.getResolvedString();
                }

                //if the identityString still contains unresolved variables then make the title readable and replace the idstring with the nflow.id
                if (propertyExpressionResolver.containsVariablesPatterns(identityString)) {
                    title = propertyExpressionResolver.replaceAll(title, " {runtime variable} ");
                    identityString = propertyExpressionResolver.replaceAll(identityString, nflowMetadata.getId());
                }

                //find any datasource matching this DsName and identity String, if not create one
                //if it is the Source ensure the nflow matches this ds
                if (isCreateDatasource(datasourceDefinition, nflowMetadata)) {
                    Map<String, String> controllerServiceProperties = parseControllerServiceProperties(datasourceDefinition, nflowProperties);
                    Map<String, Object> properties = new HashMap<String, Object>(identityStringPropertyResolution.getResolvedVariables());
                    properties.putAll(controllerServiceProperties);
                    DerivedDatasource
                        derivedDatasource =
                        datasourceProvider.ensureDerivedDatasource(datasourceDefinition.getDatasourceType(), identityString, title, desc, properties);
                    if (derivedDatasource != null) {
                        if ("HiveDatasource".equals(derivedDatasource.getDatasourceType())
                            && Optional.ofNullable(nflowMetadata.getTable()).map(TableSetup::getTableSchema).map(TableSchema::getFields).isPresent()) {
                            derivedDatasource.setGenericProperties(Collections.singletonMap("columns", (Serializable) nflowMetadata.getTable().getTableSchema().getFields()));
                        }
                        return derivedDatasource.getId();
                    }
                }
                return null;


            } else {
                return null;
            }


        }, MetadataAccess.SERVICE);

    }


    private List<String> getNflowInputProcessorTypes(NflowMetadata nflowMetadata) {
        List<String> types = new ArrayList<>();
        types.add(nflowMetadata.getInputProcessorType());
        if (nflowMetadata != null && "com.onescorpin.nifi.v2.core.watermark.LoadHighWaterMark".equalsIgnoreCase(nflowMetadata.getInputProcessorType())) {
            types.add("com.onescorpin.nifi.v2.sqoop.core.ImportSqoop");
            types.add("com.onescorpin.nifi.v2.ingest.GetTableData");
        }
        return types;
    }

    /**
     * Create Datasources for all DESTINATIONS and only if the SOURCE matches the assigned source for this nflow.
     */
    private boolean isCreateDatasource(DatasourceDefinition datasourceDefinition, NflowMetadata nflowMetadata) {
        return DatasourceDefinition.ConnectionType.DESTINATION.equals(datasourceDefinition.getConnectionType()) ||
               (DatasourceDefinition.ConnectionType.SOURCE.equals(datasourceDefinition.getConnectionType()) && (
                   getNflowInputProcessorTypes(nflowMetadata).contains(datasourceDefinition.getProcessorType())));
    }

    private Map<String, String> parseDataTransformControllerServiceProperties(DatasourceDefinition datasourceDefinition, String controllerServiceName) {
        Map<String, String> properties = new HashMap<>();
        if(datasourceDefinition != null) {
            try {
                if (StringUtils.isNotBlank(controllerServiceName)) {
                    //{Source Database Connection:Database Connection URL}
                    List<String>
                        controllerServiceProperties =
                        datasourceDefinition.getDatasourcePropertyKeys().stream().filter(k -> k.matches("\\{" + JDBC_CONNECTION_KEY + ":(.*)\\}")).collect(Collectors.toList());
                    List<String> serviceProperties = new ArrayList<>();
                    controllerServiceProperties.stream().forEach(p -> {
                        String property = p.substring(StringUtils.indexOf(p, ":") + 1, p.length() - 1);
                        serviceProperties.add(property);
                    });
                    ControllerServiceDTO csDto = nifiControllerServiceProperties.getControllerServiceByName(controllerServiceName);
                    if (csDto != null) {

                        serviceProperties.stream().forEach(p -> {

                            if (csDto != null) {
                                String value = csDto.getProperties().get(p);
                                if (value != null) {
                                    properties.put(p, value);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                log.warn("An error occurred trying to parse controller service properties for data transformation when deriving the datasource for {}, {}. {} ",
                         datasourceDefinition.getDatasourceType(),
                         datasourceDefinition.getConnectionType(), e.getMessage(), e);
            }
        }

        return properties;
    }

    /**
     * Parse the defintion metadata for the {propertyKey:CS Property Key} objects and pick out the values in the controller service
     *
     * @param datasourceDefinition the definition to use
     * @param nflowProperties       the nflow properties that match this definition
     * @return a Map of the Controller Service Property Key, Value
     */
    private Map<String, String> parseControllerServiceProperties(DatasourceDefinition datasourceDefinition, List<NifiProperty> nflowProperties) {
        Map<String, String> properties = new HashMap<>();
        try {
            //{Source Database Connection:Database Connection URL}
            List<String> controllerServiceProperties = datasourceDefinition.getDatasourcePropertyKeys().stream().filter(k -> k.matches("\\{(.*):(.*)\\}")).collect(Collectors.toList());
            Map<String, List<String>> serviceProperties = new HashMap<>();
            controllerServiceProperties.stream().forEach(p -> {
                String service = p.substring(1, StringUtils.indexOf(p, ":"));
                String property = p.substring(StringUtils.indexOf(p, ":") + 1, p.length() - 1);
                if (!serviceProperties.containsKey(service)) {
                    serviceProperties.put(service, new ArrayList<>());
                }
                serviceProperties.get(service).add(property);
            });

            serviceProperties.entrySet().stream().forEach(e -> {

                String service = e.getKey();
                String controllerServiceId = nflowProperties.stream()
                    .filter(p -> StringUtils.isNotBlank(p.getValue())
                                 && p.getPropertyDescriptor() != null
                                 && p.getPropertyDescriptor().getName().equalsIgnoreCase(service)
                                 && StringUtils.isNotBlank(p.getPropertyDescriptor().getIdentifiesControllerService())).map(p -> p.getValue()).findFirst().orElse(null);
                if (controllerServiceId != null) {
                    ControllerServiceDTO csDto = nifiControllerServiceProperties.getControllerServiceById(controllerServiceId);
                    if(csDto != null) {
                        e.getValue().stream().forEach(propertyKey -> {
                            String value = csDto.getProperties().get(propertyKey);
                            if (value != null) {
                                properties.put(propertyKey, value);
                            }
                        });
                    }

                }

            });
        } catch (Exception e) {
            log.warn("An error occurred trying to parse controller service properties when deriving the datasource for {}, {}. {} ", datasourceDefinition.getDatasourceType(),
                     datasourceDefinition.getConnectionType(), e.getMessage(), e);
        }

        return properties;
    }

    /**
     * Indicates if there are no definitions with the specified connection type in the collection.
     */
    private boolean noneMatch(@Nonnull final Collection<TemplateProcessorDatasourceDefinition> definitions, @Nonnull final DatasourceDefinition.ConnectionType connectionType) {
        return definitions.stream()
            .map(definition -> datasourceDefinitionProvider.findByProcessorType(definition.getProcessorType()))
            .map(DatasourceDefinition::getConnectionType)
            .noneMatch(connectionType::equals);
    }
}
