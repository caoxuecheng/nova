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


import com.onescorpin.policy.BasePolicyAnnotationTransformer;
import com.onescorpin.policy.precondition.Precondition;
import com.onescorpin.policy.precondition.PreconditionPolicy;
import com.onescorpin.policy.rest.model.FieldRuleProperty;
import com.onescorpin.policy.rest.model.PreconditionRule;
import com.onescorpin.policy.rest.model.PreconditionRuleBuilder;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Transform a user interface {@link PreconditionRule} to/from the domain {@link com.onescorpin.policy.precondition.Precondition}
 * Domain classes need to be annotated with the {@link PreconditionPolicy} annotation
 */
public class PreconditionAnnotationTransformer
    extends BasePolicyAnnotationTransformer<PreconditionRule, Precondition, PreconditionPolicy> implements PreconditionTransformer {

    private static final PreconditionAnnotationTransformer instance = new PreconditionAnnotationTransformer();

    public static PreconditionAnnotationTransformer instance() {
        return instance;
    }

    @Override
    public PreconditionRule buildUiModel(PreconditionPolicy annotation, Precondition policy,
                                         List<FieldRuleProperty> properties) {
        String desc = annotation.description();
        String shortDesc = annotation.shortDescription();
        if (StringUtils.isBlank(desc) && StringUtils.isNotBlank(shortDesc)) {
            desc = shortDesc;
        }
        if (StringUtils.isBlank(shortDesc) && StringUtils.isNotBlank(desc)) {
            shortDesc = desc;
        }

        PreconditionRule
            rule =
            new PreconditionRuleBuilder(annotation.name()).objectClassType(policy.getClass()).description(
                desc).shortDescription(shortDesc).addProperties(properties).build();
        return rule;
    }

    @Override
    public Class<PreconditionPolicy> getAnnotationClass() {
        return PreconditionPolicy.class;
    }
}
