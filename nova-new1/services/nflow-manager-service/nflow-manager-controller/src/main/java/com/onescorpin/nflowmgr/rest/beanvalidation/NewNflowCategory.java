package com.onescorpin.nflowmgr.rest.beanvalidation;

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

import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.support.SystemNamingService;

import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Validate the properties are correct for a new nflow category
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NewNflowCategory.Validator.class)
public @interface NewNflowCategory {

    String message() default "The minimum required properties were not included for creation of a new category";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<NewNflowCategory, NflowCategory> {

        @Override
        public void initialize(final NewNflowCategory newNflowCategory) {
        }

        @Override
        public boolean isValid(final NflowCategory nflowCategory, final ConstraintValidatorContext constraintValidatorContext) {
            if (nflowCategory == null)
                return false;
            if (StringUtils.isEmpty(nflowCategory.getName()))
                return false;
            if (StringUtils.isEmpty(nflowCategory.getSystemName()))
                return false;

            //we must be receiving a valid system name
            return nflowCategory.getSystemName().equals(SystemNamingService.generateSystemName(nflowCategory.getSystemName()));
        }

    }
}
