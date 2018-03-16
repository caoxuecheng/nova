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
import com.onescorpin.metadata.api.nflow.NflowCriteria;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.metadata.api.sla.NflowExecutedSinceSchedule;
import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder;
import com.onescorpin.scheduler.util.CronExpressionUtil;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class NflowExecutedSinceScheduleAssessor extends MetadataMetricAssessor<NflowExecutedSinceSchedule> {

    @Override
    public boolean accepts(Metric metric) {
        return metric instanceof NflowExecutedSinceSchedule;
    }

    @Override
    public void assess(NflowExecutedSinceSchedule metric, MetricAssessmentBuilder<Serializable> builder) {
        Date prev = CronExpressionUtil.getPreviousFireTime(metric.getCronExpression(), 2);
        DateTime schedTime = new DateTime(prev);
        String nflowName = metric.getNflowName();
        NflowCriteria crit = getNflowProvider().nflowCriteria().name(nflowName);
        List<Nflow> nflows = getNflowProvider().getNflows(crit);

        if (nflows.size() > 0) {
            Nflow nflow = nflows.get(0);
            List<NflowOperation> list = this.getNflowOperationsProvider().findLatestCompleted(nflow.getId());

            if (!list.isEmpty()) {
                NflowOperation latest = list.get(0);

                if (latest.getStopTime().isAfter(schedTime)) {
                    builder
                        .result(AssessmentResult.SUCCESS)
                        .message("Nflow " + nflow.getName() + " has executed at least 1 operation since " + schedTime);
                } else {
                    builder
                        .result(AssessmentResult.FAILURE)
                        .message("Nflow " + nflow.getName() + " has not executed any data operations since " + schedTime);
                }
            }
        }
    }

}
