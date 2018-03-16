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
import com.onescorpin.security.UsernamePrincipal;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.action.AllowedEntityActionsProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class, ModeShapeAuthConfig.class, JcrNflowSecurityTestConfig.class})
public class JcrNflowAllowedActionsTest {

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

    private String categorySystemName;
    private Nflow.ID idA;
    private Nflow.ID idB;
    private Nflow.ID idC;

    @Before
    public void createNflows() {
        categorySystemName = metadata.commit(() -> {
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER1));
            actionsProvider.getAllowedActions(AllowedActions.SERVICES).ifPresent(allowed -> allowed.enableAll(TEST_USER2));
            Category cat = categoryProvider.ensureCategory("test");
            cat.getAllowedActions().enableAll(TEST_USER1);
            cat.getAllowedActions().enableAll(TEST_USER2);
            return cat.getSystemName();
        }, JcrMetadataAccess.SERVICE);

        this.idA = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categorySystemName, "NflowA");
            nflow.setDescription("Nflow A");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, TEST_USER1);

        this.idB = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categorySystemName, "NflowB");
            nflow.setDescription("Nflow B");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, TEST_USER2);

        this.idC = metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.ensureNflow(categorySystemName, "NflowC");
            nflow.setDescription("Nflow C");
            nflow.setJson("{ \"property\":\"value\" }");
            nflow.setState(State.ENABLED);
            return nflow.getId();
        }, TEST_USER2);
    }

    @After
    public void cleanup() {
        metadata.commit(() -> {
            this.nflowProvider.deleteNflow(idC);
            this.nflowProvider.deleteNflow(idB);
            this.nflowProvider.deleteNflow(idA);
        }, MetadataAccess.SERVICE);
    }

    @Test
    public void testSeeOnlyOwnNflows() {
        int nflowCnt1 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER1);

        assertThat(nflowCnt1).isEqualTo(1);

        int nflowCnt2 = metadata.read(() -> this.nflowProvider.getNflows().size(), TEST_USER2);

        assertThat(nflowCnt2).isEqualTo(2);
    }

    @Test
    public void testSeeOwnNflowContentOnly() {
        metadata.read(() -> {
            Nflow nflowA = this.nflowProvider.getNflow(idA);

            assertThat(nflowA.getDescription()).isNotNull().isEqualTo("Nflow A");
            assertThat(nflowA.getJson()).isNotNull();
            assertThat(nflowA.getState()).isNotNull();

            Nflow nflowB = this.nflowProvider.getNflow(idB);

            assertThat(nflowB).isNull();
        }, TEST_USER1);
    }

    @Test
    public void testLimitRelationshipResults() {
        metadata.commit(() -> {
            Nflow nflowA = this.nflowProvider.getNflow(idA);
            Nflow nflowB = this.nflowProvider.getNflow(idB);
            Nflow nflowC = this.nflowProvider.getNflow(idC);

            nflowC.addDependentNflow(nflowA);
            nflowC.addDependentNflow(nflowB);
        }, MetadataAccess.SERVICE);

        metadata.read(() -> {
            Nflow nflowC = this.nflowProvider.getNflow(idC);
            List<Nflow> deps = nflowC.getDependentNflows();

            assertThat(deps).hasSize(1).extracting("id").contains(this.idB);
        }, TEST_USER2);
    }

    @Test
    public void testSummaryOnlyRead() {
        metadata.commit(() -> {
            Nflow nflow = this.nflowProvider.findById(idB);
            nflow.getAllowedActions().enable(TEST_USER1, NflowAccessControl.ACCESS_NFLOW);
        }, TEST_USER2);

        metadata.read(() -> {
            Nflow nflow = this.nflowProvider.findById(idB);

            assertThat(nflow.getName()).isNotNull().isEqualTo("NflowB");
            assertThat(nflow.getCategory()).isNotNull().hasFieldOrPropertyWithValue("systemName", this.categorySystemName);

            assertThat(nflow.getJson()).isNull();
            assertThat(nflow.getState()).isNull();
        }, TEST_USER1);
    }
}
