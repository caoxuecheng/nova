package com.onescorpin.nifi.core.api.cleanup;

/*-
 * #%L
 * onescorpin-nifi-core-service-api
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

import com.onescorpin.metadata.rest.model.event.NflowCleanupTriggerEvent;

import javax.annotation.Nonnull;

/**
 * Listens for cleanup events.
 */
public interface CleanupListener {

    /**
     * Processes the specified cleanup event.
     *
     * @param event the cleanup event
     */
    void triggered(@Nonnull NflowCleanupTriggerEvent event);
}
