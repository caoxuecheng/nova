package com.onescorpin.metadata.modeshape.security;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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

import org.modeshape.jcr.security.SecurityContext;
import org.springframework.security.authentication.jaas.JaasGrantedAuthority;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A security context that is in effect when an operation is being executed with
 * the credential authenticated via Spring security.
 */
public class SpringSecurityContext implements SecurityContext {

    private final Authentication authentication;
    private final Set<Principal> principals;

    public SpringSecurityContext(Authentication auth) {
        this(auth, Collections.emptySet());
    }

    public SpringSecurityContext(Authentication auth, Collection<Principal> additionalPrincipals) {
        this.authentication = auth;
        this.principals = Collections.unmodifiableSet(new HashSet<>(additionalPrincipals));
    }

    @Override
    public String getUserName() {
        return this.authentication.getName();
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean hasRole(String roleName) {
        boolean matched = this.authentication.getAuthorities().stream().anyMatch(grant -> {
            if (grant instanceof JaasGrantedAuthority) {
                JaasGrantedAuthority jaasGrant = (JaasGrantedAuthority) grant;

                return matches(roleName, jaasGrant.getPrincipal());
            } else {
                if (roleName.equals(grant.getAuthority())) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        if (matched) {
            return true;
        } else {
            return this.principals.stream().anyMatch(principal -> matches(roleName, principal));
        }
    }

    @Override
    public void logout() {
        // Ignored
    }

    private boolean matches(String roleName, Principal principal) {
        if (principal.getName().equals(roleName)) {
            return true;
        } else if (principal instanceof Group) {
            Group group = (Group) principal;
            return Collections.list(group.members()).stream().anyMatch((p) -> matches(roleName, p));
        } else {
            return false;
        }
    }

}
