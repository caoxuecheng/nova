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
import com.onescorpin.jobrepo.query.model.JobStatusCount;
import com.onescorpin.jobrepo.query.model.transform.JobStatusTransform;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.jobrepo.job.BatchAndStreamingJobStatusCount;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.cache.util.TimeUtil;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.nflow.security.NflowAclCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * Created by sr186054 on 9/27/17.
 */
public class RunningJobsCache implements TimeBasedCache<JobStatusCount> {

    private static final Logger log = LoggerFactory.getLogger(RunningJobsCache.class);

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private NflowAclCache nflowAclCache;

    @Inject
    private BatchJobExecutionProvider jobExecutionProvider;

    LoadingCache<Long, List<JobStatusCount>> runningJobsCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<Long, List<JobStatusCount>>() {
        @Override
        public List<JobStatusCount> load(Long millis) throws Exception {
            return fetchRunningJobs();
        }
    });


    protected List<JobStatusCount> fetchRunningJobs() {
        List<JobStatusCount> runningCounts = metadataAccess.read(() -> {
            List<BatchAndStreamingJobStatusCount> counts = jobExecutionProvider.getBatchAndStreamingJobCounts(jobExecutionProvider.RUNNING_FILTER);
            if (counts != null) {
                return counts.stream().map(c -> JobStatusTransform.jobStatusCount(c)).collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, MetadataAccess.SERVICE);
        return runningCounts;
    }


    public List<JobStatusCount> getRunningJobs(Long time) {
        return runningJobsCache.getUnchecked(time);
    }


    public List<JobStatusCount> getUserRunningJobs(Long time) {
        RoleSetExposingSecurityExpressionRoot userContext = nflowAclCache.userContext();
        return getRunningJobs(time).stream().filter(f -> nflowAclCache.hasAccess(userContext, f.getNflowId())).collect(Collectors.toList());
    }

    public List<JobStatusCount> getUserRunningJobs() {
        Long time = TimeUtil.getTimeNearestSecond();
        return getUserRunningJobs(time);
    }

    @Override
    public List<JobStatusCount> getCache(Long time) {
        return getUserRunningJobs(time);
    }

    @Override
    public List<JobStatusCount> getUserCache(Long time) {
        return getUserRunningJobs(time);
    }

    @Override
    public boolean isAvailable() {
        return nflowAclCache.isUserCacheAvailable();
    }

}
