package com.onescorpin.integration.nflow;

/*-
 * #%L
 * nova-nflow-manager-controller
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

import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.service.template.ExportImportTemplateService;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to load multiple nflows in a loop
 */
@Ignore
public class NflowLoadTest extends NflowIT {

    private static final Logger LOG = LoggerFactory.getLogger(NflowLoadTest.class);

    /**
     * Load the Data Ingest template
     * @throws Exception
     */
    //@Test
    //@Ignore
    public void loadTestDataIngest() throws Exception {
        // the num of categories to use
        int categories = 1;
        //the num of nflows to create in each category
        int maxNflowsInCategory = 20;
        //the category id to start.   categories are created as cat_#

        int startingCategoryId = 62;
        prepare();
        String templatePath = sampleTemplatesPath + DATA_INGEST_ZIP;
        loadTest(templatePath, "nflow", categories, maxNflowsInCategory, startingCategoryId);
    }

    /**
     * Load the simple template with just 3 processors
     * @throws Exception
     */
   // @Test
   // @Ignore
    public void loadSimpleNflow() throws Exception {
        // the num of categories to use
        int categories = 50;
        //the num of nflows to create in each category. nflows are labled cat_#_nflow_#.  it start creating nflows after the last nflow in the category
        int maxNflowsInCategory = 20;

        //the category id to start.   categories are created as cat_#
        int startingCategoryId = 3;
        prepare();
        String templatePath = getClass().getClassLoader().getResource("com/onescorpin/integration/simple_template.template.zip").toURI().getPath();
        loadTest(templatePath, "simple_nflow", categories, maxNflowsInCategory, startingCategoryId);
    }

    /**
     * Bulk load a template into NiFi and Nova
     * categories are named  'category_#
     * nflows are named  cat_#_nflowName_#
     *
     * @param templatePath the template to use
     * @param nflowName the name of the nflow
     * @param categories the number of categories to use/create
     * @param nflowsInCategory the number of nflows to create in each category
     * @param startingCategoryId the category number to use for the first category
     * @throws Exception
     */
    private void loadTest(String templatePath, String nflowName, int categories, int nflowsInCategory, int startingCategoryId) throws Exception {

        ExportImportTemplateService.ImportTemplate ingestTemplate = importTemplate(templatePath);

        for (int i = startingCategoryId; i < (startingCategoryId + categories); i++) {
            //create new category
            String categoryName = "category_" + i;
            NflowCategory category = getorCreateCategoryByName(categoryName);
            Integer maxNflowId = category.getRelatedNflows();
            maxNflowId += 1;
            for (int j = maxNflowId; j < (nflowsInCategory + maxNflowId); j++) {
                try {

                    String updateNflowName = "cat_" + i + "_" + nflowName + "_" + j;
                    NflowMetadata nflow = getCreateNflowRequest(category, ingestTemplate, updateNflowName);
                    long start = System.currentTimeMillis();
                    NflowMetadata response = createNflow(nflow).getNflowMetadata();
                    LOG.info("Time to save: {} was: {} ms ", updateNflowName, (System.currentTimeMillis() - start));
                } catch (Exception e) {

                }

            }
        }

    }


    @Override
    public void startClean() {
        int i = 0;
        //   super.startClean();
    }

    @Override
    public void cleanup() {
        //noop
        int i = 0;
    }
}
