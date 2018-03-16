package com.onescorpin.nflowmgr.rest;

/*-
 * #%L
 * onescorpin-metadata-rest-controller
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

import com.onescorpin.nflowmgr.service.datasource.DatasourceModelTransform;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.nflow.NflowDestination;
import com.onescorpin.metadata.rest.model.nflow.NflowSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public class NflowLineageBuilder {

    Map<String, Nflow> processedDomainNflows = new HashMap<>();
    Map<String, com.onescorpin.metadata.rest.model.nflow.Nflow> restNflows = new HashMap<>();

    Map<String, Datasource> restDatasources = new HashMap<>();

    /**
     * The {@code Datasource} transformer
     */
    @Nonnull
    private final DatasourceModelTransform datasourceTransform;

    private Nflow domainNflow;

    /**
     * The nflow model transformer
     */
    @Nonnull
    private final Model model;

    /**
     * Constructs a {@code NflowLineageBuilder} for the specified nflow.
     *
     * @param domainNflow          the nflow
     * @param model               the nflow model transformer
     * @param datasourceTransform the datasource transformer
     */
    public NflowLineageBuilder(Nflow domainNflow, @Nonnull final Model model, @Nonnull final DatasourceModelTransform datasourceTransform) {
        this.domainNflow = domainNflow;
        this.model = model;
        this.datasourceTransform = datasourceTransform;
        build(this.domainNflow);
    }


    public com.onescorpin.metadata.rest.model.nflow.Nflow build() {
        return build(this.domainNflow);
    }

    private Datasource buildDatasource(com.onescorpin.metadata.api.datasource.Datasource domainDatasource) {
        Datasource ds = restDatasources.get(domainDatasource.getId().toString());
        if (ds == null) {
            // build the data source
            ds = datasourceTransform.toDatasource(domainDatasource, DatasourceModelTransform.Level.BASIC);

            restDatasources.put(ds.getId(), ds);
            //populate the Nflow relationships
            if (domainDatasource.getNflowSources() != null) {

                List<com.onescorpin.metadata.rest.model.nflow.Nflow> nflowList = new ArrayList<>();
                for (com.onescorpin.metadata.api.nflow.NflowSource domainSrc : domainDatasource.getNflowSources()) {
                    com.onescorpin.metadata.rest.model.nflow.Nflow nflow = build(domainSrc.getNflow());
                    nflowList.add(nflow);
                }
                ds.getSourceForNflows().addAll(nflowList);
            }
            if (domainDatasource.getNflowDestinations() != null) {
                List<com.onescorpin.metadata.rest.model.nflow.Nflow> nflowList = new ArrayList<>();
                for (com.onescorpin.metadata.api.nflow.NflowDestination domainDest : domainDatasource.getNflowDestinations()) {
                    com.onescorpin.metadata.rest.model.nflow.Nflow nflow = build(domainDest.getNflow());
                    nflowList.add(nflow);
                }
                ds.getDestinationForNflows().addAll(nflowList);
            }
        }
        return ds;

    }

    private com.onescorpin.metadata.rest.model.nflow.Nflow build(Nflow domainNflow) {
        com.onescorpin.metadata.rest.model.nflow.Nflow
            nflow =
            restNflows.containsKey(domainNflow.getId().toString()) ? restNflows.get(domainNflow.getId().toString()) : model.domainToNflow(domainNflow);
        restNflows.put(nflow.getId(), nflow);

        @SuppressWarnings("unchecked")
        List<? extends com.onescorpin.metadata.api.nflow.NflowSource> sources = domainNflow.getSources();
        Set<NflowSource> nflowSources = new HashSet<NflowSource>();
        if (sources != null) {

            sources.stream().forEach(nflowSource -> {
                NflowSource src = new NflowSource();
                Datasource ds = buildDatasource(nflowSource.getDatasource());
                src.setDatasource(ds);
                nflowSources.add(src);
            });
        }
        nflow.setSources(nflowSources);
        Set<NflowDestination> nflowDestinations = new HashSet<NflowDestination>();
        List<? extends com.onescorpin.metadata.api.nflow.NflowDestination> destinations = domainNflow.getDestinations();
        if (destinations != null) {
            destinations.stream().forEach(nflowDestination -> {
                NflowDestination dest = new NflowDestination();
                Datasource ds = buildDatasource(nflowDestination.getDatasource());
                dest.setDatasource(ds);
                nflowDestinations.add(dest);
            });
        }
        nflow.setDestinations(nflowDestinations);

        if (domainNflow.getDependentNflows() != null) {
            List<Nflow> depNflows = domainNflow.getDependentNflows();
            depNflows.stream().forEach(depNflow -> {
                com.onescorpin.metadata.rest.model.nflow.Nflow restNflow = restNflows.get(depNflow.getId().toString());
                if (restNflow == null) {
                    com.onescorpin.metadata.rest.model.nflow.Nflow depRestNflow = model.domainToNflow(depNflow);
                    restNflows.put(depRestNflow.getId(), depRestNflow);
                    nflow.getDependentNflows().add(depRestNflow);
                    build(depNflow);
                } else {
                    nflow.getDependentNflows().add(restNflow);
                }

            });
        }
        if (domainNflow.getUsedByNflows() != null) {
            List<Nflow> usedByNflows = domainNflow.getUsedByNflows();
            usedByNflows.stream().forEach(usedByNflow -> {
                com.onescorpin.metadata.rest.model.nflow.Nflow restNflow = restNflows.get(usedByNflow.getId().toString());
                if (restNflow == null) {
                    com.onescorpin.metadata.rest.model.nflow.Nflow usedByRestNflow = model.domainToNflow(usedByNflow);
                    restNflows.put(usedByRestNflow.getId(), usedByRestNflow);
                    nflow.getUsedByNflows().add(usedByRestNflow);
                    build(usedByNflow);
                } else {
                    nflow.getUsedByNflows().add(restNflow);
                }

            });
        }
        return nflow;
    }
}
