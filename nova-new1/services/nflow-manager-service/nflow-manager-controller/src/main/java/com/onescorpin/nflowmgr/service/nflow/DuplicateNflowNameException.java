/**
 *
 */
package com.onescorpin.nflowmgr.service.nflow;

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

/**
 * Thrown when there is an attempt to create a new nflow using an existing category and nflow combination.
 */
public class DuplicateNflowNameException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String categoryName;
    private final String nflowName;

    public DuplicateNflowNameException(String categoryName, String nflowName) {
        super("A nflow already exists in category \"" + categoryName + "\" with name \"" + nflowName + "\"");
        this.categoryName = categoryName;
        this.nflowName = nflowName;
    }

    /**
     * @return the nflowName
     */
    public String getNflowName() {
        return nflowName;
    }

    /**
     * @return the categoryName
     */
    public String getCategoryName() {
        return categoryName;
    }
}
