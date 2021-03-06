/**
 *
 */
package com.onescorpin.metadata.core.dataset;

/*-
 * #%L
 * onescorpin-metadata-core
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
public class DatasourceException extends RuntimeException {

    private static final long serialVersionUID = 787147228293321761L;

    /**
     * @param message
     */
    public DatasourceException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public DatasourceException(String message, Throwable cause) {
        super(message, cause);
    }

}
