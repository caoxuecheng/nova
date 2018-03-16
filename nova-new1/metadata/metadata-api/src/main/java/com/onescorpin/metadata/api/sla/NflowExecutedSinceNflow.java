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

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;

import java.beans.Transient;

/**
 */
public class NflowExecutedSinceNflow extends DependentNflow {


    private String sinceCategoryAndNflowName;

    private String sinceNflowName;
    private String sinceCategoryName;

    public NflowExecutedSinceNflow() {

    }

    public NflowExecutedSinceNflow(@JsonProperty("sinceCategoryAndNflowName") String sinceCategoryAndNflow, @JsonProperty("categoryAndNflow") String categoryAndNflow) {
        super(categoryAndNflow);
        this.sinceCategoryAndNflowName = sinceCategoryAndNflow;
        this.sinceCategoryName = StringUtils.substringBefore(sinceCategoryAndNflowName, ".");
        this.sinceNflowName = StringUtils.substringAfter(sinceCategoryAndNflowName, ".");
    }

    public NflowExecutedSinceNflow(String hasRunCategory, String hasRunNflow, String sinceCategory, String sinceNflow) {
        super(hasRunCategory, hasRunNflow);
        this.sinceNflowName = sinceNflow;
        this.sinceCategoryName = sinceCategory;
        this.sinceCategoryAndNflowName = sinceCategory + "." + this.sinceNflowName;
    }

    public String getSinceNflowName() {
        return sinceNflowName;
    }

    public void setSinceNflowName(String sinceNflowName) {
        this.sinceNflowName = sinceNflowName;
    }

    public String getSinceCategoryName() {
        return sinceCategoryName;
    }

    public void setSinceCategoryName(String sinceCategoryName) {
        this.sinceCategoryName = sinceCategoryName;
    }

    public String getSinceCategoryAndNflowName() {
        return sinceCategoryAndNflowName;
    }

    public void setSinceCategoryAndNflowName(String sinceCategoryAndNflowName) {
        this.sinceCategoryAndNflowName = sinceCategoryAndNflowName;
    }

    @Override
    @Transient
    public String getDescription() {
        return "Check if nflow " + getSinceCategoryAndNflowName() + " has executed successfully since nflow " + getSinceCategoryAndNflowName();
    }
}
