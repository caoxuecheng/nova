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

import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.modeshape.category.JcrCategory;

import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 */
@Component
public class NflowTestUtil {


    @Inject
    CategoryProvider categoryProvider;

    @Inject
    NflowProvider nflowProvider;

    @Inject
    NflowManagerTemplateProvider nflowManagerTemplateProvider;

    /**
     * must be called within metdata.commit()
     */
    public Category findOrCreateCategory(String categorySystemName) {
        Category category = categoryProvider.findBySystemName(categorySystemName);
        if (category == null) {
            category = createCategory(categorySystemName);
        }
        return category;
    }

    public Category createCategory(String categorySystemName) {
        JcrCategory cat = (JcrCategory) categoryProvider.ensureCategory(categorySystemName);
        cat.setDescription(categorySystemName + " desc");
        cat.setTitle(categorySystemName);
        categoryProvider.update(cat);
        return cat;
    }

    public Nflow findOrCreateNflow(String categorySystemName, String nflowSystemName, String nflowTemplate) {
        Category category = findOrCreateCategory(categorySystemName);
        Nflow nflow = nflowProvider.ensureNflow(category.getId(), nflowSystemName);
        nflow.setDisplayName(nflowSystemName);
        NflowManagerTemplate template = findOrCreateTemplate(nflowTemplate);
        nflow.setTemplate(template);
        return nflowProvider.update(nflow);
    }

    public Nflow findOrCreateNflow(Category category, String nflowSystemName, NflowManagerTemplate template) {
        Nflow nflow = nflowProvider.ensureNflow(category.getId(), nflowSystemName);
        nflow.setDisplayName(nflowSystemName);
        nflow.setTemplate(template);
        nflow.setJson(sampleNflowJson());
        return nflowProvider.update(nflow);
    }

    private String sampleNflowJson() {
        return "";
    }

    public Nflow findNflow(String categorySystemName, String nflowSystemName) {
        Nflow nflow = nflowProvider.findBySystemName(categorySystemName, nflowSystemName);
        return nflow;
    }

    /**
     * returns a NflowManagerTemplate. Must be called within a metadata.commit() call
     */
    public NflowManagerTemplate findOrCreateTemplate(String templateName) {
        NflowManagerTemplate template = nflowManagerTemplateProvider.findByName(templateName);
        if (template == null) {
            template = nflowManagerTemplateProvider.ensureTemplate(templateName);
            return template;
        } else {
            return template;
        }
    }

}
