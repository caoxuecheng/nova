package com.onescorpin.metadata.jpa.cache;
/*-
 * #%L
 * nova-operational-metadata-jpa
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
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.common.EntityAccessControlled;
import com.onescorpin.metadata.jpa.nflow.security.NflowAclCache;
import com.onescorpin.security.AccessController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * find data specific to a given user.
 * If the NflowAclCache is populated it will utilize this, otherwise it will go to the source and query for the data
 */
public abstract class UserCacheBean<K, V, T, ID extends Serializable> extends CacheBean<K, V> implements CacheBackedProviderListener<ID, T> {
    private static final Logger log = LoggerFactory.getLogger(UserCacheBean.class);
    @Inject
    private NflowAclCache nflowAclCache;

    @Inject
    private AccessController accessController;

    @Inject
    private MetadataAccess metadataAccess;

    public UserCacheBean() {
    }

    public List<V> findAllWithoutAcl() {
        return findAll(false);
    }

    public List<V> findAll() {
        return findAll(true);
    }

    public V findByIdWithoutAcl(K cacheKey) {
        return findById(cacheKey, false);
    }

    public V findById(K cacheKey) {
        return findById(cacheKey, true);
    }

    public List<V> findByIdsWithoutAcl(Set<K> keys) {
        return findByIds(keys, false);
    }

    public List<V> findByIds(Set<K> keys) {
        return findByIds(keys, true);
    }

    public List<V> findByIds(List<K> keys) {
        return findByIds(new HashSet<K>(keys), true);
    }

    private List<V> findAll(boolean addAcl) {
        if (nflowAclCache.isUserCacheAvailable() && !cache.asMap().isEmpty()) {
            return cache.asMap().values().stream().filter(item -> isVaildUserNflow(item, addAcl)).collect(Collectors.toList());
        } else {
            if (addAcl && accessController.isEntityAccessControlled()) {
                return metadataAccess.read(() -> fetchAllWithAcl().stream().map(v -> transform(v)).collect(Collectors.toList()));
            } else {
                return metadataAccess.read(() -> fetchAll().stream().map(v -> transform(v)).collect(Collectors.toList()), MetadataAccess.SERVICE);
            }
        }
    }

    private V findById(K cacheKey, boolean addAcl) {
        if (nflowAclCache.isUserCacheAvailable() && !cache.asMap().isEmpty()) {
            V value = cache.getIfPresent(cacheKey);
            if (isVaildUserNflow(value, addAcl)) {
                return value;
            } else {
                return null;
            }
        } else {
            if (addAcl && accessController.isEntityAccessControlled()) {
                return metadataAccess.read(() -> transform(fetchByIdWithAcl(cacheKey)));
            } else {
                return metadataAccess.read(() -> transform(fetchById(cacheKey)), MetadataAccess.SERVICE);
            }
        }
    }


    public List<V> findByIds(Set<K> keys, boolean addAcl) {
        if (keys != null && !keys.isEmpty()) {

            if (nflowAclCache.isUserCacheAvailable() && !cache.asMap().isEmpty()) {
                return keys.stream()
                    .map(name -> cache.getIfPresent(name))
                    .filter(Objects::nonNull)
                    .filter(f -> isVaildUserNflow(f, addAcl))
                    .collect(Collectors.toList());
            } else {

                if (addAcl && accessController.isEntityAccessControlled()) {
                    return metadataAccess.read(() -> fetchForIdsWithAcl(keys).stream().map(v -> transform(v)).collect(Collectors.toList()));
                } else {
                    return metadataAccess.read(() -> fetchForIds(keys).stream().map(v -> transform(v)).collect(Collectors.toList()), MetadataAccess.SERVICE);
                }

            }
        } else {
            return null;
        }
    }


    private boolean isVaildUserNflow(V item, boolean addAcl) {
        return (item != null && (!addAcl || (addAcl && nflowAclCache.hasAccess(getNflowId(item)))));
    }


    @Override
    public void onAddedItem(ID key, T value) {
        add(getCacheKey(value), transform(value));
    }

    @Override
    public void onRemovedItem(T value) {
        K id = getCacheKey(value);
        if (id != null) {
            invalidate(id);
        }
    }

    @Override
    public void onRemoveAll() {
        invalidateAll();
    }

    @Override
    public void onPopulated() {
        setPopulated(true);
    }

    @EntityAccessControlled
    public abstract List<T> fetchAllWithAcl();

    @EntityAccessControlled(enabled = false)
    public abstract List<T> fetchAll();

    @EntityAccessControlled
    public abstract T fetchByIdWithAcl(K cacheKey);

    public abstract T fetchById(K cacheKey);

    public abstract List<T> fetchForIds(Set<K> cacheKeys);

    @EntityAccessControlled
    public abstract List<T> fetchForIdsWithAcl(Set<K> cacheKeys);

    public abstract K getCacheKey(T dbItem);

    public abstract String getNflowId(V item);

    public abstract V transform(T dbItem);

    public Long size(){
        return cache.size();
    }


}
