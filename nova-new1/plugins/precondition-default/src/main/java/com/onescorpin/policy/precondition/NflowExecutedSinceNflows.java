package com.onescorpin.policy.precondition;

/*-
 * #%L
 * onescorpin-precondition-default
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceNflow;
import com.onescorpin.metadata.rest.model.sla.Obligation;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.ObligationGroup;
import com.onescorpin.policy.PolicyProperty;
import com.onescorpin.policy.PolicyPropertyRef;
import com.onescorpin.policy.PolicyPropertyTypes;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Precondition to trigger a nflow when another nflow finishes
 */
@PreconditionPolicy(name = PreconditionPolicyConstants.NFLOW_EXECUTED_SINCE_NFLOWS_NAME, description = "Policy will trigger the nflow when all of the supplied nflows have successfully finished")
public class NflowExecutedSinceNflows implements Precondition, DependentNflowPrecondition {

    /**
     * the name of the current "trigger" nflow that should get executed when the {@link #categoryAndNflowList} signal they are complete
     */
    @PolicyProperty(name = "Since Nflow", type = PolicyPropertyTypes.PROPERTY_TYPE.currentNflow, hidden = true)
    private String sinceCategoryAndNflowName;

    /**
     * a comman separated list of nflows that this current nflow depends upon
     **/
    @PolicyProperty(name = "Dependent Nflows", required = true, type = PolicyPropertyTypes.PROPERTY_TYPE.nflowChips, placeholder = "Start typing a nflow",
                    hint = "Select nflow(s) that this nflow is dependent upon")
    private String categoryAndNflows;

    /**
     * a derived list created from the {@link #categoryAndNflows} string
     **/
    private List<String> categoryAndNflowList;

    public NflowExecutedSinceNflows(@PolicyPropertyRef(name = "Since Nflow") String sinceCategoryAndNflowName, @PolicyPropertyRef(name = "Dependent Nflows") String categoryAndNflows) {
        this.sinceCategoryAndNflowName = sinceCategoryAndNflowName;
        this.categoryAndNflows = categoryAndNflows;
        categoryAndNflowList = Arrays.asList(StringUtils.split(categoryAndNflows, ","));
    }

    public String getSinceCategoryAndNflowName() {
        return sinceCategoryAndNflowName;
    }

    public void setSinceCategoryAndNflowName(String sinceCategoryAndNflowName) {
        this.sinceCategoryAndNflowName = sinceCategoryAndNflowName;
    }

    public String getCategoryAndNflows() {
        return categoryAndNflows;
    }

    public void setCategoryAndNflows(String categoryAndNflows) {
        this.categoryAndNflows = categoryAndNflows;
    }

    public List<String> getCategoryAndNflowList() {
        return categoryAndNflowList;
    }

    public void setCategoryAndNflowList(List<String> categoryAndNflowList) {
        this.categoryAndNflowList = categoryAndNflowList;
    }

    @Override
    public List<String> getDependentNflowNames() {
        return categoryAndNflowList;
    }

    @Override
    public Set<com.onescorpin.metadata.rest.model.sla.ObligationGroup> buildPreconditionObligations() {
        return Sets.newHashSet(buildPreconditionObligation());
    }

    /**
     * Builds the ObligationGroup that holds the metric that will be used to assess if this precondition is met or not
     */
    public com.onescorpin.metadata.rest.model.sla.ObligationGroup buildPreconditionObligation() {
        Set<Metric> metrics = new HashSet<>();
        for (String categoryAndNflow : categoryAndNflowList) {
            NflowExecutedSinceNflow metric = new NflowExecutedSinceNflow(sinceCategoryAndNflowName, categoryAndNflow);
            metrics.add(metric);
        }
        Obligation obligation = new Obligation();
        obligation.setMetrics(Lists.newArrayList(metrics));
        com.onescorpin.metadata.rest.model.sla.ObligationGroup group = new com.onescorpin.metadata.rest.model.sla.ObligationGroup();
        group.addObligation(obligation);
        group.setCondition(ObligationGroup.Condition.REQUIRED.name());
        return group;
    }

}
