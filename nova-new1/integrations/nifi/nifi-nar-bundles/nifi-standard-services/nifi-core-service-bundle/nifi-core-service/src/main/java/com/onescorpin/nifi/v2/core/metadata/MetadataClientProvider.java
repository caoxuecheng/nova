package com.onescorpin.nifi.v2.core.metadata;

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

import com.onescorpin.metadata.api.op.NflowDependencyDeltaResults;
import com.onescorpin.metadata.rest.client.MetadataClient;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.DatasourceCriteria;
import com.onescorpin.metadata.rest.model.data.DirectoryDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTableDatasource;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.nflow.NflowDestination;
import com.onescorpin.metadata.rest.model.op.DataOperation;
import com.onescorpin.metadata.rest.model.op.DataOperation.State;
import com.onescorpin.metadata.rest.model.op.Dataset;
import com.onescorpin.metadata.rest.model.op.Dataset.ChangeType;
import com.onescorpin.metadata.rest.model.op.Dataset.ContentType;
import com.onescorpin.metadata.rest.model.op.FileList;
import com.onescorpin.metadata.rest.model.op.HiveTablePartitions;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.nifi.core.api.metadata.MetadataProvider;

import org.joda.time.DateTime;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;


public class MetadataClientProvider implements MetadataProvider {

    private MetadataClient client;

    /**
     * constructor creates a MetaDataClientProvider with the default URI constant
     */
    public MetadataClientProvider() {
        this(URI.create("http://localhost:8077/api/v1/metadata"));
    }

    /**
     * constructor creates a MetaDataClientProvider with the URI provided
     *
     * @param baseUri the REST endpoint of the Metadata store
     */
    public MetadataClientProvider(URI baseUri) {
        this(new MetadataClient(baseUri));
    }

    public MetadataClientProvider(URI baseUri, String username, String password) {
        this(new MetadataClient(baseUri, username, password));
    }

    /**
     * constructor creates a MetadataClientProvider with the required {@link MetadataClient}
     *
     * @param client the MetadataClient will be used to connect with the Metadata store
     */
    public MetadataClientProvider(MetadataClient client) {
        super();
        this.client = client;
    }

    @Override
    public String getNflowId(String category, String nflowName) {
        Nflow nflow = getNflow(category, nflowName);
        return nflow == null ? null : nflow.getId();
    }

    @Override
    public Nflow getNflow(@Nonnull String category, @Nonnull String nflowName) {
        List<Nflow> nflows = this.client.getNflows(this.client.nflowCriteria().category(category).name(nflowName));

        if (nflows.isEmpty()) {
            return null;
        } else {
            return nflows.get(0);
        }
    }

    @Override
    public NflowDependencyDeltaResults getNflowDependentResultDeltas(String nflowId) {
        return this.client.getNflowDependencyDeltas(nflowId);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#ensureNflow(java.lang.String, java.lang.String)
     */
    @Override
    public Nflow ensureNflow(String categoryName, String nflowName, String descr) {
        return this.client
            .buildNflow(categoryName, nflowName)
            .description(descr)
            .post();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#getDatasourceByName(java.lang.String)
     */
    @Override
    public Datasource getDatasourceByName(String dsName) {
        DatasourceCriteria criteria = this.client.datasourceCriteria().name(dsName);
        List<Datasource> list = this.client.getDatasources(criteria);

        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#ensureNflowSource(java.lang.String, java.lang.String)
     */
    @Override
    public Nflow ensureNflowSource(String nflowId, String datasourceId) {
        return this.client.addSource(nflowId, datasourceId);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#ensureNflowDestination(java.lang.String, java.lang.String)
     */
    @Override
    public Nflow ensureNflowDestination(String nflowId, String datasourceId) {
        return this.client.addDestination(nflowId, datasourceId);
    }

    @Override
    public Properties updateNflowProperties(String nflowId, Properties props) {
        return this.client.mergeNflowProperties(nflowId, props);
    }

    @Override
    public Nflow ensurePrecondition(String nflowId, Metric... metrics) {
        return this.client.setPrecondition(nflowId, metrics);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#ensureDirectoryDatasource(java.lang.String, java.lang.String, java.nio.file.Path)
     */
    @Override
    public DirectoryDatasource ensureDirectoryDatasource(String datasetName, String descr, Path path) {
        return this.client.buildDirectoryDatasource(datasetName)
            .description(descr)
            .path(path.toString())
            .post();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#ensureHiveTableDatasource(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public HiveTableDatasource ensureHiveTableDatasource(String dsName, String descr, String databaseName, String tableName) {
        return this.client.buildHiveTableDatasource(dsName)
            .description(descr)
            .database(databaseName)
            .tableName(tableName)
            .post();
    }

    @Override
    public Dataset createDataset(DirectoryDatasource dds, Path... paths) {
        return createDataset(dds, new ArrayList<>(Arrays.asList(paths)));
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#createDatasourceSet(com.onescorpin.metadata.rest.model.data.DirectoryDatasource, java.util.ArrayList)
     */
    @Override
    public Dataset createDataset(DirectoryDatasource dds, ArrayList<Path> paths) {
        FileList files = new FileList(paths);
        return new Dataset(dds, ChangeType.UPDATE, ContentType.FILES, files);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#createChangeSet(com.onescorpin.metadata.rest.model.data.HiveTableDatasource, int)
     */
    @Override
    public Dataset createDataset(HiveTableDatasource hds, HiveTablePartitions partitions) {
        return new Dataset(hds, ChangeType.UPDATE, ContentType.PARTITIONS, partitions);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#beginOperation(com.onescorpin.metadata.rest.model.nflow.NflowDestination, org.joda.time.DateTime)
     */
    @Override
    public DataOperation beginOperation(NflowDestination nflowDestination, DateTime opStart) {
        return this.client.beginOperation(nflowDestination.getId(), "");
    }

    /* (non-Javadoc)
     * @see com.onescorpin.controller.metadata.MetadataProvider#updateOperation(java.lang.String, java.lang.String, com.onescorpin.metadata.rest.model.data.Datasource)
     */
    @Override
    public DataOperation completeOperation(String id, String status, Dataset dataset) {
        DataOperation op = this.client.getDataOperation(id);
        op.setStatus(status);
        op.setDataset(dataset);
        op.setState(State.SUCCESS);

        return this.client.updateDataOperation(op);
    }

    @Override
    public DataOperation completeOperation(String id, String string, State state) {
        DataOperation op = this.client.getDataOperation(id);
        op.setState(state);

        return this.client.updateDataOperation(op);
    }

    @Override
    public Properties getNflowProperties(@Nonnull String id) {
        return client.getNflowProperties(id);
    }

    @Override
    public Properties mergeNflowProperties(@Nonnull String id, @Nonnull Properties props) {
        return client.mergeNflowProperties(id, props);
    }

    @Override
    public Optional<Datasource> getDatasource(@Nonnull final String id) {
        return client.getDatasource(id);
    }
}
