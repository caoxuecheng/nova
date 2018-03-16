package com.onescorpin.nflowmgr.service.category;

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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.onescorpin.nflowmgr.InvalidOperationException;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowCategoryBuilder;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.UserField;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.nflowmgr.rest.support.SystemNamingService;
import com.onescorpin.nflowmgr.service.FileObjectPersistence;
import com.onescorpin.security.action.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

/**
 * An in-memory implementation of {@link NflowManagerCategoryService} for testing the REST API.
 */
public class InMemoryNflowManagerCategoryService implements NflowManagerCategoryService {

    private Map<String, NflowCategory> categories = new HashMap<>();

    @PostConstruct
    private void loadCategories() {
        Collection<NflowCategory> savedCategories = FileObjectPersistence.getInstance().getCategoriesFromFile();
        if (savedCategories != null) {
            for (NflowCategory c : savedCategories) {
                categories.put(c.getId(), c);
            }
        }
        if (categories.isEmpty()) {
            bootstrapCategories();
        }

    }

    private void bootstrapCategories() {

        List<NflowCategory> nflowCategoryList = new ArrayList<>();
        nflowCategoryList.add(
            new NflowCategoryBuilder("Employees").description("Employee profile data and records").icon("people").iconColor("#F06292")
                .build());
        nflowCategoryList.add(
            new NflowCategoryBuilder("Sales").description("Sales data including opportunities and leads").icon("phone_android")
                .iconColor("#90A4AE").build());
        nflowCategoryList.add(
            new NflowCategoryBuilder("Online").description("Web traffic data and reports of online activity").icon("web")
                .iconColor("#66BB6A").build());
        nflowCategoryList.add(
            new NflowCategoryBuilder("Payroll").description("Payroll records for employees").icon("attach_money").iconColor("#FFCA28")
                .build());
        nflowCategoryList.add(new NflowCategoryBuilder("Travel").description("Employee travel records including all expense reports")
                                 .icon("local_airport").iconColor("#FFF176").build());
        nflowCategoryList.add(new NflowCategoryBuilder("Data").description("General Data ").icon("cloud").iconColor("#AB47BC").build());
        nflowCategoryList.add(
            new NflowCategoryBuilder("Emails").description("All email traffic data archived for the last 5 years").icon("email")
                .iconColor("#FF5252").build());
        nflowCategoryList.add(new NflowCategoryBuilder("Customers").description("All customer data for various companies").icon("face")
                                 .iconColor("#FF5252").build());

        for (NflowCategory category : nflowCategoryList) {
            category.setId(UUID.randomUUID().toString());
            categories.put(category.getId(), category);
        }

    }

    @Override
    public boolean checkCategoryPermission(String id, Action action, Action... more) {
        return true;
    }

    @Override
    public Collection<NflowCategory> getCategories() {
        return categories.values();
    }

    @Override
    public Collection<NflowCategory> getCategories(boolean includeNflowDetails) {
        return categories.values();
    }

    @Override
    public NflowCategory getCategoryBySystemName(final String name) {
        return Iterables.tryFind(categories.values(), new Predicate<NflowCategory>() {
            @Override
            public boolean apply(NflowCategory nflowCategory) {
                return nflowCategory.getSystemName().equalsIgnoreCase(name);
            }
        }).orNull();
    }

    @Override
    public NflowCategory getCategoryById(final String id) {
        return Iterables.tryFind(categories.values(), new Predicate<NflowCategory>() {
            @Override
            public boolean apply(NflowCategory nflowCategory) {
                return nflowCategory.getId().equalsIgnoreCase(id);
            }
        }).orNull();
    }

    @Override
    public void saveCategory(final NflowCategory category) {
        if (category.getId() == null) {
            category.setId(UUID.randomUUID().toString());
            category.setSystemName(SystemNamingService.generateSystemName(category.getName()));
        } else {
            NflowCategory oldCategory = categories.get(category.getId());

            if (oldCategory != null && !oldCategory.getName().equalsIgnoreCase(category.getName())) {
                ///names have changed
                //only regenerate the system name if there are no related nflows
                if (oldCategory.getRelatedNflows() == 0) {
                    category.setSystemName(SystemNamingService.generateSystemName(category.getName()));
                }
            }
            List<NflowSummary> nflows = categories.get(category.getId()).getNflows();

            category.setNflows(nflows);

        }
        categories.put(category.getId(), category);

        FileObjectPersistence.getInstance().writeCategoriesToFile(categories.values());
    }

    @Override
    public boolean deleteCategory(String categoryId) throws InvalidOperationException {
        NflowCategory category = categories.get(categoryId);
        if (category != null) {
            //dont allow if category has nflows on it
            if (category.getRelatedNflows() > 0) {
                throw new InvalidOperationException(
                    "Unable to delete Category " + category.getName() + ".  This category has " + category.getRelatedNflows()
                    + " nflows associated to it.");
            } else {
                categories.remove(categoryId);
                FileObjectPersistence.getInstance().writeCategoriesToFile(categories.values());
                return true;
            }
        }
        return false;

    }

    @Nonnull
    @Override
    public Set<UserField> getUserFields() {
        return Collections.emptySet();
    }

    @Override
    public void setUserFields(@Nonnull final Set<UserField> userFields) {
        // do nothing
    }

    @Nonnull
    @Override
    public Set<UserProperty> getUserProperties() {
        return Collections.emptySet();
    }
}
