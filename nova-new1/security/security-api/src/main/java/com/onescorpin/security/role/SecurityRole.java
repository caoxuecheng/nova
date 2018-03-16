package com.onescorpin.security.role;

/*-
 * #%L
 * nova-security-api
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

import com.onescorpin.security.action.Action;
import com.onescorpin.security.action.AllowedActions;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Defines a role used for representing a set of predefined permissions.
 */
public interface SecurityRole {

    // Entity names
    String SERVICES = "services";
    String NFLOW = "nflow";
    String CATEGORY = "category";
    String TEMPLATE = "template";
    String DATASOURCE = "datasource";
    List<String> ENTITIES = Collections.unmodifiableList(Arrays.asList(SERVICES, NFLOW, CATEGORY, TEMPLATE, DATASOURCE));

    enum ENTITY_TYPE {
        NFLOW,CATEGORY,DATASOURCE,TEMPLATE,SLA
    }


    Principal getPrincipal();

    String getSystemName();

    String getTitle();

    String getDescription();

    void setDescription(String description);

    AllowedActions getAllowedActions();

    void setPermissions(Action... actions);

    void setPermissions(Collection<Action> actions);
}
