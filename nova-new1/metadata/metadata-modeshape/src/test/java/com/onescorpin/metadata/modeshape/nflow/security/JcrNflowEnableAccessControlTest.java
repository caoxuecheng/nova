package com.onescorpin.metadata.modeshape.nflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.security.ModeShapeAuthConfig;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.UsernamePrincipal;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class, ModeShapeAuthConfig.class, JcrNflowSecurityTestConfig.class})
public class JcrNflowEnableAccessControlTest {

    private static final UsernamePrincipal TEST_USER1 = new UsernamePrincipal("tester1");
    private static final UsernamePrincipal TEST_USER2 = new UsernamePrincipal("tester2");

    @Inject
    private MetadataAccess metadata;

    @Inject
    private CategoryProvider categoryProvider;

    @Inject
    private NflowProvider nflowProvider;

    @Inject
    private AllowedEntityActionsProvider actionsProvider;
    
    @Inject
    private AccessController accessController;

    private String categoryName;
    private Nflow.ID idA;
    private Nflow.ID idB;
    private Nflow.ID idC;

    @Before
    public void createCategoryNflows() {
        when(this.accessController.isEntityAccessControlled()).thenReturn(true);
        
        categoryName = metadata.commit(() -> {
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER1));
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER2));
            Category cat = categoryProvider.ensureCategory("test");
            cat.getAllowedActions().enableAll(TEST_USER1);
            cat.getAllowedActions().enableAll(TEST_USER2);
            return cat.getSystemName();
        }, JcrMetadataAccess.SERVICE);
    }

    @After
    public void cleanup() {
        metadata.commit(() -> {
            if (idC != null) this.nflowProvider.deleteNflow(idC);
            if (idB != null) this.nflowProvider.deleteNflow(idB);
            if (idA != null) this.nflowProvider.deleteNflow(idA);
        }, MetadataAccess.SERVICE);
    }
    
    @Test
    public void testCreateNflowsNoAccessControl() {
        when(this.accessController.isEntityAccessControlled()).thenReturn(false);
        createNflows();
        
        int nflowCnt1 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER1);
        
        assertThat(nflowCnt1).isEqualTo(3);
        
        int nflowCnt2 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER2);
        
        assertThat(nflowCnt2).isEqualTo(3);
    }

    @Test
    public void testCreateNflowsWithAccessControl() {
        when(this.accessController.isEntityAccessControlled()).thenReturn(true);
        createNflows();
        
        int nflowCnt1 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER1);

        assertThat(nflowCnt1).isEqualTo(1);

        int nflowCnt2 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER2);

        assertThat(nflowCnt2).isEqualTo(2);
    }
    
    @Test
    public void testEnableNflowAccessControl() {
        when(this.accessController.isEntityAccessControlled()).thenReturn(false);
        
        createNflows();
        
        metadata.commit(() -> {
            JcrNflow nflowA = (JcrNflow) this.nflowProvider.getNflow(idA);
            this.actionsProvider.getAvailableActions(AllowedActions.NFLOW)
                .ifPresent(actions -> nflowA.enableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser(), Collections.emptyList()));
        }, TEST_USER1);
        
        
        int nflowCnt1 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER1);
        
        assertThat(nflowCnt1).isEqualTo(3);
        
        int nflowCnt2 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER2);
        
        assertThat(nflowCnt2).isEqualTo(2);
    }
    
    @Test
    public void testDisableNflowAccessControl() {
        when(this.accessController.isEntityAccessControlled()).thenReturn(true);
        
        createNflows();
        
        metadata.commit(() -> {
            JcrNflow nflowB = (JcrNflow) this.nflowProvider.getNflow(idB);
            this.actionsProvider.getAvailableActions(AllowedActions.NFLOW)
                .ifPresent(actions -> nflowB.disableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser()));
            JcrNflow nflowC = (JcrNflow) this.nflowProvider.getNflow(idC);
            this.actionsProvider.getAvailableActions(AllowedActions.NFLOW)
                .ifPresent(actions -> nflowC.disableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser()));
        }, TEST_USER2);
        
        
        int nflowCnt1 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER1);
        
        assertThat(nflowCnt1).isEqualTo(3);
        
        int nflowCnt2 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER2);
        
        assertThat(nflowCnt2).isEqualTo(2);
    }

    private void createNflows() {
        this.idA = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowA");
            nflow.setDescription("Nflow A");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, TEST_USER1);
        
        this.idB = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowB");
            nflow.setDescription("Nflow B");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, TEST_USER2);
        
        this.idC = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowC");
            nflow.setDescription("Nflow C");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, TEST_USER2);
    }
}
