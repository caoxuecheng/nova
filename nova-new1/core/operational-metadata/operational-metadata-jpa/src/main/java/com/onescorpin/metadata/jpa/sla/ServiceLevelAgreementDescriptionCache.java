package com.onescorpin.metadata.jpa.sla;
/*-
 * #%L
 * onescorpin-operational-metadata-jpa
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

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementDescriptionProvider;
import com.onescorpin.metadata.jpa.cache.ClusterAwareDtoCache;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementDescription;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Cache of Service Level Agreements and their corresponding Nflows
 * Cache is sync/maintained via the JpaServiceLevelAgreementDescriptionProvider
 */
public class ServiceLevelAgreementDescriptionCache extends ClusterAwareDtoCache<ServiceLevelAgreementDescription, ServiceLevelAgreement.ID, CachedServiceLevelAgreement, String> {

    @Inject
    ServiceLevelAgreementDescriptionProvider serviceLevelAgreementDescriptionProvider;

    @Inject
    MetadataAccess metadataAccess;

    @Override
    public String getClusterMessageKey() {
        return "SLA_DESCRIPTION_CACHE";
    }

    @Override
    public String getProviderName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public ServiceLevelAgreement.ID entityIdForDtoId(String dtoId) {
        return serviceLevelAgreementDescriptionProvider.resolveId(dtoId);
    }

    @Override
    public String dtoIdForEntity(ServiceLevelAgreementDescription entity) {
        return entity.getSlaId().toString();
    }

    @Override
    public String getDtoId(CachedServiceLevelAgreement dto) {
        return dto.getId();
    }

    @PostConstruct
    private void init() {
        clusterService.subscribe(this, getClusterMessageKey());
    }

    @Override
    public CachedServiceLevelAgreement transformEntityToDto(String dtoId, ServiceLevelAgreementDescription entity) {
        return metadataAccess.read(() -> {
            JpaServiceLevelAgreementDescription jpaSla = ((JpaServiceLevelAgreementDescription) entity);
            Set<CachedServiceLevelAgreement.SimpleNflow> nflows = new HashSet<>();
            if (jpaSla.getNflows() != null && !jpaSla.getNflows().isEmpty()) {
                nflows = jpaSla.getNflows().stream().map(f -> new CachedServiceLevelAgreement.SimpleNflow(f.getId().toString(), f.getName())).collect(Collectors.toSet());
            }
            return new CachedServiceLevelAgreement(dtoId, entity.getName(), nflows);
        }, MetadataAccess.SERVICE);
    }

    @Override
    public ServiceLevelAgreementDescription fetchForKey(ServiceLevelAgreement.ID entityId) {
        return
            metadataAccess.read(() -> {
                return serviceLevelAgreementDescriptionProvider.findOne(entityId);
            }, MetadataAccess.SERVICE);
    }

    @Override
    public List<ServiceLevelAgreementDescription> fetchAll() {
        return metadataAccess.read(() -> {
            return serviceLevelAgreementDescriptionProvider.findAll();
        }, MetadataAccess.SERVICE);
    }

    @Override
    public Collection<CachedServiceLevelAgreement> populateCache() {
        return metadataAccess.read(() -> super.populateCache(), MetadataAccess.SERVICE);
    }
}
