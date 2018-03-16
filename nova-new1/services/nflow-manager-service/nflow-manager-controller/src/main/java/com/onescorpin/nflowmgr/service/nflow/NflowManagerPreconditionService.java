package com.onescorpin.nflowmgr.service.nflow;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.onescorpin.policy.PolicyPropertyTypes;
import com.onescorpin.policy.precondition.PreconditionPolicyRuleCache;
import com.onescorpin.policy.precondition.transform.PreconditionAnnotationTransformer;
import com.onescorpin.policy.rest.model.PreconditionRule;

import java.util.List;

import javax.inject.Inject;

/**
 *
 */
public class NflowManagerPreconditionService {

    @Inject
    NflowManagerNflowService nflowManagerNflowService;

    @Inject
    PreconditionPolicyRuleCache preconditionPolicyRuleCache;

    public List<PreconditionRule> getPossiblePreconditions() {
        List<PreconditionRule> rules = preconditionPolicyRuleCache.getPreconditionRules();
        //find and attach Nflow Lookup list to those that are of that type

        nflowManagerNflowService
            .applyNflowSelectOptions(
                PreconditionAnnotationTransformer.instance()
                    .findPropertiesForRulesetMatchingRenderTypes(rules, new String[]{PolicyPropertyTypes.PROPERTY_TYPE.nflowChips.name(),
                                                                                     PolicyPropertyTypes.PROPERTY_TYPE.nflowSelect.name(),
                                                                                     PolicyPropertyTypes.PROPERTY_TYPE.currentNflow.name()}));
        return rules;
    }

}
