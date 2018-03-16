package com.onescorpin.search.rest.model;

/*-
 * #%L
 * nova-search-rest-model
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

import java.util.List;

/**
 * Stores the search results coming from a nflow metadata
 */
public class NflowMetadataSearchResultData extends AbstractSearchResultData {

    private String nflowSystemName;
    private String nflowTitle;
    private String nflowDescription;
    private String nflowCategoryId;
    private List<String> nflowTags;

    public NflowMetadataSearchResultData() {
        final String ICON = "linear_scale";
        final String COLOR = "Maroon";
        super.setIcon(ICON);
        super.setColor(COLOR);
        super.setType(SearchResultType.NOVA_NFLOWS);
    }

    public String getNflowSystemName() {
        return nflowSystemName;
    }

    public void setNflowSystemName(String nflowSystemName) {
        this.nflowSystemName = nflowSystemName;
    }

    public String getNflowTitle() {
        return nflowTitle;
    }

    public void setNflowTitle(String nflowTitle) {
        this.nflowTitle = nflowTitle;
    }

    public String getNflowDescription() {
        return nflowDescription;
    }

    public void setNflowDescription(String nflowDescription) {
        this.nflowDescription = nflowDescription;
    }

    public String getNflowCategoryId() {
        return nflowCategoryId;
    }

    public void setNflowCategoryId(String nflowCategoryId) {
        this.nflowCategoryId = nflowCategoryId;
    }

    public List<String> getNflowTags() {
        return nflowTags;
    }

    public void setNflowTags(List<String> nflowTags) {
        this.nflowTags = nflowTags;
    }
}
