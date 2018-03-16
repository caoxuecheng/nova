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


/**
 * Validates email address
 */
@Validator(name = "Email", description = "Valid email address")
public class EmailValidator extends RegexValidator implements ValidationPolicy<String> {

    private static final EmailValidator instance = new EmailValidator();

    private EmailValidator() {
        super(
            "^([\\w\\d\\-\\.]+)@{1}(([\\w\\d\\-]{1,67})|([\\w\\d\\-]+\\.[\\w\\d\\-]{1,67}))\\.(([a-zA-Z\\d]{2,4})(\\.[a-zA-Z\\d]{2})?)$");
    }

    public static EmailValidator instance() {
        return instance;
    }

}