/**
 *
 */
package com.onescorpin.nifi.v2.common;

/*-
 * #%L
 * onescorpin-nifi-core-processors
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

import org.apache.nifi.processor.exception.ProcessException;

/**
 * Thrown when the lookup of the nflow ID fails.
 */
public class NflowIdNotFoundException extends ProcessException {

    private static final long serialVersionUID = 1L;

    private final String category;
    private final String nflowName;

    /**
     * @param message
     */
    public NflowIdNotFoundException(String message) {
        this(message, null, null);
    }

    /**
     * @param message
     */
    public NflowIdNotFoundException(String category, String name) {
        this("ID for nflow " + category + "/" + name + " could not be located", category, name);
    }

    /**
     * @param message
     */
    public NflowIdNotFoundException(String message, String category, String name) {
        super(message);
        this.category = category;
        this.nflowName = name;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return the nflowName
     */
    public String getNflowName() {
        return nflowName;
    }
}
