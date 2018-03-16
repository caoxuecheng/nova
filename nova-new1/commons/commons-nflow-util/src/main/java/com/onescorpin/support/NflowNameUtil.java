package com.onescorpin.support;

/*-
 * #%L
 * onescorpin-commons-nflow-util
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

/**
 * Utility to parse and return nflow names from a category and nflow string to ensure everything returns the same name.
 */
public class NflowNameUtil {


    /**
     * Parse the category name from a full nflow name  (category.nflow)
     */
    public static String category(String name) {
        return StringUtils.trim(StringUtils.substringBefore(name, "."));
    }

    /**
     * parse the nflow name from a full nflow name (category.nflow)
     */
    public static String nflow(String name) {
        return StringUtils.trim(StringUtils.substringAfterLast(name, "."));
    }

    /**
     * return the full nflow name from a category and nflow.
     *
     * @return returns the (category.nflow) name
     */
    public static String fullName(String category, String nflow) {
        return StringUtils.trim(category + "." + nflow);
    }


}
