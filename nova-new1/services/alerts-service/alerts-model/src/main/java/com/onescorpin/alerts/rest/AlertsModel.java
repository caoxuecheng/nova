package com.onescorpin.alerts.rest;

/*-
 * #%L
 * onescorpin-alerts-model
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

import com.onescorpin.alerts.AlertConstants;
import com.onescorpin.alerts.api.AlertSummary;
import com.onescorpin.alerts.api.EntityAlert;
import com.onescorpin.alerts.api.SourceAlert;
import com.onescorpin.alerts.rest.model.Alert;
import com.onescorpin.alerts.rest.model.AlertSummaryGrouped;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.alerts.EntityAwareAlertSummary;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by sr186054 on 7/22/17.
 */
@Component
public class AlertsModel {

    private static final Logger log = LoggerFactory.getLogger(AlertsModel.class);

    /**
     *
     * @param alertSummary
     * @return
     */
    public String alertTypeDisplayName(AlertSummary alertSummary) {
        String type = alertSummary.getType();
        return alertTypeDisplayName(type);
    }


    public String alertTypeDisplayName(String type) {
        String part = type;
        if (part.startsWith(AlertConstants.NOVA_ALERT_TYPE_PREFIX+"/alert")) {
            part = StringUtils.substringAfter(part, AlertConstants.NOVA_ALERT_TYPE_PREFIX+"/alert");
        }
         else if (part.startsWith(AlertConstants.NOVA_ALERT_TYPE_PREFIX)) {
            part = StringUtils.substringAfter(part, AlertConstants.NOVA_ALERT_TYPE_PREFIX);
        } else {
            int idx = StringUtils.lastOrdinalIndexOf(part, "/", 2);
            part = StringUtils.substring(part, idx);
        }
        String[] parts = part.split("/");
        return Arrays.asList(parts).stream().map(s -> StringUtils.capitalize(s)).collect(Collectors.joining(" "));
    }

    public Collection<AlertSummaryGrouped> groupAlertSummaries(List<AlertSummary> alertSummaries) {
        Map<String, AlertSummaryGrouped> group = new HashMap<>();
        alertSummaries.forEach(alertSummary -> {

            String key = alertSummary.getType() + ":" + alertSummary.getSubtype();

            String displayName = alertTypeDisplayName(alertSummary);
            if (alertSummary instanceof EntityAwareAlertSummary) {
                EntityAwareAlertSummary entityAwareAlertSummary = (EntityAwareAlertSummary) alertSummary;
                key = entityAwareAlertSummary.getGroupByKey();
                group.computeIfAbsent(key, key1 -> new AlertSummaryGrouped.Builder().typeString(alertSummary.getType())
                    .typeDisplayName(displayName)
                    .subType(entityAwareAlertSummary.getSubtype())
                    .nflowId(entityAwareAlertSummary.getNflowId() != null ? entityAwareAlertSummary.getNflowId().toString() : null)
                    .nflowName(entityAwareAlertSummary.getNflowName())
                    .slaId(entityAwareAlertSummary.getSlaId() != null ? entityAwareAlertSummary.getSlaId().toString() : null)
                    .slaName(entityAwareAlertSummary.getSlaName()).build()).add(toModel(alertSummary.getLevel()), alertSummary.getCount(), alertSummary.getLastAlertTimestamp());
            } else {
                group.computeIfAbsent(key, key1 -> new AlertSummaryGrouped.Builder().typeString(alertSummary.getType())
                    .typeDisplayName(displayName)
                    .subType(alertSummary.getSubtype())
                    .build()).add(toModel(alertSummary.getLevel()), alertSummary.getCount(), alertSummary.getLastAlertTimestamp());
            }
        });
        return group.values();
    }


    public com.onescorpin.alerts.rest.model.Alert toModel(com.onescorpin.alerts.api.Alert alert) {
        com.onescorpin.alerts.api.Alert baseAlert = alert;
        try {
            if (Proxy.isProxyClass(alert.getClass())) {
                SourceAlert sourceAlert = (SourceAlert) Proxy.getInvocationHandler(alert);
                if (sourceAlert != null) {
                    baseAlert = sourceAlert.getWrappedAlert();
                }
            }
        }catch (Exception e){
            //unable to get base alert from proxy.  log the exception but continue
            log.error("Unable to get base alert from wrapped proxy for : {}, {} ",alert,e.getMessage(),e);

        }
        com.onescorpin.alerts.rest.model.Alert result = new com.onescorpin.alerts.rest.model.Alert();
        result.setId(alert.getId().toString());
        result.setActionable(alert.isActionable());
        result.setCreatedTime(alert.getCreatedTime());
        result.setLevel(toModel(alert.getLevel()));
        result.setState(toModel(alert.getState()));
        result.setType(alert.getType());
        result.setDescription(alert.getDescription());
        result.setCleared(alert.isCleared());
        result.setContent(alert.getContent() != null ? alert.getContent().toString() : null);
        result.setSubtype(alert.getSubtype());
        alert.getEvents().forEach(e -> result.getEvents().add(toModel(e)));
        if(baseAlert instanceof EntityAlert){
            result.setEntityId(((EntityAlert)baseAlert).getEntityId() != null ? ((EntityAlert)baseAlert).getEntityId().toString(): null);
            result.setEntityType(((EntityAlert)baseAlert).getEntityType());
        }
        return result;
    }

    public com.onescorpin.alerts.rest.model.AlertChangeEvent toModel(com.onescorpin.alerts.api.AlertChangeEvent event) {
        com.onescorpin.alerts.rest.model.AlertChangeEvent result = new com.onescorpin.alerts.rest.model.AlertChangeEvent();
        result.setCreatedTime(event.getChangeTime());
        result.setDescription(event.getDescription());
        result.setState(toModel(event.getState()));
        result.setUser(event.getUser() != null ? event.getUser().getName() : null);
        try {
        result.setContent(event.getContent() != null ? ObjectMapperSerializer.serialize(event.getContent()) : null);
        }catch (Exception e){

        }
        return result;
    }

    public Alert.Level toModel(com.onescorpin.alerts.api.Alert.Level level) {
        // Currently identical
        return Alert.Level.valueOf(level.name());
    }

    public Alert.State toModel(com.onescorpin.alerts.api.Alert.State state) {
        // Currently identical
        return Alert.State.valueOf(state.name());
    }

    public com.onescorpin.alerts.api.Alert.State toDomain(Alert.State state) {
        return com.onescorpin.alerts.api.Alert.State.valueOf(state.name());
    }

    public com.onescorpin.alerts.api.Alert.Level toDomain(Alert.Level level) {
        // Currently identical
        return com.onescorpin.alerts.api.Alert.Level.valueOf(level.name());
    }

}
