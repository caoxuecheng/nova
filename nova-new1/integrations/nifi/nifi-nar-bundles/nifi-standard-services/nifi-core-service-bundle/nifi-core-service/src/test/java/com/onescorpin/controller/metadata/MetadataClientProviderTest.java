package com.onescorpin.controller.metadata;

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

import com.onescorpin.metadata.api.sla.DatasourceUpdatedSinceNflowExecuted;
import com.onescorpin.metadata.api.sla.DatasourceUpdatedSinceSchedule;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceNflow;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceSchedule;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.DirectoryDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTableDatasource;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.nflow.NflowDestination;
import com.onescorpin.metadata.rest.model.op.DataOperation;
import com.onescorpin.metadata.rest.model.op.DataOperation.State;
import com.onescorpin.metadata.rest.model.op.Dataset;
import com.onescorpin.nifi.v2.core.metadata.MetadataClientProvider;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;
import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore  // TODO Requires the metadata server running.  Add support for embedded test server.
public class MetadataClientProviderTest {

    private MetadataClientProvider provider;

    @Before
    public void setUp() throws Exception {
        this.provider = new MetadataClientProvider();
    }

    @Test
    public void testEnsureNflow() {
        Nflow nflow = this.provider.ensureNflow("category", "test1", "");

        assertThat(nflow).isNotNull();

        String nflowId = nflow.getId();
        nflow = this.provider.ensureNflow("category", "test1", "");

        assertThat(nflow).isNotNull();
        assertThat(nflow.getId()).isEqualTo(nflowId);
    }

    @Test
    public void testGetDatasourceByName() {
        this.provider.ensureDirectoryDatasource("test2", "", Paths.get("aaa", "bbb"));
        Datasource ds = this.provider.getDatasourceByName("test2");

        assertThat(ds).isNotNull();
    }

    @Test
    public void testEnsureNflowSource() {
        Nflow nflow = this.provider.ensureNflow("category", "test3", "");
        Datasource ds = this.provider.ensureDirectoryDatasource("test3", "", Paths.get("aaa", "bbb"));
        nflow = this.provider.ensureNflowSource(nflow.getId(), ds.getId());

        assertThat(nflow.getSources()).hasSize(1);

        nflow = this.provider.ensureNflowSource(nflow.getId(), ds.getId());

        assertThat(nflow.getSources()).hasSize(1);
    }

    @Test
    public void testEnsureNflowDestination() {
        Nflow nflow = this.provider.ensureNflow("category", "test4", "");
        Datasource ds = this.provider.ensureDirectoryDatasource("test4", "", Paths.get("aaa", "bbb"));
        nflow = this.provider.ensureNflowDestination(nflow.getId(), ds.getId());

        assertThat(nflow.getDestinations()).hasSize(1);

        nflow = this.provider.ensureNflowDestination(nflow.getId(), ds.getId());

        assertThat(nflow.getDestinations()).hasSize(1);
    }

    @Test
    public void testEnsurePrecondition() {
        Nflow nflow = this.provider.ensureNflow("category", "test5", "");
        try {
            nflow = this.provider.ensurePrecondition(nflow.getId(),
                                                    new DatasourceUpdatedSinceNflowExecuted("ds5", "test5"),
                                                    new DatasourceUpdatedSinceSchedule("ds5", "0 0 6 * * ? *"),
                                                    new NflowExecutedSinceNflow("category", "dep5", "category", "test5"),
                                                    new NflowExecutedSinceSchedule("category", "test5", "0 0 6 * * ? *"),
                                                    new com.onescorpin.metadata.api.sla.WithinSchedule("0 0 6 * * ? *", "2 hours"));
        } catch (ParseException e) {
            e.printStackTrace();
            ;
        }

        assertThat(nflow).isNotNull();
    }

    @Test
    public void testEnsureDirectoryDatasource() {
        this.provider.ensureDirectoryDatasource("test6", "", Paths.get("aaa", "bbb"));
        Datasource ds = this.provider.getDatasourceByName("test6");

        assertThat(ds).isNotNull();
        assertThat(ds).isInstanceOf(DirectoryDatasource.class);

        String dsId = ds.getId();
        DirectoryDatasource dds = (DirectoryDatasource) ds;

        assertThat(dds.getPath()).contains("aaa/bbb");

        ds = this.provider.ensureDirectoryDatasource("test6", "", Paths.get("aaa", "bbb"));

        assertThat(ds).isNotNull();
        assertThat(ds.getId()).isEqualTo(dsId);
    }

    @Test
    public void testEnsureHiveTableDatasource() {
        this.provider.ensureHiveTableDatasource("test7", "", "testdb", "test_table");
        Datasource ds = this.provider.getDatasourceByName("test7");

        assertThat(ds).isNotNull();
        assertThat(ds).isInstanceOf(HiveTableDatasource.class);

        String dsId = ds.getId();
        HiveTableDatasource dds = (HiveTableDatasource) ds;

        assertThat(dds.getTableName()).contains("test_table");

        ds = this.provider.ensureHiveTableDatasource("test7", "", "testdb", "test_table");

        assertThat(ds).isNotNull();
        assertThat(ds.getId()).isEqualTo(dsId);
    }

    @Test
    public void testBeginOperation() {
        Nflow nflow = this.provider.ensureNflow("category", "test9", "");
        Datasource ds = this.provider.ensureDirectoryDatasource("test9", "", Paths.get("aaa", "bbb"));
        nflow = this.provider.ensureNflowDestination(nflow.getId(), ds.getId());
        NflowDestination dest = nflow.getDestination(ds.getId());

        DataOperation op = this.provider.beginOperation(dest, new DateTime());

        assertThat(op).isNotNull();
        assertThat(op.getState()).isEqualTo(State.IN_PROGRESS);
    }

    @Test
    public void testCompleteOperation() {
        Nflow nflow = this.provider.ensureNflow("category", "test10", "");
        DirectoryDatasource ds = this.provider.ensureDirectoryDatasource("test10", "", Paths.get("aaa", "bbb"));
        nflow = this.provider.ensureNflowDestination(nflow.getId(), ds.getId());
        NflowDestination dest = nflow.getDestination(ds.getId());
        DataOperation op = this.provider.beginOperation(dest, new DateTime());

        Dataset set = this.provider.createDataset(ds, Paths.get("a.txt"), Paths.get("b.txt"));
        op = this.provider.completeOperation(op.getId(), "", set);

        assertThat(op).isNotNull();
        assertThat(op.getState()).isEqualTo(State.SUCCESS);
    }

}
