/**
 * 
 */
package com.onescorpin.metadata.modeshape.category;

/*-
 * #%L
 * nova-metadata-modeshape
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
import java.util.stream.Stream;

import javax.jcr.Node;

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.security.RoleMembership;
import com.onescorpin.metadata.modeshape.security.role.JcrAbstractRoleMembership;

/**
 *
 */
public class JcrNflowDefaultRoleMembership extends JcrAbstractRoleMembership {
    
    private CategoryDetails category;
    
    
    public JcrNflowDefaultRoleMembership(Node node, CategoryDetails cat) {
        super(node);
        this.category = cat;
    }

    public JcrNflowDefaultRoleMembership(Node node, Node roleNode, CategoryDetails cat) {
        super(node, roleNode);
        this.category = cat;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.modeshape.security.role.JcrAbstractRoleMembership#enable(java.security.Principal)
     */
    @Override
    protected void enable(Principal principal) {
        this.category.getNflows().forEach(nflow -> enableOnly(principal, streamAllRoleMemberships(nflow), nflow.getAllowedActions()));
    }


    /* (non-Javadoc)
     * @see com.onescorpin.metadata.modeshape.security.role.JcrAbstractRoleMembership#disable(java.security.Principal)
     */
    @Override
    protected void disable(Principal principal) {
        this.category.getNflows().forEach(nflow -> enableOnly(principal, streamAllRoleMemberships(nflow), nflow.getAllowedActions()));
    }

    /**
     * Streams all nflow role memberships (category-level and nflow-level) that apply to the specified nflow.
     */
    protected Stream<RoleMembership> streamAllRoleMemberships(Nflow nflow) {
        return Stream.concat(this.category.getNflowRoleMemberships().stream(), nflow.getRoleMemberships().stream());
    }
}
