package com.onescorpin.policy;

/*-
 * #%L
 * onescorpin-field-policy-core
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

import com.onescorpin.policy.rest.model.BaseUiPolicyRule;
import com.onescorpin.policy.rest.model.FieldStandardizationRule;
import com.onescorpin.policy.rest.model.FieldValidationRule;
import com.onescorpin.policy.standardization.StandardizationPolicy;
import com.onescorpin.policy.validation.ValidationPolicy;
import com.onescorpin.standardization.transform.StandardizationAnnotationTransformer;
import com.onescorpin.validation.transform.ValidatorAnnotationTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
/**
 * Transform a User Interface policy to the domain object
 */
public class FieldPolicyTransformer {

    private com.onescorpin.policy.rest.model.FieldPolicy uiFieldPolicy;
    private FieldPolicyTransformerListener listener;

    public FieldPolicyTransformer(com.onescorpin.policy.rest.model.FieldPolicy uiFieldPolicy) {
        this.uiFieldPolicy = uiFieldPolicy;
    }

    public void setListener(FieldPolicyTransformerListener listener) {
        this.listener = listener;
    }



    public List<BaseFieldPolicy> getStandardizationAndValidationPolicies() {
        List<BaseFieldPolicy> policies = new ArrayList<>();
        List<FieldStandardizationRule> standardization = uiFieldPolicy.getStandardization();
        List<FieldValidationRule> validation = uiFieldPolicy.getValidation();
        List<BaseUiPolicyRule> allUiPolicies = new ArrayList<>();

        if(standardization != null){
            allUiPolicies.addAll(standardization);
        }
        if(validation != null){
            allUiPolicies.addAll(validation);
        }

        //ensure the sequence is set
        int idx = 0;
        for(BaseUiPolicyRule rule : allUiPolicies){
            if(rule.getSequence() == null){
                rule.setSequence(idx);
            }
            idx++;
        }

        Collections.sort(allUiPolicies, new Comparator<BaseUiPolicyRule>() {
            @Override
            public int compare(BaseUiPolicyRule o1, BaseUiPolicyRule o2) {
                if(o1 == null && o2 == null ){
                    return 0;
                }
                if(o1 == null && o2 != null){
                    return 1;
                }
                if(o1 != null && o2 == null){
                    return -1;
                }
                Integer sq1 = o1.getSequence();
                Integer sq2 = o2.getSequence();
               return sq1.compareTo(sq2);
            }
        });


        if (allUiPolicies != null) {
            for (BaseUiPolicyRule rule : allUiPolicies) {
                try {
                    if(rule instanceof FieldStandardizationRule){
                        StandardizationPolicy policy = StandardizationAnnotationTransformer.instance().fromUiModel((FieldStandardizationRule)rule);
                        policies.add(policy);
                        if (listener != null) {
                            listener.onAddStandardizationPolicy(policy);
                        }
                    }
                    else if(rule instanceof FieldValidationRule) {
                        ValidationPolicy policy = ValidatorAnnotationTransformer.instance().fromUiModel((FieldValidationRule)rule);
                        policies.add(policy);
                        if (listener != null) {
                            listener.onAddValidationPolicy(policy);
                        }
                    }




                } catch (PolicyTransformException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return policies;
    }


    /**
     * Build the domain level policies attached to the field holding both the Standardization and Validation domain objects transformed from the user interface object
     */
    public FieldPolicy buildPolicy() {
        return FieldPolicyBuilder.newBuilder().fieldName(uiFieldPolicy.getFieldName()).nflowFieldName(uiFieldPolicy.getNflowFieldName()).setProfile(uiFieldPolicy.isProfile()).addPolicies(getStandardizationAndValidationPolicies())
           .setPartitionColumn(uiFieldPolicy.isPartitionColumn()).build();

    }

}
