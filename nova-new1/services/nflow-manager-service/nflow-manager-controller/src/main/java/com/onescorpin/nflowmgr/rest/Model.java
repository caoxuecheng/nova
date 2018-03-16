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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.onescorpin.nflowmgr.service.datasource.DatasourceModelTransform;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.nflow.Nflow.State;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.DatasourceDefinition;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.nflow.NflowCategory;
import com.onescorpin.metadata.rest.model.nflow.NflowDestination;
import com.onescorpin.metadata.rest.model.nflow.NflowPrecondition;
import com.onescorpin.metadata.rest.model.nflow.NflowSource;
import com.onescorpin.metadata.rest.model.nflow.InitializationStatus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

/**
 * Convenience functions and methods to transform between the metadata domain model and the REST model.
 */
public class Model {

    public static final Function<com.onescorpin.metadata.api.nflow.InitializationStatus, InitializationStatus> DOMAIN_TO_INIT_STATUS
        = new Function<com.onescorpin.metadata.api.nflow.InitializationStatus, InitializationStatus>() {
        @Override
        public InitializationStatus apply(com.onescorpin.metadata.api.nflow.InitializationStatus domain) {
            InitializationStatus status = new InitializationStatus();
            status.setState(InitializationStatus.State.valueOf(domain.getState().name()));
            status.setTimestamp(domain.getTimestamp());
            return status;
        }
    };

    public static final Function<com.onescorpin.metadata.api.nflow.NflowPrecondition, NflowPrecondition> DOMAIN_TO_NFLOW_PRECOND
        = new Function<com.onescorpin.metadata.api.nflow.NflowPrecondition, NflowPrecondition>() {
        @Override
        public NflowPrecondition apply(com.onescorpin.metadata.api.nflow.NflowPrecondition domain) {
            NflowPrecondition precond = new NflowPrecondition();
            return precond;
        }
    };

    /**
     * Convert a Domin DatasourceDefinition to the Rest Model
     */
    public static final Function<com.onescorpin.metadata.api.datasource.DatasourceDefinition, DatasourceDefinition> DOMAIN_TO_DS_DEFINITION
        = new Function<com.onescorpin.metadata.api.datasource.DatasourceDefinition, DatasourceDefinition>() {
        @Override
        public DatasourceDefinition apply(com.onescorpin.metadata.api.datasource.DatasourceDefinition domain) {
            DatasourceDefinition dsDef = new DatasourceDefinition();
            dsDef.setDatasourceType(domain.getDatasourceType());
            dsDef.setProcessorType(domain.getProcessorType());
            if (domain.getConnectionType() != null) {
                dsDef.setConnectionType(DatasourceDefinition.ConnectionType.valueOf(domain.getConnectionType().name()));
            }
            dsDef.setIdentityString(domain.getIdentityString());
            dsDef.setDatasourcePropertyKeys(domain.getDatasourcePropertyKeys());
            dsDef.setTitle(domain.getTitle());
            dsDef.setDescription(domain.getDescription());
            return dsDef;
        }
    };

    public static final Function<Category, NflowCategory> DOMAIN_TO_NFLOW_CATEGORY = new Function<Category, NflowCategory>() {
        @Override
        public NflowCategory apply(Category category) {
            NflowCategory nflowCategory = new NflowCategory();
            nflowCategory.setId(category.getId().toString());
            nflowCategory.setSystemName(category.getSystemName());
            nflowCategory.setDisplayName(category.getDisplayName());
            nflowCategory.setDescription(category.getDescription());
            return nflowCategory;
        }
    };


    public static com.onescorpin.metadata.api.nflow.Nflow updateDomain(Nflow nflow, com.onescorpin.metadata.api.nflow.Nflow domain) {
        domain.setDisplayName(nflow.getDisplayName());
        domain.setDescription(nflow.getDescription());
        domain.setState(State.valueOf(nflow.getState().name()));
        return domain;
    }

    public static void validateCreate(Nflow nflow) {
        // ignored
    }

    /**
     * The {@code Datasource} transformer.
     */
    @Nonnull
    private final DatasourceModelTransform datasourceTransform;

    /**
     * Constructs a {@code Model}.
     *
     * @param datasourceTransform the datasource transformer
     */
    public Model(@Nonnull final DatasourceModelTransform datasourceTransform) {
        this.datasourceTransform = datasourceTransform;
    }

    /**
     * Transforms the specified domain object to a REST object.
     *
     * @param domain the domain object
     * @return the REST object
     */
    public Nflow domainToNflow(@Nonnull final com.onescorpin.metadata.api.nflow.Nflow domain) {
        return domainToNflow(domain, true);
    }

    /**
     * Transforms the specified domain object to a REST object.
     *
     * @param domain     the domain object
     * @param addSources {@code true} to include sources in the REST object, or {@code false} otherwise
     * @return the REST object
     */
    public Nflow domainToNflow(@Nonnull final com.onescorpin.metadata.api.nflow.Nflow domain, final boolean addSources) {
        Nflow nflow = new Nflow();
        nflow.setId(domain.getId().toString());
        nflow.setSystemName(domain.getName());
        nflow.setDisplayName(domain.getDisplayName());
        nflow.setDescription(domain.getDescription());
        nflow.setState(Nflow.State.valueOf(domain.getState().name()));
        nflow.setCreatedTime(domain.getCreatedTime());
        nflow.setCurrentInitStatus(DOMAIN_TO_INIT_STATUS.apply(domain.getCurrentInitStatus()));
        if (domain.getCategory() != null) {
            nflow.setCategory(DOMAIN_TO_NFLOW_CATEGORY.apply(domain.getCategory()));
        }

        if (addSources) {
            @SuppressWarnings("unchecked")
            Collection<NflowSource> sources = Collections2.transform(domain.getSources(), this::domainToNflowSource);
            nflow.setSources(new HashSet<>(sources));

            @SuppressWarnings("unchecked")
            Collection<NflowDestination> destinations = Collections2.transform(domain.getDestinations(), this::domainToNflowDestination);
            nflow.setDestinations(new HashSet<>(destinations));
        }

        for (Entry<String, Object> entry : domain.getProperties().entrySet()) {
            if (entry.getValue() != null) {
                nflow.getProperties().setProperty(entry.getKey(), entry.getValue().toString());
            }
        }

        return nflow;
    }

    /**
     * Transforms the specified domain object to a REST object.
     *
     * @param domain the domain object
     * @return the REST object
     */
    public NflowSource domainToNflowSource(@Nonnull final com.onescorpin.metadata.api.nflow.NflowSource domain) {
        NflowSource src = new NflowSource();
        src.setDatasource(domainToDs(domain.getDatasource()));
        return src;
    }

    /**
     * Transforms the specified domain object to a REST object.
     *
     * @param domain the domain object
     * @return the REST object
     */
    public NflowDestination domainToNflowDestination(@Nonnull final com.onescorpin.metadata.api.nflow.NflowDestination domain) {
        NflowDestination dest = new NflowDestination();
        dest.setDatasource(domainToDs(domain.getDatasource()));
        return dest;
    }

    /**
     * Transforms the specified domain object to a REST object.
     *
     * @param domain the domain object
     * @return the REST object
     */
    private Datasource domainToDs(@Nonnull final com.onescorpin.metadata.api.datasource.Datasource domain) {
        return datasourceTransform.toDatasource(domain, DatasourceModelTransform.Level.CONNECTIONS);
    }
}
