package com.onescorpin.metadata.modeshape.sla;

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
import com.onescorpin.metadata.api.extension.ExtensibleEntity;
import com.onescorpin.metadata.api.extension.ExtensibleEntityProvider;
import com.onescorpin.metadata.api.extension.ExtensibleTypeProvider;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreementProvider;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.JcrTestConfig;
import com.onescorpin.metadata.modeshape.ModeShapeEngineConfig;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.security.AdminCredentials;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testng.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class})
@ComponentScan(basePackages = {"com.onescorpin.metadata.modeshape.op"})
public class JcrNflowSlaTest {

    private static String NFLOW_SLA = "nflowSla";
    @Inject
    CategoryProvider categoryProvider;
    @Inject
    NflowProvider nflowProvider;
    @Inject
    ServiceLevelAgreementProvider slaProvider;
    @Inject
    NflowServiceLevelAgreementProvider nflowSlaProvider;
    @Inject
    private ExtensibleTypeProvider typeProvider;
    @Inject
    private ExtensibleEntityProvider entityProvider;
    @Inject
    private JcrMetadataAccess metadata;

    public Set<Nflow.ID> createNflows(int number) {
        Set<Nflow.ID> nflowIds = new HashSet<>();
        String categorySystemName = "my_category";
        for (int i = 0; i < number; i++) {
            JcrNflow.NflowId nflowId = createNflow(categorySystemName, "my_nflow" + i);
            nflowIds.add(nflowId);
        }
        return nflowIds;

    }


    public JcrNflow.NflowId createNflow(final String categorySystemName, final String nflowSystemName) {

        Category category = metadata.commit(() -> {
            JcrCategory cat = (JcrCategory) categoryProvider.ensureCategory(categorySystemName);
            cat.setDescription(categorySystemName + " desc");
            cat.setTitle(categorySystemName);
            categoryProvider.update(cat);
            return cat;
        }, MetadataAccess.ADMIN);

        return metadata.commit(() -> {
            JcrCategory cat = (JcrCategory) categoryProvider.ensureCategory(categorySystemName);
            JcrNflow nflow = (JcrNflow) nflowProvider.ensureNflow(categorySystemName, nflowSystemName, nflowSystemName + " desc");

            nflow.setTitle(nflowSystemName);
            return nflow.getId();
        }, MetadataAccess.ADMIN);
    }


    @Before
    public void setUp() throws Exception {
        JcrNflowServiceLevelAgreementProvider jcrNflowSlaProvider = (JcrNflowServiceLevelAgreementProvider) nflowSlaProvider;
        jcrNflowSlaProvider.createType();
    }

    @Test
    public void testCreateNflowSLAEntity() {
        //create 2 nflows
        final int numberOfNflows = 2;
        Set<Nflow.ID> nflowIds = createNflows(numberOfNflows);
        final String nflowSlaTitle = "My New SLA";
        final String nonNflowSlaTitle = "No Nflow SLA";
        ExtensibleEntity.ID nflowSlaEntityId = createNflowSLAEntity(nflowIds, nflowSlaTitle);
        ServiceLevelAgreement.ID nonNflowSla = createGenericSla(nonNflowSlaTitle);

        ServiceLevelAgreement.ID slaId = metadata.read(new AdminCredentials(), () -> {

            JcrNflowServiceLevelAgreementProvider jcrNflowSlaProvider = (JcrNflowServiceLevelAgreementProvider) nflowSlaProvider;

            //ASSERT everything is good

            //Assert query returns the correct result
            List<ExtensibleEntity> entities = jcrNflowSlaProvider.findAllRelationships();
            Assert.assertEquals(entities.size(), 1);

            //Assert relationships are correct
            JcrNflowServiceLevelAgreementRelationship entity = (JcrNflowServiceLevelAgreementRelationship) jcrNflowSlaProvider.getRelationship(nflowSlaEntityId);
            ServiceLevelAgreement nflowSla = entity.getAgreement();
            Assert.assertNotNull(nflowSla);

            List<? extends ServiceLevelAgreement> agreements = slaProvider.getAgreements();
            //assert both agreements are there
            Assert.assertEquals(agreements.size(), 2);

            Set<JcrNflow> nflows = entity.getPropertyAsSet(JcrNflowServiceLevelAgreementRelationship.NFLOWS, JcrNflow.class);
            Assert.assertEquals(nflows.size(), numberOfNflows);
            for (JcrNflow nflow : nflows) {
                Assert.assertTrue(nflowIds.contains(nflow.getId()));
            }

            //find it by the SLA now
            JcrNflowServiceLevelAgreementRelationship finalNflowSla = (JcrNflowServiceLevelAgreementRelationship) jcrNflowSlaProvider.findRelationship(nflowSla.getId());
            Assert.assertNotNull(finalNflowSla);

            //query for SLA objects and assert the result is correct
            List<NflowServiceLevelAgreement> nflowAgreements = jcrNflowSlaProvider.findAllAgreements();

            Assert.assertEquals(nflowAgreements.size(), 1);
            int nonNflowSlaCount = 0;
            for (NflowServiceLevelAgreement agreement : nflowAgreements) {
                Set<? extends Nflow> slaNflows = agreement.getNflows();
                String title = agreement.getName();
                if (slaNflows != null) {
                    Assert.assertEquals(title, nflowSlaTitle);
                    Assert.assertEquals(slaNflows.size(), numberOfNflows);
                    for (Nflow nflow : slaNflows) {
                        Assert.assertTrue(nflowIds.contains(nflow.getId()));
                    }
                } else {
                    Assert.assertEquals(title, nonNflowSlaTitle);
                    nonNflowSlaCount++;
                }

            }
            Assert.assertEquals(nonNflowSlaCount, 0);

            //find by Nflow
            for (Nflow.ID nflowId : nflowIds) {
                List<NflowServiceLevelAgreement> nflowServiceLevelAgreements = jcrNflowSlaProvider.findNflowServiceLevelAgreements(nflowId);
                Assert.assertTrue(nflowServiceLevelAgreements != null && !nflowServiceLevelAgreements.isEmpty());

            }

            return nflowSla.getId();
        });

        ExtensibleEntity entity = metadata.read(new AdminCredentials(), () -> {
            ExtensibleEntity e = entityProvider.getEntity(nflowSlaEntityId);
            Set<Node> nflows = (Set<Node>) e.getPropertyAsSet(JcrNflowServiceLevelAgreementRelationship.NFLOWS, Node.class);
            Assert.assertEquals(nflows.size(), numberOfNflows);
            for (Node nflow : nflows) {
                try {
                    Assert.assertTrue(nflowIds.contains(nflowProvider.resolveNflow(nflow.getIdentifier())));
                } catch (RepositoryException e1) {
                    e1.printStackTrace();
                }
            }
            return e;
        });

        //now remove the nflow relationships
        boolean removedNflowRelationships = metadata.commit(new AdminCredentials(), () -> {
            ServiceLevelAgreement sla = slaProvider.getAgreement(slaId);
            return nflowSlaProvider.removeNflowRelationships(slaId);

        });

        //query for the nflows related to this SLA and verify there are none
        metadata.read(new AdminCredentials(), () -> {
            NflowServiceLevelAgreement nflowServiceLevelAgreement = nflowSlaProvider.findAgreement(slaId);
            Assert.assertTrue(nflowServiceLevelAgreement.getNflows() == null || (nflowServiceLevelAgreement.getNflows().isEmpty()));
            return null;
        });


    }


    public ExtensibleEntity.ID createNflowSLAEntity(Set<Nflow.ID> nflowIdList, String title) {
        return metadata.commit(new AdminCredentials(), () -> {
            ServiceLevelAgreement sla = slaProvider.builder().name(title).description(title + " DESC").build();
            ExtensibleEntity entity = nflowSlaProvider.relate(sla, nflowIdList);
            return entity.getId();
        });

    }

    public ServiceLevelAgreement.ID createGenericSla(String title) {
        return metadata.commit(new AdminCredentials(), () -> {
            ServiceLevelAgreement sla = slaProvider.builder().name(title).description(title + " DESC").build();
            return sla.getId();
        });

    }


}
