package com.onescorpin.metadata.sla.api;

/*-
 * #%L
 * onescorpin-sla-api
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

import java.io.Serializable;

/**
 * Defines a base type of any metric that must be satisfied as part of an obligation in an SLA.
 */
public interface Metric extends Serializable {

    /**
     * @return a description of the metric
     */
    String getDescription();

}
