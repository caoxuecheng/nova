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

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.metadata.api.op.NflowOperationsProvider;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceNflow;
import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 *
 */
@SuppressWarnings("Duplicates")
public class NflowExecutedSinceNflowAssessor extends MetadataMetricAssessor<NflowExecutedSinceNflow> {

    private static final Logger LOG = LoggerFactory.getLogger(NflowExecutedSinceNflowAssessor.class);


    @Override
    public boolean accepts(Metric metric) {
        return metric instanceof NflowExecutedSinceNflow;
    }

    @Override
    public void assess(NflowExecutedSinceNflow metric, MetricAssessmentBuilder<Serializable> builder) {
        LOG.debug("Assessing metric {}", metric.getDescription());

        NflowProvider nflowProvider = getNflowProvider();
        NflowOperationsProvider opsProvider = getNflowOperationsProvider();
        List<Nflow> mainNflows = nflowProvider.getNflows(nflowProvider.nflowCriteria().name(metric.getNflowName()).category(metric.getCategoryName()));
        LOG.debug("Main nflows {}", mainNflows);
        List<Nflow> triggeredNflows = nflowProvider.getNflows(nflowProvider.nflowCriteria().name(metric.getSinceNflowName()).category(metric.getSinceCategoryName()));
        LOG.debug("Triggered nflows {}", triggeredNflows);

        builder.metric(metric);

        if (!mainNflows.isEmpty() && !triggeredNflows.isEmpty()) {
            Nflow mainNflow = mainNflows.get(0);
            Nflow triggeredNflow = triggeredNflows.get(0);
            List<NflowOperation> mainNflowOps = opsProvider.findLatestCompleted(mainNflow.getId());
            List<NflowOperation> triggeredNflowOps = opsProvider.findLatest(triggeredNflow.getId());

            if (mainNflowOps.isEmpty()) {
                // If the nflow we are checking has never run then it can't have run before the "since" nflow.
                LOG.debug("Main nflow ops is empty");
                builder
                    .result(AssessmentResult.FAILURE)
                    .message("Main nflow " + mainNflow.getName() + " has never executed.");
            } else {
                // If the "since" nflow has never run then the tested nflow has run before it.
                if (triggeredNflowOps.isEmpty()) {
                    LOG.debug("Triggered nflow ops is empty");
                    builder
                        .result(AssessmentResult.SUCCESS)
                        .message("Triggered nflow " + triggeredNflow.getName() + " has never executed");
                } else {

                    DateTime mainNflowStopTime = mainNflowOps.get(0).getStopTime();
                    DateTime triggeredNflowStartTime = triggeredNflowOps.get(0).getStartTime();
                    LOG.debug("Main nflow stop time {}", mainNflowStopTime);
                    LOG.debug("Triggered nflow start time {}", triggeredNflowStartTime);

                    if (mainNflowStopTime.isBefore(triggeredNflowStartTime)) {
                        LOG.debug("Main nflow stop time is before triggered nflow start time");
                        builder
                            .result(AssessmentResult.FAILURE)
                            .message("Main nflow " + mainNflow.getName() + " has not executed since triggered nflow "
                                     + triggeredNflow.getName() + ": " + triggeredNflowStartTime);
                    } else {
                        LOG.debug("Main nflow stop time is after triggered nflow start time");
                        boolean isMainNflowRunning = opsProvider.isNflowRunning(mainNflow.getId());
                        if (isMainNflowRunning) {
                            //todo whether to trigger the nflow while the other one is already running should be a
                            // configuration parameter defined by the user
                            LOG.debug("Main nflow is still running");
                            builder
                                .result(AssessmentResult.SUCCESS)
                                .message(
                                    "Triggered nflow " + triggeredNflow.getName() + " has executed since nflow " + mainNflow.getName() + ", but main nflow " + mainNflow.getName() + " is still running");
                        } else {
                            LOG.debug("Main is not running");
                            builder
                                .result(AssessmentResult.SUCCESS)
                                .message("Triggered nflow " + triggeredNflow.getName() + " has executed since main nflow " + mainNflow.getName() + ".");
                        }
                    }
                }
            }
        } else {
            LOG.debug("Either triggered or main nflow does not exist");
            builder
                .result(AssessmentResult.FAILURE)
                .message("Either nflow " + metric.getSinceCategoryAndNflowName() + " and/or nflow " + metric.getSinceCategoryAndNflowName() + " does not exist.");
        }
    }
}
