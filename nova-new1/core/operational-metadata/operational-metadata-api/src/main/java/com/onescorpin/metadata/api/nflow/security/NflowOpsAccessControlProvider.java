/**
 * 
 */
package com.onescorpin.metadata.api.nflow.security;

/*-
 * #%L
 * nova-operational-metadata-api
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

import java.security.Principal;
import java.util.List;
import java.util.Set;

import com.onescorpin.metadata.api.nflow.Nflow;

/**
 * A provider for granting and revoking visibility to nflow operations by certain users.
 */
public interface NflowOpsAccessControlProvider {

    /**
     * Grants access to operations of a nflow to one or more principals.
     * @param nflowId the nflow ID
     * @param principal a principal
     * @param more any additional principals
     */
    void grantAccess(Nflow.ID nflowId, Principal principal, Principal... more);
    
    /**
     * Grants access to operations of a nflow to only the one or more principals
     * specified; revoking access to all others.
     * @param nflowId the nflow ID
     * @param principal a principal
     * @param more any additional principals
     */
    void grantAccessOnly(Nflow.ID nflowId, Principal principal, Principal... more);
    
    /**
     * Grants access to operations of a nflow to a set of principals.
     * @param nflowId the nflow ID
     * @param principals the principals
     */
    void grantAccess(Nflow.ID nflowId, Set<Principal> principals); 
    
    /**
     * Grants access to operations of a nflow to only specified set of principals;
     * revoking access to all others.
     * @param nflowId the nflow ID
     * @param principals the principals
     */
    void grantAccessOnly(Nflow.ID nflowId, Set<Principal> principals);
    
    /**
     * Revokes access to operations of a nflow for one or more principals.
     * @param nflowId the nflow ID
     * @param principal a principal
     * @param more any additional principals
     */
    void revokeAccess(Nflow.ID nflowId, Principal principal, Principal... more);
    
    /**
     * Revokes access to operations of a nflow for a set of principals.
     * @param nflowId the nflow ID
     * @param principals the principals
     */
    void revokeAccess(Nflow.ID nflowId, Set<Principal> principals);
    
    /**
     * Revokes access to operations of all nflows for one or more principals.
     * @param principal a principal
     * @param more any additional principals
     */
    void revokeAllAccess(Principal principal, Principal... more);
    
    /**
     * Revokes access to operations of all nflows for a set of principals.
     * @param principals the principals
     */
    void revokeAllAccess(Set<Principal> principals);
    
    /**
     * Revokes access to operations of a nflow for all principals.
     * @param nflowId the nflow ID
     */
    void revokeAllAccess(Nflow.ID nflowId);
    
    
    /**
     * Returns the set of all principals that have been granted 
     * access to a nflow's operations.
     * @param nflowId the nflow ID
     * @return all principals with access
     */
    Set<Principal> getPrincipals(Nflow.ID nflowId);

    List<? extends NflowOpsAclEntry> findAll();

    List<? extends NflowOpsAclEntry> findForNflow(String nflowId);

}
