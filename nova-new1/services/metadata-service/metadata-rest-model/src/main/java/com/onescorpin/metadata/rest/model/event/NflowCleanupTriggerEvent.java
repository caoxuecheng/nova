package com.onescorpin.metadata.rest.model.event;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

import javax.annotation.Nonnull;

/**
 * An event that triggers the cleanup of a nflow.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NflowCleanupTriggerEvent implements Serializable {

    private static final long serialVersionUID = 1012854550068482906L;

    /**
     * Category system name
     */
    private String categoryName;

    /**
     * Nflow id
     */
    private String nflowId;

    /**
     * Nflow system name
     */
    private String nflowName;

    /**
     * Constructs a {@code NflowCleanupTriggerEvent}.
     */
    public NflowCleanupTriggerEvent() {
    }

    /**
     * Constructs a {@code NflowCleanupTriggerEvent} with the specified nflow id.
     *
     * @param id the nflow id
     */
    public NflowCleanupTriggerEvent(@Nonnull final String id) {
        this.nflowId = id;
    }

    /**
     * Gets the target nflow id.
     *
     * @return the nflow id
     */
    public String getNflowId() {
        return nflowId;
    }

    /**
     * Sets the target nflow id.
     *
     * @param nflowId the nflow id
     */
    public void setNflowId(@Nonnull final String nflowId) {
        this.nflowId = nflowId;
    }

    /**
     * Gets the target nflow name.
     *
     * @return the nflow system name
     */
    public String getNflowName() {
        return nflowName;
    }

    /**
     * Sets the target nflow name.
     *
     * @param nflowName the nflow system name
     */
    public void setNflowName(@Nonnull final String nflowName) {
        this.nflowName = nflowName;
    }

    /**
     * Gets the target category name.
     *
     * @return the category system name
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Sets the target category name.
     *
     * @param categoryName the category system name
     */
    public void setCategoryName(@Nonnull final String categoryName) {
        this.categoryName = categoryName;
    }

    @Nonnull
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + (nflowId != null ? nflowId : categoryName + "." + nflowName);
    }
}
