package com.onescorpin.nflowmgr.rest.model;

/*-
 * #%L
 * onescorpin-nflow-manager-rest-model
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.onescorpin.nifi.rest.model.NifiProcessorSchedule;
import com.onescorpin.policy.rest.model.PreconditionRule;

import java.util.List;

/**
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NflowSchedule extends NifiProcessorSchedule {

    List<PreconditionRule> preconditions;


    public List<PreconditionRule> getPreconditions() {
        return preconditions;
    }

    public void setPreconditions(List<PreconditionRule> preconditions) {
        this.preconditions = preconditions;
    }


    public boolean hasPreconditions() {
        return this.preconditions != null && !this.preconditions.isEmpty();
    }
}
