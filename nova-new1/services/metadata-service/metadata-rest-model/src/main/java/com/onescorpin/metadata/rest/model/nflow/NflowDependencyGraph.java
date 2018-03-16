/**
 *
 */
package com.onescorpin.metadata.rest.model.nflow;

/*-
 * #%L
 * onescorpin-metadata-rest-model
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.onescorpin.metadata.rest.model.sla.ServiceLevelAssessment;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"nflow", "preconditonResult", "dependencies"})
public class NflowDependencyGraph {

    private Nflow nflow;
    private ServiceLevelAssessment preconditonResult;
    private List<NflowDependencyGraph> dependencies = new ArrayList<>();

    public NflowDependencyGraph() {
    }

    public NflowDependencyGraph(Nflow nflow, ServiceLevelAssessment preconditonResult) {
        super();
        this.nflow = nflow;
        this.preconditonResult = preconditonResult;
    }

    public Nflow getNflow() {
        return nflow;
    }

    public void setNflow(Nflow nflow) {
        this.nflow = nflow;
    }

    public ServiceLevelAssessment getPreconditonResult() {
        return preconditonResult;
    }

    public void setPreconditonResult(ServiceLevelAssessment preconditonResult) {
        this.preconditonResult = preconditonResult;
    }

    public List<NflowDependencyGraph> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<NflowDependencyGraph> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependecy(NflowDependencyGraph dep) {
        this.dependencies.add(dep);
    }
}
