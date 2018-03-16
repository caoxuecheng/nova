/**
 *
 */
package com.onescorpin.metadata.rest.model.nflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.onescorpin.security.rest.model.ActionGroup;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/*-
 * #%L
 * onescorpin-metadata-rest-model
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

/**
 *
 */
@SuppressWarnings("serial")
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nflow implements Serializable {

    private String id;
    private String systemName;
    private String displayName;
    private String description;
    private String owner;
    private State state;
    private DateTime createdTime;
    // TODO versions
    private NflowPrecondition precondition;
    private Set<NflowSource> sources = new HashSet<>();
    private Set<NflowDestination> destinations = new HashSet<>();
    private Properties properties = new Properties();
    private NflowCategory category;
    private InitializationStatus currentInitStatus;
    /**
     * Nflows that this nflow is dependent upon  (parents)
     */
    private Set<Nflow> dependentNflows;
    private Set<String> dependentNflowIds;
    /**
     * Nflows that depend upon this nflow (children)
     */
    private Set<Nflow> usedByNflows;
    private Set<String> usedByNflowIds;
    private ActionGroup allowedActions;

    /**
     * Last modified time
     */
    private DateTime modifiedTime;
    private Map<String, String> userProperties;

    public Nflow() {
        super();
    }

    @JsonProperty
    public String getCollectedUserProperties() {
        return userProperties != null ? userProperties.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(",")) : "";
    }

    public ActionGroup getAllowedActions() {
        return allowedActions;
    }
    
    public void setAllowedActions(ActionGroup allowedActions) {
        this.allowedActions = allowedActions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public NflowPrecondition getPrecondition() {
        return precondition;
    }

    public void setPrecondition(NflowPrecondition trigger) {
        this.precondition = trigger;
    }

    public Set<NflowSource> getSources() {
        return sources;
    }

    public void setSources(Set<NflowSource> sources) {
        this.sources = sources;
    }

    public Set<NflowDestination> getDestinations() {
        return destinations;
    }

    public void setDestinations(Set<NflowDestination> destinations) {
        this.destinations = destinations;
    }

    public NflowDestination getDestination(String datasourceId) {
        for (NflowDestination dest : this.destinations) {
            if (datasourceId.equals(dest.getDatasourceId()) ||
                dest.getDatasource() != null && datasourceId.equals(dest.getDatasource().getId())) {
                return dest;
            }
        }

        return null;
    }

    public NflowCategory getCategory() {
        return category;
    }

    public void setCategory(NflowCategory category) {
        this.category = category;
    }

    public InitializationStatus getCurrentInitStatus() {
        return currentInitStatus;
    }

    public void setCurrentInitStatus(InitializationStatus currentInitStatus) {
        this.currentInitStatus = currentInitStatus;
    }

    public Set<Nflow> getDependentNflows() {
        if (dependentNflows == null) {
            dependentNflows = new HashSet<>();
        }
        return dependentNflows;
    }

    public void setDependentNflows(Set<Nflow> dependentNflows) {
        this.dependentNflows = dependentNflows;
        //mark the inverse relationship
        if (dependentNflows != null) {
            dependentNflows.stream().forEach(dependentNflow -> {
                dependentNflow.getUsedByNflows().add(this);
            });
        }
    }

    public Set<Nflow> getUsedByNflows() {
        if (usedByNflows == null) {
            usedByNflows = new HashSet<>();
        }
        return usedByNflows;
    }

    public Set<String> getDependentNflowIds() {
        return dependentNflowIds;
    }

    public void setDependentNflowIds(Set<String> dependentNflowIds) {
        this.dependentNflowIds = dependentNflowIds;
    }

    public Set<String> getUsedByNflowIds() {
        return usedByNflowIds;
    }

    public void setUsedByNflowIds(Set<String> usedByNflowIds) {
        this.usedByNflowIds = usedByNflowIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Nflow nflow = (Nflow) o;

        if (id != null ? !id.equals(nflow.id) : nflow.id != null) {
            return false;
        }
        return !(systemName != null ? !systemName.equals(nflow.systemName) : nflow.systemName != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (systemName != null ? systemName.hashCode() : 0);
        return result;
    }

    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    public void setUserProperties(Map<String, String> userProperties) {
        this.userProperties = userProperties;
    }

    public enum State {ENABLED, DISABLED, DELETED}

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }
}
