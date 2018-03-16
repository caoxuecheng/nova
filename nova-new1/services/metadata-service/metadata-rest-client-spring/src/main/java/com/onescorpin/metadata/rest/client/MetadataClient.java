/**
 *
 */
package com.onescorpin.metadata.rest.client;

/*-
 * #%L
 * onescorpin-metadata-rest-client-spring
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.onescorpin.metadata.api.op.NflowDependencyDeltaResults;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.DatasourceCriteria;
import com.onescorpin.metadata.rest.model.data.DirectoryDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTableColumn;
import com.onescorpin.metadata.rest.model.data.HiveTableDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTablePartition;
import com.onescorpin.metadata.rest.model.extension.ExtensibleTypeDescriptor;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.nflow.NflowCategory;
import com.onescorpin.metadata.rest.model.nflow.NflowCriteria;
import com.onescorpin.metadata.rest.model.nflow.NflowDependencyGraph;
import com.onescorpin.metadata.rest.model.nflow.NflowPrecondition;
import com.onescorpin.metadata.rest.model.nflow.InitializationStatus;
import com.onescorpin.metadata.rest.model.nifi.NiFiFlowCacheSync;
import com.onescorpin.metadata.rest.model.op.DataOperation;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAgreement;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.support.NflowNameUtil;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;

/**
 * A client for accessing the metadata store
 */
public class MetadataClient {

    public static final List<MediaType> ACCEPT_TYPES = Collections.unmodifiableList(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
    public static final ParameterizedTypeReference<List<ExtensibleTypeDescriptor>> TYPE_LIST = new ParameterizedTypeReference<List<ExtensibleTypeDescriptor>>() {
    };
    public static final ParameterizedTypeReference<List<Nflow>> NFLOW_LIST = new ParameterizedTypeReference<List<Nflow>>() {
    };
    public static final ParameterizedTypeReference<List<Datasource>> DATASOURCE_LIST = new ParameterizedTypeReference<List<Datasource>>() {
    };
    private static final Logger log = LoggerFactory.getLogger(MetadataClient.class);
    private static final Function<UriComponentsBuilder, UriComponentsBuilder> ALL_DATASOURCES = new TargetDatasourceCriteria();
    private static final Function<UriComponentsBuilder, UriComponentsBuilder> ALL_NFLOWS = new TargetNflowCriteria();

    private final URI base;

    /**
     * The base uri to access anything under the /proxy folder for Nova
     */
    private final URI proxyBase;

    private final RestTemplate template;
    private String category;

    /**
     * constructor, creates a metadata client for the base uri given
     *
     * @param base the URI of the metadata server
     */
    public MetadataClient(URI base) {
        this(base, null, null, null);
    }

    /**
     * constructor, creates a metadata client for the base uri given, using the sslContext provided
     *
     * @param base       the URI of the metadata server
     * @param sslContext the SSL context
     */
    public MetadataClient(URI base, SSLContext sslContext) {
        this(base, null, null, sslContext);
    }

    /**
     * constructor, creates a metadata client for the base uri given and authenticates to the server with the username and password provided.
     *
     * @param base     the URI of the metadata server
     * @param username the username to access the server
     * @param password the password of the user
     */
    public MetadataClient(URI base, String username, String password) {
        this(base, username, password, null);
    }

    /**
     * constructor, creates a metadata client for the base uri given, using the sslContext provided and authenticates to the server
     * with the username and password.
     *
     * @param base       the URI of the metadata server
     * @param username   the username to access the server
     * @param password   the password of the user
     * @param sslContext the SSL context
     */
    public MetadataClient(URI base, String username, String password, SSLContext sslContext) {
        this(base, createCredentialProvider(username, password), sslContext);
    }

    public MetadataClient(URI base, CredentialsProvider credsProvider, SSLContext sslContext) {
        super();
        this.base = base;
        this.proxyBase = URI.create(this.base.getScheme()+"://"+this.base.getHost()+"/proxy");
        if (credsProvider != null) {
            HttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .setSSLContext(sslContext != null ? sslContext : null)
                .build();
            ClientHttpRequestFactory reqFactory = new HttpComponentsClientHttpRequestFactoryBasicAuth(new HttpHost(base.getHost(), 
                                                                                                                   base.getPort(), 
                                                                                                                   base.getScheme()), 
                                                                                                      httpClient);
            this.template = new RestTemplate(reqFactory);
        } else {
            this.template = new RestTemplate();
        }

        ObjectMapper mapper = createObjectMapper();
        this.template.getMessageConverters().add(new MappingJackson2HttpMessageConverter(mapper));
    }

    /**
     * creates a credentials provider
     *
     * @param username the user name f
     * @param password the password
     * @return a credentials provider
     */
    public static CredentialsProvider createCredentialProvider(String username, String password) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        return credsProvider;
    }

//    public static CredentialsProvider createBasicCredentialProvider(String username, String password) {
//        CredentialsProvider credsProvider = new BasicCredentialsProvider();
//        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
//    }
    
    /**
     * Converts one or more strings to a path, compatible with the OS representation of a path
     *
     * @param first The base path
     * @param more  Additional directories to join to create a path, if any
     * @return The completed, OS compatible path
     */
    public static Path path(String first, String... more) {
        return Paths.get(first, more);
    }

    /**
     * gets the extensible types from the metadata store
     *
     * @return a list of extensible type descriptors
     */
    public List<ExtensibleTypeDescriptor> getExtensibleTypes() {
        return get(path("extension", "type"), null, TYPE_LIST);
    }

    /**
     * gets the extensible type descriptor from the metadata store
     *
     * @param nameOrId the name or ID of the descriptor
     * @return the extensible type descriptor
     */
    public ExtensibleTypeDescriptor getExtensibleType(String nameOrId) {
        return get(path("extension", "type", nameOrId), ExtensibleTypeDescriptor.class);
    }

    /**
     * Creates a NflowBuilder initialized with the name and category given
     *
     * @param categoryName the name of the category
     * @param name         the name of the nflow
     * @return a NflowBuilder used to further construct nflow details
     */
    public NflowBuilder buildNflow(String categoryName, String name) {
        return new NflowBuilderImpl(categoryName, name);
    }

    /**
     * get the named high water mark for the nflow given by nflowId
     *
     * @param nflowId        the id of the nflow
     * @param waterMarkName the name of the water mark
     * @return a string option
     */
    public Optional<String> getHighWaterMarkValue(String nflowId, String waterMarkName) {
        return optinal(() -> get(path("nflow", nflowId, "watermark", waterMarkName), String.class));
    }

    /**
     * Update the named high water mar, for the nflow given by nflowId, with the given value
     *
     * @param nflowId        the id of the nflow
     * @param waterMarkName the name of the water mark
     * @param value         the new value for the high water mark
     */
    public void updateHighWaterMarkValue(String nflowId, String waterMarkName, String value) {
        put(path("nflow", nflowId, "watermark", waterMarkName), value, MediaType.TEXT_PLAIN);
    }

    /**
     * Find out if the nflow given has been initialized
     *
     * @param nflowId the id of the nflow
     * @return an InitializationStatus, which can be used to get the status
     */
    public InitializationStatus getCurrentInitStatus(String nflowId) {
        return nullable(() -> get(Paths.get("nflow", nflowId, "initstatus"), InitializationStatus.class));
    }

    /**
     * update the initialization status of the nflow
     *
     * @param nflowId the id of the nflow
     * @param status the status of the nflow
     */
    public void updateCurrentInitStatus(String nflowId, InitializationStatus status) {
        put(Paths.get("nflow", nflowId, "initstatus"), status, MediaType.APPLICATION_JSON);
    }

    /**
     * adds a datasource, to serve as the source, to the nflow
     *
     * @param nflowId       the id of the nflow
     * @param datasourceId the id of the datasource to be added
     * @return the nflow
     */
    public Nflow addSource(String nflowId, String datasourceId) {
        Form form = new Form();
        form.add("datasourceId", datasourceId);

        return post(path("nflow", nflowId, "source"), form, Nflow.class);
    }

    /**
     * adds a datasource, to serve as the destination, of the nflow
     *
     * @param nflowId       the id of the nflow
     * @param datasourceId the id of the datasource to be added
     * @return the nflow
     */
    public Nflow addDestination(String nflowId, String datasourceId) {
        Form form = new Form();
        form.add("datasourceId", datasourceId);

        return post(path("nflow", nflowId, "destination"), form, Nflow.class);
    }

    /**
     * creates a SLA with the metadata server, returns the SLA as created
     *
     * @param sla the SLA to create
     * @return the SLA as created by the server
     */
    public ServiceLevelAgreement createSla(ServiceLevelAgreement sla) {
        return post(path("sla"), sla, MediaType.APPLICATION_JSON, ServiceLevelAgreement.class);
    }

    /**
     * makes a precondition for the nflow given
     *
     * @param nflowId  the id of the nflow
     * @param metrics on or more metrics that represent the precondition
     */
    public Nflow setPrecondition(String nflowId, Metric... metrics) {
        return setPrecondition(nflowId, Arrays.asList(metrics));
    }

    /**
     * makes a precondition for the nflow given
     *
     * @param nflowId  the id of the nflow
     * @param metrics on or more metrics that represent the precondition
     */
    public Nflow setPrecondition(String nflowId, List<Metric> metrics) {
        NflowPrecondition precond = new NflowPrecondition("Nflow " + nflowId + " Precondition", "", metrics);
        return setPrecondition(nflowId, precond);
    }

    /**
     * makes a precondition for the nflow given
     *
     * @param nflowId  the id of the nflow
     * @param precond the precondition
     */
    public Nflow setPrecondition(String nflowId, NflowPrecondition precond) {
        return post(path("nflow", nflowId, "precondition"), precond, MediaType.APPLICATION_JSON, Nflow.class);
    }

    /**
     * a factory method to creates a new nflow criteria
     *
     * @return a nflow criteria instance
     */
    public NflowCriteria nflowCriteria() {
        return new TargetNflowCriteria();
    }

    /**
     * gets the list of all nflows
     *
     * @return all nflows
     */
    public List<Nflow> getNflows() {
        return getNflows((NflowCriteria) ALL_NFLOWS);
    }

    /**
     * gets a list of nflows matching the criteria given
     *
     * @param criteria the criteria of the nflows to return
     * @return a list of nflows
     */
    public List<Nflow> getNflows(NflowCriteria criteria) {
        try {
            return get(path("nflow"), (TargetNflowCriteria) criteria, NFLOW_LIST);
        } catch (ClassCastException e) {
            throw new IllegalThreadStateException("Unknown criteria type: " + criteria.getClass());
        }
    }


    /**
     * get the nflow matching the id given
     *
     * @param id the id of the nflow
     * @return the nflow instance
     */
    public Nflow getNflow(String id) {
        return get(path("nflow", id), Nflow.class);
    }

    /**
     * get the nflow matching the named nflow in the category given
     *
     * @param category the name of the category
     * @param nflow     the name of the nflow
     */
    public Nflow getNflow(String category,String nflow) {
        return get(path("nflows","name",category+"."+nflow), Nflow.class);
    }


    /**
     * get the nflow
     */
    public NflowDependencyGraph getNflowDependency(String id) {
        return get(path("nflow", id, "depnflows"), NflowDependencyGraph.class);
    }

    public NflowDependencyDeltaResults getNflowDependencyDeltas(String nflowId) {
        return get(path("nflow", nflowId, "depnflows", "delta"), NflowDependencyDeltaResults.class);
    }

    public Nflow updateNflow(Nflow nflow) {
        // Using POST here in since it behaves more like a PATCH than a PUT, and PATCH is not supported in jersey.
        return post(path("nflow", nflow.getId()), nflow, MediaType.APPLICATION_JSON, Nflow.class);
    }

    /**
     * Gets the properties of the specified nflow.
     *
     * @param id the nflow id
     * @return the metadata properties
     */
    public Properties getNflowProperties(@Nonnull final String id) {
        return get(path("nflow", id, "props"), Properties.class);
    }

    /**
     * merge the properties given into the nflow
     *
     * @param nflowId the id of the nflow
     * @param props  a list of properties to merge into the nflow
     * @return the properties of the nflow after the merge
     */
    public Properties mergeNflowProperties(String nflowId, Properties props) {
        return post(path("nflow", nflowId, "props"), props, MediaType.APPLICATION_JSON, Properties.class);
    }

    /**
     * replace the properties of the nflow with thos given in props
     *
     * @param nflowId the id of the nflow
     * @param props  a list of properties to merge into the nflow
     * @return the properties of the nflow after the operation has succeeded
     */
    public Properties replaceNflowProperties(String nflowId, Properties props) {
        return put(path("nflow", nflowId), props, MediaType.APPLICATION_JSON, Properties.class);
    }

    /**
     * a factory to create a new object that acts as a DirectoryDatasourceBuilder initialized with the name given
     *
     * @param name the name to reference the directory data source by
     * @return the directory data source builder
     */
    public DirectoryDatasourceBuilder buildDirectoryDatasource(String name) {
        return new DirectoryDatasourceBuilderImpl(name);
    }

    /**
     * a factory to create a new object that acts as a HiveTableDatasourceBuilder initialized with the name given
     *
     * @param name the name to reference the hive table data source by
     * @return the hive table source builder
     */
    public HiveTableDatasourceBuilder buildHiveTableDatasource(String name) {
        return new HiveTableDatasourceBuilderImpl(name);
    }

    /**
     * a factory method to create a new data source criteria
     *
     * @return a data source criteria model object
     */
    public DatasourceCriteria datasourceCriteria() {
        return new TargetDatasourceCriteria();
    }

    /**
     * get the data sources available in the system
     *
     * @return the list of datasources
     */
    public List<Datasource> getDatasources() {
        return get(path("datasource"), ALL_DATASOURCES, DATASOURCE_LIST);
    }

    /**
     * get the data sources available in the system, that match the criteria given
     *
     * @return the list of datasources
     */
    public List<Datasource> getDatasources(DatasourceCriteria criteria) {
        try {
            return get(path("datasource"), (TargetDatasourceCriteria) criteria, DATASOURCE_LIST);
        } catch (ClassCastException e) {
            throw new IllegalThreadStateException("Unknown criteria type: " + criteria.getClass());
        }
    }

    /**
     * tells that an operation has begun, and allows us to set the status
     *
     * @param nflowDestinationId the id of the destination nflow
     * @param status            the status of the operation since begun
     * @return the operation
     */
    public DataOperation beginOperation(String nflowDestinationId, String status) {
        Form form = new Form();
        form.add("nflowDestinationId", nflowDestinationId);
        form.add("status", status);

        return post(path("dataop"), form, DataOperation.class);
    }

    /**
     * update the system with the info given in the operation. In other words the operation
     * will be consulted and the metadata within will be persisted to the server.
     *
     * @param op the operation
     * @return the operation, as recorded by the system
     */
    public DataOperation updateDataOperation(DataOperation op) {
        return put(path("dataop", op.getId()), op, MediaType.APPLICATION_JSON, DataOperation.class);
    }

    /**
     * get the operation with the given id
     *
     * @param id the id of the operation
     * @return the operation with the given id
     */
    public DataOperation getDataOperation(String id) {
        return get(path("dataop", id), DataOperation.class);
    }

    /**
     * checks the pre-conditions of the nflow with the given id and returns the result in an assessment object
     *
     * @param nflowId the id of the nflow
     * @return the assessment of the nflow, which can be consulted to check the state of the conditions for the SLA
     */
    public ServiceLevelAssessment assessPrecondition(String nflowId) {
        return get(path("nflow", nflowId, "precondition", "assessment"), ServiceLevelAssessment.class);
    }

    /**
     * get the results of the preconditions for the nflow identified by id
     *
     * @param nflowId the id of the nflow
     * @return a string representation of the assessment of the nflow
     */
    public String getPreconditionResult(String nflowId) {
        return get(path("nflow", nflowId, "precondition", "assessment", "result"), String.class);
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
// TODO Module dependency is causing a conflict somehow.
//        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(Include.NON_NULL);
        return mapper;
    }

    private <R> Optional<R> optinal(Supplier<R> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    private <R> R nullable(Supplier<R> supplier) {
        try {
            return supplier.get();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                throw e;
            }
        }
    }

    private NflowPrecondition createTrigger(List<Metric> metrics) {
        if (!metrics.isEmpty()) {
            NflowPrecondition trigger = new NflowPrecondition();
            trigger.addMetrics("", metrics);
            return trigger;
        } else {
            return null;
        }
    }

    private Nflow postNflow(Nflow nflow) {
        return post(path("nflow"), nflow, MediaType.APPLICATION_JSON, Nflow.class);
    }

    private HiveTableDatasource postDatasource(HiveTableDatasource ds) {
        return post(path("datasource", "hivetable"), ds, MediaType.APPLICATION_JSON, HiveTableDatasource.class);
    }

    private DirectoryDatasource postDatasource(DirectoryDatasource ds) {
        return post(path("datasource", "directory"), ds, MediaType.APPLICATION_JSON, DirectoryDatasource.class);
    }

    /**
     * get flow updates since the last time we synchronized with NiFi
     *
     * @param syncId the synchronization id
     * @return the cache of flow events
     */
    public NiFiFlowCacheSync getFlowUpdates(String syncId) {
        return get(path("nifi-provenance", "nifi-flow-cache", "get-flow-updates"), new NifiFlowSyncParameters(syncId), NiFiFlowCacheSync.class);
    }

    /**
     * reset flow events for the given id
     *
     * @param syncId the synchronization id
     * @return the cache of flow events after reset
     */
    public NiFiFlowCacheSync resetFlowUpdates(String syncId) {
        return get(path("nifi-provenance", "nifi-flow-cache", "reset-flow-updates"), new NifiFlowSyncParameters(syncId), NiFiFlowCacheSync.class);
    }

    /**
     * finds the max event for NiFi
     *
     * @param clusterNodeId the NifI cluster to query
     * @return the id of the most recent event
     */
    public Long findNiFiMaxEventId(String clusterNodeId) {
        log.info("findNifiMaxEventId ", clusterNodeId);
        clusterNodeId = org.apache.commons.lang3.StringUtils.isBlank(clusterNodeId)?"NODE" : clusterNodeId;
        return get(path("nifi-provenance", "max-event-id"), new MaxNifiEventParameters(clusterNodeId), Long.class);
    }

    /**
     * reset the max event for NiFi
     *
     * @param clusterNodeId the NifI cluster to query
     * @return the id of the most recent event
     */
    public Long resetNiFiMaxEventId(String clusterNodeId) {
        log.info("resetMaxEventId ", clusterNodeId);
        clusterNodeId = org.apache.commons.lang3.StringUtils.isBlank(clusterNodeId)?"NODE" : clusterNodeId;
        return post(path("nifi-provenance", "reset-max-event-id",clusterNodeId), null, Long.class);
    }

    /**
     * queries to see if NiFi has new data flow events
     *
     * @return true if there new events, false otherwise
     */
    public Boolean isNiFiFlowDataAvailable() {
        return get(path("nifi-provenance", "nifi-flow-cache", "available"), Boolean.class);
    }

    /**
     * Gets the data source with the specified id.
     *
     * @param id the data source id
     * @return the data source, if found
     * @throws RestClientException if the data source is unavailable
     */
    public Optional<Datasource> getDatasource(@Nonnull final String id) {
        try {
            return Optional.of(get(path("datasource", id),
                                   uri -> (uri != null) ? uri.queryParam("sensitive", true) : null,
                                   Datasource.class));
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    private UriComponentsBuilder base(Path path) {
        return UriComponentsBuilder.fromUri(this.base).path("/").path(path.toString());
    }

    private UriComponentsBuilder baseProxy(Path path) {
        return UriComponentsBuilder.fromUri(this.proxyBase).path("/").path(path.toString());
    }

    private <R> R get(Path path, Class<R> resultType) {
        return get(path, null, resultType);
    }

    private <R> R getProxy(Path path, Class<R> resultType) {
        return getProxy(path, null, resultType);
    }

    private <R> R get(Path path, Function<UriComponentsBuilder, UriComponentsBuilder> filterFunct, Class<R> resultType) {
        return this.template.getForObject(
            (filterFunct != null ? filterFunct.apply(base(path)) : base(path)).build().toUri(),
            resultType);
    }

    private <R> R getProxy(Path path, Function<UriComponentsBuilder, UriComponentsBuilder> filterFunct, Class<R> resultType) {
        return this.template.getForObject(
            (filterFunct != null ? filterFunct.apply(baseProxy(path)) : baseProxy(path)).build().toUri(),
            resultType);
    }

    private <R> R get(Path path, ParameterizedTypeReference<R> responseEntity) {
        return get(path, null, responseEntity);
    }

    private <R> R get(Path path, Function<UriComponentsBuilder, UriComponentsBuilder> filterFunct, ParameterizedTypeReference<R> responseEntity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(ACCEPT_TYPES);

        ResponseEntity<R> resp = this.template.exchange(
            (filterFunct != null ? filterFunct.apply(base(path)) : base(path)).build().toUri(),
            HttpMethod.GET,
            new HttpEntity<Object>(headers),
            responseEntity);

        return handle(resp);
    }

    private <R> R post(Path path, Form form, Class<R> resultType) {
        return this.template.postForObject(base(path).build().toUri(), form, resultType);
    }

    private <R> R post(Path path, Object body, MediaType mediaType, Class<R> resultType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setAccept(ACCEPT_TYPES);

        return this.template.postForObject(base(path).build().toUri(),
                                           new HttpEntity<>(body, headers),
                                           resultType);
    }

    private void put(Path path, Object body, MediaType mediaType) {
        put(path, body, mediaType, null);
    }

    private <R> R put(Path path, Object body, MediaType mediaType, Class<R> resultType) {
        URI uri = base(path).build().toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setAccept(ACCEPT_TYPES);

        this.template.put(uri, new HttpEntity<>(body, headers));
        // Silly that put() doesn't return an object.
        if (resultType != null) {
            return get(path, resultType);
        } else {
            return null;
        }
    }

    private <R> R handle(ResponseEntity<R> resp) {
        if (resp.getStatusCode().is2xxSuccessful()) {
            return resp.getBody();
        } else {
            throw new WebResponseException(ResponseEntity.status(resp.getStatusCode()).headers(resp.getHeaders()).build());
        }
    }
    
    
    /**
     * Preemptively uses BASIC auth rather than negotiating the auth scheme.
     */
    private class HttpComponentsClientHttpRequestFactoryBasicAuth extends HttpComponentsClientHttpRequestFactory {

        private HttpHost host;

        public HttpComponentsClientHttpRequestFactoryBasicAuth(HttpHost host, HttpClient httpClient) {
            super(httpClient);
            this.host = host;
        }

        protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
            return createHttpContext();
        }

        private HttpContext createHttpContext() {
            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(host, basicAuth);

            BasicHttpContext localcontext = new BasicHttpContext();
            localcontext.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
            return localcontext;
        }
    }

    private static class Form extends LinkedMultiValueMap<String, String> {

    }

    private static class MaxNifiEventParameters implements Function<UriComponentsBuilder, UriComponentsBuilder> {

        private String clusterNodeId;

        public MaxNifiEventParameters(String clusterNodeId) {
            this.clusterNodeId = clusterNodeId;
        }

        public UriComponentsBuilder apply(UriComponentsBuilder target) {
            UriComponentsBuilder result = target;

            if (!Strings.isNullOrEmpty(this.clusterNodeId)) {
                result = result.queryParam("clusterNodeId", this.clusterNodeId);
            }
            return result;
        }

    }

    private static class NifiFlowSyncParameters implements Function<UriComponentsBuilder, UriComponentsBuilder> {

        private String syncId;

        public NifiFlowSyncParameters(String syncId) {
            this.syncId = syncId;
        }

        public UriComponentsBuilder apply(UriComponentsBuilder target) {
            UriComponentsBuilder result = target;

            if (!Strings.isNullOrEmpty(this.syncId)) {
                result = result.queryParam("syncId", this.syncId);
            }
            return result;
        }

    }

    private static class TargetDatasourceCriteria implements DatasourceCriteria, Function<UriComponentsBuilder, UriComponentsBuilder> {

        private String name;
        private String owner;
        private DateTime createdOn;
        private DateTime createdAfter;
        private DateTime createdBefore;
        private Set<String> types = new HashSet<>();

        public UriComponentsBuilder apply(UriComponentsBuilder target) {
            UriComponentsBuilder result = target;

            if (!Strings.isNullOrEmpty(this.name)) {
                result = result.queryParam(NAME, this.name);
            }
            if (!Strings.isNullOrEmpty(this.owner)) {
                result = result.queryParam(OWNER, this.owner);
            }
            if (this.createdOn != null) {
                result = result.queryParam(ON, this.createdOn.toString());
            }
            if (this.createdAfter != null) {
                result = result.queryParam(AFTER, this.createdAfter.toString());
            }
            if (this.createdBefore != null) {
                result = result.queryParam(BEFORE, this.createdBefore.toString());
            }
            if (!this.types.isEmpty()) {
                result = result.queryParam(TYPE, types.toArray(new Object[types.size()]));
            }

            return result;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.DatasourceCriteria#name(java.lang.String)
         */
        @Override
        public DatasourceCriteria name(String name) {
            this.name = name;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.DatasourceCriteria#createdOn(org.joda.time.DateTime)
         */
        @Override
        public DatasourceCriteria createdOn(DateTime time) {
            this.createdOn = time;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.DatasourceCriteria#createdAfter(org.joda.time.DateTime)
         */
        @Override
        public DatasourceCriteria createdAfter(DateTime time) {
            this.createdAfter = time;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.DatasourceCriteria#createdBefore(org.joda.time.DateTime)
         */
        @Override
        public DatasourceCriteria createdBefore(DateTime time) {
            this.createdBefore = time;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.DatasourceCriteria#owner(java.lang.String)
         */
        @Override
        public DatasourceCriteria owner(String owner) {
            this.owner = owner;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.DatasourceCriteria#type(java.lang.Class, java.lang.Class[])
         */
        @Override
        @SuppressWarnings("unchecked")
        public DatasourceCriteria type(Class<? extends Datasource> type, Class<? extends Datasource>... others) {
            this.types.add(type.getSimpleName());
            for (Class<? extends Datasource> t : others) {
                this.types.add(t.getSimpleName());
            }
            return this;
        }
    }

    private static class TargetNflowCriteria implements NflowCriteria, Function<UriComponentsBuilder, UriComponentsBuilder> {

        private String category;
        private String name;
        private String sourceId;
        private String destinationId;

        public UriComponentsBuilder apply(UriComponentsBuilder target) {
            UriComponentsBuilder result = target;

            if (!Strings.isNullOrEmpty(this.name)) {
                result = result.queryParam(CATEGORY, this.category);
            }
            if (!Strings.isNullOrEmpty(this.name)) {
                result = result.queryParam(NAME, this.name);
            }
            if (!Strings.isNullOrEmpty(this.sourceId)) {
                result = result.queryParam(SRC_ID, this.sourceId);
            }
            if (!Strings.isNullOrEmpty(this.destinationId)) {
                result = result.queryParam(DEST_ID, this.destinationId);
            }

            return result;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.NflowCriteria#sourceDatasource(java.lang.String)
         */
        @Override
        public NflowCriteria sourceDatasource(String dsId) {
            this.sourceId = dsId;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.NflowCriteria#destinationDatasource(java.lang.String)
         */
        @Override
        public NflowCriteria destinationDatasource(String dsId) {
            this.destinationId = dsId;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.client.NflowCriteria#name(java.lang.String)
         */
        @Override
        public NflowCriteria name(String name) {
            this.name = name;
            return this;
        }

        /* (non-Javadoc)
         * @see com.onescorpin.metadata.rest.model.nflow.NflowCriteria#category(java.lang.String)
         */
        @Override
        public NflowCriteria category(String category) {
            this.category = category;
            return this;
        }
    }

    private class NflowBuilderImpl implements NflowBuilder {

        private String displayName;
        private String systemName;
        private String systemCategoryName;
        private String description;
        private String owner;
        private List<Metric> preconditionMetrics = new ArrayList<>();
        private Properties properties = new Properties();

        public NflowBuilderImpl(String category, String name) {
            this.systemCategoryName = category;
            this.systemName = name;
        }

        @Override
        public NflowBuilder displayName(String name) {
            this.displayName = name;
            return this;
        }

        @Override
        public NflowBuilder description(String descr) {
            this.description = descr;
            return this;
        }

        @Override
        public NflowBuilder owner(String owner) {
            this.owner = owner;
            return this;
        }

        @Override
        public NflowBuilder preconditionMetric(Metric... metrics) {
            for (Metric m : metrics) {
                this.preconditionMetrics.add(m);
            }
            return this;
        }

        @Override
        public NflowBuilder property(String key, String value) {
            this.properties.setProperty(key, value);
            return this;
        }

        @Override
        public Nflow build() {
            Nflow nflow = new Nflow();
            NflowCategory nflowCategory = new NflowCategory();
            nflowCategory.setSystemName(this.systemCategoryName);
            nflow.setCategory(nflowCategory);
            nflow.setSystemName(this.systemName);
            nflow.setDisplayName(this.displayName != null ? this.displayName : this.systemName);
            nflow.setDescription(this.description);
            nflow.setOwner(this.owner);
            nflow.setPrecondition(createTrigger(this.preconditionMetrics));
            nflow.setProperties(properties);

            return nflow;
        }

        @Override
        public Nflow post() {
            Nflow nflow = build();
            return postNflow(nflow);
        }

    }

    private abstract class DatasourceBuilderImpl<B extends DatasourceBuilder<B, D>, D extends Datasource> implements DatasourceBuilder<B, D> {

        protected String name;
        protected String description;
        protected String owner;
        protected boolean encrypted;
        protected boolean compressed;

        public DatasourceBuilderImpl(String name) {
            this.name = name;
        }

        @Override
        public B description(String descr) {
            this.description = descr;
            return self();
        }

        @Override
        public B owner(String owner) {
            this.owner = owner;
            return self();
        }

        @Override
        public B encrypted(boolean flag) {
            this.encrypted = flag;
            return self();
        }

        @Override
        public B compressed(boolean flag) {
            this.compressed = flag;
            return self();
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }

    private class HiveTableDatasourceBuilderImpl
        extends DatasourceBuilderImpl<HiveTableDatasourceBuilder, HiveTableDatasource>
        implements HiveTableDatasourceBuilder {

        private String database;
        private String tableName;
        private String modifiers;
        private List<HiveTableColumn> fields = new ArrayList<>();
        private List<HiveTablePartition> partitions = new ArrayList<>();

        public HiveTableDatasourceBuilderImpl(String name) {
            super(name);
        }

        @Override
        public HiveTableDatasourceBuilder database(String name) {
            this.database = name;
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder tableName(String name) {
            this.tableName = name;
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder modifiers(String mods) {
            this.modifiers = mods;
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder field(String name, String type) {
            this.fields.add(new HiveTableColumn(name, type));
            return this;
        }

        @Override
        public HiveTableDatasourceBuilder partition(String name, String formula, String value, String... more) {
            this.partitions.add(new HiveTablePartition(name, formula, value, more));
            return this;
        }

        @Override
        public HiveTableDatasource build() {
            HiveTableDatasource src = new HiveTableDatasource();
            src.setName(this.name);
            src.setDescription(this.description);
            src.setOwner(this.owner);
            src.setEncrypted(this.encrypted);
            src.setCompressed(this.compressed);
            src.setDatabase(this.database);
            src.setTableName(this.tableName);
            src.setModifiers(this.modifiers);
            src.getColumns().addAll(this.fields);
            src.getPartitions().addAll(this.partitions);

            return src;
        }

        @Override
        public HiveTableDatasource post() {
            HiveTableDatasource ds = build();
            return postDatasource(ds);
        }
    }

    private class DirectoryDatasourceBuilderImpl
        extends DatasourceBuilderImpl<DirectoryDatasourceBuilder, DirectoryDatasource>
        implements DirectoryDatasourceBuilder {

        private String path;
        private List<String> regexList = new ArrayList<>();
        private List<String> globList = new ArrayList<>();

        public DirectoryDatasourceBuilderImpl(String name) {
            super(name);
        }

        @Override
        public DirectoryDatasourceBuilder path(String path) {
            this.path = path;
            return this;
        }

        @Override
        public DirectoryDatasourceBuilder regexPattern(String pattern) {
            this.regexList.add(pattern);
            return this;
        }

        @Override
        public DirectoryDatasourceBuilder globPattern(String pattern) {
            this.globList.add(pattern);
            return this;
        }

        @Override
        public DirectoryDatasource build() {
            DirectoryDatasource src = new DirectoryDatasource();
            src.setName(this.name);
            src.setDescription(this.description);
            src.setOwner(this.owner);
            src.setEncrypted(this.encrypted);
            src.setCompressed(this.compressed);
            src.setPath(this.path);

            for (String p : this.regexList) {
                src.addRegexPattern(p);
            }

            for (String p : this.globList) {
                src.addGlobPattern(p);
            }

            return src;
        }

        @Override
        public DirectoryDatasource post() {
            DirectoryDatasource dds = build();
            return postDatasource(dds);
        }

    }



}
