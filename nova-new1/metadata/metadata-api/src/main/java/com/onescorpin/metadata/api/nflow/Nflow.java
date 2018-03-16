package com.onescorpin.metadata.api.nflow;

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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.onescorpin.metadata.api.MissingUserPropertyException;
import com.onescorpin.metadata.api.Propertied;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.security.AccessControlled;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

/**
 * A nflow is a specification for how data should flow into and out of a system.
 *
 * @param  the type of parent category
 */
public interface Nflow extends Propertied, AccessControlled, Serializable {

    ID getId();

    String getName();

    String getQualifiedName();

    String getDisplayName();

    void setDisplayName(String name);

    String getDescription();

    void setDescription(String descr);

    State getState();

    void setState(State state);

    boolean isInitialized();

    InitializationStatus getCurrentInitStatus();

    void updateInitStatus(InitializationStatus status);

    List<InitializationStatus> getInitHistory();

    NflowPrecondition getPrecondition();

    List<Nflow> getDependentNflows();

    boolean addDependentNflow(Nflow nflow);

    boolean removeDependentNflow(Nflow nflow);

    List<Nflow> getUsedByNflows();

    boolean addUsedByNflow(Nflow nflow);

    boolean removeUsedByNflow(Nflow nflow);

    Category getCategory();

    String getVersionName();

    DateTime getCreatedTime();

    DateTime getModifiedTime();

    List<ServiceLevelAgreement> getServiceLevelAgreements();

    /**
     * Gets the user-defined properties for this nflow.
     *
     * @return the user-defined properties
     * @since 0.3.0
     */
    @Nonnull
    Map<String, String> getUserProperties();

    /**
     * Replaces the user-defined properties for this nflow with the specified properties.
     *
     * <p>If the user-defined field descriptors are given then a check is made to ensure that all required properties are specified. These field descriptors should be the union of
     * {@link NflowProvider#getUserFields()} and {@link com.onescorpin.metadata.api.category.CategoryProvider#getNflowUserFields(Category.ID)} with precedence given to the first.</p>
     *
     * @param userProperties the new user-defined properties
     * @param userFields     the user-defined fields
     * @throws MissingUserPropertyException if a required property is empty or missing
     * @see NflowProvider#getUserFields() for the user-defined field descriptors for all nflows
     * @see com.onescorpin.metadata.api.category.CategoryProvider#getNflowUserFields(Category.ID) for the user-defined field descriptors for all nflows within a given category
     * @since 0.4.0
     */
    void setUserProperties(@Nonnull Map<String, String> userProperties, @Nonnull Set<UserFieldDescriptor> userFields);

    List<? extends NflowSource> getSources();

    NflowSource getSource(Datasource.ID id);

    List<? extends NflowDestination> getDestinations();

    NflowDestination getDestination(Datasource.ID id);

    List<? extends HadoopSecurityGroup> getSecurityGroups();

    void setSecurityGroups(List<? extends HadoopSecurityGroup> securityGroups);

    /**
     * @param waterMarkName the name of the high water mark
     * @return an optional string value of the high water mark
     */
    Optional<String> getWaterMarkValue(String waterMarkName);

    /**
     * @return the set of existing high water mark names
     */
    Set<String> getWaterMarkNames();

    /**
     * @param waterMarkName the name of the high water mark
     * @param value         the current value of the water mark
     */
    void setWaterMarkValue(String waterMarkName, String value);

    Set<String> getTags();
    
    /**
     * Sets the tags for this nflow.
     *
     * @param tags set of tags
     */
    void setTags(@Nullable Set<String> tags);

    String getJson();

    void setJson(String json);

    NflowManagerTemplate getTemplate();

    void setTemplate(NflowManagerTemplate template);

    String getNifiProcessGroupId();

    void setNifiProcessGroupId(String nifiProcessGroupId);

    void setVersionName(String version);

    void clearSourcesAndDestinations();

    enum State {NEW, ENABLED, DISABLED, DELETED}

    interface ID extends Serializable {

    }
}
