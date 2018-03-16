/**
 * 
 */
package com.onescorpin.server.upgrade;

/*-
 * #%L
 * nova-operational-metadata-upgrade-service
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

import java.net.URL;

import com.onescorpin.NovaVersion;

/**
 *
 */
public interface UpgradeState { 
    
    default boolean isTargetVersion(NovaVersion version) {
        return false;
    }
    
    default boolean isTargetFreshInstall() {
        return false;
    }

    void upgradeTo(NovaVersion startingVersion);

    default URL getResource(String name) {
        String relName = name.startsWith("/") ? name.substring(1, name.length()) : name;
        return getClass().getResource(relName);
    }

}
