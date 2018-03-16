/**
 *
 */
package com.onescorpin.metadata.core.sla;

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

import com.onescorpin.metadata.api.sla.WithinSchedule;
import com.onescorpin.metadata.core.sla.nflow.MetadataMetricAssessor;
import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder;
import com.onescorpin.scheduler.util.CronExpressionUtil;

import org.joda.time.DateTime;

import java.io.Serializable;

/**
 *
 */
public class WithinScheduleAssessor extends MetadataMetricAssessor<WithinSchedule> {

    @Override
    public boolean accepts(Metric metric) {
        return metric instanceof WithinSchedule;
    }

    @Override
    public void assess(WithinSchedule metric,
                       MetricAssessmentBuilder<Serializable> builder) {
        DateTime start = new DateTime(CronExpressionUtil.getPreviousFireTime(metric.getCronExpression()));
        DateTime end = start.withPeriodAdded(metric.getPeriod(), 1);

        builder.metric(metric);

        if (start.isBeforeNow() && end.isAfterNow()) {
            builder
                .result(AssessmentResult.SUCCESS)
                .message("Current time falls between the schedule " + start + " - " + end);
        } else {
            builder
                .result(AssessmentResult.FAILURE)
                .message("Current time does not falls between the schedule " + start + " - " + end);
        }
    }
}
