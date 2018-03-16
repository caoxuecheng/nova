package com.onescorpin.ingest;

/*-
 * #%L
 * onescorpin-nifi-core-processors
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

import com.google.common.collect.ImmutableSet;
import com.klarna.hiverunner.HiveShell;
import com.klarna.hiverunner.StandaloneHiveRunner;
import com.klarna.hiverunner.annotations.HiveProperties;
import com.klarna.hiverunner.annotations.HiveRunnerSetup;
import com.klarna.hiverunner.annotations.HiveSQL;
import com.klarna.hiverunner.config.HiveRunnerConfig;
import com.onescorpin.util.ColumnSpec;
import com.onescorpin.util.TableRegisterConfiguration;
import com.onescorpin.util.TableType;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(StandaloneHiveRunner.class)
public class TableRegisterSupportTest {

    /**
     * Explicit test class configuration of the HiveRunner runtime. See {@link HiveRunnerConfig} for further details.
     */
    @HiveRunnerSetup
    public final HiveRunnerConfig CONFIG = new HiveRunnerConfig() {{
        setHiveExecutionEngine("mr");
    }};
    final Connection connection = Mockito.mock(Connection.class);
    /**
     * Cater for all the parameters in the script that we want to test. Note that the "hadoop.tmp.dir" is one of the dirs defined by the test harness
     */
    @HiveProperties
    public Map<String, String> hiveProperties = MapUtils.putAll(new HashMap<String, String>(), new String[]{
        "MY.HDFS.DIR", "${hadoop.tmp.dir}",
        "my.schema", "bar",
        });

    /**
     * Define the script files under test. The files will be loaded in the given order. <p/> The HiveRunner instantiate and inject the HiveShell
     */
    @HiveSQL(files = {
        "hive-test-support/create_table.sql"
    }, encoding = "UTF-8")
    private HiveShell hiveShell;

    @Test
    public void testShowLocation() {

        List<Object[]> values = hiveShell.executeStatement("show table extended like foo");
        for (Object[] o : values) {
            String value = o[0].toString();
            if (value.startsWith("location:")) {
                System.out.println(value.substring(9));
            }
        }

    }

    @Test
    public void testSplit() {

        // Extract schema vs table
        String[] schemaPart = "foo.bar".split("\\.");
        assertEquals(schemaPart.length, 2);
        String schema = schemaPart[0];
        String targetTable = schemaPart[1];
        assertNotNull(schema);
        assertNotNull(targetTable);

    }


    @Test
    public void testTableCreate() {
        ColumnSpec[] specs = ColumnSpec.createFromString("id|bigint|my comment\nname|string\ncompany|string|some description\nzip|string\nphone|string\nemail|string\ncountry|string\nhired|date");
        ColumnSpec[] parts = ColumnSpec.createFromString("year|int\ncountry|string");

        TableRegisterSupport support = new TableRegisterSupport(connection);
        TableType[] tableTypes = new TableType[]{TableType.NFLOW, TableType.INVALID, TableType.VALID, TableType.MASTER};
        for (TableType tableType : tableTypes) {
            String
                ddl =
                support.createDDL("bar", "employee", specs, parts, "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'", "stored as orc", "tblproperties (\"orc.compress\"=\"SNAPPY\")",
                                  tableType);
            // Hack to make a legal file root
            ddl = ddl.replace("LOCATION '", "LOCATION '${hiveconf:MY.HDFS.DIR}");
            hiveShell.execute(ddl);
        }
    }

    @Test
    public void testTableCreateS3() {
        ColumnSpec[] specs = ColumnSpec.createFromString("id|bigint|my comment\nname|string\ncompany|string|some description\nzip|string\nphone|string\nemail|string\ncountry|string\nhired|date");
        ColumnSpec[] parts = ColumnSpec.createFromString("year|int\ncountry|string");
        TableRegisterConfiguration conf = new TableRegisterConfiguration("s3a://testBucket/model.db/", "s3a://testBucket/model.db/", "s3a://testBucket/app/warehouse/");
        TableRegisterSupport support = new TableRegisterSupport(connection, conf);
        TableType[] tableTypes = new TableType[]{TableType.NFLOW, TableType.INVALID, TableType.VALID, TableType.MASTER};
        for (TableType tableType : tableTypes) {
            String ddl =
                support.createDDL("bar", "employee", specs, parts, "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'", "stored as orc", "tblproperties (\"orc.compress\"=\"SNAPPY\")",
                                  tableType);
            String location = StringUtils.substringBetween(ddl, "LOCATION '", "'");
            if (tableType == TableType.MASTER) {
                assertEquals("Master location does not match", "s3a://testBucket/app/warehouse/bar/employee", location);
            } else {
                assertEquals("Locations do not match", "s3a://testBucket/model.db/bar/employee/" + tableType.toString().toLowerCase(), location);
            }
        }
    }

    @Test
    public void testRemovingColumns() {
        ColumnSpec[] nflowSpecs = ColumnSpec.createFromString("id|string|my comment|0|0|0|id\n"
                                                         + "name|string||0|0|0|name\n"
                                                         + "company|string|some description|0|0|0|change_company\n"
                                                         + "zip|string||0|0|0|zip_code\n"
                                                         + "phone|string\n"
                                                         + "email|string\n"
                                                         + "country|string\n"
                                                         + "hired|string");

        ColumnSpec[] targetSpecs = ColumnSpec.createFromString("id|bigint|my comment|0|0|0|id\n"
                                                             + "name|string||0|0|0|name\n"
                                                             + "change_company|string|some description|0|0|0|company\n"
                                                             + "zip_code|string||0|0|0|zip\n"
                                                             + "email|string\n"
                                                             + "hired|date||0|0|0|hired");

        ColumnSpec[] parts = ColumnSpec.createFromString("year|int\ncountry|string");
        TableRegisterConfiguration conf = new TableRegisterConfiguration();

        TableRegisterSupport support = new TableRegisterSupport(connection, conf);
        ColumnSpec[] invalidColumnSpecs = support.adjustInvalidColumnSpec(nflowSpecs,targetSpecs);

            assertEquals(targetSpecs.length,invalidColumnSpecs.length);

        Map<String,ColumnSpec> nflowColumnSpecMap = Arrays.asList(nflowSpecs).stream().collect(Collectors.toMap(ColumnSpec::getName, Function.identity()));

            for(ColumnSpec invalid: invalidColumnSpecs)
            {
                if(StringUtils.isNotBlank(invalid.getOtherColumnName())) {
                    assertEquals(invalid.getDataType(), nflowColumnSpecMap.get(invalid.getOtherColumnName()).getDataType());
                }
            }

        TableType[] tableTypes = new TableType[]{TableType.NFLOW, TableType.INVALID, TableType.VALID, TableType.MASTER};
        for (TableType tableType : tableTypes) {
            ColumnSpec[] useColumnSpecs = targetSpecs;
            if(tableType == TableType.INVALID){
                useColumnSpecs = invalidColumnSpecs;
            }
            else if(tableType == TableType.NFLOW){
                useColumnSpecs = nflowSpecs;
            }


            String ddl = support.createDDL("source_table","target_table",useColumnSpecs,parts, "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'","stored as orc","tblproperties (\"orc.compress\"=\"SNAPPY\")",tableType);
            int i = 0;


        }
    }


    /**
     * Verify dropping a table.
     */
    @Test
    public void testDropTable() throws Exception {
        // Mock SQL objects
        final Statement statement = Mockito.mock(Statement.class);
        Mockito.when(statement.execute(Mockito.anyString())).then(invocation -> {
            final String sql = (String) invocation.getArguments()[0];
            if (sql.equals("DROP TABLE IF EXISTS `invalid`")) {
                throw new SQLException();
            }
            return true;
        });

        Mockito.when(connection.createStatement()).thenReturn(statement);

        // Test dropping table with success
        final TableRegisterSupport support = new TableRegisterSupport(connection);
        Assert.assertTrue(support.dropTable("`nflow`"));
        Mockito.verify(statement).execute("DROP TABLE IF EXISTS `nflow`");

        // Test dropping table with exception
        Assert.assertFalse(support.dropTable("`invalid`"));
    }

    /**
     * Verify exception if the connection is null.
     */
    @Test(expected = NullPointerException.class)
    public void testDropTableWithNullConnection() {
        new TableRegisterSupport(null).dropTable("invalid");
    }

    /**
     * Verify dropping multiple tables.
     */
    @Test
    public void testDropTables() throws Exception {
        // Mock SQL objects
        final Statement statement = Mockito.mock(Statement.class);
        Mockito.when(statement.execute(Mockito.anyString())).then(invocation -> {
            final String sql = (String) invocation.getArguments()[0];
            if (sql.startsWith("DROP TABLE IF EXISTS `invalid`")) {
                throw new SQLException();
            }
            return true;
        });

        final Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.createStatement()).thenReturn(statement);

        // Test dropping tables with success
        TableRegisterSupport support = new TableRegisterSupport(connection);
        Assert.assertTrue(support.dropTables("cat", "nflow", EnumSet.of(TableType.MASTER, TableType.VALID, TableType.INVALID), ImmutableSet.of("backup.nflow")));
        Mockito.verify(statement).execute("DROP TABLE IF EXISTS `cat`.`nflow`");
        Mockito.verify(statement).execute("DROP TABLE IF EXISTS `cat`.`nflow_valid`");
        Mockito.verify(statement).execute("DROP TABLE IF EXISTS `cat`.`nflow_invalid`");
        Mockito.verify(statement).execute("DROP TABLE IF EXISTS backup.nflow");

        // Test dropping tables with exception
        Assert.assertFalse(support.dropTables("invalid", "nflow", EnumSet.allOf(TableType.class), ImmutableSet.of()));
        Assert.assertFalse(support.dropTables("cat", "nflow", ImmutableSet.of(), ImmutableSet.of("`invalid`")));
    }

}
