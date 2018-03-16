package com.onescorpin.metadata.api.alerts;

/*-
 * #%L
 * onescorpin-alerts-default
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

import com.google.common.collect.ImmutableList;
import com.onescorpin.alerts.api.Alert;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

import java.util.stream.Collectors;

/**
 * Created by sr186054 on 7/21/17.
 */
public class NovaEntityAwareAlertSummary implements EntityAwareAlertSummary {

    private String type;

    private String subtype;

    private Alert.Level level;

    private OpsManagerNflow.ID nflowId;
    private String nflowName;
    private ServiceLevelAgreement.ID slaId;
    private String slaName;

    private Long count;

    private Long lastAlertTimestamp;

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    @Override
    public Alert.Level getLevel() {
        return level;
    }

    public void setLevel(Alert.Level level) {
        this.level = level;
    }

    @Override
    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public Long getLastAlertTimestamp() {
        return lastAlertTimestamp;
    }

    public void setLastAlertTimestamp(Long lastAlertTimestamp) {
        this.lastAlertTimestamp = lastAlertTimestamp;
    }

    @Override
    public OpsManagerNflow.ID getNflowId() {
        return nflowId;
    }

    public void setNflowId(OpsManagerNflow.ID nflowId) {
        this.nflowId = nflowId;
    }

    @Override
    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

    @Override
    public ServiceLevelAgreement.ID getSlaId() {
        return slaId;
    }

    public void setSlaId(ServiceLevelAgreement.ID slaId) {
        this.slaId = slaId;
    }

    @Override
    public String getSlaName() {
        return slaName;
    }

    public void setSlaName(String slaName) {
        this.slaName = slaName;
    }

    @Override
    public String getGroupByKey(){
       return new ImmutableList.Builder<String>()
           .add(getNflowId() != null ? getNflowId().toString() : "")
           .add(getSlaId() != null ? getSlaId().toString() : "")
           .add(getType())
           .add(getSubtype() != null ? getSubtype() : "").build().stream().collect(Collectors.joining(":"));
    }
}
