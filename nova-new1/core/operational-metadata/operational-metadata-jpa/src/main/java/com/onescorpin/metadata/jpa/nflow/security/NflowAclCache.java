package com.onescorpin.metadata.jpa.nflow.security;
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

import com.onescorpin.metadata.api.nflow.security.NflowOpsAclEntry;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.cache.CacheBackedProviderListener;
import com.onescorpin.metadata.jpa.cache.CacheListBean;
import com.onescorpin.security.AccessController;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.inject.Inject;

/**
 * Created by sr186054 on 9/29/17.
 */
public class NflowAclCache extends CacheListBean<String, NflowOpsAclEntry> implements CacheBackedProviderListener<JpaNflowOpsAclEntry.EntryId, JpaNflowOpsAclEntry> {

    private static final Logger log = LoggerFactory.getLogger(NflowAclCache.class);
    @Inject
    AccessController accessController;


    public boolean isAvailable() {
        return isPopulated();
    }

    public boolean hasAccess(RoleSetExposingSecurityExpressionRoot userContext, String nflowId) {
        if (StringUtils.isBlank(nflowId) || !accessController.isEntityAccessControlled()) {
            return true;
        }
        return get(nflowId).stream()
            .anyMatch(acl -> ((acl.getPrincipalType() == NflowOpsAclEntry.PrincipalType.GROUP && userContext.getGroups().contains(acl.getPrincipalName()))
                              || (acl.getPrincipalType() == NflowOpsAclEntry.PrincipalType.USER && userContext.getName().equals(acl.getPrincipalName()))));
    }


    public boolean hasAccess(String nflowId) {
        return hasAccess(userContext(), nflowId);
    }


    public RoleSetExposingSecurityExpressionRoot userContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new RoleSetExposingSecurityExpressionRoot(authentication);
    }


    public boolean isUserCacheAvailable() {
        return (!accessController.isEntityAccessControlled() || (accessController.isEntityAccessControlled() && isAvailable()));
    }


    private String getKey(NflowOpsAclEntry entry) {
        return entry.getNflowId().toString();
    }

    @Override
    public void onAddedItem(JpaNflowOpsAclEntry.EntryId key, JpaNflowOpsAclEntry value) {
        add(key.getUuid().toString(), value);
    }

    @Override
    public void onRemovedItem(JpaNflowOpsAclEntry value) {
        remove(getKey(value), value);
    }

    @Override
    public void onRemoveAll() {
        invalidateAll();
    }

    @Override
    public void onPopulated() {
        log.info("NflowAclCache populated.");
        setPopulated(true);
    }

    @Override
    public boolean isEqual(NflowOpsAclEntry value1, NflowOpsAclEntry value2) {
        return value1.getId().equals(value2.getId());
    }
}
