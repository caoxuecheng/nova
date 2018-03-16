/**
 *
 */
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

import com.onescorpin.DateTimeUtil;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.sla.api.AssessmentResult;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder;
import com.onescorpin.metadata.sla.spi.MetricAssessor;
import com.onescorpin.scheduler.util.CronExpressionUtil;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;

import javax.inject.Inject;

/**
 * Metric assessor to assess the {@link NflowOnTimeArrivalMetric}
 */
public class NflowOnTimeArrivalMetricAssessor implements MetricAssessor<NflowOnTimeArrivalMetric, Serializable> {

    private static final Logger LOG = LoggerFactory.getLogger(NflowOnTimeArrivalMetricAssessor.class);

    @Inject
    private OpsManagerNflowProvider nflowProvider;

    @Inject
    private MetadataAccess metadataAccess;


    /* (non-Javadoc)
     * @see com.onescorpin.metadata.sla.spi.MetricAssessor#accepts(com.onescorpin.metadata.sla.api.Metric)
     */
    @Override
    public boolean accepts(Metric metric) {
        return metric instanceof NflowOnTimeArrivalMetric;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.sla.spi.MetricAssessor#assess(com.onescorpin.metadata.sla.api.Metric, com.onescorpin.metadata.sla.spi.MetricAssessmentBuilder)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void assess(NflowOnTimeArrivalMetric metric, MetricAssessmentBuilder builder) {
        LOG.debug("Assessing metric: ", metric);

        builder.metric(metric);

        String nflowName = metric.getNflowName();
        DateTime lastNflowTime = nflowProvider.getLastActiveTimeStamp(nflowName);

        Long nowDiff = 0L;
        Period nowDiffPeriod = new Period(nowDiff.longValue());

        if(lastNflowTime != null) {
            nowDiff =   DateTime.now().getMillis() - lastNflowTime.getMillis();
            nowDiffPeriod = new Period(nowDiff.longValue());
        }
        Long latePeriodMillis = metric.getLatePeriod().toStandardDuration().getMillis();
        Long duration = CronExpressionUtil.getCronInterval(metric.getExpectedExpression());
        Period acceptedPeriod = new Period(duration + latePeriodMillis);
        Date expectedDate = CronExpressionUtil.getPreviousFireTime(metric.getExpectedExpression());
        DateTime expectedTime = new DateTime(expectedDate);
        LOG.debug("Calculated the Expected Date to be {}  ", expectedTime);
        DateTime lateTime = expectedTime.plus(metric.getLatePeriod());
        LOG.debug("CurrentTime is: {}.  Comparing {} against the lateTime of {} ", DateTime.now(), lastNflowTime, lateTime);
        builder.compareWith(expectedDate, nflowName);

        if (lastNflowTime == null) {
            LOG.debug("Nflow with the specified name {} not found", nflowName);
            builder.message("Nflow with the specified name " + nflowName + " not found ")
                .result(AssessmentResult.WARNING);
        } else if (lastNflowTime.isAfter(expectedTime) && lastNflowTime.isBefore(lateTime)) {
            LOG.debug("Data for nflow {} arrived on {}, which was before late time: {}", nflowName, lastNflowTime, lateTime);

            builder.message("Data for nflow " + nflowName + " arrived on " + lastNflowTime + ", which was before late time:  " + lateTime)
                .result(AssessmentResult.SUCCESS);
        }
        else if(lastNflowTime.isAfter(lateTime)){
            LOG.debug("Data for nflow {} has not arrived before the late time: {} ", nflowName, lateTime);
            builder.message("Data for nflow " + nflowName + " has not arrived before the late time: " + lateTime + "\n The last successful nflow was on " + lastNflowTime)
                .result(AssessmentResult.FAILURE);
        }
        else if (nowDiff <= (duration + latePeriodMillis)) {
            LOG.debug("Data for nflow {} has arrived before the late time: {}. The last successful nflow was on {}.  It has been {} since data has arrived.  The allowed duration is {} ", nflowName,
                      lateTime, lastNflowTime, DateTimeUtil.formatPeriod(nowDiffPeriod), DateTimeUtil.formatPeriod(acceptedPeriod));
            builder.message("Data for nflow " + nflowName + " has arrived on time.  \n The last successful nflow was on " + lastNflowTime + ". It has been " + DateTimeUtil
                .formatPeriod(nowDiffPeriod) + " since data has arrived.  The allowed duration is " + DateTimeUtil.formatPeriod(acceptedPeriod))
                .result(AssessmentResult.SUCCESS);
        } else if (nowDiff > (duration + latePeriodMillis)) {
            //error its been greater that the duration of the cron + lateTime
            LOG.debug("Data for nflow {} has not arrived before the late time: {}. The last successful nflow was on {}.  It has been {} since data has arrived.  The allowed duration is {} ", nflowName,
                      lateTime, lastNflowTime,
                      DateTimeUtil.formatPeriod(nowDiffPeriod), DateTimeUtil.formatPeriod(acceptedPeriod));
            builder.message("Data for nflow " + nflowName + " has not arrived on time. \n The last successful nflow was on " + lastNflowTime + ".  It has been " + DateTimeUtil
                .formatPeriod(nowDiffPeriod) + " since data has arrived. The allowed duration is " + DateTimeUtil.formatPeriod(acceptedPeriod))
                .result(AssessmentResult.FAILURE);
        } else if (DateTime.now().isBefore(lateTime)) { //&& lastNflowTime.isBefore(expectedTime)
            LOG.debug("CurrentTime {} is before the lateTime of {}.  Not Assessing", DateTime.now(), lateTime);
            return;
        } else {
            LOG.debug("Data for nflow {} has not arrived before the late time: {} ", nflowName, lateTime);

            builder.message("Data for nflow " + nflowName + " has not arrived before the late time: " + lateTime + "\n The last successful nflow was on " + lastNflowTime)
                .result(AssessmentResult.FAILURE);
        }
    }


    public MetadataAccess getMetadataAccess() {
        return metadataAccess;
    }

    public void setMetadataAccess(MetadataAccess metadataAccess) {
        this.metadataAccess = metadataAccess;
    }
}
