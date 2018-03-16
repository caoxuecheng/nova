package com.onescorpin.metadata.modeshape.nflow.security;

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

import com.onescorpin.metadata.api.category.security.CategoryAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.security.TemplateAccessControl;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.nflow.NflowDetails;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.security.JcrAccessControlUtil;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.action.Action;
import com.onescorpin.security.action.AllowedActions;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.security.Privilege;


/**
 * A type of allowed actions that applies to nflows.  It intercepts certain action enable/disable
 * calls related to visibility to update the underlying JCR node structure's ACL lists.
 */
public class JcrNflowAllowedActions extends JcrAllowedActions {

    private JcrNflow nflow;

    public JcrNflowAllowedActions(Node allowedActionsNode) {
        super(allowedActionsNode);
        this.nflow = JcrUtil.getJcrObject(JcrUtil.getParent(allowedActionsNode), JcrNflow.class);
    }

    public JcrNflowAllowedActions(Node allowedActionsNode, NflowOpsAccessControlProvider opsAccessProvider) {
        super(allowedActionsNode);
        this.nflow = JcrUtil.getJcrObject(JcrUtil.getParent(allowedActionsNode), JcrNflow.class, opsAccessProvider);
    }

    @Override
    public boolean enable(Principal principal, Set<Action> actions) {
        boolean changed = super.enable(principal, actions);
        updateEntityAccess(principal, getEnabledActions(principal));
        return changed;
    }

    @Override
    public boolean enableOnly(Principal principal, Set<Action> actions) {
        // Never replace permissions of the owner
        if (! principal.equals(this.nflow.getOwner())) {
            boolean changed = super.enableOnly(principal, actions);
            updateEntityAccess(principal, getEnabledActions(principal));
            return changed;
        } else {
            return false;
        }
    }

    @Override
    public boolean enableOnly(Principal principal, AllowedActions actions) {
        // Never replace permissions of the owner
        if (! principal.equals(this.nflow.getOwner())) {
            boolean changed = super.enableOnly(principal, actions);
            updateEntityAccess(principal, getEnabledActions(principal));
            return changed;
        } else {
            return false;
        }
    }

    @Override
    public boolean disable(Principal principal, Set<Action> actions) {
        // Never disable permissions of the owner
        if (! principal.equals(this.nflow.getOwner())) {
            boolean changed = super.disable(principal, actions);
            updateEntityAccess(principal, getEnabledActions(principal));
            return changed;
        } else {
            return false;
        }
    }

    @Override
    public void setupAccessControl(Principal owner) {
        enable(owner, NflowAccessControl.EDIT_DETAILS);
        enable(owner, NflowAccessControl.ACCESS_OPS);
        enable(JcrMetadataAccess.ADMIN, NflowAccessControl.EDIT_DETAILS);
        enable(JcrMetadataAccess.ADMIN, NflowAccessControl.ACCESS_OPS);

        super.setupAccessControl(owner);
    }
    
    @Override
    public void removeAccessControl(Principal owner) {
        super.removeAccessControl(owner);
        
        this.nflow.getNflowDetails().ifPresent(d -> JcrAccessControlUtil.clearHierarchyPermissions(d.getNode(), nflow.getNode()));
        this.nflow.getNflowData().ifPresent(d -> JcrAccessControlUtil.clearHierarchyPermissions(d.getNode(), nflow.getNode()));
    }

    @Override
    protected boolean isAdminAction(Action action) {
        return action.implies(NflowAccessControl.CHANGE_PERMS);
    }

    protected void updateEntityAccess(Principal principal, Set<? extends Action> actions) {
        Set<String> detailPrivs = new HashSet<>();
        Set<String> dataPrivs = new HashSet<>();
        Set<String> summaryPrivs = new HashSet<>();
        
        // Enable/disable nflow ops access
        if (actions.stream().filter(action -> action.implies(NflowAccessControl.ACCESS_OPS)).findFirst().isPresent()) {
            this.nflow.getOpsAccessProvider().ifPresent(provider -> provider.grantAccess(nflow.getId(), principal));
        } else {
            this.nflow.getOpsAccessProvider().ifPresent(provider -> provider.revokeAccess(nflow.getId(), principal));
        }

        // Collect all JCR privilege changes based on the specified actions.
        actions.forEach(action -> {
            if (action.implies(NflowAccessControl.CHANGE_PERMS)) {
                Collections.addAll(summaryPrivs, Privilege.JCR_READ_ACCESS_CONTROL, Privilege.JCR_MODIFY_ACCESS_CONTROL);
                Collections.addAll(detailPrivs, Privilege.JCR_READ_ACCESS_CONTROL, Privilege.JCR_MODIFY_ACCESS_CONTROL);
                Collections.addAll(dataPrivs, Privilege.JCR_READ_ACCESS_CONTROL, Privilege.JCR_MODIFY_ACCESS_CONTROL);
            } else if (action.implies(NflowAccessControl.EDIT_DETAILS)) {
                //also add read to the category summary
                final AllowedActions categoryAllowedActions = nflow.getCategory().getAllowedActions();
                if (categoryAllowedActions.hasPermission(CategoryAccessControl.CHANGE_PERMS)) {
                    categoryAllowedActions.enable(principal, CategoryAccessControl.ACCESS_DETAILS);
                }
                //If a user has Edit access for the nflow, they need to be able to also Read the template
                this.nflow.getNflowDetails()
                    .map(NflowDetails::getTemplate)
                    .map(NflowManagerTemplate::getAllowedActions)
                    .filter(allowedActions -> allowedActions.hasPermission(TemplateAccessControl.CHANGE_PERMS))
                    .ifPresent(allowedActions -> allowedActions.enable(principal, TemplateAccessControl.ACCESS_TEMPLATE));
                
                summaryPrivs.add(Privilege.JCR_ALL);                
                detailPrivs.add(Privilege.JCR_ALL);
                dataPrivs.add(Privilege.JCR_ALL);                
            } else if (action.implies(NflowAccessControl.EDIT_SUMMARY)) {
                //also add read to the category summary
                final AllowedActions categoryAllowedActions = nflow.getCategory().getAllowedActions();
                if (categoryAllowedActions.hasPermission(CategoryAccessControl.CHANGE_PERMS)) {
                    categoryAllowedActions.enable(principal, CategoryAccessControl.ACCESS_CATEGORY);
                }
                
                summaryPrivs.add(Privilege.JCR_ALL);                
            } else if (action.implies(NflowAccessControl.ACCESS_DETAILS)) {
                //also add read to the category summary
                final AllowedActions categoryAllowedActions = nflow.getCategory().getAllowedActions();
                if (categoryAllowedActions.hasPermission(CategoryAccessControl.CHANGE_PERMS)) {
                    categoryAllowedActions.enable(principal, CategoryAccessControl.ACCESS_DETAILS);
                }
                //If a user has Read access for the nflow, they need to be able to also Read the template
                this.nflow.getNflowDetails()
                    .map(NflowDetails::getTemplate)
                    .map(NflowManagerTemplate::getAllowedActions)
                    .filter(allowedActions -> allowedActions.hasPermission(TemplateAccessControl.CHANGE_PERMS))
                    .ifPresent(allowedActions -> allowedActions.enable(principal, TemplateAccessControl.ACCESS_TEMPLATE));
                
                summaryPrivs.add(Privilege.JCR_READ);
                detailPrivs.add(Privilege.JCR_READ);                
                dataPrivs.add(Privilege.JCR_READ);                
            } else if (action.implies(NflowAccessControl.ACCESS_NFLOW)) {
                //also add read to the category summary
                final AllowedActions categoryAllowedActions = nflow.getCategory().getAllowedActions();
                if (categoryAllowedActions.hasPermission(CategoryAccessControl.CHANGE_PERMS)) {
                    categoryAllowedActions.enable(principal, CategoryAccessControl.ACCESS_CATEGORY);
                }
                
                summaryPrivs.add(Privilege.JCR_READ);
            }
        });
        
        JcrAccessControlUtil.setPermissions(this.nflow.getNode(), principal, summaryPrivs);
        this.nflow.getNflowSummary().ifPresent(s -> JcrAccessControlUtil.setPermissions(s.getNode(), principal, summaryPrivs));
        this.nflow.getNflowDetails().ifPresent(d -> JcrAccessControlUtil.setPermissions(d.getNode(), principal, detailPrivs));
        this.nflow.getNflowData().ifPresent(d -> JcrAccessControlUtil.setPermissions(d.getNode(), principal, dataPrivs));
    }
}
