/**
 * 
 */
package com.onescorpin.metadata.modeshape.security.role;

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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.JcrTestConfig;
import com.onescorpin.metadata.modeshape.ModeShapeEngineConfig;
import com.onescorpin.metadata.modeshape.security.ModeShapeAuthConfig;
import com.onescorpin.metadata.modeshape.security.action.JcrActionTreeBuilder;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.support.JcrTool;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.action.Action;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.role.ImmutableAllowedActions;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { ModeShapeEngineConfig.class, JcrTestConfig.class, ModeShapeAuthConfig.class, JcrSecurityRoleProviderTestConfig.class })
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class JcrSecurityRoleProviderTest {
    

    @Inject
    private MetadataAccess metadata;

    @Inject
    private SecurityRoleProvider provider;
    
    private AllowedActions testActions;
    
    @Before
    public void setup() {
        this.testActions = metadata.read(() -> { 
            Node temp = JcrUtil.createNode(JcrMetadataAccess.getActiveSession().getRootNode(), "temp", "tba:allowedActions");
            JcrActionTreeBuilder<?> bldr = new JcrActionTreeBuilder<>(temp, null);
            bldr
                .action(NflowAccessControl.EDIT_DETAILS)
                .action(NflowAccessControl.ENABLE_DISABLE)
                .action(NflowAccessControl.EXPORT)
                .action(NflowAccessControl.ENABLE_DISABLE)
                .add();
            JcrAllowedActions actions = JcrUtil.createJcrObject(temp, JcrAllowedActions.class);
            return new ImmutableAllowedActions(actions);
        }, MetadataAccess.SERVICE);
    }
    
    @Test
    public void testCreateRole() {
        String name = metadata.commit(() -> {
            SecurityRole role = createRole("nflowEditor", "Editor", "Can edit nflows", NflowAccessControl.EDIT_DETAILS, NflowAccessControl.ENABLE_DISABLE, NflowAccessControl.EXPORT);
            
            assertThat(role).isNotNull().extracting("systemName", "title", "description").contains("nflowEditor", "Editor", "Can edit nflows");
            assertThat(role.getAllowedActions().getAvailableActions().stream().flatMap(action -> action.stream()))
                .extracting("systemName")
                .contains(NflowAccessControl.ACCESS_DETAILS.getSystemName(), NflowAccessControl.EDIT_DETAILS.getSystemName(), NflowAccessControl.ENABLE_DISABLE.getSystemName(), NflowAccessControl.EXPORT.getSystemName());
            
            return role.getSystemName();
        }, MetadataAccess.SERVICE);
    }
    
    @Test
    public void testFindRole() {
        metadata.commit(() -> {
            createRole("nflowEditor", "Editor", "Can edit nflows", NflowAccessControl.EDIT_DETAILS, NflowAccessControl.ENABLE_DISABLE, NflowAccessControl.EXPORT);
        }, MetadataAccess.SERVICE);
        
        metadata.read(() -> { 
            Optional<SecurityRole> option = this.provider.getRole(SecurityRole.NFLOW, "nflowEditor");
            
            assertThat(option).isNotNull();
            assertThat(option.isPresent()).isTrue();
            assertThat(option.get()).isNotNull().extracting("systemName", "title", "description").contains("nflowEditor", "Editor", "Can edit nflows");
            assertThat(option.get().getAllowedActions().getAvailableActions().stream().flatMap(action -> action.stream()))
                .extracting("systemName")
                .contains(NflowAccessControl.ACCESS_DETAILS.getSystemName(), 
                          NflowAccessControl.EDIT_DETAILS.getSystemName(), 
                          NflowAccessControl.ENABLE_DISABLE.getSystemName(), 
                          NflowAccessControl.EXPORT.getSystemName());
        }, MetadataAccess.SERVICE);
        
        metadata.read(() -> { 
            Optional<SecurityRole> option = this.provider.getRole(SecurityRole.NFLOW, "bogus");
            
            assertThat(option).isNotNull();
            assertThat(option.isPresent()).isFalse();
        }, MetadataAccess.SERVICE);
    }
    
    @Test
    public void testFindRoles() {
        metadata.commit(() -> {
            createRole("nflowEditor", "Editor", "Can edit nflows", NflowAccessControl.EDIT_DETAILS, NflowAccessControl.ENABLE_DISABLE, NflowAccessControl.EXPORT);
            createRole("nflowViewer", "Viewer", "Can view nflows only", NflowAccessControl.ACCESS_DETAILS);
        }, MetadataAccess.SERVICE);
        
        metadata.read(() -> { 
            List<SecurityRole> list = this.provider.getEntityRoles(SecurityRole.NFLOW);
            
            assertThat(list).isNotNull().hasSize(2);
            
            assertThat(list.get(0)).isNotNull().extracting("systemName", "title", "description").contains("nflowEditor", "Editor", "Can edit nflows");
            assertThat(list.get(0).getAllowedActions().getAvailableActions().stream().flatMap(action -> action.stream()))
                .extracting("systemName")
                .contains(NflowAccessControl.ACCESS_DETAILS.getSystemName(), 
                          NflowAccessControl.EDIT_DETAILS.getSystemName(), 
                          NflowAccessControl.ENABLE_DISABLE.getSystemName(), 
                          NflowAccessControl.EXPORT.getSystemName());
            
            assertThat(list.get(1)).isNotNull().extracting("systemName", "title", "description").contains("nflowViewer", "Viewer", "Can view nflows only");
            assertThat(list.get(1).getAllowedActions().getAvailableActions().stream().flatMap(action -> action.stream()))
                .extracting("systemName")
                .contains(NflowAccessControl.ACCESS_DETAILS.getSystemName())
                .doesNotContain(NflowAccessControl.EDIT_DETAILS.getSystemName());
        }, MetadataAccess.SERVICE);
    }
    
    
    @Test
    public void testRemoveRole() {
        metadata.commit(() -> {
            createRole("nflowEditor", "Editor", "Can edit nflows", NflowAccessControl.EDIT_DETAILS, NflowAccessControl.ENABLE_DISABLE, NflowAccessControl.EXPORT);
        }, MetadataAccess.SERVICE);
        
        boolean deleted = metadata.commit(() -> {
            return this.provider.removeRole(SecurityRole.NFLOW, "nflowEditor");
        }, MetadataAccess.SERVICE);
        
        assertThat(deleted).isTrue();
        
        metadata.read(() -> { 
            Optional<SecurityRole> option = this.provider.getRole(SecurityRole.NFLOW, "nflowEditor");
            
            assertThat(option).isNotNull();
            assertThat(option.isPresent()).isFalse();
        }, MetadataAccess.SERVICE);
        
        deleted = metadata.commit(() -> {
            return this.provider.removeRole(SecurityRole.NFLOW, "nflowEditor");
        }, MetadataAccess.SERVICE);
        
        assertThat(deleted).isFalse();
    }
    
    @Test
    public void testSetPermissions() {
        metadata.commit(() -> {
            createRole("nflowEditor", "Editor", "Can edit nflows", NflowAccessControl.ACCESS_NFLOW);
        }, MetadataAccess.SERVICE);

        metadata.commit(() -> {
            Optional<SecurityRole> option = this.provider.getRole(SecurityRole.NFLOW, "nflowEditor");
            
            assertThat(option.get().getAllowedActions().getAvailableActions().stream().flatMap(action -> action.stream()))
                .extracting("systemName")
                .contains(NflowAccessControl.ACCESS_NFLOW.getSystemName())
                .doesNotContain(NflowAccessControl.EDIT_DETAILS.getSystemName());
            
            this.provider.setPermissions(SecurityRole.NFLOW, "nflowEditor", NflowAccessControl.EDIT_DETAILS, NflowAccessControl.EXPORT);
        }, MetadataAccess.SERVICE);
        
        metadata.read(() -> {
            Optional<SecurityRole> option = this.provider.getRole(SecurityRole.NFLOW, "nflowEditor");
            
            assertThat(option.get().getAllowedActions().getAvailableActions().stream().flatMap(action -> action.stream()))
                .extracting("systemName")
                .contains(NflowAccessControl.ACCESS_NFLOW.getSystemName(),
                          NflowAccessControl.ACCESS_DETAILS.getSystemName(), 
                          NflowAccessControl.EDIT_DETAILS.getSystemName(), 
                          NflowAccessControl.EXPORT.getSystemName())
                .doesNotContain(NflowAccessControl.ENABLE_DISABLE.getSystemName());
        }, MetadataAccess.SERVICE);
        
    }
    
    private SecurityRole createRole(String sysName, String title, String descr, Action... perms) {
        SecurityRole role = this.provider.createRole(SecurityRole.NFLOW, sysName, title, descr);
        role.setPermissions(perms);
        return role;
    }

}
