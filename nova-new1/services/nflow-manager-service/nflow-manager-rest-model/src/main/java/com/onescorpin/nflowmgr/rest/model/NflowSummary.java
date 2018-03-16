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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.onescorpin.security.rest.model.EntityAccessControl;

import java.util.Date;

/**
 * Lightweight view of Nflow Data with just the essential nflow information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NflowSummary extends EntityAccessControl implements UINflow {

    private String categoryName;
    private String systemCategoryName;
    private String categoryId;
    private String categoryIcon;
    private String categoryIconColor;
    private String id;
    private String nflowId;
    private String nflowName;
    private String systemNflowName;
    private boolean active;
    private String state;
    private Date updateDate;
    private String templateName;
    private String templateId;

    public NflowSummary() {

    }

    public NflowSummary(NflowMetadata nflowMetadata) {
        this.id = nflowMetadata.getId();
        this.nflowName = nflowMetadata.getNflowName();
        this.categoryId = nflowMetadata.getCategory().getId();
        this.categoryName = nflowMetadata.getCategory().getName();
        this.systemCategoryName = nflowMetadata.getCategory().getSystemName();
        this.systemNflowName = nflowMetadata.getSystemNflowName();
        this.updateDate = nflowMetadata.getUpdateDate();
        this.nflowId = nflowMetadata.getNflowId();
        this.categoryIcon = nflowMetadata.getCategoryIcon();
        this.categoryIconColor = nflowMetadata.getCategoryIconColor();
        this.active = nflowMetadata.isActive();
        this.state = nflowMetadata.getState();
        this.templateId = nflowMetadata.getTemplateId();
        this.templateName = nflowMetadata.getTemplateName();
    }

    @Override
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    @Override
    public String getCategoryAndNflowDisplayName() {
        return this.categoryName + "." + this.nflowName;
    }

    public String getCategoryAndNflowSystemName() {
        return this.systemCategoryName + "." + this.systemNflowName;
    }


    @Override
    public String getSystemCategoryName() {
        return systemCategoryName;
    }

    public void setSystemCategoryName(String systemCategoryName) {
        this.systemCategoryName = systemCategoryName;
    }

    @Override
    public String getSystemNflowName() {
        return systemNflowName;
    }

    public void setSystemNflowName(String systemNflowName) {
        this.systemNflowName = systemNflowName;
    }

    @Override
    public String getNflowId() {
        return nflowId;
    }

    public void setNflowId(String nflowId) {
        this.nflowId = nflowId;
    }

    @Override
    public String getCategoryIcon() {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    @Override
    public String getCategoryIconColor() {
        return categoryIconColor;
    }

    public void setCategoryIconColor(String categoryIconColor) {
        this.categoryIconColor = categoryIconColor;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }
}
