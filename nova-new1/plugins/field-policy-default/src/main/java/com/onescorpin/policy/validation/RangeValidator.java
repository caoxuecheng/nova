package com.onescorpin.policy.validation;

/*-
 * #%L
 * onescorpin-field-policy-default
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


import com.onescorpin.policy.PolicyProperty;
import com.onescorpin.policy.PolicyPropertyRef;
import com.onescorpin.policy.PolicyPropertyTypes;

import org.apache.commons.lang3.Validate;

/**
 * validate the value is within  a given range (not including the min/max)
 */
@Validator(name = "Range", description = "Numeric must fall within range")
public class RangeValidator implements ValidationPolicy<Number> {

    @PolicyProperty(name = "Min", type = PolicyPropertyTypes.PROPERTY_TYPE.number, hint = "Minimum Value")
    private Double min;
    @PolicyProperty(name = "Max", type = PolicyPropertyTypes.PROPERTY_TYPE.number, hint = "Maximum Value")
    private Double max;

    public RangeValidator(@PolicyPropertyRef(name = "Min") Number min, @PolicyPropertyRef(name = "Max") Number max) {
        super();
        this.min = (min != null ? min.doubleValue() : null);
        this.max = (max != null ? max.doubleValue() : null);
        if (min != null && max != null) {
            Validate.isTrue(this.min <= this.max, "Minimum must smaller than Maximum");
        }
    }

    @Override
    public boolean validate(Number value) {
        if (value == null) {
            return true;
        }
        double dval = value.doubleValue();
        if (min != null) {
            if (dval < min) {
                return false;
            }
        }
        if (max != null) {
            if (dval > max) {
                return false;
            }
        }
        return true;
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }
}
