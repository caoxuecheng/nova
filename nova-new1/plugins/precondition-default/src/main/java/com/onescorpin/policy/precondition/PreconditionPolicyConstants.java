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

/**
 * Naming constants for the precondition used for the ui display of these rules
 */
public interface PreconditionPolicyConstants {

    String NFLOW_EXECUTED_SINCE_NFLOWS_NAME = "Dependent upon Nflow(s)";

    String NFLOW_EXECUTED_SINCE_NFLOWS_OR_TIME_NAME = "Dependent upon Nflow(s) or a Schedule";
}
