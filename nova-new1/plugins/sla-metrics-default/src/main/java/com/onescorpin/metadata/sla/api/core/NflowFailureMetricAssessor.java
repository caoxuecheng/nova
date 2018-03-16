package com.onescorpin.metadata.sla.api.core;

/*-
 * #%L
 * onescorpin-sla-metrics-default
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

import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder;
import com.onescorpin.metadata.sla.spi.MetricAssessor;

import org.joda.time.DateTime;

import java.io.Serializable;

import javax.inject.Inject;

/**
 * SLA assessor used to asses the {@link NflowFailedMetric} and violate the SLA if the nflow fails
 */
public class NflowFailureMetricAssessor implements MetricAssessor<NflowFailedMetric, Serializable> {

    @Inject
    private NflowFailureService nflowFailureService;

    @Override
    public boolean accepts(Metric metric) {
        return metric instanceof NflowFailedMetric;
    }

    @Override
    public void assess(NflowFailedMetric metric, MetricAssessmentBuilder<Serializable> builder) {
        builder.metric(metric);

        String nflowName = metric.getNflowName();

        NflowFailureService.LastNflowJob lastNflowJob = nflowFailureService.findLastJob(nflowName);
        if(lastNflowJob == null){
            String msg = "Nflow " + nflowName + " is does not exist.";
            builder.message(msg).result(AssessmentResult.WARNING);
        }
        else if(!lastNflowJob.equals(NflowFailureService.EMPTY_JOB)){
            DateTime lastTime = lastNflowJob.getDateTime();

            //compare with the latest nflow time, alerts with same timestamps will not be raised
            builder.compareWith(nflowName, lastTime.getMillis());

            if (nflowFailureService.isExistingFailure(lastNflowJob)) {
                String msg = "Nflow " + nflowName + " is still failed.  The last job failed at " + lastNflowJob.getDateTime();
                builder.message(msg).result(AssessmentResult.WARNING);
            } else if (lastNflowJob.isFailure()) {
                builder.message("Nflow " + nflowName + " has failed ").result(AssessmentResult.FAILURE);
            } else {
                builder.message("Nflow " + nflowName + " has succeeded ").result(AssessmentResult.SUCCESS);
            }
        }
    }
}
