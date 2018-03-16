package com.onescorpin.metadata.cache;
/*-
 * #%L
 * onescorpin-job-repository-controller
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

import com.onescorpin.alerts.rest.model.AlertSummaryGrouped;
import com.onescorpin.jobrepo.query.model.DataConfidenceSummary;
import com.onescorpin.jobrepo.query.model.NflowSummary;
import com.onescorpin.rest.model.search.SearchResult;
import com.onescorpin.servicemonitor.model.ServiceStatusResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sr186054 on 9/22/17.
 */
public class Dashboard {

    public static enum Status {
        READY, NOT_READY
    }

    public static final Dashboard NOT_READY = new Dashboard(Status.NOT_READY);

    private String user;
    private Long time;
    private Status status;
    private Map<String, Long> healthCounts = new HashMap<>();

    private SearchResult nflows;


    private List<AlertSummaryGrouped> alerts;

    private List<ServiceStatusResponse> serviceStatus;

    private DataConfidenceSummary dataConfidenceSummary;

    public Dashboard(Status status) {
        this.status = status;
    }

    public Dashboard(Long time, String user, Map<String, Long> healthCounts, SearchResult<NflowSummary> nflows, List<AlertSummaryGrouped> alerts, DataConfidenceSummary dataConfidenceSummary,
                     List<ServiceStatusResponse> serviceStatus) {
        this.time = time;
        this.user = user;
        this.healthCounts = healthCounts;
        this.nflows = nflows;
        this.alerts = alerts;
        this.dataConfidenceSummary = dataConfidenceSummary;
        this.serviceStatus = serviceStatus;
        this.status = Status.READY;
    }

    public List<AlertSummaryGrouped> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<AlertSummaryGrouped> alerts) {
        this.alerts = alerts;
    }

    public DataConfidenceSummary getDataConfidenceSummary() {
        return dataConfidenceSummary;
    }

    public void setDataConfidenceSummary(DataConfidenceSummary dataConfidenceSummary) {
        this.dataConfidenceSummary = dataConfidenceSummary;
    }

    public List<ServiceStatusResponse> getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(List<ServiceStatusResponse> serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Map<String, Long> getHealthCounts() {
        return healthCounts;
    }

    public void setHealthCounts(Map<String, Long> healthCounts) {
        this.healthCounts = healthCounts;
    }

    public Status getStatus() {
        return status;
    }

    public SearchResult getNflows() {
        return nflows;
    }

    public void setNflows(SearchResult nflows) {
        this.nflows = nflows;
    }
}
