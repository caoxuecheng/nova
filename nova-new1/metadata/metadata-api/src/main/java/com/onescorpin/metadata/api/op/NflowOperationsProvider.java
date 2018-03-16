/**
 *
 */
package com.onescorpin.metadata.api.op;

/*-
 * #%L
 * onescorpin-metadata-api
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

import com.onescorpin.metadata.api.nflow.Nflow;

import java.util.List;
import java.util.Set;

/**
 *
 */
public interface NflowOperationsProvider {

    NflowOperationCriteria criteria();

    /**
     * Get the NflowOperation for the supplied id
     */
    NflowOperation getOperation(NflowOperation.ID id);

    //  List<NflowOperation> find(NflowOperationCriteria criteria);

    /**
     * Find the last Completed Nflow Operation for the {@code nflowId}
     */
    List<NflowOperation> findLatestCompleted(Nflow.ID nflowId);

    /**
     * Find the last Nflow Operation of any status for the {@code nflowId}
     */
    List<NflowOperation> findLatest(Nflow.ID nflowId);

    boolean isNflowRunning(Nflow.ID nflowId);

    // List<NflowOperation> find(Nflow.ID nflowId, int limit);

    /**
     * Get a listing of all the Dependent Job Executions and their associated executionContext data Map for the supplied {@code nflowId}
     *
     * @param nflowId the nflow that has dependents
     * @param props  filter to include only these property names from the respective job execution context.  null or empty set will return all data in the execution context
     */
    NflowDependencyDeltaResults getDependentDeltaResults(Nflow.ID nflowId, Set<String> props);

    //  Map<DateTime, Map<String, Object>> getAllResults(NflowOperationCriteria criteria, Set<String> props);
}
