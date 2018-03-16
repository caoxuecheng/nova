package com.onescorpin.nifi.core.api.metadata;

/*-
 * #%L
 * onescorpin-nifi-core-service-api
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
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.DirectoryDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTableDatasource;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.nflow.NflowDestination;
import com.onescorpin.metadata.rest.model.op.DataOperation;
import com.onescorpin.metadata.rest.model.op.DataOperation.State;
import com.onescorpin.metadata.rest.model.op.Dataset;
import com.onescorpin.metadata.rest.model.op.HiveTablePartitions;
import com.onescorpin.metadata.sla.api.Metric;

import org.joda.time.DateTime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;

/**
 * Provides services for working with nflows and data sources
 */
public interface MetadataProvider {

    /**
     * gets the id for the nflow given
     *
     * @param category the category containing the nflow
     * @param nflowName the name of the nflow
     * @return a nflow id
     */
    String getNflowId(String category, String nflowName);

    /**
     * returns the api service class used to obtain information about nflow deltas
     *
     * @param nflowId the nflow in question
     * @return a service object to get more information about nflow deltas
     */
    NflowDependencyDeltaResults getNflowDependentResultDeltas(String nflowId);

    /**
     * Ensures the nflow exists in the metadata store.  Finds an existing nflow of if none exists then one is created.
     *
     * @param categoryName The category for the nflow
     * @param nflowName     The name of the nflow
     * @param description  A description to associate with the nflow
     * @return the nflow
     */
    Nflow ensureNflow(String categoryName, String nflowName, String description);

    /**
     * gets the data source service by the given name
     *
     * @param dsName the data source name
     * @return the data source description
     */
    Datasource getDatasourceByName(String dsName);

    /**
     * ensures the nflow source given by the nflowId and datasourceId
     *
     * @param nflowId       the nflow id
     * @param datasourceId the datasource id
     * @return the nflow
     */
    Nflow ensureNflowSource(String nflowId, String datasourceId);

    /**
     * ensures the nflow destination given by the nflowId and datasourceId
     *
     * @param nflowId       the nflow id
     * @param datasourceId the datasource id
     * @return the nflow
     */
    Nflow ensureNflowDestination(String nflowId, String datasourceId);

    /**
     * gets, or creates a nflow, based on certain preconditions given as Metric's
     *
     * @param nflowId  the nflow id
     * @param metrics the conditions to be met
     * @return the nflow
     */
    Nflow ensurePrecondition(String nflowId, Metric... metrics);

    /**
     * merge the given properties to the current nflow properties
     *
     * @param nflowId the nflow id
     * @param props  the datasource id
     * @return the updated properties
     */
    Properties updateNflowProperties(String nflowId, Properties props);

    /**
     * gets, or creates, a datasource
     *
     * @param datasetName the dataset name
     * @param descr       A description for the datasource
     * @param path        the path for the data
     * @return a directory data source that can be used to fetch or modify metadata
     */
    DirectoryDatasource ensureDirectoryDatasource(String datasetName, String descr, Path path);

    /**
     * gets, or creates, a table in hive to use a data source
     *
     * @param datasetName  the dataset name
     * @param descr        a description for the data source
     * @param databaseName the database name where the table (will) reside(s)
     * @param tableName    the name of the table
     * @return a HiveTableDatasource that can be used to fetch or modify table metadata
     */
    HiveTableDatasource ensureHiveTableDatasource(String datasetName, String descr, String databaseName, String tableName);

    /**
     * creates a file system Dataset at one or more paths
     *
     * @param dds   the directory datasource which describes the locations of the data
     * @param paths one or more paths
     * @return a dataset object that is used to fetch or modify meta data
     */
    Dataset createDataset(DirectoryDatasource dds, Path... paths);

    /**
     * create a data set that can be used to describe the metadata for a directory data source and it's paths
     *
     * @param dds   the directory datasource which describes the dataset
     * @param paths one or more paths
     * @return a dataset object that is used to fetch or modify meta data
     */
    Dataset createDataset(DirectoryDatasource dds, ArrayList<Path> paths);

    /**
     * create a data set that can be used to describe the metadata for a hive data source and it's partitions
     *
     * @param hds        the datasource that describes the hive table
     * @param partitions the partitions of the table
     * @return a dataset object that is used to fetch or modify meta data
     */
    Dataset createDataset(HiveTableDatasource hds, HiveTablePartitions partitions);

    /**
     * Begin tracking the run time of the operation on the nflowDestination
     *
     * @param nflowDestination the nflow destination
     * @param opStart         the time it starts
     * @return a data operation
     */
    DataOperation beginOperation(NflowDestination nflowDestination, DateTime opStart);

    /**
     * complete the operation begun by beginOperation
     *
     * @param id        the id of the operation
     * @param status    the status of the operation
     * @param changeSet the changeset resulting from the operation
     * @return a data operation object which is used to track the metadata of the operation
     */
    DataOperation completeOperation(String id, String status, Dataset changeSet);

    /**
     * complete the operation begun by beginOperation
     *
     * @param id     the id of the operation
     * @param status the status of the operation
     * @param state  the state of the operation
     * @return a data operation object which is used to track the metadata of the operation
     */
    DataOperation completeOperation(String id, String status, State state);

    /**
     * Gets the properties of the specified nflow.
     *
     * @param id the nflow id
     * @return the properties
     */
    Properties getNflowProperties(@Nonnull final String id);

    /**
     * Gets the nflow for given category and nflow names
     *
     * @param category category system name
     * @param nflow nflow system name
     * @return the nflow definition
     */
    Nflow getNflow(@Nonnull final String category, @Nonnull final String nflow);

    /**
     * Merges the specified properties into the nflow's properties.
     *
     * @param id    the nflow id
     * @param props the new properties
     * @return the merged properties
     */
    Properties mergeNflowProperties(@Nonnull final String id, @Nonnull final Properties props);

    /**
     * Gets the data source with the specified id.
     *
     * @param id the data source id
     * @return the data source, if found
     */
    Optional<Datasource> getDatasource(@Nonnull String id);
}
