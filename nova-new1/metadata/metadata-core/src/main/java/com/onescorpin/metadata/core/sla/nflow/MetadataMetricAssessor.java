/**
 *
 */
package com.onescorpin.metadata.core.sla.nflow;

/*-
 * #%L
 * onescorpin-metadata-core
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

import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.op.NflowOperationsProvider;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.spi.MetricAssessor;

import java.io.Serializable;

import javax.inject.Inject;

/**
 *
 */
public abstract class MetadataMetricAssessor<M extends Metric>
    implements MetricAssessor<M, Serializable> {

    @Inject
    private NflowProvider nflowProvider;

    @Inject
    private NflowOperationsProvider operationsProvider;

    protected NflowProvider getNflowProvider() {
        return this.nflowProvider;
    }

    protected NflowOperationsProvider getNflowOperationsProvider() {
        return this.operationsProvider;
    }
}
