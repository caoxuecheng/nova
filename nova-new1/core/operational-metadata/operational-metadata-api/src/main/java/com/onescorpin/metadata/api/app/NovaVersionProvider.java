package com.onescorpin.metadata.api.app;

/*-
 * #%L
 * onescorpin-operational-metadata-api
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

import com.onescorpin.NovaVersion;

/**
 * Provider to return/update metadata representing the current Nova version deployed
 */
public interface NovaVersionProvider {
    
    /**
     * @return true if the current version is equal to the latest version
     */
    boolean isUpToDate();

    /**
     * Return the current Nova version,
     *
     * @return the current nova version deployed
     */
    NovaVersion getCurrentVersion();
    
    /**
     * Sets a new Nova current version.
     * @param version the new version
     */
    void setCurrentVersion(NovaVersion version);
    
    /**
     * @return the version of the deployed Nova build
     */
    NovaVersion getBuildVersion();
}
