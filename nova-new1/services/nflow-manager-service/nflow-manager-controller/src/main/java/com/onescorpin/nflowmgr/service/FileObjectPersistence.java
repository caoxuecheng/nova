package com.onescorpin.nflowmgr.service;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * persist the nflow metadata to the file.
 * This should only be used in test cases and is not scalable for production use.
 */
public class FileObjectPersistence {

    private static String filePath = "/tmp";
    private static String NFLOW_METADATA_FILENAME = "nflow-metadata2.json";
    private static String NFLOW_CATEGORIES_FILENAME = "nflow-categories2.json";
    private static String TEMPLATE_METADATA_FILENAME = "registered-templates2.json";

    public static FileObjectPersistence getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void writeCategoriesToFile(Collection<NflowCategory> categories) {

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath + "/" + NFLOW_CATEGORIES_FILENAME);
        try {
            mapper.writeValue(file, categories);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeNflowsToFile(Collection<NflowMetadata> nflows) {

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath + "/" + NFLOW_METADATA_FILENAME);
        try {
            mapper.writeValue(file, nflows);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeTemplatesToFile(Collection<RegisteredTemplate> templates) {

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath + "/" + TEMPLATE_METADATA_FILENAME);
        try {
            mapper.writeValue(file, templates);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<NflowCategory> getCategoriesFromFile() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath + "/" + NFLOW_CATEGORIES_FILENAME);
        Collection<NflowCategory> categories = null;
        if (file.exists()) {
            try {
                categories = mapper.readValue(file, new TypeReference<List<NflowCategory>>() {
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return categories;
    }

    public Collection<NflowMetadata> getNflowsFromFile() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath + "/" + NFLOW_METADATA_FILENAME);
        Collection<NflowMetadata> nflows = null;
        if (file.exists()) {
            try {
                nflows = mapper.readValue(file, new TypeReference<List<NflowMetadata>>() {
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return nflows;
    }

    public Collection<RegisteredTemplate> getTemplatesFromFile() {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(filePath + "/" + TEMPLATE_METADATA_FILENAME);
        Collection<RegisteredTemplate> templates = null;
        if (file.exists()) {
            try {
                templates = mapper.readValue(file, new TypeReference<List<RegisteredTemplate>>() {
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return templates;
    }

    private static class LazyHolder {

        static final FileObjectPersistence INSTANCE = new FileObjectPersistence();
    }
}
