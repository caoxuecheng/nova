package com.onescorpin.nflowmgr.service.nflow;

import com.onescorpin.nflowmgr.rest.model.EntityVersion;

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

import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nflowmgr.rest.model.NflowSummary;
import com.onescorpin.nflowmgr.rest.model.NflowVersions;
import com.onescorpin.nflowmgr.rest.model.NifiNflow;
import com.onescorpin.nflowmgr.rest.model.UINflow;
import com.onescorpin.nflowmgr.rest.model.UserField;
import com.onescorpin.nflowmgr.rest.model.UserProperty;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.modeshape.versioning.VersionNotFoundException;
import com.onescorpin.policy.rest.model.FieldRuleProperty;
import com.onescorpin.security.action.Action;

import java.io.Serializable;
import java.security.AccessControlException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Common Nflow Manager actions
 */
public interface NflowManagerNflowService {

    /**
     * Checks the current security context has been granted permission to perform the specified action(s)
     * on the nflow with the specified nflow ID.  If the nflow does not exist then no check is made.
     *
     * @param id     the nflow ID
     * @param action an action to check
     * @param more   any additional actions to check
     * @return true if the nflow existed, otherwise false
     * @throws AccessControlException thrown if the nflow exists and the action(s) checked are not permitted
     */
    boolean checkNflowPermission(String id, Action action, Action... more);

    /**
     * Return a nflow matching its system category and system nflow name
     *
     * @param categoryName a system category name
     * @param nflowName     a system nflow name
     * @return a nflow matching its system category and system nflow name
     */
    NflowMetadata getNflowByName(String categoryName, String nflowName);

    /**
     * Return a nflow matching the incoming id
     *
     * @param id a nflow id
     * @return a nflow matching the id, or null if not found
     */
    NflowMetadata getNflowById(String id);

    /**
     * Return a nflow matching the nflowId.
     *
     * @param refreshTargetTableSchema if true it will attempt to update the metadata of the destination table {@link NflowMetadata#table} with the real the destination
     * @return a nflow matching the nflowId
     */
    NflowMetadata getNflowById(String id, boolean refreshTargetTableSchema);

    /**
     * Get all versions of the nflow with the specified ID.  The results will
     * have at least one version: the current nflow version.  The results may
     * also contain the state of the version of each nflow itself.
     * @param nflowId the nflow's ID
     * @param includeNflows 
     * @return the nflow versions
     */
    NflowVersions getNflowVersions(String nflowId, boolean includeNflows);

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
     * Return a list of nflows, optionally returning a more verbose object populating all the templates and properties.
     * Verbose will return {@link NflowMetadata} objects, false will return {@link NflowSummary} objects
     * <p>
     * The sized of the returned list will not be greater than the limit parameter, and the first element 
     * of the list will be the n'th nflow in the list of all nflows as specified by the start parameter.
     *
     * @param verbose true will return {@link NflowMetadata} objects, false will return {@link NflowSummary} objects
     * @param pageable describes the page requested
     * @param filter TODO
     * @return a list of nflow objects
     */
    Page<UINflow> getNflows(boolean verbose, Pageable pageable, String filter);
    
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
     * Find all the nflows assigned to a given template
     *
     * @param registeredTemplateId a registered template id
     * @return all the nflows assigned to a given template
     */
    List<NflowMetadata> getNflowsWithTemplate(String registeredTemplateId);

    /**
     * Converts the specified nflow id to a {@link Nflow.ID} object.
     *
     * @param fid the nflow id, usually a string
     * @return the {@link Nflow.ID} object
     */
    Nflow.ID resolveNflow(@Nonnull Serializable fid);

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
     * @throws RuntimeException if the nflow cannot be deleted
     */
    void deleteNflow(@Nonnull String nflowId);

    /**
     * Allows a nflow's cleanup flow to run.
     *
     * @param nflowId the nflow id to be cleaned up
     * @throws RuntimeException if the metadata property cannot be set
     */
    void enableNflowCleanup(@Nonnull String nflowId);

    /**
     * Change the state of the nflow to be {@link NflowMetadata.STATE#ENABLED}
     *
     * @return a summary of the nflow after being enabled
     */
    NflowSummary enableNflow(String nflowId);

    /**
     * Change the state of the nflow to be {@link NflowMetadata.STATE#DISABLED}
     *
     * @return a summary of the nflow after being disabled
     */
    NflowSummary disableNflow(String nflowId);


    void applyNflowSelectOptions(List<FieldRuleProperty> properties);

    /**
     * Gets the user-defined fields for nflows.
     *
     * @return the user-defined fields
     */
    @Nonnull
    Set<UserField> getUserFields();

    /**
     * Sets the user-defined fields for nflows.
     *
     * @param userFields the new set of user-defined fields
     */
    void setUserFields(@Nonnull Set<UserField> userFields);

    /**
     * Gets the user-defined fields for nflows within the specified category.
     *
     * @param categoryId the category id
     * @return the user-defined fields, if the category exists
     */
    @Nonnull
    Optional<Set<UserProperty>> getUserFields(@Nonnull String categoryId);


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
