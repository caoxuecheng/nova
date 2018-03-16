package com.onescorpin.nflowmgr.service.nflow;

/*-
 * #%L
 * nova-nflow-manager-controller
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

import com.onescorpin.discovery.schema.Field;
import com.onescorpin.discovery.schema.TableSchema;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.schema.TableSetup;
import com.onescorpin.hive.service.HiveService;
import com.onescorpin.hive.util.HiveUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Operates on Hive tables used by nflows.
 */
public class NflowHiveTableService {

    /**
     * Hive thrift service
     */
    @Nonnull
    private final HiveService hiveService;

    /**
     * Constructs a {@code NflowHiveTableService}.
     *
     * @param hiveService the Hive thrift service
     */
    public NflowHiveTableService(@Nonnull final HiveService hiveService) {
        this.hiveService = hiveService;
    }

    /**
     * Updates the specified column specification.
     *
     * @param nflow          the nflow to update
     * @param oldColumnName the old column name
     * @param newColumn     the new column specification
     * @throws DataAccessException if there is any problem
     */
    public void changeColumn(@Nonnull final NflowMetadata nflow, @Nonnull final String oldColumnName, @Nonnull final Field newColumn) {
        final StringBuilder query = new StringBuilder();
        query.append("ALTER TABLE ").append(HiveUtils.quoteIdentifier(nflow.getSystemCategoryName())).append('.').append(HiveUtils.quoteIdentifier(nflow.getSystemNflowName()))
            .append(" CHANGE COLUMN ").append(HiveUtils.quoteIdentifier(oldColumnName)).append(' ').append(HiveUtils.quoteIdentifier(newColumn.getName()))
            .append(' ').append(newColumn.getDerivedDataType());
        if (newColumn.getDescription() != null) {
            query.append(" COMMENT ").append(HiveUtils.quoteString(newColumn.getDescription()));
        }
        hiveService.update(query.toString());
    }

    /**
     * Updates the column descriptions in the Hive metastore for the specified nflow.
     *
     * @param nflow the nflow to update
     * @throws DataAccessException if there is any problem
     */
    public void updateColumnDescriptions(@Nonnull final NflowMetadata nflow) {
        final List<Field> nflowFields = Optional.ofNullable(nflow.getTable()).map(TableSetup::getTableSchema).map(TableSchema::getFields).orElse(null);
        if (nflowFields != null && !nflowFields.isEmpty()) {
            final TableSchema hiveSchema = hiveService.getTableSchema(nflow.getSystemCategoryName(), nflow.getSystemNflowName());
            if (hiveSchema != null) {
                final Map<String, Field> hiveFieldMap = hiveSchema.getFields().stream().collect(Collectors.toMap(field -> field.getName().toLowerCase(), Function.identity()));
                nflowFields.stream()
                    .filter(nflowField -> {
                        final Field hiveField = hiveFieldMap.get(nflowField.getName().toLowerCase());
                        return hiveField != null && (StringUtils.isNotEmpty(nflowField.getDescription()) || StringUtils.isNotEmpty(hiveField.getDescription()))
                               && !Objects.equals(nflowField.getDescription(), hiveField.getDescription());
                    })
                    .forEach(nflowField -> changeColumn(nflow, nflowField.getName(), nflowField));
            }
        }
    }
}
