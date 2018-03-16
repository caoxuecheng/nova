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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.jcr.Node;

import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.security.RoleMembership;
import com.onescorpin.metadata.modeshape.common.JcrPropertiesEntity;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.security.JcrHadoopSecurityGroup;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.security.role.JcrAbstractRoleMembership;
import com.onescorpin.metadata.modeshape.security.role.JcrEntityRoleMembership;
import com.onescorpin.metadata.modeshape.security.role.JcrSecurityRole;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.role.SecurityRole;

/**
 *
 */
public class CategoryDetails extends JcrPropertiesEntity {
    
    public static final String HADOOP_SECURITY_GROUPS = "tba:securityGroups";
    public static final String NFLOW_ROLE_MEMBERSHIPS = "tba:nflowRoleMemberships";
    public static final String NFLOW_ROLE_MEMBERSHIPS_TYPE = "tba:CategoryNflowRoleMemberships";

    private Optional<NflowOpsAccessControlProvider> opsAccessProvider;
    
    /**
     * @param node
     */
    public CategoryDetails(Node node) {
        super(node);
    }
    
    public CategoryDetails(Node node, Optional<NflowOpsAccessControlProvider> opsPvdr) {
        super(node);
        this.opsAccessProvider = opsPvdr;
    }
    
    public List<? extends Nflow> getNflows() {
        List<JcrNflow> nflows = JcrUtil.getChildrenMatchingNodeType(this.node, 
                                                                  "tba:nflow", 
                                                                  JcrNflow.class,
                                                                  this.opsAccessProvider.map(p -> new Object[] { p }).orElse(new Object[0]));
        return nflows;
    }

    @Nonnull
    public Map<String, String> getUserProperties() {
        return JcrPropertyUtil.getUserProperties(node);
    }

    public void setUserProperties(@Nonnull final Map<String, String> userProperties, @Nonnull final Set<UserFieldDescriptor> userFields) {
        JcrPropertyUtil.setUserProperties(node, userFields, userProperties);
    }

    public List<? extends HadoopSecurityGroup> getSecurityGroups() {
        Set<Node> list = JcrPropertyUtil.getReferencedNodeSet(this.node, HADOOP_SECURITY_GROUPS);
        List<HadoopSecurityGroup> hadoopSecurityGroups = new ArrayList<>();
        if (list != null) {
            for (Node n : list) {
                hadoopSecurityGroups.add(JcrUtil.createJcrObject(n, JcrHadoopSecurityGroup.class));
            }
        }
        return hadoopSecurityGroups;
    }

    public void setSecurityGroups(List<? extends HadoopSecurityGroup> hadoopSecurityGroups) {
        JcrPropertyUtil.setProperty(this.node, HADOOP_SECURITY_GROUPS, null);

        for (HadoopSecurityGroup securityGroup : hadoopSecurityGroups) {
            Node securityGroupNode = ((JcrHadoopSecurityGroup) securityGroup).getNode();
            JcrPropertyUtil.addToSetProperty(this.node, HADOOP_SECURITY_GROUPS, securityGroupNode, true);
        }
    }

    public Set<RoleMembership> getNflowRoleMemberships() {
        Node defaultsNode = JcrUtil.getNode(getNode(), NFLOW_ROLE_MEMBERSHIPS);
        return JcrUtil.getPropertyObjectSet(defaultsNode, JcrAbstractRoleMembership.NODE_NAME, JcrNflowDefaultRoleMembership.class, this).stream()
                        .map(RoleMembership.class::cast)
                        .collect(Collectors.toSet());
    }
    
    public Optional<RoleMembership> getNflowRoleMembership(String roleName) {
        Node defaultsNode = JcrUtil.getNode(getNode(), NFLOW_ROLE_MEMBERSHIPS);
        return JcrEntityRoleMembership.find(defaultsNode, roleName, JcrNflowDefaultRoleMembership.class, this).map(RoleMembership.class::cast);
    }
    
    public void enableNflowRoles(List<SecurityRole> nflowRoles) {
        Node nflowRolesNode = JcrUtil.getOrCreateNode(getNode(), NFLOW_ROLE_MEMBERSHIPS, NFLOW_ROLE_MEMBERSHIPS_TYPE);
        nflowRoles.forEach(role -> JcrAbstractRoleMembership.create(nflowRolesNode, ((JcrSecurityRole) role).getNode(), JcrNflowDefaultRoleMembership.class, this));
    }
}
