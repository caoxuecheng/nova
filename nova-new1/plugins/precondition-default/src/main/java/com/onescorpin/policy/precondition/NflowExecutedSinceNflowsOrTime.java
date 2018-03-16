package com.onescorpin.policy.precondition;

/*-
 * #%L
 * onescorpin-precondition-default
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

import com.google.common.collect.Lists;
import com.onescorpin.metadata.api.sla.WithinSchedule;
import com.onescorpin.metadata.rest.model.sla.Obligation;
import com.onescorpin.metadata.sla.api.ObligationGroup;
import com.onescorpin.policy.PolicyProperty;
import com.onescorpin.policy.PolicyPropertyRef;
import com.onescorpin.policy.PolicyPropertyTypes;

import org.joda.time.Period;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * A precondition building upon the  {@link NflowExecutedSinceNflows} that will also fire if a given time/cron expression is met
 */
@PreconditionPolicy(name = PreconditionPolicyConstants.NFLOW_EXECUTED_SINCE_NFLOWS_OR_TIME_NAME,
                    shortDescription = "Policy will trigger the nflow when all of the supplied nflows have successfully finished or always at the given cron expression supplied",
                    description = "Policy will trigger the nflow when all of the supplied nflows have successfully finished or always at the given cron expression supplied.  Both the Cron Expression and the Nflows input are required attributes")
public class NflowExecutedSinceNflowsOrTime extends NflowExecutedSinceNflows {

    /**
     * a cron expression to check against to see if this precondition is valid
     */
    @PolicyProperty(name = "Cron Expression", type = PolicyPropertyTypes.PROPERTY_TYPE.cron, required = true, hint = "Supply a cron expression to indicate when this nflow should run")
    private String cronExpression;


    public NflowExecutedSinceNflowsOrTime(@PolicyPropertyRef(name = "Cron Expression") String cronExpression, @PolicyPropertyRef(name = "Since Nflow") String sinceCategoryAndNflowName,
                                        @PolicyPropertyRef(name = "Dependent Nflows") String categoryAndNflows) {
        super(sinceCategoryAndNflowName, categoryAndNflows);
        this.cronExpression = cronExpression;
    }


    @Override
    public Set<com.onescorpin.metadata.rest.model.sla.ObligationGroup> buildPreconditionObligations() {
        Set<com.onescorpin.metadata.rest.model.sla.ObligationGroup> preconditionGroups = new HashSet<>();
        preconditionGroups.addAll(super.buildPreconditionObligations());

        try {
            Period p = new Period(0, 0, 1, 0);
            String withinPeriod = p.toString();
            WithinSchedule metric = new WithinSchedule(cronExpression, withinPeriod);
            Obligation obligation = new Obligation();
            obligation.setMetrics(Lists.newArrayList(metric));
            com.onescorpin.metadata.rest.model.sla.ObligationGroup group = new com.onescorpin.metadata.rest.model.sla.ObligationGroup();
            group.addObligation(obligation);
            group.setCondition(ObligationGroup.Condition.SUFFICIENT.name());
            preconditionGroups.add(group);
        } catch (ParseException e) {

        }
        return preconditionGroups;
    }

}
