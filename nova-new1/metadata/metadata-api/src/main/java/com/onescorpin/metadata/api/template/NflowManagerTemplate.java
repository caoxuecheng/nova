package com.onescorpin.metadata.api.template;

/*-
 * #%L
 * onescorpin-metadata-api
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

import java.io.Serializable;
import java.util.List;

import org.joda.time.DateTime;

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.security.AccessControlled;

/**
 */
public interface NflowManagerTemplate extends AccessControlled {

    List<Nflow> getNflows();

    boolean addNflow(Nflow nflow);

    boolean removeNflow(Nflow nflow);

    ID getId();

    String getName();

    void setName(String name);

    String getNifiTemplateId();

    void setNifiTemplateId(String nifiTemplateId);

    String getDescription();

    void setDescription(String description);

    boolean isDefineTable();

    void setDefineTable(boolean defineTable);

    boolean isDataTransformation();

    void setDataTransformation(boolean dataTransformation);

    boolean isAllowPreconditions();

    void setAllowPreconditions(boolean allowedPreconditions);

    String getIcon();

    void setIcon(String icon);

    String getIconColor();

    void setIconColor(String iconColor);

    String getJson();

    void setJson(String json);

    DateTime getCreatedTime();

    DateTime getModifiedTime();

    State getState();

    void setState(State state);

    Long getOrder();

    void setOrder(Long order);

    boolean isStream();

    void setStream(boolean stream);

    String getTemplateTableOption();

    void setTemplateTableOption(String templateTableOption);

    enum State {
        ENABLED, DISABLED
    }

    interface ID extends Serializable {

    }

}
