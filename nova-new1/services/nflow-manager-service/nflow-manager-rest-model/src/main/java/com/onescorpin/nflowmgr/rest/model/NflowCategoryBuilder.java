package com.onescorpin.nflowmgr.rest.model;

/*-
 * #%L
 * onescorpin-nflow-manager-rest-model
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

import com.onescorpin.nflowmgr.rest.support.SystemNamingService;

/**
 */
public class NflowCategoryBuilder {

    private String name;
    private String description;
    private String icon;
    private String iconColor;
    private int relatedNflows;


    public NflowCategoryBuilder(String name) {
        this.name = name;
        this.iconColor = "black";
    }

    public NflowCategoryBuilder description(String description) {
        this.description = description;
        return this;
    }

    public NflowCategoryBuilder icon(String icon) {
        this.icon = icon;
        return this;
    }

    public NflowCategoryBuilder iconColor(String iconColor) {
        this.iconColor = iconColor;
        return this;
    }

    public NflowCategoryBuilder relatedNflows(int relatedNflows) {
        this.relatedNflows = relatedNflows;
        return this;
    }

    public NflowCategory build() {
        NflowCategory category = new NflowCategory();
        category.setName(this.name);
        category.setDescription(this.description);
        category.setIconColor(this.iconColor);
        category.setIcon(this.icon);

        category.setSystemName(SystemNamingService.generateSystemName(category.getName()));
        return category;
    }


}

