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

import com.onescorpin.metadata.api.sla.DatasourceUpdatedSinceNflowExecuted;
import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder;

import java.io.Serializable;

/**
 *
 */
public class DatasourceUpdatedSinceNflowExecutedAssessor extends MetadataMetricAssessor<DatasourceUpdatedSinceNflowExecuted> {

    @Override
    public boolean accepts(Metric metric) {
        return metric instanceof DatasourceUpdatedSinceNflowExecuted;
    }

    @Override
    public void assess(DatasourceUpdatedSinceNflowExecuted metric,
                       MetricAssessmentBuilder<Serializable> builder) {
        builder
            .metric(metric)
            .message("This metric is no longer supported")
            .result(AssessmentResult.FAILURE);

//        NflowProvider fPvdr = getNflowProvider();
//        DatasourceProvider dsPvdr = getDatasetProvider();
//        DataOperationsProvider opPvdr = getDataOperationsProvider();
//        Collection<Nflow> nflows = fPvdr.getNflows(fPvdr.nflowCriteria().name(metric.getNflowName()));
//        List<Datasource> datasources = dsPvdr.getDatasources(dsPvdr.datasetCriteria().name(metric.getDatasourceName()).limit(1));
//        
//        builder.metric(metric);
//        
//        if (! nflows.isEmpty() && ! datasources.isEmpty()) {
//            Nflow nflow = nflows.iterator().next();
//            Datasource datasource = datasources.get(0);
//            List<DataOperation> nflowOps = opPvdr.getDataOperations(opPvdr.dataOperationCriteria()
//                    .nflow(nflow.getId())
//                    .state(State.SUCCESS));
//            List<DataOperation> datasourceOps = opPvdr.getDataOperations(opPvdr.dataOperationCriteria()
//                    .dataset(datasource.getId())
//                    .state(State.SUCCESS));
//            ArrayList<Dataset<Datasource, ChangeSet>> result = new ArrayList<>();
//        
//            // If the nflow we are checking has never run then it can't have run before the "since" nflow.
//            if (datasourceOps.isEmpty()) {
//                builder
//                    .result(AssessmentResult.FAILURE)
//                    .message("The dependent datasource has never been updated: " + datasource.getName());
//            } else {
//                DateTime datasourceTime = datasourceOps.iterator().next().getStopTime();
//
//                if (nflowOps.isEmpty()) {
//                    // If the datasource has been updated at least once and nflow has never executed then this condition is true.
//                    // Collects any datasource changes that have occurred since the nflow last ran.
//                    // Returns the highest found incompleteness factor.
//                    int incompleteness = collectChangeSetsSince(result, datasourceOps, new DateTime(1));
//                    
//                    builder
//                        .result(incompleteness > 0 ? AssessmentResult.WARNING : AssessmentResult.SUCCESS)
//                        .message("The datasource has updated yet the nflow has never been executed")
//                        .data(result);
//                } else {
//                    DateTime nflowTime = nflowOps.iterator().next().getStopTime();
//                    
//                    if (datasourceTime.isBefore(nflowTime)) {
//                        builder
//                            .result(AssessmentResult.FAILURE)
//                            .message("The datasource has not been updated since " + nflowTime);
//                    } else {
//                        // Collects any datasource changes that have occurred since the nflow last ran.
//                        // Returns the highest found incompleteness factor.
//                        int incompleteness = collectChangeSetsSince(result, datasourceOps, nflowTime);
//                        
//                        builder
//                            .result(incompleteness > 0 ? AssessmentResult.WARNING : AssessmentResult.SUCCESS)
//                            .message("There have been " + result.size() + " change sets produced since " + nflowTime)
//                            .data(result);
//                    }
//                }
//            }
//        }
    }
}
