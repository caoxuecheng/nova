package com.onescorpin.metadata.api.nflow;

import com.onescorpin.metadata.api.BaseProvider;

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

import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.versioning.EntityVersionProvider;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

public interface NflowProvider extends BaseProvider<Nflow, Nflow.ID>, EntityVersionProvider<Nflow, Nflow.ID> {

    NflowSource ensureNflowSource(Nflow.ID nflowId, Datasource.ID dsId);

    NflowSource ensureNflowSource(Nflow.ID nflowId, Datasource.ID id, ServiceLevelAgreement.ID slaId);

    NflowDestination ensureNflowDestination(Nflow.ID nflowId, Datasource.ID dsId);

    Nflow ensureNflow(Category.ID categoryId, String nflowSystemName);

    Nflow ensureNflow(String categorySystemName, String nflowSystemName);

    Nflow ensureNflow(String categorySystemName, String nflowSystemName, String descr);

    Nflow ensureNflow(String categorySystemName, String nflowSystemName, String descr, Datasource.ID destId);

    Nflow ensureNflow(String categorySystemName, String nflowSystemName, String descr, Datasource.ID srcId, Datasource.ID destId);

    Nflow createPrecondition(Nflow.ID nflowId, String descr, List<Metric> metrics);

    PreconditionBuilder buildPrecondition(Nflow.ID nflowId);

    Nflow findBySystemName(String systemName);

    Nflow findBySystemName(String categorySystemName, String systemName);

    NflowCriteria nflowCriteria();

    Nflow getNflow(Nflow.ID id);

    List<? extends Nflow> getNflows();

    List<Nflow> getNflows(NflowCriteria criteria);

    Nflow addDependent(Nflow.ID targetId, Nflow.ID dependentId);

    Nflow removeDependent(Nflow.ID nflowId, Nflow.ID depId);

    void populateInverseNflowDependencies();


    void removeNflowSources(Nflow.ID nflowId);

    void removeNflowSource(Nflow.ID nflowId, Datasource.ID dsId);

    void removeNflowDestination(Nflow.ID nflowId, Datasource.ID dsId);

    void removeNflowDestinations(Nflow.ID nflowId);

//    NflowSource getNflowSource(NflowSource.ID id);
//    NflowDestination getNflowDestination(NflowDestination.ID id);

    Nflow.ID resolveNflow(Serializable fid);

//    NflowSource.ID resolveSource(Serializable sid);
//    NflowDestination.ID resolveDestination(Serializable sid);

    boolean enableNflow(Nflow.ID id);

    boolean disableNflow(Nflow.ID id);

    /**
     * Deletes the nflow with the specified id.
     *
     * @param nflowId the nflow id to be deleted
     * @throws RuntimeException if the nflow cannot be deleted
     */
    void deleteNflow(Nflow.ID nflowId);

    Nflow updateNflowServiceLevelAgreement(Nflow.ID nflowId, ServiceLevelAgreement sla);

    /**
     * Merge properties and return the newly merged properties
     */
    Map<String, Object> mergeNflowProperties(Nflow.ID nflowId, Map<String, Object> properties);

    Map<String, Object> replaceProperties(Nflow.ID nflowId, Map<String, Object> properties);

    /**
     * Gets the user fields for all nflows.
     *
     * @return user field descriptors
     * @since 0.4.0
     */
    @Nonnull
    Set<UserFieldDescriptor> getUserFields();

    /**
     * Sets the user fields for all nflows.
     *
     * @param userFields user field descriptors
     * @since 0.4.0
     */
    void setUserFields(@Nonnull Set<UserFieldDescriptor> userFields);

    List<? extends Nflow> findByTemplateId(NflowManagerTemplate.ID templateId);

    List<? extends Nflow> findByCategoryId(Category.ID categoryId);

    // TODO Methods to add policy info to source
}
