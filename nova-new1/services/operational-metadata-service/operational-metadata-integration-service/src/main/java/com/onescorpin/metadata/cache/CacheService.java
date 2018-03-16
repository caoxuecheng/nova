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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.onescorpin.alerts.rest.model.AlertSummaryGrouped;
import com.onescorpin.jobrepo.query.model.CheckDataJob;
import com.onescorpin.jobrepo.query.model.DataConfidenceSummary;
import com.onescorpin.jobrepo.query.model.NflowStatus;
import com.onescorpin.jobrepo.query.model.JobStatusCount;
import com.onescorpin.metadata.cache.util.TimeUtil;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.nflow.security.NflowAclCache;
import com.onescorpin.rest.model.search.SearchResult;
import com.onescorpin.security.AccessController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

/**
 * Created by sr186054 on 9/21/17.
 */
@Component
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);


    @Inject
    AccessController accessController;

    @Inject
    NflowHealthSummaryCache nflowHealthSummaryCache;

    @Inject
    AlertsCache alertsCache;

    @Inject
    NflowAclCache nflowAclCache;

    @Inject
    ServiceStatusCache serviceStatusCache;

    @Inject
    RunningJobsCache runningJobsCache;

    @Inject
    DataConfidenceJobsCache dataConfidenceJobsCache;

    @Value("${nova.ops.mgr.dashboard.threads:20}")
    private int dashboardThreads = 20;

    ExecutorService executor = Executors.newFixedThreadPool(dashboardThreads, new ThreadFactoryBuilder().setNameFormat("nova-dashboard-pool-%d").build());

    /**
     * Threaded Task that releases the barrier counter
     */
    private class ThreadedDashboardTask implements Runnable {
        CyclicBarrier barrier;
        Runnable runnable;
        String name;
        public ThreadedDashboardTask(CyclicBarrier barrier, Runnable runnable){
            this.barrier = barrier;
            this.runnable = runnable;
            this.name = UUID.randomUUID().toString();
        }
        public void run(){
            try {
                this.runnable.run();
            }catch (Exception e){

            }finally {
                try {
                    barrier.await();
                }catch (Exception e){

                }
            }
            }

    }

    /**
     * Action called after getting nflow data to create the Dashboard view as it pertains to the User
     */
    private class DashboardAction implements  Runnable{

        private Long time;
        private Dashboard dashboard;

        private NflowHealthSummaryCache.NflowSummaryFilter nflowSummaryFilter;
        private RoleSetExposingSecurityExpressionRoot userContext;

        public DashboardAction(Long time, RoleSetExposingSecurityExpressionRoot userContext,NflowHealthSummaryCache.NflowSummaryFilter nflowSummaryFilter){
            this.time = time;
            this.nflowSummaryFilter = nflowSummaryFilter;
            this.userContext = userContext;
        }
        public void run() {
            try {
                DataConfidenceSummary dataConfidenceSummary = new DataConfidenceSummary(dataConfidenceJobsCache.getUserDataConfidenceJobs(time,userContext), 60);
                dashboard =
                    new Dashboard(time, userContext.getName(), nflowHealthSummaryCache.getUserNflowHealthCounts(time, userContext), nflowHealthSummaryCache.getUserNflowHealth(time, nflowSummaryFilter,userContext),
                                  alertsCache.getUserCache(time,userContext), dataConfidenceSummary, serviceStatusCache.getUserCache(time));
            } catch (Exception e) {
                log.error("Error getting the dashboard ", e);
                throw new RuntimeException("Unable to get the Dashboard " + e.getMessage());
            }
        }

        public Dashboard getDashboard(){
            return dashboard;
        }
    }

    /**
     * We need the Acl List populated in order to do the correct fetch
     * Fetch the components of the dashboard in separate threads
     */
    public Dashboard getDashboard(NflowHealthSummaryCache.NflowSummaryFilter nflowSummaryFilter) {
        Dashboard dashboard = null;
        if (!accessController.isEntityAccessControlled() || (accessController.isEntityAccessControlled() && nflowAclCache.isAvailable())) {
            Long time = TimeUtil.getTimeNearestFiveSeconds();
            RoleSetExposingSecurityExpressionRoot userContext = nflowAclCache.userContext();
            DashboardAction dashboardAction = new DashboardAction(time, userContext,nflowSummaryFilter);
            CyclicBarrier barrier = new CyclicBarrier(5, dashboardAction);
            List<ThreadedDashboardTask> tasks = new ArrayList<>();
            tasks.add(new ThreadedDashboardTask(barrier,() -> nflowHealthSummaryCache.getCache(time)));
            tasks.add(new ThreadedDashboardTask(barrier,() -> dataConfidenceJobsCache.getCache(time)));
            tasks.add(new ThreadedDashboardTask(barrier,() -> alertsCache.getCache(time)));
            tasks.add(new ThreadedDashboardTask(barrier,() -> serviceStatusCache.getCache(time)));
            tasks.stream().forEach(t -> executor.submit(t));
            try {
                barrier.await();
            }catch (Exception e) {
            }
            return dashboardAction.getDashboard();
        } else {
            return Dashboard.NOT_READY;
        }
    }

    public Map<String,Long> getUserNflowHealthCounts(){
        Long time = TimeUtil.getTimeNearestFiveSeconds();
       return nflowHealthSummaryCache.getUserNflowHealthCounts(time);
    }


    public NflowStatus getUserNflowHealth(String nflowName) {
        return nflowHealthSummaryCache.getUserNflow(nflowName);
    }

    public NflowStatus getUserNflowHealth() {
        return nflowHealthSummaryCache.getUserNflows();
    }

    public SearchResult getUserNflowHealthWithFilter(NflowHealthSummaryCache.NflowSummaryFilter filter) {
        return nflowHealthSummaryCache.getUserNflowHealth(filter);
    }

    public List<CheckDataJob> getUserDataConfidenceJobs() {
        return dataConfidenceJobsCache.getUserDataConfidenceJobs();
    }

    public List<JobStatusCount> getUserRunningJobs() {
        return runningJobsCache.getUserRunningJobs();
    }


    public List<AlertSummaryGrouped> getUserAlertSummary() {
        return alertsCache.getUserAlertSummary();
    }

    public List<AlertSummaryGrouped> getUserAlertSummaryForNflowId(String nflowId) {
        return alertsCache.getUserAlertSummaryForNflowId(nflowId);
    }

    public List<AlertSummaryGrouped> getUserAlertSummaryForNflowName(String nflowName) {
        return alertsCache.getUserAlertSummaryForNflowName(nflowName);
    }


}
