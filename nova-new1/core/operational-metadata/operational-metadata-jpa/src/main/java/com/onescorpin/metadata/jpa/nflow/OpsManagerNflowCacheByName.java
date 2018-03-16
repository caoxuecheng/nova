package com.onescorpin.metadata.jpa.nflow;
/*-
 * #%L
 * onescorpin-operational-metadata-integration-service
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

import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.jpa.cache.UserCacheBean;
import com.onescorpin.metadata.jpa.common.EntityAccessControlled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Cache backed by a repository that will quickly get access to a Nflow based upon its unique categorySystemName.nflowSystemName
 */
public class OpsManagerNflowCacheByName extends UserCacheBean<String, OpsManagerNflow, JpaOpsManagerNflow, JpaOpsManagerNflow.ID> {

    private static final Logger log = LoggerFactory.getLogger(OpsManagerNflowCacheByName.class);

    @Inject
    private OpsManagerNflowRepository repository;


    @Override
    @EntityAccessControlled
    public List<JpaOpsManagerNflow> fetchAllWithAcl() {
        return repository.findAllWithAcl();
    }

    @Override
    public List<JpaOpsManagerNflow> fetchAll() {
        return repository.findAll();
    }

    @Override
    @EntityAccessControlled
    public JpaOpsManagerNflow fetchByIdWithAcl(String cacheKey) {
        return repository.findByNameWithAcl(cacheKey);
    }

    @Override
    public JpaOpsManagerNflow fetchById(String cacheKey) {
        return repository.findByNameWithoutAcl(cacheKey);
    }

    @Override

    public List<JpaOpsManagerNflow> fetchForIds(Set<String> cacheKeys) {
        return repository.findByNamesWithoutAcl(cacheKeys);
    }

    @Override
    @EntityAccessControlled
    public List<JpaOpsManagerNflow> fetchForIdsWithAcl(Set<String> cacheKeys) {
        return repository.findByNamesWithAcl(cacheKeys);
    }

    @Override
    public String getNflowId(OpsManagerNflow item) {
        return item.getId().toString();
    }

    @Override
    public OpsManagerNflow transform(JpaOpsManagerNflow dbItem) {
        return dbItem;
    }


    public OpsManagerNflowCacheByName() {

    }

    public String getCacheKey(JpaOpsManagerNflow dbItem) {
        return dbItem.getName();
    }


}
