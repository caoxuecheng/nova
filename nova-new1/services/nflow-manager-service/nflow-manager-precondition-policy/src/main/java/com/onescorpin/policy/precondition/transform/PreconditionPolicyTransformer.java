package com.onescorpin.policy.precondition.transform;

/*-
 * #%L
 * onescorpin-nflow-manager-precondition-policy
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
import com.onescorpin.metadata.rest.model.sla.ObligationGroup;
import com.onescorpin.policy.PolicyPropertyTypes;
import com.onescorpin.policy.PolicyTransformException;
import com.onescorpin.policy.precondition.Precondition;
import com.onescorpin.policy.rest.model.FieldRuleProperty;
import com.onescorpin.policy.rest.model.PreconditionRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Transformation class to assist in obtaining the transformed preconditions
 */
public class PreconditionPolicyTransformer {

    private List<PreconditionRule> preconditionRules;

    public PreconditionPolicyTransformer(List<PreconditionRule> preconditionRules) {
        this.preconditionRules = preconditionRules;
    }

    /**
     * applies the nflow names back to the precondition properties
     *
     * @param category the system category name
     * @param nflow     the system nflow name
     */
    public void applyNflowNameToCurrentNflowProperties(String category, String nflow) {
        if (this.preconditionRules != null) {
            List<FieldRuleProperty>
                properties =
                PreconditionAnnotationTransformer.instance().findPropertiesForRulesetMatchingRenderType(preconditionRules, PolicyPropertyTypes.PROPERTY_TYPE.currentNflow.name());
            if (properties != null && !properties.isEmpty()) {
                for (FieldRuleProperty property : properties) {
                    property.setValue(category + "." + nflow);
                }
            }
        }
    }

    public List<Precondition> getPreconditionPolicies() {
        List<Precondition> policies = new ArrayList<>();
        if (preconditionRules != null) {
            for (PreconditionRule rule : preconditionRules) {
                try {
                    Precondition policy = PreconditionAnnotationTransformer.instance().fromUiModel(rule);
                    policies.add(policy);
                } catch (PolicyTransformException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return policies;

    }

    public List<ObligationGroup> getPreconditionObligationGroups() {
        List<ObligationGroup> policies = new ArrayList<>();
        if (preconditionRules != null) {
            for (PreconditionRule rule : preconditionRules) {
                try {
                    Precondition policy = PreconditionAnnotationTransformer.instance().fromUiModel(rule);
                    policies.addAll(Lists.newArrayList(policy.buildPreconditionObligations()));
                } catch (PolicyTransformException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return policies;
    }

}
