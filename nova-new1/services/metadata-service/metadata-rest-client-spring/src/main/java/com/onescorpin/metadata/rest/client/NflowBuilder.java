/**
 *
 */
package com.onescorpin.metadata.rest.client;

/*-
 * #%L
 * onescorpin-metadata-rest-client-spring
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

import com.onescorpin.metadata.rest.model.nflow.Nflow;
import com.onescorpin.metadata.sla.api.Metric;

/**
 * A builder for nflow models
 */
public interface NflowBuilder {

    NflowBuilder displayName(String name);

    NflowBuilder description(String descr);

    NflowBuilder owner(String owner);

    NflowBuilder preconditionMetric(Metric... metrics);

    NflowBuilder property(String key, String value);

    Nflow build();

    Nflow post();
}
