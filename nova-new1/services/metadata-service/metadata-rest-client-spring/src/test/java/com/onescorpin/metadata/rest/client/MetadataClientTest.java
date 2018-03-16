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

import com.onescorpin.metadata.api.op.NflowDependencyDeltaResults;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceNflow;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.DirectoryDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTableDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTablePartition;
import com.onescorpin.metadata.rest.model.extension.ExtensibleTypeDescriptor;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.op.DataOperation;
import com.onescorpin.metadata.rest.model.op.DataOperation.State;
import com.onescorpin.metadata.rest.model.op.Dataset;
import com.onescorpin.metadata.rest.model.op.Dataset.ChangeType;
import com.onescorpin.metadata.rest.model.op.Dataset.ContentType;
import com.onescorpin.metadata.rest.model.op.HiveTablePartitions;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

//@Ignore  // Requires a running metadata server
public class MetadataClientTest {

    private MetadataClient client;

    @BeforeClass
    public void connect() {
        client = new MetadataClient(URI.create("http://localhost:8420/api/v1/metadata/"));
    }


    //    @Test
    public void testGetExtensibleTypes() {
        List<ExtensibleTypeDescriptor> types = client.getExtensibleTypes();

        assertThat(types).extracting("name")
            .contains("nflow", "datasource");
        assertThat(types.get(0).getFields()).extracting("name")
            .isNotEmpty();
    }

    //    @Test
    public void testGetExtensibleTypeByName() {
        ExtensibleTypeDescriptor type = client.getExtensibleType("nflow");

        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo("nflow");
    }

    //    @Test(dependsOnMethods="testGetExtensibleTypeByName")
    public void testGetExtensibleTypeById() {
        ExtensibleTypeDescriptor nflow = client.getExtensibleType("nflow");
        ExtensibleTypeDescriptor type = client.getExtensibleType(nflow.getId());

        assertThat(type).isNotNull();
        assertThat(type.getName()).isEqualTo("nflow");
    }

    //    @Test()
    public void testCreateNflowSubtype() {
        ExtensibleTypeDescriptor subtype = new ExtensibleTypeDescriptor("testNflow", "nflow");
    }

    //    @Test
    public void testBuildNflow() throws ParseException {
        Nflow nflow = buildNflow("category", "nflow1").post();

        assertThat(nflow).isNotNull();

        nflow = client.getNflow(nflow.getId());

        assertThat(nflow).isNotNull();
    }

    //    @Test
    public void testGetNflows() throws ParseException {
        List<Nflow> nflows = client.getNflows();

        assertThat(nflows).isNotNull().isNotEmpty();
    }

    //    @Test
    public void testMergeNflowProperties() throws ParseException {
        Nflow nflow = buildNflow("category", "nflow1").post();

        assertThat(nflow).isNotNull();
        assertThat(nflow.getProperties()).isNotNull().hasSize(1).containsEntry("key1", "value1");

        Properties props = new Properties();
        props.setProperty("testKey", "testValue");

        Properties result = client.mergeNflowProperties(nflow.getId(), props);

        assertThat(result).isNotNull().hasSize(2).containsEntry("testKey", "testValue");
    }

    //    @Test
    public void testUpdateNflow() throws ParseException {
        Nflow nflow = buildNflow("category", "nflow1").post();

        assertThat(nflow.getDescription()).isEqualTo("nflow1 nflow");
        assertThat(nflow.getState()).isEqualTo(Nflow.State.ENABLED);
//        assertThat(nflow.isInitialized()).isFalse();

        nflow.setDescription("Description changed");
        nflow.setState(Nflow.State.DISABLED);
//        nflow.setInitialized(true);

        Nflow result = client.updateNflow(nflow);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo("Description changed");
        assertThat(result.getState()).isEqualTo(Nflow.State.DISABLED);
//        assertThat(nflow.isInitialized()).isTrue();
    }

    //    @Test
    public void testAddNflowSource() throws ParseException {
        Nflow nflow = buildNflow("category", "nflow1").post();
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();

        Nflow result = client.addSource(nflow.getId(), ds.getId());

        assertThat(result).isNotNull();
    }

    //    @Test
    public void testAddNflowDestination() throws ParseException {
        Nflow nflow = buildNflow("category", "nflow1").post();
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();

        Nflow result = client.addDestination(nflow.getId(), ds.getId());

        assertThat(result).isNotNull();
    }

    //    @Test
    public void testBuildHiveTableDatasource() {
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();

        assertThat(ds).isNotNull();
    }

    //    @Test
    public void testBuildDirectoryDatasource() {
        DirectoryDatasource ds = buildDirectoryDatasource("test-dir").post();

        assertThat(ds).isNotNull();
    }

    //    @Test
    public void testListDatasources() {
        buildDirectoryDatasource("ds1").post();
        buildHiveTableDatasource("ds2").post();
        buildDirectoryDatasource("ds3").post();

        List<Datasource> list = client.getDatasources();

        assertThat(list)
            .isNotNull()
            .isNotEmpty();
    }

    //    @Test
    public void testBeginOperation() throws ParseException {
        Nflow nflow = buildNflow("category", "nflow1").post();
        HiveTableDatasource ds = buildHiveTableDatasource("test-table").post();
        nflow = client.addDestination(nflow.getId(), ds.getId());
        String destId = nflow.getDestinations().iterator().next().getId();

        DataOperation op = client.beginOperation(destId, "");

        assertThat(op).isNotNull();
    }

    //    @Test
    public void testCompleteOperation() throws ParseException {
        Nflow nflowA = buildNflow("category", "nflowA").post();
        HiveTableDatasource dsA = buildHiveTableDatasource("test-table").post();
        nflowA = client.addDestination(nflowA.getId(), dsA.getId());

        Nflow nflowB = buildNflow("category", "nflowB", "category", "nflowA").post();
        nflowB = client.addSource(nflowB.getId(), dsA.getId());
        String destA = nflowA.getDestinations().iterator().next().getId();

        DataOperation op = client.beginOperation(destA, "");
        op.setState(State.SUCCESS);

        HiveTablePartitions changeSet = new HiveTablePartitions();
        changeSet.setPartitions(Arrays.asList(new HiveTablePartition("month", null, "Jan", "Feb"),
                                              new HiveTablePartition("year", null, "2015", "2016")));
        Dataset dataset = new Dataset(new DateTime(), dsA, ChangeType.UPDATE, ContentType.PARTITIONS, changeSet);
        op.setDataset(dataset);

        op = client.updateDataOperation(op);

        assertThat(op).isNotNull();
    }

    //    @Test
    public void testCheckPrecondition() throws ParseException {
        Nflow nflowA = buildNflow("category", "nflowA").post();
        Nflow nflowB = buildNflow("category", "nflowB", "category", "nflowA").post();

        HiveTableDatasource dsA = buildHiveTableDatasource("test-table").post();
        nflowA = client.addDestination(nflowA.getId(), dsA.getId());
        String destIdA = nflowA.getDestinations().iterator().next().getId();

        DataOperation op = client.beginOperation(destIdA, "");
        op.setState(State.SUCCESS);
        op.setDataset(new Dataset(new DateTime(), dsA, ChangeType.UPDATE, ContentType.PARTITIONS));
        op = client.updateDataOperation(op);

        ServiceLevelAssessment assmt = client.assessPrecondition(nflowB.getId());

        assertThat(assmt).isNotNull();
    }

    // @Test
    public void testGetNflowDependencyDeltas() {
        NflowDependencyDeltaResults props = client.getNflowDependencyDeltas("90056286-a3b0-493c-89a4-91cb1e7529b6");

        assertThat(props).isNotNull();
    }

    private NflowBuilder buildNflow(String category, String name) throws ParseException {
        return client.buildNflow(category, name)
            .description(name + " nflow")
            .owner("owner")
            .displayName(name)
            .property("key1", "value1");
//                .preconditionMetric(NflowExecutedSinceScheduleMetric.named(name, "0 0 6 * * ? *"));
    }

    private NflowBuilder buildNflow(String category, String name, String dependentCategory, String dependent) throws ParseException {
        return client.buildNflow(category, name)
            .description(name + " nflow")
            .owner("owner")
            .displayName(name)
            .property("key1", "value1")
            .preconditionMetric(new NflowExecutedSinceNflow(dependentCategory, dependent, category, name));
    }

    private DirectoryDatasourceBuilder buildDirectoryDatasource(String name) {
        return client.buildDirectoryDatasource(name)
            .description(name + " datasource")
            .compressed(true)
            .owner("owner")
            .path("/tmp/test")
            .globPattern("*.txt");
    }

    private HiveTableDatasourceBuilder buildHiveTableDatasource(String name) {
        return client.buildHiveTableDatasource(name)
            .description(name + " datasource")
            .encrypted(true)
            .owner("owner")
            .database("testdb")
            .tableName("test_table")
            .field("key", "INT")
            .field("value", "VARCHAR")
            .partition("month", null, "jan", "feb", "mar")
            .partition("year", null, "2016");
    }
}
