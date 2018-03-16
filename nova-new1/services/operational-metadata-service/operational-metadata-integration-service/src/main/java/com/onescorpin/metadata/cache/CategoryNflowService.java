package com.onescorpin.metadata.cache;
/*-
 * #%L
 * onescorpin-job-repository-controller
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
import com.onescorpin.nflowmgr.service.category.SimpleCategoryCache;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.jpa.common.EntityAccessControlled;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Service to get Nflows Grouped by Category using Access Control
 */
public class CategoryNflowService {

    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;

    @Inject
    SimpleCategoryCache categoryCache;

    @EntityAccessControlled
    public Map<NflowCategory, List<SimpleNflow>> getNflowsByCategory(){
        Map<String,List<OpsManagerNflow>> categoryNflows = opsManagerNflowProvider.getNflowsGroupedByCategory();
        Map<String,NflowCategory> nflowCategoryMap = categoryCache.getCategoriesByName();
        Map<NflowCategory,List<SimpleNflow>> nflowCategoryListMap = categoryNflows.entrySet().stream().filter(e -> nflowCategoryMap.containsKey(e.getKey())).collect(Collectors.toMap(e -> nflowCategoryMap.get(e.getKey()),e -> e.getValue().stream().map(f -> new SimpleNflow(f.getId().toString(),f.getName(),nflowCategoryMap.get(e.getKey()))).collect(Collectors.toList())));
        return nflowCategoryListMap;
    }

    public static class SimpleNflow {
        private String name;
        private String id;
        private NflowCategory category;

        public SimpleNflow(String id, String name, NflowCategory category) {
            this.name = name;
            this.id = id;
            this.category = category;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public NflowCategory getCategory() {
            return category;
        }
    }
}
