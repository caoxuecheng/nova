package com.onescorpin.metadata.cache;
/*-
 * #%L
 * onescorpin-operational-metadata-integration-service
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.onescorpin.alerts.api.Alert;
import com.onescorpin.alerts.api.AlertCriteria;
import com.onescorpin.alerts.api.AlertProvider;
import com.onescorpin.alerts.api.AlertSummary;
import com.onescorpin.alerts.api.core.AlertCriteriaInput;
import com.onescorpin.alerts.rest.AlertsModel;
import com.onescorpin.alerts.rest.model.AlertSummaryGrouped;
import com.onescorpin.alerts.service.ServiceStatusAlerts;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementDescriptionProvider;
import com.onescorpin.metadata.cache.util.TimeUtil;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.nflow.security.NflowAclCache;
import com.onescorpin.metadata.jpa.sla.CachedServiceLevelAgreement;
import com.onescorpin.metadata.jpa.sla.ServiceLevelAgreementDescriptionCache;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Created by sr186054 on 9/27/17.
 */
public class AlertsCache implements TimeBasedCache<AlertSummaryGrouped> {


    private static final Logger log = LoggerFactory.getLogger(AlertsCache.class);


    @Inject
    private NflowAclCache nflowAclCache;

    @Inject
    private AlertProvider alertProvider;

    @Inject
    private ServiceLevelAgreementDescriptionProvider serviceLevelAgreementDescriptionProvider;

    @Inject
    private ServiceLevelAgreementDescriptionCache serviceLevelAgreementDescriptionCache;

    @Inject
    private AlertsModel alertsModel;


    LoadingCache<Long, List<AlertSummaryGrouped>> alertSummaryCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).build(new CacheLoader<Long, List<AlertSummaryGrouped>>() {
        @Override
        public List<AlertSummaryGrouped> load(Long millis) throws Exception {
            return fetchUnhandledAlerts();
        }
    });

    public List<AlertSummaryGrouped> getAlertSummary(Long time) {
        return alertSummaryCache.getUnchecked(time);
    }

    public List<AlertSummaryGrouped> getUserAlertSummary() {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserAlertSummary(time);
    }

    public List<AlertSummaryGrouped> getUserAlertSummary(Long time) {
        return getUserAlertSummary(time, null, null);
    }

    public List<AlertSummaryGrouped> getUserAlertSummaryForNflowId(String nflowId) {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserAlertSummaryForNflowId(time, nflowId);
    }


    public List<AlertSummaryGrouped> getUserAlertSummaryForNflowId(Long time, String nflowId) {
        if (StringUtils.isBlank(nflowId)) {
            return new ArrayList<>();
        } else {
            return getUserAlertSummary(time, null, nflowId);
        }
    }

    public List<AlertSummaryGrouped> getUserAlertSummaryForNflowName(String nflowName) {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserAlertSummaryForNflowName(time, nflowName);
    }

    public List<AlertSummaryGrouped> getUserAlertSummaryForNflowName(Long time, String nflowName) {
        if (StringUtils.isBlank(nflowName)) {
            return new ArrayList<>();
        } else {
            return getUserAlertSummary(time, nflowName, null);
        }
    }

    @Override
    public List<AlertSummaryGrouped> getCache(Long time) {
        return getAlertSummary(time);
    }

    @Override
    public List<AlertSummaryGrouped> getUserCache(Long time) {
        return getUserAlertSummary(time);
    }
    public List<AlertSummaryGrouped> getUserCache(Long time, RoleSetExposingSecurityExpressionRoot userContext) {
        return getUserAlertSummary(time, null, null,userContext);
    }

    protected List<AlertSummaryGrouped> fetchUnhandledAlerts() {
        List<AlertSummary> alerts = new ArrayList<>();
        AlertCriteria criteria = alertProvider.criteria();
        new AlertCriteriaInput.Builder()
            .state(Alert.State.UNHANDLED)
            .asServiceAccount(true)
            .onlyIfChangesDetected(true)
            .applyToCriteria(criteria);
        Iterator<? extends AlertSummary> itr = alertProvider.getAlertsSummary(criteria);
        if (itr.hasNext()) {
            itr.forEachRemaining(alerts::add);
            List<AlertSummaryGrouped> latestAlerts = new ArrayList<>(alertsModel.groupAlertSummaries(alerts));
            return latestAlerts;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isAvailable() {
        return nflowAclCache.isUserCacheAvailable();
    }


    private boolean filterSlaAlertForUserAccess(RoleSetExposingSecurityExpressionRoot userContext, AlertSummaryGrouped alertSummary, String nflowName) {
        if (StringUtils.isBlank(alertSummary.getSlaId())) {
            return true;
        } else {
            CachedServiceLevelAgreement slaDescription = serviceLevelAgreementDescriptionCache.getUnchecked(alertSummary.getSlaId());
            if (slaDescription != null) {
                return slaDescription.getNflows().stream().filter(f -> (StringUtils.isNotBlank(nflowName) ? f.getName().equalsIgnoreCase(nflowName) : true))
                    .anyMatch(f -> nflowAclCache.hasAccess(userContext, f.getId().toString()));
            } else {
                return false;
            }
        }
    }

    private boolean hasAccess(RoleSetExposingSecurityExpressionRoot userContext, AlertSummaryGrouped alertSummary, String nflowName, String nflowId) {
        if (alertSummary.getNflowId() != null) {
            return
                (StringUtils.isNotBlank(nflowName) ? alertSummary.getNflowName().equalsIgnoreCase(nflowName) : StringUtils.isNotBlank(nflowId) ? alertSummary.getNflowId().equalsIgnoreCase(nflowId) : true)
                && nflowAclCache.hasAccess(userContext, alertSummary.getNflowId());
        } else if (alertSummary.getSlaId() != null) {
            return filterSlaAlertForUserAccess(userContext, alertSummary, nflowName);
        } else {
            if(StringUtils.isNotBlank(nflowName)){
                return !alertSummary.getType().equals(ServiceStatusAlerts.SERVICE_STATUS_ALERT_TYPE);
            }
            return true;
        }
    }

    private List<AlertSummaryGrouped> getUserAlertSummary(Long time, String nflowName, String nflowId) {
        RoleSetExposingSecurityExpressionRoot userContext = nflowAclCache.userContext();
      return getUserAlertSummary(time,nflowName,nflowId,userContext);
    }

    private List<AlertSummaryGrouped> getUserAlertSummary(Long time, String nflowName, String nflowId,RoleSetExposingSecurityExpressionRoot userContext ) {
        return getAlertSummary(time).stream()
            .filter(alertSummaryGrouped -> hasAccess(userContext, alertSummaryGrouped, nflowName, nflowId))
            .collect(Collectors.toList());
    }
}
