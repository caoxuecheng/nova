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
import com.onescorpin.jobrepo.query.model.CheckDataJob;
import com.onescorpin.jobrepo.query.model.transform.JobModelTransform;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowOperationStatusEvent;
import com.onescorpin.metadata.api.event.nflow.OperationStatus;
import com.onescorpin.metadata.api.event.job.DataConfidenceJobDetected;
import com.onescorpin.metadata.api.event.job.DataConfidenceJobEvent;
import com.onescorpin.metadata.api.nflow.LatestNflowJobExecution;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.metadata.cache.util.TimeUtil;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.nflow.security.NflowAclCache;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Created by sr186054 on 9/27/17.
 */
public class DataConfidenceJobsCache implements TimeBasedCache<CheckDataJob> {

    private static final Logger log = LoggerFactory.getLogger(DataConfidenceJobsCache.class);

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;

    @Inject
    private NflowAclCache nflowAclCache;

    @Inject
    private MetadataEventService metadataEventService;

    private List<CheckDataJob> latestCache = null;

    private AtomicBoolean needsRefresh = new AtomicBoolean(true);

    private DateTime needsRefreshAt = null;


    private final BatchJobExecutionUpdatedListener batchJobExecutionUpdatedListener = new BatchJobExecutionUpdatedListener();

    private final DataConfidenceJobDetectedListener dataConfidenceJobDetectedListener = new DataConfidenceJobDetectedListener();

    @PostConstruct
    private void init() {
        metadataEventService.addListener(batchJobExecutionUpdatedListener);
        metadataEventService.addListener(dataConfidenceJobDetectedListener);
    }


    private LoadingCache<Long, List<CheckDataJob>> checkDataJobCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.SECONDS).build(new CacheLoader<Long, List<CheckDataJob>>() {
        @Override
        public List<CheckDataJob> load(Long millis) throws Exception {
            return fetchDataConfidenceSummary();
        }
    });


    private List<CheckDataJob> fetchDataConfidenceSummary() {
        if (latestCache == null || needsRefresh.get()) {
            DateTime refrshStart = DateTime.now();
            latestCache = metadataAccess.read(() -> {

                List<? extends LatestNflowJobExecution> latestCheckDataJobs = opsManagerNflowProvider.findLatestCheckDataJobs();

                if (latestCheckDataJobs != null) {
                    return latestCheckDataJobs.stream().map(latestNflowJobExecution -> JobModelTransform.checkDataJob(latestNflowJobExecution)).collect(Collectors.toList());
                    // return new DataConfidenceSummary(checkDataJobs, 60);
                } else {
                    return Collections.emptyList();
                }
            }, MetadataAccess.SERVICE);
            //reset the refresh flag including if any updates happened while we were loading
            if (needsRefreshAt != null) {
                needsRefresh.set(needsRefreshAt.isAfter(refrshStart));
            } else {
                needsRefresh.set(false);
            }
            log.debug("Loaded Data Confidence Summary from the database");
            return latestCache;
        } else {
            log.debug("Returning Cached Data Confidence Summary");
            return latestCache;
        }
    }

    public List<CheckDataJob> getDataConfidenceSummary(Long time) {
        return checkDataJobCache.getUnchecked(time);
    }

    public List<CheckDataJob> getUserDataConfidenceJobs() {
        Long time = TimeUtil.getTimeNearestFiveSeconds();
        return getUserDataConfidenceJobs(time);
    }

    public List<CheckDataJob> getUserDataConfidenceJobs(Long time) {
        RoleSetExposingSecurityExpressionRoot userContext = nflowAclCache.userContext();
        return getUserDataConfidenceJobs(time,userContext);
    }

    public List<CheckDataJob> getUserDataConfidenceJobs(Long time, RoleSetExposingSecurityExpressionRoot userContext) {
        return getDataConfidenceSummary(time).stream().filter(checkDataJob -> nflowAclCache.hasAccess(userContext, checkDataJob.getNflowId())).collect(Collectors.toList());
    }


    @Override
    public List<CheckDataJob> getUserCache(Long time) {
        return getUserDataConfidenceJobs(time);
    }

    @Override
    public List<CheckDataJob> getCache(Long time) {
        return getDataConfidenceSummary(time);
    }

    @Override
    public boolean isAvailable() {
        return nflowAclCache.isUserCacheAvailable();
    }


    private class BatchJobExecutionUpdatedListener implements MetadataEventListener<NflowOperationStatusEvent> {

        public void notify(@Nonnull final NflowOperationStatusEvent metadataEvent) {
            OperationStatus change = metadataEvent.getData();
            if (NflowOperation.NflowType.CHECK == change.getNflowType()) {
                needsRefreshAt = DateTime.now();
                needsRefresh.set(true);
            }


        }
    }

    private class DataConfidenceJobDetectedListener implements MetadataEventListener<DataConfidenceJobEvent> {

        public void notify(@Nonnull final DataConfidenceJobEvent metadataEvent) {
            DataConfidenceJobDetected change = metadataEvent.getData();
            needsRefreshAt = DateTime.now();
            needsRefresh.set(true);
        }
    }
}
