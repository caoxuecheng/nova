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

import com.onescorpin.nflowmgr.InvalidOperationException;
import com.onescorpin.nflowmgr.rest.model.EntityVersion;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NflowVersions;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.RegisteredTemplate;
import com.onescorpin.nflowmgr.rest.model.UINflow;
import com.onescorpin.nflowmgr.rest.model.UserFieldCollection;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.metadata.modeshape.versioning.VersionNotFoundException;
import com.onescorpin.nifi.rest.client.NifiClientRuntimeException;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.security.action.Action;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.security.AccessControlException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Provides access to category, nflow, and template metadata.
 */
@Service
public interface MetadataService {
    
    /**
     * Checks the current security context has been granted permission to perform the specified action(s) 
     * on the nflow with the specified nflow ID.  If the nflow does not exist then no check is made.
     * @param id the nflow ID
     * @param action an action to check
     * @param more any additional actions to check
     * @return true if the nflow existed, otherwise false
     * @throws AccessControlException thrown if the nflow exists and the action(s) checked are not permitted
     */
    boolean checkNflowPermission(String id, Action action, Action... more);

    /**
     * Register a template, save it, and return
     *
     * @param registeredTemplate a template to register/update
     * @return the registered template
     */
    RegisteredTemplate registerTemplate(RegisteredTemplate registeredTemplate);

    /**
     * Return all properties registered for a template
     *
     * @param templateId a template id
     * @return all properties registered for a template
     */
    List<NifiProperty> getTemplateProperties(String templateId);

    /**
     * Deletes a template
     *
     * @param templateId a registered template id
     */
    void deleteRegisteredTemplate(String templateId);

    /**
     * Return all registered templates
     *
     * @return a list of all registered templates
     */
    List<RegisteredTemplate> getRegisteredTemplates();

    /**
     * Finds a template by its name
     * @param templateName the name of the template to look for
     * @return the template
     */
    RegisteredTemplate findRegisteredTemplateByName(final String templateName);

    /**
     * Create a new Nflow in NiFi
     *
     * @param nflowMetadata metadata about the nflow
     * @return an object with status information about the newly created nflow, or error information if unsuccessful
     */
    NifiNflow createNflow(NflowMetadata nflowMetadata);

    /**
     * Deletes the specified nflow.
     *
     * @param nflowId the nflow id
     * @throws NflowCleanupFailedException  if the cleanup flow was started but failed to complete successfully
     * @throws NflowCleanupTimeoutException if the cleanup flow was started but failed to complete in the allotted time
     * @throws IllegalArgumentException    if the nflow does not exist
     * @throws IllegalStateException       if there are dependent nflows
     * @throws NifiClientRuntimeException  if the nflow cannot be deleted from NiFi
     * @throws RuntimeException            if the nflow could not be deleted for any other reason
     */
    void deleteNflow(@Nonnull String nflowId);

    /**
     * Change the state of the nflow to be {@link NflowMetadata.STATE#ENABLED}
     *
     * @param nflowId the nflow id
     * @return a summary of the nflow after being enabled
     */
    NflowSummary enableNflow(String nflowId);

    /**
     * Change the state of the nflow to be {@link NflowMetadata.STATE#DISABLED}
     *
     * @param nflowId the nflow id
     * @return a summary of the nflow after being disabled
     */
    NflowSummary disableNflow(String nflowId);

    /**
     * @return a list of all the nflows in the system
     */
    Collection<NflowMetadata> getNflows();

    /**
     * Return a list of nflows, optionally returning a more verbose object populating all the templates and properties.
     * Verbose will return {@link NflowMetadata} objects, false will return {@link NflowSummary} objects
     *
     * @param verbose true will return {@link NflowMetadata} objects, false will return {@link NflowSummary} objects
     * @return a list of nflow objects
     */
    Collection<? extends UINflow> getNflows(boolean verbose);

    /**
     * Gets a page worth of nflows, optionally returning a more verbose object populating all the templates and properties.
     * Verbose will return {@link NflowMetadata} objects, false will return {@link NflowSummary} objects
     *
     * @param verbose true will return {@link NflowMetadata} objects, false will return {@link NflowSummary} objects
     * @param pageable describes the page to be returned
     * @param filter TODO
     * @return a page of nflows determined by the values of limit and start
     */
    Page<UINflow> getNflowsPage(boolean verbose, Pageable pageable, String filter);
    
    /**
     * @return a list of nflows
     */
    List<NflowSummary> getNflowSummaryData();

    /**
     * Return a list of nflows in a given category
     *
     * @param categoryId the category to look at
     * @return a list of nflows in a given category
     */
    List<NflowSummary> getNflowSummaryForCategory(String categoryId);

    /**
     * Return a nflow matching on its system category name and  system nflow name
     *
     * @param categoryName the system name for a category
     * @param nflowName     the system nflow name
     * @return a nflow matching on its system category name and  system nflow name
     */
    NflowMetadata getNflowByName(String categoryName, String nflowName);

    /**
     * Return a nflow matching the nflowId
     *
     * @param nflowId the nflow id
     * @return a nflow matching the nflowId, null if not found
     */
    NflowMetadata getNflowById(String nflowId);

    /**
     * Return a nflow matching the nflowId.
     *
     * @param nflowId                   the nflow id
     * @param refreshTargetTableSchema if true it will attempt to update the metadata of the destination table {@link NflowMetadata#table} with the real the destination
     * @return a nflow matching the nflowId
     */
    NflowMetadata getNflowById(String nflowId, boolean refreshTargetTableSchema);

    /**
     * Return the categories
     *
     * @return the categories
     */
    Collection<NflowCategory> getCategories();

    /**
     * Returns the categories
     * @param includeNflowDetails true to return the list of related nflows.  if true this will be a slower call
     * @return the categories
     */
    Collection<NflowCategory> getCategories(boolean includeNflowDetails);

    /**
     * Return a category matching a system name
     *
     * @param name a category system name
     * @return the matching category, or null if not found
     */
    NflowCategory getCategoryBySystemName(final String name);

    /**
     * Return a category via its id
     * @param categoryId category id
     * @return the matching category, or null if not found
     */
    NflowCategory getCategoryById(final String categoryId);

    /**
     * save a category
     *
     * @param category a category to save
     */
    void saveCategory(NflowCategory category);

    /**
     * Delete a category
     *
     * @return true if deleted, false if not
     * @throws InvalidOperationException if unable to delete (categories cannot be deleted if there are nflows assigned to them)
     */
    boolean deleteCategory(String categoryId) throws InvalidOperationException;

    /**
     * Gets the user-defined fields for all categories.
     *
     * @return the user-defined fields
     */
    @Nonnull
    Set<UserProperty> getCategoryUserFields();

    /**
     * Gets the user-defined fields for all nflows within the specified category.
     *
     * @param categoryId the category id
     * @return the user-defined fields, if the category exists
     */
    @Nonnull
    Optional<Set<UserProperty>> getNflowUserFields(@Nonnull String categoryId);

    /**
     * Get all versions of the nflow with the specified ID.  The results will
     * have at least one version: the current nflow version.  The results may
     * also contain the state of the version of each nflow itself.
     * @param nflowId the nflow's ID
     * @param includeContent indicates whether the nflow content should be included in the results
     * @return the nflow versions
     */
    NflowVersions getNflowVersions(String nflowId, boolean includeContent);

    /**
     * Get a version for the given nflow and version ID.  The returned 
     * optional will be empty if no nflow exists with the given ID.  A
     * VersionNotFoundException will 
     * @param nflowId the nflow ID
     * @param versionId the version ID
     * @param includeContent indicates whether the nflow content should be included in the version
     * @return an optional nflow version
     * @throws VersionNotFoundException if no version exists with the given ID
     */
    Optional<EntityVersion> getNflowVersion(String nflowId, String versionId, boolean includeContent);

    /**
     * Gets the user-defined fields for all categories and nflows.
     *
     * @return the user-defined fields
     */
    @Nonnull
    UserFieldCollection getUserFields();

    /**
     * Sets the user-defined fields for all categories and nflows.
     *
     * @param userFields the new user-defined fields
     */
    void setUserFields(@Nonnull UserFieldCollection userFields);


    /**
     * Update a given nflows datasources clearing its sources/destinations before revaluating the data
     * @param nflowId the id of the nflow rest model to update
     */
    void updateNflowDatasources(String nflowId);

    /**
     * Iterate all of the nflows, clear all sources/destinations and reassign
     * Note this will be an expensive call if you have a lot of nflows
     */
    void updateAllNflowsDatasources();


}
