package com.onescorpin.nflowmgr.rest.model;

/*-
 * #%L
 * onescorpin-nflow-manager-rest-model
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.onescorpin.nflowmgr.rest.support.SystemNamingService;
import com.onescorpin.metadata.MetadataField;
import com.onescorpin.security.rest.model.EntityAccessControl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * A category is a collection of zero or more nflows in the Nflow Manager.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NflowCategory extends EntityAccessControl {

    private String id;
    @MetadataField
    private String name;
    @MetadataField
    private String systemName;
    private String icon;
    private String iconColor;
    private String description;

    private List<HadoopSecurityGroup> securityGroups;

    /**
     * User-defined fields for nflows within this category
     */
    private Set<UserField> userFields;

    /**
     * User-defined business metadata
     */
    private Set<UserProperty> userProperties;

    @JsonIgnore
    private List<NflowSummary> nflows;

    private int relatedNflows;

    private Date createDate;
    private Date updateDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconColor() {
        return iconColor;
    }

    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the user-defined fields for nflows within this category.
     *
     * @return the user-defined fields
     * @see #setUserFields(Set)
     * @since 0.4.0
     */
    public Set<UserField> getUserFields() {
        return userFields;
    }

    /**
     * Sets the user-defined fields for nflows within this category.
     *
     * @param userFields the new user-defined fields
     * @see #setUserFields(Set)
     * @since 0.4.0
     */
    public void setUserFields(final Set<UserField> userFields) {
        this.userFields = userFields;
    }

    /**
     * Gets the user-defined business metadata for this category.
     *
     * @return the user-defined properties
     * @see #setUserProperties(Set)
     * @since 0.3.0
     */
    public Set<UserProperty> getUserProperties() {
        return userProperties;
    }

    /**
     * Sets the user-defined business metadata for this category.
     *
     * @param userProperties the user-defined properties
     * @see #getUserProperties()
     * @since 0.3.0
     */
    public void setUserProperties(final Set<UserProperty> userProperties) {
        this.userProperties = userProperties;
    }

    public List<NflowSummary> getNflows() {
        if (nflows == null) {
            nflows = new ArrayList<>();
        }
        return nflows;
    }

    public void setNflows(List<NflowSummary> nflows) {
        this.nflows = nflows;
    }

    @JsonIgnore
    public void removeRelatedNflow(final NflowMetadata nflow) {
        NflowSummary match = Iterables.tryFind(nflows, new Predicate<NflowSummary>() {
            @Override
            public boolean apply(NflowSummary metadata) {
                return nflow.getNflowName().equalsIgnoreCase(metadata.getNflowName());
            }
        }).orNull();
        if (match != null) {
            getNflows().remove(match);
        }
    }

    @JsonIgnore
    public void addRelatedNflow(final NflowSummary nflow) {
        if (nflows != null) {
            List<NflowSummary> arr = Lists.newArrayList(nflows);
            NflowSummary match = Iterables.tryFind(arr, new Predicate<NflowSummary>() {
                @Override
                public boolean apply(NflowSummary metadata) {
                    return nflow.getNflowName().equalsIgnoreCase(metadata.getNflowName());
                }
            }).orNull();
            if (match != null) {
                nflows.remove(match);
            }
        }
        getNflows().add(nflow);
        relatedNflows = getNflows().size();

    }

    public int getRelatedNflows() {
        return relatedNflows;
    }

    public void setRelatedNflows(int relatedNflows) {
        this.relatedNflows = relatedNflows;
    }

//    @JsonIgnore
//    public void generateSystemName() {
//        this.systemName = SystemNamingService.generateSystemName(systemName);
//    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public List<HadoopSecurityGroup> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(List<HadoopSecurityGroup> securityGroups) {
        this.securityGroups = securityGroups;
    }
}
