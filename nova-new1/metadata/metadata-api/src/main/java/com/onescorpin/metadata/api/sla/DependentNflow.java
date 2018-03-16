/**
 *
 */
package com.onescorpin.metadata.api.sla;

/*-
 * #%L
 * onescorpin-metadata-api
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

import com.onescorpin.metadata.sla.api.Metric;

import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public abstract class DependentNflow implements Metric {

    private String nflowName;
    private String categoryName;

    private String categoryAndNflow;

    public DependentNflow() {
    }

    public DependentNflow(String categoryAndNflow) {
        super();
        this.categoryName = StringUtils.substringBefore(categoryAndNflow, ".");
        this.nflowName = StringUtils.substringAfter(categoryAndNflow, ".");
        this.categoryAndNflow = categoryAndNflow;
    }

    public DependentNflow(String categoryName, String nflowName) {
        super();
        this.categoryName = categoryName;
        this.nflowName = nflowName;
        this.categoryAndNflow = categoryName + "." + nflowName;
    }

    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryAndNflow() {
        return categoryAndNflow;
    }
}
