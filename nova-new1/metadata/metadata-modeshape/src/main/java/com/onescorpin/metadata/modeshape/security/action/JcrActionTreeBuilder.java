/**
 *
 */
package com.onescorpin.metadata.modeshape.security.action;

import javax.jcr.Node;
import javax.jcr.security.Privilege;

import org.modeshape.jcr.security.SimplePrincipal;

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

import com.onescorpin.metadata.modeshape.security.JcrAccessControlUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.action.Action;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.config.ActionBuilder;
import com.onescorpin.security.action.config.ActionsTreeBuilder;

/**
 *
 */
public class JcrActionTreeBuilder<P> extends JcrAbstractActionsBuilder implements ActionsTreeBuilder<P> {

    private Node actionsNode;
    private P parentBuilder;

    public JcrActionTreeBuilder(Node actionsNode, P parent) {
        this.actionsNode = actionsNode;
        this.parentBuilder = parent;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.security.action.config.ActionsTreeBuilder#action(com.onescorpin.security.action.Action)
     */
    @Override
    public ActionsTreeBuilder<P> action(Action action) {
        Node currentNode = this.actionsNode;

        for (Action current : action.getHierarchy()) {
            currentNode = JcrUtil.getOrCreateNode(currentNode, current.getSystemName(), JcrAllowableAction.NODE_TYPE);
        }

        return new JcrActionBuilder<>(currentNode, this)
            .title(action.getTitle())
            .description(action.getDescription())
            .add();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.security.action.config.ActionsTreeBuilder#action(java.lang.String)
     */
    @Override
    public ActionBuilder<ActionsTreeBuilder<P>> action(String systemName) {
        Node actionNode = JcrUtil.getOrCreateNode(this.actionsNode, systemName, JcrAllowableAction.NODE_TYPE);
        return new JcrActionBuilder<>(actionNode, this);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.security.action.config.ActionsTreeBuilder#add()
     */
    @Override
    public P add() {
        JcrAccessControlUtil.addPermissions(this.actionsNode, getManagementPrincipal(), Privilege.JCR_ALL);
        JcrAccessControlUtil.addPermissions(this.actionsNode, SimplePrincipal.EVERYONE, Privilege.JCR_READ);
        return this.parentBuilder;
    }

    public AllowedActions build() {
        
        return null;
    }
}
