/**
 *
 */
package com.onescorpin.metadata.modeshape.nflow.security;

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

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.Nflow.State;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.JcrTestConfig;
import com.onescorpin.metadata.modeshape.ModeShapeEngineConfig;
import com.onescorpin.metadata.modeshape.security.ModeShapeAuthConfig;
import com.onescorpin.metadata.modeshape.support.JcrTool;
import com.onescorpin.security.UsernamePrincipal;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { ModeShapeEngineConfig.class, JcrTestConfig.class, ModeShapeAuthConfig.class, JcrNflowSecurityTestConfig.class })
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class JcrNflowRolesTest {
    
    private static final UsernamePrincipal TEST_USER1 = new UsernamePrincipal("tester1");
    private static final UsernamePrincipal TEST_USER2 = new UsernamePrincipal("tester2");
    private static final UsernamePrincipal TEST_USER3 = new UsernamePrincipal("tester3");

    @Inject
    private MetadataAccess metadata;
    
    @Inject
    private CategoryProvider categoryProvider;
    
    @Inject
    private NflowProvider nflowProvider;
    
    @Inject
    private SecurityRoleProvider roleProvider;

    @Inject
    private AllowedEntityActionsProvider actionsProvider;
    
    private JcrTool tool = new JcrTool(true, System.out);

    private String categoryName;
    private Nflow.ID idA;
    private Nflow.ID idB;
    private Nflow.ID idC;
    
    @Before
    public void createNflows() {
        this.categoryName = metadata.commit(() -> {
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER1));
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER2));
            
            this.roleProvider.createRole(SecurityRole.NFLOW, "testEditor", "Editor", "Can edit nflows")
                .setPermissions(NflowAccessControl.EDIT_DETAILS, NflowAccessControl.ENABLE_DISABLE, NflowAccessControl.EXPORT);
            this.roleProvider.createRole(SecurityRole.NFLOW, "testViewer", "Viewer", "Can view nflows only")
                .setPermissions(NflowAccessControl.ACCESS_NFLOW);
            
            Category cat = categoryProvider.ensureCategory("test");
            cat.getAllowedActions().enableAll(TEST_USER1);
            cat.getAllowedActions().enableAll(TEST_USER2);
            
            return cat.getSystemName();
        }, JcrMetadataAccess.SERVICE);
        
        this.idA = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowA");
            nflow.setDescription("Nflow A");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, JcrMetadataAccess.SERVICE);
        
        this.idB = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowB");
            nflow.setDescription("Nflow B");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, JcrMetadataAccess.SERVICE);
        
        this.idC = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowC");
            nflow.setDescription("Nflow C");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, JcrMetadataAccess.SERVICE);
        
//        metadata.commit(() -> tool.printSubgraph(JcrMetadataAccess.getActiveSession(), "/metadata/nflows/test"), JcrMetadataAccess.SERVICE);
    }
    
    @Test
    public void testSeeOnlyOwnNflows() {
        metadata.commit(() -> {
            this.nflowProvider.findById(idA).getRoleMembership("testEditor").ifPresent(m -> m.addMember(TEST_USER1));
            this.nflowProvider.findById(idB).getRoleMembership("testEditor").ifPresent(m -> m.addMember(TEST_USER2));
            this.nflowProvider.findById(idB).getRoleMembership("testViewer").ifPresent(m -> m.addMember(TEST_USER1));
            this.nflowProvider.findById(idC).getRoleMembership("testEditor").ifPresent(m -> m.addMember(TEST_USER2));
        }, JcrMetadataAccess.SERVICE);

        int nflowCnt1 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER1);
        
        assertThat(nflowCnt1).isEqualTo(2);
        
        int nflowCnt2 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER2);
        
        assertThat(nflowCnt2).isEqualTo(2);
        
        int nflowCnt3 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER3);
        
        assertThat(nflowCnt3).isEqualTo(0);
    }
    
    @Test
    public void testAddMembership() {
        metadata.read(() -> {
            Nflow nflowA = this.nflowProvider.getNflow(idA);
            
            assertThat(nflowA).isNull();
            
            Nflow nflowB = this.nflowProvider.getNflow(idB);
            
            assertThat(nflowB).isNull();
        }, TEST_USER3);
        
        metadata.commit(() -> {
            this.nflowProvider.findById(idA).getRoleMembership("testViewer").ifPresent(m -> m.addMember(TEST_USER3));
            this.nflowProvider.findById(idB).getRoleMembership("testEditor").ifPresent(m -> m.addMember(TEST_USER3));
        }, JcrMetadataAccess.SERVICE);
        
        metadata.read(() -> {
            Nflow nflowA = this.nflowProvider.getNflow(idA);
            
            assertThat(nflowA.getDescription()).isNotNull().isEqualTo("Nflow A");
            assertThat(nflowA.getJson()).isNull();
            assertThat(nflowA.getState()).isNull();
            
            Nflow nflowB = this.nflowProvider.getNflow(idB);
            
            assertThat(nflowB.getDescription()).isNotNull().isEqualTo("Nflow B");
            assertThat(nflowB.getJson()).isNotNull();
            assertThat(nflowB.getState()).isNotNull();
        }, TEST_USER3);
    }
    
    @Test
    public void testRemoveMembership() {
        metadata.commit(() -> {
            this.nflowProvider.findById(idA).getRoleMembership("testViewer").ifPresent(m -> m.addMember(TEST_USER3));
            this.nflowProvider.findById(idA).getRoleMembership("testEditor").ifPresent(m -> m.addMember(TEST_USER3));
            this.nflowProvider.findById(idB).getRoleMembership("testEditor").ifPresent(m -> m.addMember(TEST_USER3));
        }, JcrMetadataAccess.SERVICE);
        
        metadata.read(() -> {
            Nflow nflowA = this.nflowProvider.getNflow(idA);
            
            assertThat(nflowA.getDescription()).isNotNull().isEqualTo("Nflow A");
            assertThat(nflowA.getJson()).isNotNull();
            assertThat(nflowA.getState()).isNotNull();
            
            Nflow nflowB = this.nflowProvider.getNflow(idB);
            
            assertThat(nflowB.getDescription()).isNotNull().isEqualTo("Nflow B");
            assertThat(nflowB.getJson()).isNotNull();
            assertThat(nflowB.getState()).isNotNull();
        }, TEST_USER3);
        
        metadata.commit(() -> {
            this.nflowProvider.findById(idA).getRoleMembership("testEditor").ifPresent(m -> m.removeMember(TEST_USER3));
        }, JcrMetadataAccess.SERVICE);
        
        metadata.read(() -> {
            Nflow nflowA = this.nflowProvider.getNflow(idA);
            
            assertThat(nflowA.getDescription()).isNotNull().isEqualTo("Nflow A");
            assertThat(nflowA.getJson()).isNull();
            assertThat(nflowA.getState()).isNull();
            
            Nflow nflowB = this.nflowProvider.getNflow(idB);
            
            assertThat(nflowB.getDescription()).isNotNull().isEqualTo("Nflow B");
            assertThat(nflowB.getJson()).isNotNull();
            assertThat(nflowB.getState()).isNotNull();
        }, TEST_USER3);
        
        metadata.commit(() -> {
            this.nflowProvider.findById(idA).getRoleMembership("testViewer").ifPresent(m -> m.removeMember(TEST_USER3));
            this.nflowProvider.findById(idB).getRoleMembership("testEditor").ifPresent(m -> m.removeMember(TEST_USER3));
        }, JcrMetadataAccess.SERVICE);
        
        metadata.read(() -> {
            Nflow nflowA = this.nflowProvider.getNflow(idA);
            
            assertThat(nflowA).isNull();
            
            Nflow nflowB = this.nflowProvider.getNflow(idB);
            
            assertThat(nflowB).isNull();
        }, TEST_USER3);
    }
}
