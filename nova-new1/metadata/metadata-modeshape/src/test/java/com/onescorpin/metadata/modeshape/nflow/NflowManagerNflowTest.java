package com.onescorpin.metadata.modeshape.nflow;

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
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.datasource.DerivedDatasource;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.api.template.TemplateDeletionException;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.JcrTestConfig;
import com.onescorpin.metadata.modeshape.ModeShapeEngineConfig;
import com.onescorpin.metadata.modeshape.security.AdminCredentials;
import com.onescorpin.support.NflowNameUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testng.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class, NflowTestConfig.class})
@ComponentScan(basePackages = {"com.onescorpin.metadata.modeshape"})
public class NflowManagerNflowTest {

    private static final Logger log = LoggerFactory.getLogger(NflowManagerNflowTest.class);

    @Inject
    NflowProvider nflowProvider;

    @Inject
    NflowManagerTemplateProvider nflowManagerTemplateProvider;

    @Inject
    private DatasourceProvider datasourceProvider;

    @Inject
    private JcrMetadataAccess metadata;

    @Inject
    private NflowTestUtil nflowTestUtil;


    private boolean deleteTemplate(String templateName) {
        //try to delete the template.  This should fail since there are nflows attached to it
        return metadata.commit(() -> {
            NflowManagerTemplate template = nflowTestUtil.findOrCreateTemplate(templateName);
            return nflowManagerTemplateProvider.deleteTemplate(template.getId());

        }, MetadataAccess.SERVICE);
    }

    private void setupNflowAndTemplate(String categorySystemName, String nflowName, String templateName) {
        //first create the category
        metadata.commit(() -> {
            Category category = nflowTestUtil.findOrCreateCategory(categorySystemName);
            return category.getId();
        }, MetadataAccess.SERVICE);

        //creqte the nflow
        metadata.commit(() -> {
            Nflow nflow = nflowTestUtil.findOrCreateNflow(categorySystemName, nflowName, templateName);
            return nflow.getId();
        }, MetadataAccess.SERVICE);

        //ensure the nflow relates to the template
        metadata.read(() -> {
            NflowManagerTemplate template = nflowTestUtil.findOrCreateTemplate(templateName);
            List<Nflow> nflows = template.getNflows();
            Assert.assertTrue(nflows != null && nflows.size() > 0);
        }, MetadataAccess.SERVICE);
    }

    /**
     * Test querying a large number of nflows
     */
    @Test
    public void testLotsOfNflows() {
        //increase to query more .. i.e. 1000
        int numberOfNflows = 5;

        int numberOfNflowsPerCategory = 20;
        String templateName = "my_template";

        int categories = numberOfNflows / numberOfNflowsPerCategory;
        //create all the categories
        metadata.commit(() -> {
            for (int i = 1; i <= categories; i++) {
                Category category = nflowTestUtil.createCategory("category_" + i);
            }
        }, MetadataAccess.ADMIN);

        metadata.commit(() -> {
            NflowManagerTemplate template = nflowTestUtil.findOrCreateTemplate(templateName);
        }, MetadataAccess.ADMIN);

        //create the nflows
        metadata.commit(() -> {

            NflowManagerTemplate template = nflowTestUtil.findOrCreateTemplate(templateName);
            Category category = null;

            int categoryNum = 0;
            String categoryName = "category" + categoryNum;
            for (int i = 0; i < numberOfNflows; i++) {
                if (i % numberOfNflowsPerCategory == 0) {
                    categoryNum++;
                    categoryName = "category_" + categoryNum;
                    category = nflowTestUtil.findOrCreateCategory(categoryName);
                }
                Nflow nflow = nflowTestUtil.findOrCreateNflow(category, "nflow_" + i, template);
            }
        }, MetadataAccess.ADMIN);

        //now query it
        long time = System.currentTimeMillis();

        Integer size = metadata.read(new AdminCredentials(), () -> {
            List<Nflow> nflows = nflowProvider.findAll();
            return nflows.size();
        });
        long stopTime = System.currentTimeMillis();
        log.info("Time to query {} nflows was {} ms", size, (stopTime - time));

    }

    @Test
    public void testNflowDatasource() {
        String categorySystemName = "my_category";
        String nflowName = "my_nflow";
        String templateName = "my_template";
        String description = " my nflow description";
        setupNflowAndTemplate(categorySystemName, nflowName, templateName);
//        boolean isDefineTable = true;
//        boolean isGetFile = false;

        metadata.commit(() -> {

            Nflow nflow = nflowTestUtil.findNflow(categorySystemName, nflowName);

            Set<Datasource.ID> sources = new HashSet<Datasource.ID>();
            Set<com.onescorpin.metadata.api.datasource.Datasource.ID> destinations = new HashSet<>();
            //Add Table Dependencies
            String uniqueName = NflowNameUtil.fullName(categorySystemName, nflowName);

            DerivedDatasource srcDatasource = datasourceProvider.ensureDatasource(uniqueName, nflow.getDescription(), DerivedDatasource.class);
            sources.add(srcDatasource.getId());
            DerivedDatasource destDatasource = datasourceProvider.ensureDatasource("destination", nflow.getDescription(), DerivedDatasource.class);
            destinations.add(destDatasource.getId());

            sources.stream().forEach(sourceId -> nflowProvider.ensureNflowSource(nflow.getId(), sourceId));
            destinations.stream().forEach(destinationId -> nflowProvider.ensureNflowDestination(nflow.getId(), destinationId));
        }, MetadataAccess.SERVICE);

        //ensure the sources and dest got created
        metadata.read(() -> {
            Nflow nflow = nflowTestUtil.findNflow(categorySystemName, nflowName);
            Assert.assertNotNull(nflow.getSources());
            Assert.assertTrue(nflow.getSources().size() == 1, "Nflow Sources should be 1");

            Assert.assertNotNull(nflow.getDestinations());
            Assert.assertTrue(nflow.getDestinations().size() == 1, "Nflow Destinations should be 1");

            List<? extends NflowDestination> nflowDestinations = nflow.getDestinations();
            if (nflowDestinations != null) {
                NflowDestination nflowDestination = nflowDestinations.get(0);
                Datasource ds = nflowDestination.getDatasource();
                Assert.assertTrue(ds instanceof DerivedDatasource, "Datasource was not expected DerivedDatasource");
            }


        }, MetadataAccess.SERVICE);

    }

    @Test
    public void testNflowTemplates() {
        String categorySystemName = "my_category";
        String nflowName = "my_nflow";
        String templateName = "my_template";
        setupNflowAndTemplate(categorySystemName, nflowName, templateName);

        //try to delete the template.  This should fail since there are nflows attached to it
        Boolean deleteStatus = null;
        try {
            deleteStatus = deleteTemplate(templateName);
        } catch (TemplateDeletionException e) {
            Assert.assertNotNull(e);
            deleteStatus = false;
        }
        //assert that we could not delete it
        Assert.assertFalse(deleteStatus);

        //try to delete the nflow
        metadata.commit(() -> {
            Nflow nflow = nflowTestUtil.findNflow(categorySystemName, nflowName);
            Assert.assertNotNull(nflow);
            if (nflow != null) {
                nflowProvider.delete(nflow);
            }
        }, MetadataAccess.SERVICE);

        //ensure it is deleted
        metadata.read(() -> {
            Nflow nflow = nflowTestUtil.findNflow(categorySystemName, nflowName);
            Assert.assertNull(nflow);
        }, MetadataAccess.SERVICE);

        //try to delete the template.  This should succeed since the nflows are gone
        deleteStatus = deleteTemplate(templateName);
        Assert.assertEquals(deleteStatus.booleanValue(), true);


    }


}
