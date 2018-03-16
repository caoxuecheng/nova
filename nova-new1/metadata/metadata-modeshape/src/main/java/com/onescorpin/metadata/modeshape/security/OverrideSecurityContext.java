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

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;

/**
 * A security context that is in effect when an administrative operation is being executed under
 * the ModeShaepAdminPrincipal credential
 */
public class OverrideSecurityContext implements SecurityContext {

    private final OverrideCredentials credentials;

    public OverrideSecurityContext(OverrideCredentials credentials) {
        super();
        this.credentials = credentials;
    }

    @Override
    public String getUserName() {
        return credentials.getUserPrincipal().getName();
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean hasRole(String roleName) {
        return this.credentials.getRolePrincipals().stream().anyMatch((p) -> matches(roleName, p));
    }

    @Override
    public void logout() {
        // Ignored
    }

    public boolean matches(String roleName, Principal principal) {
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
