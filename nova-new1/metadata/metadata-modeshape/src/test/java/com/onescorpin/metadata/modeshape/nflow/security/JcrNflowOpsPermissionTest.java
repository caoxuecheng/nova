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

import static org.mockito.Mockito.verify;

import javax.inject.Inject;

import org.junit.Before;
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
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.JcrTestConfig;
import com.onescorpin.metadata.modeshape.ModeShapeEngineConfig;
import com.onescorpin.metadata.modeshape.security.ModeShapeAuthConfig;
import com.onescorpin.security.UsernamePrincipal;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { ModeShapeEngineConfig.class, JcrTestConfig.class, ModeShapeAuthConfig.class, JcrNflowSecurityTestConfig.class })
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class JcrNflowOpsPermissionTest {
    
    private static final UsernamePrincipal TEST_USER1 = new UsernamePrincipal("tester1");
    private static final UsernamePrincipal TEST_USER2 = new UsernamePrincipal("tester2");

    @Inject
    private MetadataAccess metadata;
    
    @Inject
    private CategoryProvider categoryProvider;
    
    @Inject
    private NflowProvider nflowProvider;
    
    @Inject
    private NflowOpsAccessControlProvider opsAccessProvider;

    @Inject
    private AllowedEntityActionsProvider actionsProvider;

    private String categoryName;
    private Nflow.ID idA;
    private Nflow.ID idB;
    
    @Before
    public void createNflows() {
        this.categoryName = metadata.commit(() -> {
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER1));
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER2));
            
            Category cat = categoryProvider.ensureCategory("test");
            cat.getAllowedActions().enableAll(TEST_USER1);
            cat.getAllowedActions().enableAll(TEST_USER2);
            
            return cat.getSystemName();
        }, JcrMetadataAccess.SERVICE);
        
        this.idA = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowA");
            return nflow.getId();
        }, JcrMetadataAccess.SERVICE);
        
        this.idB = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categoryName, "NflowB");
            return nflow.getId();
        }, JcrMetadataAccess.SERVICE);
    }
    
    @Test
    public void testGrantOpsPermission() {
        metadata.commit(() -> {
            this.nflowProvider.findById(idA).getAllowedActions().enable(TEST_USER1, NflowAccessControl.ACCESS_OPS);
        }, JcrMetadataAccess.SERVICE);
        
        verify(this.opsAccessProvider).grantAccess(idA, TEST_USER1);
    }
    
    @Test
    public void testRevokeOpsPermission() {
        metadata.commit(() -> {
            this.nflowProvider.findById(idA).getAllowedActions().disable(TEST_USER1, NflowAccessControl.ACCESS_OPS);
        }, JcrMetadataAccess.SERVICE);
        
        verify(this.opsAccessProvider).revokeAccess(idA, TEST_USER1);
    }
}
