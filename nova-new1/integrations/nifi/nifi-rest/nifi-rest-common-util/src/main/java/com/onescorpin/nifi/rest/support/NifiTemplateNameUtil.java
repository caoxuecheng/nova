package com.onescorpin.nifi.rest.support;

/*-
 * #%L
 * onescorpin-nifi-rest-common-util
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

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Utility class to support common naming of versioned process groups
 */
public class NifiTemplateNameUtil {

    /**
     * the regex pattern to match when looking for versioned process groups
     **/
    static String VERSION_NAME_REGEX = "(.*) - (\\d{13})";

    /**
     * Return the new name of the versioned process group
     *
     * @param name the process group name with out the version timestamp
     * @return the new name that has the new version timestamp
     */
    public static String getVersionedProcessGroupName(String name) {
        return name + " - " + new Date().getTime();
    }

    /**
     * Return the process group name, removing the versioned timestamp if one exists
     *
     * @param name a process group name
     * @return the process group name, removing the versioned timestamp if one exists
     */
    public static String parseVersionedProcessGroupName(String name) {
        if (isVersionedProcessGroup(name)) {
            return StringUtils.substringBefore(name, " - ");
        }
        return name;
    }

    /**
     * Check to see if the incoming name includes a versioned timestamp
     *
     * @param name the process group name
     * @return {@code true} if the incoming name contains the version timestamp, {@code false} if the name is not versioned.
     */
    public static boolean isVersionedProcessGroup(String name) {
        return StringUtils.isNotBlank(name) && name.matches(VERSION_NAME_REGEX);
    }
}
