package com.onescorpin.ingest;

/*-
 * #%L
 * nova-nifi-core-processors
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

import com.onescorpin.hive.util.HiveUtils;
import com.onescorpin.util.ColumnSpec;
import com.onescorpin.util.TableRegisterConfiguration;
import com.onescorpin.util.TableType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Generates and execute SQL queries for creating tables.
 */
public class TableRegisterSupport {

    public static final Logger logger = LoggerFactory.getLogger(TableRegisterSupport.class);

    private Connection conn;

    private TableRegisterConfiguration config;

    public TableRegisterSupport(Connection conn, TableRegisterConfiguration configuration) {
        Validate.notNull(conn, "connection required");
        Validate.notNull(configuration, "configuration required");
        this.conn = conn;
        this.config = configuration;
    }

    public TableRegisterSupport(Connection conn) {
        this(conn, new TableRegisterConfiguration());
    }

    protected TableRegisterSupport() {
    }


    public boolean registerDatabase(String source) {
        String ddl = "";
        boolean result = true;
        Validate.notNull(conn);
        try (final Statement st = conn.createStatement()) {
            ddl = createDatabaseDDL(source);
            st.execute(ddl);

        } catch (final SQLException e) {
            logger.error("Failed to create tables DDL {}", ddl, e);
            result = false;
        }
        return result;
    }

    /**
     * copy the columnSpecs and reset the datatypes to match that of the nflow column specs
     * @param nflowColumnSpecs
     * @param columnSpecs
     * @return
     */
    protected ColumnSpec[] adjustInvalidColumnSpec(ColumnSpec[] nflowColumnSpecs, ColumnSpec[] columnSpecs){
        //find the source data types from the _nflow table that match these columns and replace the data types
        Map<String,ColumnSpec> nflowColumnSpecMap = Arrays.asList(nflowColumnSpecs).stream().collect(Collectors.toMap(ColumnSpec::getName, Function.identity()));
        List<ColumnSpec> invalidColumnSpecs = Arrays.asList(columnSpecs).stream().map(c->  {
              ColumnSpec copy = new ColumnSpec(c);
               if(StringUtils.isNotBlank(copy.getOtherColumnName()) && nflowColumnSpecMap.containsKey(copy.getOtherColumnName())) {
                   ColumnSpec nflowSpec = nflowColumnSpecMap.get(copy.getOtherColumnName());
                   copy.setDataType(nflowSpec.getDataType());
                  }
                  return copy;
            }).collect(Collectors.toList());
        return invalidColumnSpecs.toArray(new ColumnSpec[invalidColumnSpecs.size()]);
    }

    /**
     * Registers the specified table by generating and executing a {@code CREATE TABLE} query.
     *
     * @param source                the name of the database
     * @param tableEntity           the name of the table
     * @param nflowColumnSpecs       the column specification for the nflow table
     * @param nflowFormatOptions     the format for the nflow table
     * @param targetFormatOptions   the format for the target table
     * @param partitions            the partitions for the target table
     * @param columnSpecs           the columns for the table
     * @param targetTableProperties the properties for the target table
     * @param tableType             the type of table
     * @param registerDatabase      {@code true} to create the database if it does not exist, or {@code false} to require an existing database
     * @return {@code true} if the table was registered, or {@code false} if there was an error
     */
    public boolean registerTable(String source, String tableEntity, ColumnSpec[] nflowColumnSpecs, String nflowFormatOptions, String targetFormatOptions, ColumnSpec[] partitions, ColumnSpec[]
        columnSpecs, String targetTableProperties, TableType tableType, boolean registerDatabase) {
        Validate.notNull(conn);

        //_invalid and _nflow tables should use the schema provided from the Source 'nflowColumnSpecs'.
        //_valid and the final nflow table should use the target schema
        ColumnSpec[] useColumnSpecs = ((tableType == TableType.NFLOW ) ? nflowColumnSpecs : columnSpecs);

        //if invalid use the nflow column specs and update the data types on the _invalid table
        if(tableType == TableType.INVALID){
           useColumnSpecs = adjustInvalidColumnSpec(nflowColumnSpecs,columnSpecs);
        }


        // Register the database
        if (registerDatabase && !registerDatabase(source)) {
            return false;
        }

        // Register the table
        String ddl = createDDL(source, tableEntity, useColumnSpecs, partitions, nflowFormatOptions, targetFormatOptions, targetTableProperties, tableType);
        return createTable(ddl);
    }

    protected boolean createTable(String ddl) {
        Validate.notNull(conn);

        try (final Statement st = conn.createStatement()) {
            logger.info("Executing {}", ddl);
            st.execute(ddl);
            return true;
        } catch (final SQLException e) {
            logger.error("Failed to create tables DDL {}", ddl, e);
            return false;
        }
    }

    public Set<String> fetchExisting(String source, String tableEntity) {
        HashSet<String> tables = new HashSet<>();
        try (final Statement st = conn.createStatement()) {
            st.execute("use " + HiveUtils.quoteIdentifier(source));
            ResultSet rs = st.executeQuery("show tables like '" + tableEntity + "*'");
            while (rs.next()) {
                tables.add(rs.getString(1));
                logger.info("Found existing table " + rs.getString(1));
            }
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to query tables", e);
        }
        return tables;
    }

    public boolean registerProfileTable(String source, String tableEntity, String targetFormatOptions) {

        String tableName = TableType.PROFILE.deriveQualifiedName(source, tableEntity);
        String columnSQL = " `columnname` string,`metrictype` string,`metricvalue` string";
        String formatSQL = TableType.PROFILE.deriveFormatSpecification("NOT_USED", targetFormatOptions);
        String partitionSQL = TableType.PROFILE.derivePartitionSpecification(null);
        String locationSQL = TableType.PROFILE.deriveLocationSpecification(config.pathForTableType(TableType.PROFILE), source, tableEntity);

        String ddl = createDDL(tableName, columnSQL, partitionSQL, formatSQL, locationSQL, "", TableType.PROFILE.isExternal());
        return createTable(ddl);
    }

    public boolean registerStandardTables(String source, String tableEntity, ColumnSpec[] nflowColumnSpecs, String nflowFormatOptions, String targetFormatOptions, ColumnSpec[] partitions, ColumnSpec[]
        columnSpecs, String tblProperties) {
        boolean result = true;
        registerDatabase(source);
        Set<String> existingTables = fetchExisting(source, tableEntity);
        TableType[] tableTypes = new TableType[]{TableType.NFLOW, TableType.INVALID, TableType.VALID, TableType.MASTER};
        for (TableType tableType : tableTypes) {
            if (!existingTables.contains(tableType.deriveTablename(tableEntity))) {
                result = registerTable(source, tableEntity, nflowColumnSpecs, nflowFormatOptions, targetFormatOptions, partitions, columnSpecs, tblProperties, tableType, false) && result;
            }
        }
        if (!existingTables.contains(TableType.PROFILE.deriveTablename(tableEntity))) {
            result = registerProfileTable(source, tableEntity, targetFormatOptions) && result;
        }
        return result;
    }

    /**
     * Returns the Hive query for creating the specified database.
     *
     * @param source the database name
     * @return the Hive query
     */
    protected String createDatabaseDDL(@Nonnull final String source) {
        return "CREATE DATABASE IF NOT EXISTS " + HiveUtils.quoteIdentifier(source);
    }

    protected String createDDL(String source, String entity, ColumnSpec[] columnSpecs, ColumnSpec[] partitions, String nflowFormatOptions, String targetFormatOptions, String targetTableProperties,
                               TableType tableType) {
        String tableName = tableType.deriveQualifiedName(source, entity);
        String partitionSQL = tableType.derivePartitionSpecification(partitions);
        String columnsSQL = tableType.deriveColumnSpecification(columnSpecs, partitions, nflowFormatOptions);
        String locationSQL = tableType.deriveLocationSpecification(config.pathForTableType(tableType), source, entity);
        String formatOptionsSQL = tableType.deriveFormatSpecification(nflowFormatOptions, targetFormatOptions);
        String tblPropertiesSQL = tableType.deriveTableProperties(targetTableProperties);

        return createDDL(tableName, columnsSQL, partitionSQL, formatOptionsSQL, locationSQL, tblPropertiesSQL, tableType.isExternal());
    }

    protected String createDDL(String tableName, String columnsSQL, String partitionSQL, String formatOptionsSQL, String locationSQL, String targetTablePropertiesSQL, boolean external) {
        StringBuilder sb = new StringBuilder();
        String externalString = (external ? " EXTERNAL " : " ");
        sb.append("CREATE").append(externalString).append("TABLE IF NOT EXISTS ")
            .append(tableName)
            .append(" (").append(columnsSQL).append(") ");

        if (!StringUtils.isEmpty(partitionSQL)) {
            sb.append(" ").append(partitionSQL);
        }
        if (!StringUtils.isEmpty(formatOptionsSQL)) {
            sb.append(" ").append(formatOptionsSQL);
        }
        sb.append(locationSQL);
        if (!StringUtils.isEmpty(targetTablePropertiesSQL)) {
            sb.append(" ").append(targetTablePropertiesSQL);
        }
        return sb.toString();
    }

    /**
     * Drops the specified Hive table.
     *
     * <p>The identifier is expected to already be quoted, if necessary.</p>
     *
     * @param identifier the identifier for the table
     * @return {@code true} on success or {@code false} on failure
     */
    public boolean dropTable(@Nonnull final String identifier) {
        Validate.notNull(conn);

        final String sql = "DROP TABLE IF EXISTS " + identifier;

        try (final Statement st = conn.createStatement()) {
            st.execute(sql);
            return true;
        } catch (final SQLException e) {
            logger.error("Failed to drop tables SQL {}", sql, e);
            return false;
        }
    }

    /**
     * Drops the specified Hive tables.
     *
     * @param source           the category system name or the database name
     * @param entity           the nflow system name or the table prefix
     * @param tableTypes       the standard table types to drop
     * @param additionalTables the identifiers of additional tables to drop
     * @return {@code true} on success or {@code false} on failure
     */
    public boolean dropTables(@Nonnull final String source, @Nonnull final String entity, @Nonnull final Set<TableType> tableTypes, @Nonnull final Set<String> additionalTables) {
        // Drop standard tables
        for (final TableType tableType : tableTypes) {
            final String identifier = tableType.deriveQualifiedName(source, entity);
            if (!dropTable(identifier)) {
                return false;
            }
        }

        // Drop additional tables
        for (final String identifier : additionalTables) {
            if (!dropTable(identifier)) {
                return false;
            }
        }

        // Return success
        return true;
    }
}
