/**
 *
 */
package com.onescorpin.nflowmgr.service;

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

import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.datasource.DerivedDatasource;
import com.onescorpin.metadata.api.nflow.Nflow.State;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.data.DatasourceDefinition;
import com.onescorpin.metadata.rest.model.data.DirectoryDatasource;
import com.onescorpin.metadata.rest.model.data.HiveTableDatasource;
import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.rest.model.nflow.NflowCategory;
import com.onescorpin.metadata.rest.model.nflow.NflowDestination;
import com.onescorpin.metadata.rest.model.nflow.NflowPrecondition;
import com.onescorpin.metadata.rest.model.nflow.NflowSource;
import com.onescorpin.metadata.rest.model.nflow.InitializationStatus;
import com.onescorpin.metadata.rest.model.op.NflowOperation;
import com.onescorpin.security.rest.controller.SecurityModelTransform;
import com.onescorpin.security.rest.model.ActionGroup;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Convenience functions and methods to transform between the metadata domain model and the REST model.
 */
public class MetadataModelTransform {
    
    @Inject
    private SecurityModelTransform actionsTransform;

    public Function<com.onescorpin.metadata.api.nflow.InitializationStatus, InitializationStatus> domainToInitStatus() {
        return (domain) -> {
            InitializationStatus status = new InitializationStatus();
            status.setState(InitializationStatus.State.valueOf(domain.getState().name()));
            status.setTimestamp(domain.getTimestamp());
            return status;
        };
    }

    // @formatter:off
    public Function<com.onescorpin.metadata.api.nflow.NflowPrecondition, NflowPrecondition> domainToNflowPrecond() {
        return (domain) -> {
            NflowPrecondition precond = new NflowPrecondition();
            return precond;
        };
    }
    
    /**
     * Convert a Domin DatasourceDefinition to the Rest Model
     */
    public Function<com.onescorpin.metadata.api.datasource.DatasourceDefinition, DatasourceDefinition> domainToDsDefinition() {
        return (domain) -> {
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
        };
    }
    
    public Function<DerivedDatasource, com.onescorpin.metadata.rest.model.data.DerivedDatasource> domainToDerivedDs() {
        return (domain) -> {
            com.onescorpin.metadata.rest.model.data.DerivedDatasource ds = new com.onescorpin.metadata.rest.model.data.DerivedDatasource();
            ds.setId(domain.getId().toString());
            ds.setName(domain.getName());
            ds.setDescription(domain.getDescription());
            ds.setProperties(domain.getProperties());
            ds.setDatasourceType(domain.getDatasourceType());
            return ds;
        };
    }
    
    public Function<com.onescorpin.metadata.api.nflow.NflowSource, NflowSource> domainToNflowSource() {
        return (domain) -> {
            NflowSource src = new NflowSource();
            src.setDatasource(domainToDs().apply(domain.getDatasource()));
            return src;
        };
    }
    
    public Function<com.onescorpin.metadata.api.nflow.NflowDestination, NflowDestination> domainToNflowDestination() {
        return (domain) -> {
            NflowDestination dest = new NflowDestination();
            dest.setDatasource(domainToDs().apply(domain.getDatasource()));
            return dest;
        };
    }
    
    public Function<com.onescorpin.metadata.api.op.NflowOperation, NflowOperation> domainToNflowOp() {
        return (domain) -> {
            NflowOperation op = new NflowOperation();
            op.setOperationId(domain.getId().toString());
            op.setStartTime(domain.getStartTime());
            op.setStopTime(domain.getStopTime());
            op.setState(NflowOperation.State.valueOf(domain.getState().name()));
            op.setStatus(domain.getStatus());
            op.setResults(domain.getResults().entrySet().stream()
                              .collect(Collectors.toMap(Map.Entry::getKey,
                                                        e -> e.getValue().toString())));

            return op;
        };
    }
    
    public Function<Category, NflowCategory> domainToNflowCategory() {
        return (category) -> {
            NflowCategory nflowCategory = new NflowCategory();
            nflowCategory.setId(category.getId().toString());
            nflowCategory.setSystemName(category.getSystemName());
            nflowCategory.setDisplayName(category.getDisplayName());
            nflowCategory.setDescription(category.getDescription());
            nflowCategory.setUserProperties(category.getUserProperties());
            return nflowCategory;
        };
    }
    
    public Function<com.onescorpin.metadata.api.nflow.Nflow, Nflow> domainToNflow() {
        return domainToNflow(true);
    }
    
    public Function<com.onescorpin.metadata.api.nflow.Nflow, Nflow> domainToNflow(boolean addSources) {
        return (domain) -> {
            Nflow nflow = new Nflow();
            nflow.setId(domain.getId().toString());
            nflow.setSystemName(domain.getName());
            nflow.setDisplayName(domain.getDisplayName());
            nflow.setDescription(domain.getDescription());
            nflow.setUserProperties(domain.getUserProperties());
            if (domain.getState() != null) nflow.setState(Nflow.State.valueOf(domain.getState().name()));
            if (domain.getCreatedTime() != null) nflow.setCreatedTime(domain.getCreatedTime());
            if (domain.getCurrentInitStatus() != null) nflow.setCurrentInitStatus(domainToInitStatus().apply(domain.getCurrentInitStatus()));
            
            if (domain.getCategory() != null) {
                nflow.setCategory(domainToNflowCategory().apply(domain.getCategory()));
            }

            if (addSources) {
                Collection<NflowSource> sources = domain.getSources().stream().map(domainToNflowSource()).collect(Collectors.toList());
                nflow.setSources(new HashSet<NflowSource>(sources));

                Collection<NflowDestination> destinations = domain.getDestinations().stream().map(domainToNflowDestination()).collect(Collectors.toList());
                nflow.setDestinations(new HashSet<NflowDestination>(destinations));
            }

            for (Entry<String, Object> entry : domain.getProperties().entrySet()) {
                if (entry.getValue() != null) {
                    nflow.getProperties().setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
            
            ActionGroup allowed = actionsTransform.toActionGroup(null).apply(domain.getAllowedActions());
            nflow.setAllowedActions(allowed);

            return nflow;
        };
    }

    
    public Function<com.onescorpin.metadata.api.datasource.Datasource, Datasource> domainToDs() {
        return domainToDs(true);
    }

    public Function<com.onescorpin.metadata.api.datasource.Datasource, Datasource> domainToDs(boolean addConnections) {
        return (domain) -> {
            Datasource ds;
            if (domain instanceof DerivedDatasource) {
                ds = domainToDerivedDs().apply((DerivedDatasource) domain);
            } else {
                ds = new Datasource();
                ds.setId(domain.getId().toString());
                ds.setName(domain.getName());
                ds.setDescription(domain.getDescription());
            }
            if (addConnections) {
                addConnections(domain, ds);
            }
            return ds;
        };
    }

    protected void addConnections(com.onescorpin.metadata.api.datasource.Datasource domain, Datasource datasource) {
        for (com.onescorpin.metadata.api.nflow.NflowSource domainSrc : domain.getNflowSources()) {
            Nflow nflow = new Nflow();
            nflow.setId(domainSrc.getNflow().getId().toString());
            nflow.setSystemName(domainSrc.getNflow().getName());

            datasource.getSourceForNflows().add(nflow);
        }
        for (com.onescorpin.metadata.api.nflow.NflowDestination domainDest : domain.getNflowDestinations()) {
            Nflow nflow = new Nflow();
            nflow.setId(domainDest.getNflow().getId().toString());
            nflow.setSystemName(domainDest.getNflow().getName());

            datasource.getDestinationForNflows().add(nflow);
        }
    }


    public com.onescorpin.metadata.api.nflow.Nflow updateDomain(Nflow nflow, com.onescorpin.metadata.api.nflow.Nflow domain) {
        domain.setDisplayName(nflow.getDisplayName());
        domain.setDescription(nflow.getDescription());
        domain.setState(State.valueOf(nflow.getState().name()));
        return domain;
    }

    public void validateCreate(Nflow nflow) {
        // TODO Auto-generated method stub

    }


    public void validateCreate(String fid, NflowDestination dest) {
        // TODO Auto-generated method stub

    }


    public void validateCreate(HiveTableDatasource ds) {
        // TODO Auto-generated method stub

    }


    public void validateCreate(DirectoryDatasource ds) {
        // TODO Auto-generated method stub

    }

    // @formatter:on

}
