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

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import static com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution.JobStatus.FAILED;

/**
 * Service to listen for nflow failure events and notify listeners when a nflow fails
 */
@Component
public class NflowFailureService {


    @Inject
    private BatchJobExecutionProvider batchJobExecutionProvider;

    @Inject
    private OpsManagerNflowProvider nflowProvider;

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private NifiNflowProcessorStatisticsProvider nifiNflowProcessorStatisticsProvider;

    public static final LastNflowJob EMPTY_JOB = new LastNflowJob("empty", DateTime.now(), true);

    /**
     * Map with the Latest recorded failure that has been assessed by the NflowFailureMetricAssessor
     */
    private Map<String, LastNflowJob> lastAssessedNflowFailureMap = new HashMap<>();

    public LastNflowJob findLastJob(String nflowName){
     return metadataAccess.read(() -> {

         OpsManagerNflow nflow = nflowProvider.findByNameWithoutAcl(nflowName);
         if(nflow == null){
             return null;
         }
         if (nflow.isStream()) {
             List<NifiNflowProcessorStats> latestStats = nifiNflowProcessorStatisticsProvider.findLatestFinishedStatsWithoutAcl(nflowName);
             Optional<NifiNflowProcessorStats> total = latestStats.stream().reduce((a, b) -> {
                 a.setFailedCount(a.getFailedCount() + b.getFailedCount());
                 return a;
             });
             if (total.isPresent()) {
                 NifiNflowProcessorStats stats = total.get();
                 boolean success = stats.getFailedCount() == 0;
                 return new LastNflowJob(nflowName, stats.getMinEventTime(), success);
             } else {
                 return EMPTY_JOB;
             }
         } else {
             BatchJobExecution latestJob = batchJobExecutionProvider.findLatestFinishedJobForNflow(nflowName);
             return latestJob != null ? new LastNflowJob(nflowName, latestJob.getEndTime(), !FAILED.equals(latestJob.getStatus())) : EMPTY_JOB;
         }
     }, MetadataAccess.SERVICE);

    }

    boolean isExistingFailure(LastNflowJob job) {
        if(job.isFailure()){
            String nflowName = job.getNflowName();
            LastNflowJob lastAssessedFailure = lastAssessedNflowFailureMap.get(nflowName);
            if (lastAssessedFailure == null) {
                lastAssessedNflowFailureMap.put(nflowName, job);
                return false;
            } else if (job.isAfter(lastAssessedFailure.getDateTime())) {
                //reassign it as the lastAssessedFailure
                lastAssessedNflowFailureMap.put(nflowName, job);
                return true;
            } else {
                //last job is before or equals to last assessed job, nothing to do, we already cached a new one
                return true;
            }
        }
        return false;

    }


    public static class LastNflowJob {

        private String nflowName;
        private DateTime dateTime;
        private boolean success = false;

        public LastNflowJob(String nflowName, DateTime dateTime, boolean success) {
            this.nflowName = nflowName;
            this.dateTime = dateTime;
            this.success = success;
        }

        public String getNflowName() {
            return nflowName;
        }

        public void setNflowName(String nflowName) {
            this.nflowName = nflowName;
        }

        public DateTime getDateTime() {
            return dateTime;
        }

        public boolean isAfter(DateTime time) {
            return dateTime != null && dateTime.isAfter(time);
        }

        public boolean isFailure(){
            return !this.success;
        }
    }


}
