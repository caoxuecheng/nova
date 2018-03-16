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

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.onescorpin.DateTimeUtil;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementMetric;
import com.onescorpin.policy.PolicyProperty;
import com.onescorpin.policy.PolicyPropertyRef;
import com.onescorpin.policy.PolicyPropertyTypes;
import com.onescorpin.policy.PropertyLabelValue;
import com.onescorpin.scheduler.util.TimerToCronExpression;

import org.joda.time.Period;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.Locale;

/**
 * SLA metric to ensure a nflow gets executed by a specified time
 * This will be exposed to the User Interface since it is annotated with {@link ServiceLevelAgreementMetric}
 */
@ServiceLevelAgreementMetric(name = "Nflow Processing deadline",
                             description = "Ensure a Nflow processes data by a specified time")
public class NflowOnTimeArrivalMetric implements Metric {

    @PolicyProperty(name = "NflowName",
                    type = PolicyPropertyTypes.PROPERTY_TYPE.nflowSelect,
                    required = true,
                    value = PolicyPropertyTypes.CURRENT_NFLOW_VALUE)
    private String nflowName;

    @PolicyProperty(name = "ExpectedDeliveryTime",
                    displayName = "Expected Delivery Time",
                    type = PolicyPropertyTypes.PROPERTY_TYPE.cron,
                    hint = "Cron Expression for when you expect to receive this data",
                    value = "0 0 12 1/1 * ? *",
                    required = true)
    private String cronString;

    @PolicyProperty(name = "NoLaterThanTime",
                    displayName = "No later than time",
                    type = PolicyPropertyTypes.PROPERTY_TYPE.string,
                    pattern = "^\\d+$",
                    patternInvalidMessage = "The value must be numeric digits",
                    hint = "Number specifying the amount of time allowed after the Expected Delivery Time",
                    group = "lateTime",
                    required = true)
    private Integer lateTime;

    @PolicyProperty(name = "NoLaterThanUnits",
                    displayName = "Units", type = PolicyPropertyTypes.PROPERTY_TYPE.select,
                    group = "lateTime",
                    labelValues = {@PropertyLabelValue(label = "Days", value = "days"),
                                   @PropertyLabelValue(label = "Hours", value = "hrs"),
                                   @PropertyLabelValue(label = "Minutes", value = "min"),
                                   @PropertyLabelValue(label = "Seconds", value = "sec")},
                    required = true)
    private String lateUnits;

    @JsonIgnore
    private CronExpression expectedExpression;

    /**
     * lateTime + lateUnits  as a Jodatime period object
     **/
    private Period latePeriod;


    public NflowOnTimeArrivalMetric() {
    }


    public NflowOnTimeArrivalMetric(@PolicyPropertyRef(name = "NflowName") String nflowName,
                                   @PolicyPropertyRef(name = "ExpectedDeliveryTime") String cronString,
                                   @PolicyPropertyRef(name = "NoLaterThanTime") Integer lateTime,
                                   @PolicyPropertyRef(name = "NoLaterThanUnits") String lateUnits) throws ParseException {
        this.nflowName = nflowName;
        this.cronString = cronString;
        this.lateTime = lateTime;
        this.lateUnits = lateUnits;

        this.expectedExpression = new CronExpression(this.cronString);
        this.latePeriod = TimerToCronExpression.timerStringToPeriod(this.lateTime + " " + this.lateUnits);
    }

    public NflowOnTimeArrivalMetric(String nflowName,
                                   CronExpression expectedExpression,
                                   Period latePeriod
    ) {
        super();
        this.nflowName = nflowName;
        this.expectedExpression = expectedExpression;
        this.latePeriod = latePeriod;
    }


    /* (non-Javadoc)
     * @see com.onescorpin.metadata.sla.api.Metric#getDescription()
     */
    @Override
    public String getDescription() {
        StringBuilder bldr = new StringBuilder("Data expected from nflow ");

        bldr.append("\"").append(this.nflowName).append("\" ")
            .append(generateCronDescription(this.getExpectedExpression().toString()))
            .append(", and no more than ").append(DateTimeUtil.formatPeriod(this.latePeriod)).append(" late");
        return bldr.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .omitNullValues()
            .add("nflowName", this.nflowName)
            .add("expectedExpression", this.expectedExpression)
            .add("latePeriod", DateTimeUtil.formatPeriod(this.latePeriod))
            .toString();
    }

    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    public CronExpression getExpectedExpression() {
        if (this.expectedExpression == null && this.cronString != null) {
            try {
                this.expectedExpression = new CronExpression(this.cronString);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return expectedExpression;
    }

    public void setExpectedExpression(CronExpression expectedExpression) {
        this.expectedExpression = expectedExpression;
    }

    public Period getLatePeriod() {
        return latePeriod;
    }

    public void setLatePeriod(Period latePeriod) {
        this.latePeriod = latePeriod;
    }

    private String generateCronDescription(String cronExp) {
        CronDefinition quartzDef = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        CronParser parser = new CronParser(quartzDef);
        Cron c = parser.parse(cronExp);
        return CronDescriptor.instance(Locale.US).describe(c);
    }

}
