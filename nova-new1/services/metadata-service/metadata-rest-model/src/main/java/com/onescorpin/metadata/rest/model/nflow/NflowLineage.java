package com.onescorpin.metadata.rest.model.nflow;

/*-
 * #%L
 * onescorpin-metadata-rest-model
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.onescorpin.metadata.rest.model.data.Datasource;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 */
public class NflowLineage {

    private Map<String, Nflow> nflowMap;

    private Nflow nflow;

    private Map<String, Datasource> datasourceMap = new HashMap<>();

    private Map<String, NflowLineageStyle> styles;

    public NflowLineage(Nflow nflow, Map<String, NflowLineageStyle> styles) {
        this.nflow = nflow;
        this.nflowMap = new HashMap<>();
        this.styles = styles;
        serialize(this.nflow);
    }

    @JsonIgnore
    private void serialize(Nflow nflow) {

        if (nflow.getDependentNflows() != null) {
            Set<String> ids = new HashSet<>();
            Set<Nflow> dependentNflows = new HashSet<>(nflow.getDependentNflows());
            nflow.setDependentNflows(null);
            dependentNflows.stream().forEach(depNflow -> {
                                                nflowMap.put(depNflow.getId(), depNflow);
                                                ids.add(depNflow.getId());

                                                if (depNflow.getDependentNflows() != null) {
                                                    serialize(depNflow);
                                                }
                                            }
            );

            nflow.setDependentNflowIds(ids);
        }
        if (nflow.getUsedByNflows() != null) {
            Set<String> ids = new HashSet<>();
            Set<Nflow> usedByNflows = new HashSet<>(nflow.getUsedByNflows());
            nflow.getUsedByNflows().clear();
            usedByNflows.stream().forEach(depNflow -> {
                                             nflowMap.put(depNflow.getId(), depNflow);
                                             ids.add(depNflow.getId());
                                             if (depNflow.getUsedByNflows() != null) {
                                                 serialize(depNflow);
                                             }
                                         }
            );
            nflow.setUsedByNflowIds(ids);
        }

        if (nflow.getSources() != null) {
            nflow.getSources().forEach(nflowSource -> {
                Datasource ds = serializeDatasource(nflowSource.getDatasource());
                nflowSource.setDatasource(null);
                if (StringUtils.isBlank(nflowSource.getDatasourceId())) {
                    nflowSource.setDatasourceId(ds != null ? ds.getId() : null);
                }
            });
        }
        if (nflow.getDestinations() != null) {
            nflow.getDestinations().forEach(nflowDestination -> {
                Datasource ds = serializeDatasource(nflowDestination.getDatasource());
                nflowDestination.setDatasource(null);
                if (StringUtils.isBlank(nflowDestination.getDatasourceId())) {
                    nflowDestination.setDatasourceId(ds != null ? ds.getId() : null);
                }
            });
        }
        nflowMap.put(nflow.getId(), nflow);
    }

    private Datasource serializeDatasource(Datasource ds) {
        if (ds != null) {
            if (!datasourceMap.containsKey(ds.getId())) {
                datasourceMap.put(ds.getId(), ds);
                if (ds.getSourceForNflows() != null) {
                    ds.getSourceForNflows().forEach(sourceNflow -> {
                        Nflow serializedNflow = nflowMap.get(sourceNflow.getId());
                        if (serializedNflow == null) {
                            serialize(sourceNflow);
                        } else {
                            sourceNflow = serializedNflow;
                        }
                    });
                }

                if (ds.getDestinationForNflows() != null) {
                    ds.getDestinationForNflows().forEach(destNflow -> {
                        Nflow serializedNflow = nflowMap.get(destNflow.getId());
                        if (serializedNflow == null) {
                            serialize(destNflow);
                        } else {
                            destNflow = serializedNflow;
                        }
                    });
                }
            }
            return datasourceMap.get(ds.getId());

        }
        return null;

    }


    public Map<String, Nflow> getNflowMap() {
        return nflowMap;
    }

    public void setNflowMap(Map<String, Nflow> nflowMap) {
        this.nflowMap = nflowMap;
    }

    public Nflow getNflow() {
        return nflow;
    }

    public void setNflow(Nflow nflow) {
        this.nflow = nflow;
    }

    public Map<String, Datasource> getDatasourceMap() {
        return datasourceMap;
    }

    public void setDatasourceMap(Map<String, Datasource> datasourceMap) {
        this.datasourceMap = datasourceMap;
    }


    public Map<String, NflowLineageStyle> getStyles() {
        return styles;
    }

    public void setStyles(Map<String, NflowLineageStyle> styles) {
        this.styles = styles;
    }
}
