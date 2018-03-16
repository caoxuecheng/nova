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

/**
 * Thrown to indicate that a nflow's cleanup flow failed.
 */
public class NflowCleanupFailedException extends RuntimeException {

    private static final long serialVersionUID = -85111900673520561L;

    /**
     * Constructs a {@code NflowCleanupFailedException} with the specified detail message.
     *
     * @param message the detail message
     */
    public NflowCleanupFailedException(final String message) {
        super(message);
    }
}
