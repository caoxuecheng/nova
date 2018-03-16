/**
 * 
 */
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

import com.google.common.collect.Lists;
import com.onescorpin.cluster.ClusterMessage;
import com.onescorpin.cluster.ClusterServiceMessageReceiver;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.nflow.Nflow.ID;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAclEntry;
import com.onescorpin.metadata.jpa.cache.AbstractCacheBackedProvider;
import com.onescorpin.metadata.jpa.cache.CacheBackedProviderClusterMessage;
import com.onescorpin.security.GroupPrincipal;
import com.onescorpin.security.UsernamePrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 *
 */
public class JpaNflowOpsAccessControlProvider extends AbstractCacheBackedProvider<JpaNflowOpsAclEntry, JpaNflowOpsAclEntry.EntryId> implements NflowOpsAccessControlProvider {

    private static final Logger log = LoggerFactory.getLogger(JpaNflowOpsAccessControlProvider.class);
    @Inject
    private MetadataEventService eventService;

    private NflowOpsAccessControlRepository repository;

    @Inject
    private NflowAclCache nflowAclCache;


    @Inject
    private MetadataAccess metadataAccess;

    @Autowired
    public JpaNflowOpsAccessControlProvider(NflowOpsAccessControlRepository nflowOpsAccessControlRepository){
        super(nflowOpsAccessControlRepository);
        this.repository = nflowOpsAccessControlRepository;
    }

    @PostConstruct
    private void init(){
        subscribeListener(nflowAclCache);
        clusterService.subscribe(this,getClusterMessageKey());
        //initially populate
        metadataAccess.read(() ->populateCache(), MetadataAccess.SERVICE );
    }

    @Override
    public JpaNflowOpsAclEntry.EntryId getId(JpaNflowOpsAclEntry value) {
        return  value.getId();
    }

    @Override
    public String getClusterMessageKey() {
        return "NFLOW_ACL_CACHE_UPDATED";
    }

    public String getProviderName() {
        return this.getClass().getName();
    }

    /* (non-Javadoc)
         * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#grantAccess(com.onescorpin.metadata.api.nflow.Nflow.ID, java.security.Principal, java.security.Principal[])
         */
    @Override
    public void grantAccess(ID nflowId, Principal principal, Principal... more) {
        Set<JpaNflowOpsAclEntry> entries = createEntries(nflowId, Stream.concat(Stream.of(principal), Arrays.stream(more)));
        this.saveList(entries);
     //   nflowAclCache.add(entries);
      //  notifyChange(nflowId, NflowAclChange.NflowAclChangeType.GRANTED,entries);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#grantAccessOnly(com.onescorpin.metadata.api.nflow.Nflow.ID, java.security.Principal, java.security.Principal[])
     */
    @Override
    public void grantAccessOnly(ID nflowId, Principal principal, Principal... more) {
        revokeAllAccess(nflowId);
        grantAccess(nflowId, principal, more);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#grantAccess(com.onescorpin.metadata.api.nflow.Nflow.ID, java.util.Set)
     */
    @Override
    public void grantAccess(ID nflowId, Set<Principal> principals) {
        Set<JpaNflowOpsAclEntry> entries = createEntries(nflowId, principals.stream());
        this.saveList(entries);
      //  nflowAclCache.add(entries);
       // notifyChange(nflowId, NflowAclChange.NflowAclChangeType.GRANTED,entries);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#grantAccessOnly(com.onescorpin.metadata.api.nflow.Nflow.ID, java.util.Set)
     */
    @Override
    public void grantAccessOnly(ID nflowId, Set<Principal> principals) {
        revokeAllAccess(nflowId);
        grantAccess(nflowId, principals);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#revokeAccess(com.onescorpin.metadata.api.nflow.Nflow.ID, java.security.Principal, java.security.Principal[])
     */
    @Override
    public void revokeAccess(ID nflowId, Principal principal, Principal... more) {
        Set<JpaNflowOpsAclEntry> entries = createEntries(nflowId, Stream.concat(Stream.of(principal), Arrays.stream(more)));
        this.delete(entries);
        //nflowAclCache.remove(nflowId.toString());
       // notifyChange(nflowId, NflowAclChange.NflowAclChangeType.REVOKED,entries);
    }
    
    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#revokeAccess(com.onescorpin.metadata.api.nflow.Nflow.ID, java.util.Set)
     */
    @Override
    public void revokeAccess(ID nflowId, Set<Principal> principals) {
        Set<JpaNflowOpsAclEntry> entries = createEntries(nflowId, principals.stream());
        this.delete(entries);
      //  this.repository.delete(entries);
       // this.nflowAclCache.remove(nflowId.toString());
       // notifyChange(nflowId, NflowAclChange.NflowAclChangeType.REVOKED,entries);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#revokeAllAccess(java.security.Principal, java.security.Principal[])
     */
    @Override
    public void revokeAllAccess(Principal principal, Principal... more) {

        Set<String> principalNames = Stream.concat(Stream.of(principal), Arrays.stream(more))
                        .map(Principal::getName)
                        .collect(Collectors.toSet());
        Set<JpaNflowOpsAclEntry> entries = this.repository.findForPrincipals(principalNames);
        this.delete(entries);

        //Set<UUID>nflowIds = this.repository.findNflowIdsForPrincipals(principalNames);
        //this.repository.deleteForPrincipals(principalNames);
        //this.notifyChange(nflowIds, NflowAclChange.NflowAclChangeType.REVOKED);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#revokeAllAccess(java.util.Set)
     */
    @Override
    public void revokeAllAccess(Set<Principal> principals) {
        Set<String> principalNames = principals.stream()
                        .map(Principal::getName)
                        .collect(Collectors.toSet());
        Set<JpaNflowOpsAclEntry> entries = this.repository.findForPrincipals(principalNames);
        this.delete(entries);
       // Set<UUID>nflowIds = this.repository.findNflowIdsForPrincipals(principalNames);
       // this.repository.deleteForPrincipals(principalNames);
       // this.notifyChange(nflowIds, NflowAclChange.NflowAclChangeType.REVOKED);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#revokeAllAccess(com.onescorpin.metadata.api.nflow.Nflow.ID)
     */
    @Override
    public void revokeAllAccess(ID nflowId) {
        List<JpaNflowOpsAclEntry> entries = this.repository.findForNflow(UUID.fromString(nflowId.toString()));
        this.delete(entries);
     //   this.repository.deleteForNflow(UUID.fromString(nflowId.toString()));
      //  this.nflowAclCache.remove(nflowId.toString());
      //  notifyChange(nflowId, NflowAclChange.NflowAclChangeType.REVOKED,null);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider#getPrincipals(com.onescorpin.metadata.api.nflow.Nflow.ID)
     */
    @Override
    public Set<Principal> getPrincipals(ID nflowId) {
        List<JpaNflowOpsAclEntry> entries = findForNflow(nflowId.toString());
        return entries.stream().map(e -> asPrincipal(e)).collect(Collectors.toSet());
    }
    
    protected Principal asPrincipal(JpaNflowOpsAclEntry entry) {
        return entry.getPrincipalType() == NflowOpsAclEntry.PrincipalType.GROUP
                        ? new GroupPrincipal(entry.getPrincipalName())
                        : new UsernamePrincipal(entry.getPrincipalName());
    }

    protected Set<JpaNflowOpsAclEntry> createEntries(ID nflowId, Stream<Principal> stream) {
        return stream.map(p -> new JpaNflowOpsAclEntry(nflowId, p)).collect(Collectors.toSet());
    }

    public List<JpaNflowOpsAclEntry> findAll(){
        return repository.findAll();
    }

    public List<JpaNflowOpsAclEntry> findForNflow(String nflowId) {
         return this.repository.findForNflow(UUID.fromString(nflowId));
    }





}
