package com.onescorpin.metadata.upgrade.v083;

/*-
 * #%L
 * nova-upgrade-service
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

import com.onescorpin.NovaVersion;
import com.onescorpin.discovery.schema.TableSchema;
import com.onescorpin.nflowmgr.rest.model.schema.TableSetup;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerNflowService;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.datasource.DerivedDatasource;
import com.onescorpin.server.upgrade.NovaUpgrader;
import com.onescorpin.server.upgrade.UpgradeState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Ensures that Hive column metadata is included with Hive derived datasources for indexing.
 *
 * <p>Indexing of the schema for the destination Hive table of Data Ingest nflows has been moved from the Index Schema Service nflow to an internal Nova service. This allows the index to include Nova
 * metadata such as column tags.</p>
 *
 * <p>The Index Schema Service only indexed Hive tables that matched the category system name and nflow system name. Likewise, this action only updates Hive derived datasources that match the category
 * system name and nflow system name.</p>
 */
@Component("hiveColumnsUpgradeAction083")
@Profile(NovaUpgrader.NOVA_UPGRADE)
public class HiveColumnsUpgradeAction implements UpgradeState {

    private static final Logger log = LoggerFactory.getLogger(HiveColumnsUpgradeAction.class);

    /**
     * Provides access to datasources
     */
    @Inject
    private DatasourceProvider datasourceProvider;

    /**
     * Provides access to nflows
     */
    @Inject
    private NflowManagerNflowService nflowService;

    @Override
    public boolean isTargetVersion(NovaVersion version) {
        return version.matches("0.8", "3", "");
    }

    @Override
    public void upgradeTo(final NovaVersion startingVersion) {
        log.info("Upgrading hive columns from version: {}", startingVersion);

        nflowService.getNflows().stream()
            .filter(nflow -> Optional.ofNullable(nflow.getTable()).map(TableSetup::getTableSchema).map(TableSchema::getFields).isPresent())
            .forEach(nflow -> {
                final TableSchema schema = nflow.getTable().getTableSchema();
                final DerivedDatasource datasource = datasourceProvider.findDerivedDatasource("HiveDatasource",
                                                                                              nflow.getSystemCategoryName() + "." + nflow.getSystemNflowName());
                if (datasource != null) {
                    log.info("Upgrading schema: {}/{}", schema.getDatabaseName(), schema.getSchemaName());
                    datasource.setGenericProperties(Collections.singletonMap("columns", (Serializable) schema.getFields()));
                }
            });
    }
}
