package com.onescorpin.metadata.sla.api.core;

/*-
 * #%L
 * onescorpin-sla-metrics-default
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

import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementMetric;
import com.onescorpin.policy.PolicyProperty;
import com.onescorpin.policy.PolicyPropertyTypes;

/**
 * SLA metric used to notify if a nflow fails
 * This will be exposed to the User Interface since it is annotated with {@link ServiceLevelAgreementMetric}
 */
@ServiceLevelAgreementMetric(name = "Nflow Failure Notification",
                             description = "Act upon a Nflow Failure")
public class NflowFailedMetric implements Metric {

    @PolicyProperty(name = "NflowName",
                    type = PolicyPropertyTypes.PROPERTY_TYPE.nflowSelect,
                    required = true,
                    value = PolicyPropertyTypes.CURRENT_NFLOW_VALUE)
    private String nflowName;


    @Override
    public String getDescription() {
        StringBuilder bldr = new StringBuilder("Nflow Failure Notification:");
        bldr.append("\"").append(this.nflowName).append("\" ");
        return bldr.toString();
    }

    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }
}
