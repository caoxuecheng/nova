/**
 *
 */
package com.onescorpin.metadata.modeshape.op;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onescorpin.jobrepo.query.model.ExecutedJob;
import com.onescorpin.jobrepo.query.model.ExecutionStatus;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowNotFoundExcepton;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.api.op.NflowDependencyDeltaResults;
import com.onescorpin.metadata.api.op.NflowOperation;
import com.onescorpin.metadata.api.op.NflowOperation.ID;
import com.onescorpin.metadata.api.op.NflowOperation.State;
import com.onescorpin.metadata.api.op.NflowOperationCriteria;
import com.onescorpin.metadata.api.op.NflowOperationsProvider;
import com.onescorpin.metadata.core.AbstractMetadataCriteria;
import com.onescorpin.metadata.modeshape.nflow.JcrNflowProvider;
import com.onescorpin.metadata.modeshape.op.NflowOperationExecutedJobWrapper.OpId;
import com.onescorpin.support.NflowNameUtil;

/**
 *
 */
public class JobRepoNflowOperationsProvider implements NflowOperationsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JobRepoNflowOperationsProvider.class);
    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;

    // @Inject
    // private NflowRepository nflowRepo;

    //  @Inject
    //  private JobRepository jobRepo;
    @Inject
    private JcrNflowProvider nflowProvider;
    @Inject
    private BatchJobExecutionProvider jobExecutionProvider;
    @Inject
    private MetadataAccess metadata;


    protected static NflowOperation.State asOperationState(ExecutionStatus status) {
        switch (status) {
            case ABANDONED:
                return State.ABANDONED;
            case COMPLETED:
                return State.SUCCESS;
            case FAILED:
                return State.FAILURE;
            case STARTED:
                return State.STARTED;
            case STARTING:
                return State.STARTED;
            case STOPPING:
                return State.STARTED;
            case STOPPED:
                return State.CANCELED;
            case UNKNOWN:
                return State.STARTED;
            default:
                return State.FAILURE;
        }
    }

    protected static NflowOperation.State asOperationState(BatchJobExecution.JobStatus status) {
        switch (status) {
            case ABANDONED:
                return State.ABANDONED;
            case COMPLETED:
                return State.SUCCESS;
            case FAILED:
                return State.FAILURE;
            case STARTED:
                return State.STARTED;
            case STARTING:
                return State.STARTED;
            case STOPPING:
                return State.STARTED;
            case STOPPED:
                return State.CANCELED;
            case UNKNOWN:
                return State.STARTED;
            default:
                return State.FAILURE;
        }
    }


    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperationsProvider#criteria()
     */
    @Override
    public NflowOperationCriteria criteria() {
        return new Criteria();
    }

    public boolean isNflowRunning(Nflow.ID nflowId) {
        if (nflowId != null) {
            return opsManagerNflowProvider.isNflowRunning(opsManagerNflowProvider.resolveId(nflowId.toString()));
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.op.NflowOperationsProvider#getOperation(com.onescorpin.metadata.api.op.NflowOperation.ID)
     */
    @Override
    public NflowOperation getOperation(ID id) {
        OpId opId = (OpId) id;
        BatchJobExecution jobExecution = jobExecutionProvider.findByJobExecutionId(new Long(opId.toString()));
        if (jobExecution != null) {
            return createOperation(jobExecution);
        } else {
            return null;
        }
    }

    @Override
    public List<NflowOperation> findLatestCompleted(Nflow.ID nflowId) {
        return metadata.read(() -> {
            List<NflowOperation> operations = new ArrayList<>();
            Nflow nflow = this.nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                BatchJobExecution latestJobExecution = this.jobExecutionProvider.findLatestCompletedJobForNflow(nflow.getQualifiedName());

                if (latestJobExecution != null) {
                    LOG.debug("Latest completed job execution id {} ", latestJobExecution.getJobExecutionId());
                    operations.add(createOperation(latestJobExecution));
                }
            }
            return operations;
        });
    }

    @Override
    public List<NflowOperation> findLatest(Nflow.ID nflowId) {
        return metadata.read(() -> {
            List<NflowOperation> operations = new ArrayList<>();
            Nflow nflow = this.nflowProvider.getNflow(nflowId);

            if (nflow != null) {
                BatchJobExecution latestJobExecution = this.jobExecutionProvider.findLatestJobForNflow(nflow.getQualifiedName());

                if (latestJobExecution != null) {
                    operations.add(createOperation(latestJobExecution));
                }
            }
            return operations;
        });
    }

    @Override
    public NflowDependencyDeltaResults getDependentDeltaResults(Nflow.ID nflowId, Set<String> props) {
        Nflow nflow = this.nflowProvider.getNflow(nflowId);

        if (nflow != null) {
            String systemNflowName = NflowNameUtil.fullName(nflow.getCategory().getSystemName(), nflow.getName());
            NflowDependencyDeltaResults results = new NflowDependencyDeltaResults(nflow.getId().toString(), systemNflowName);

            //find this nflows latest completion
            BatchJobExecution latest = jobExecutionProvider.findLatestCompletedJobForNflow(systemNflowName);

            //get the dependent nflows
            List<Nflow> dependents = nflow.getDependentNflows();
            if (dependents != null) {
                for (Nflow depNflow : dependents) {

                    String depNflowSystemName = NflowNameUtil.fullName(depNflow.getCategory().getSystemName(), depNflow.getName());
                    //find Completed nflows executed since time
                    Set<BatchJobExecution> jobs = null;
                    if (latest != null) {
                        jobs = (Set<BatchJobExecution>) jobExecutionProvider.findJobsForNflowCompletedSince(depNflowSystemName, latest.getStartTime());
                    } else {
                        BatchJobExecution job = jobExecutionProvider.findLatestCompletedJobForNflow(depNflowSystemName);
                        if (job != null) {
                            jobs = new HashSet<>();
                            jobs.add(job);
                        }
                    }

                    if (jobs != null) {
                        for (BatchJobExecution job : jobs) {
                            DateTime endTime = job.getEndTime();
                            Map<String, String> executionContext = job.getJobExecutionContextAsMap();
                            Map<String, Object> map = new HashMap<>();
                            //filter the map
                            if (executionContext != null) {
                                //add those requested to the results map
                                for (Entry<String, String> entry : executionContext.entrySet()) {
                                    if (props == null || props.isEmpty() || props.contains(entry.getKey())) {
                                        map.put(entry.getKey(), entry.getValue());
                                    }
                                }
                            }
                            results.addNflowExecutionContext(depNflowSystemName, job.getJobExecutionId(), job.getStartTime(), endTime, map);


                        }
                    } else {
                        results.getDependentNflowNames().add(depNflowSystemName);
                    }

                }
            }

            return results;
        } else {
            throw new NflowNotFoundExcepton(nflowId);
        }
    }

    /*
    @Override
    public Map<DateTime, Map<String, Object>> getAllResults(NflowOperationCriteria criteria, Set<String> props) {
        Map<DateTime, Map<String, Object>> results = new HashMap<DateTime, Map<String,Object>>();
       // List<NflowOperation> ops = find(criteria);
        
        for (NflowOperation op : ops) {
            DateTime time = op.getStopTime();
            Map<String, Object> map = results.get(time);
            
            for (Entry<String, Object> entry : op.getResults().entrySet()) {
                if (props.isEmpty() || props.contains(entry.getKey())) {
                    if (map == null) {
                        map = new HashMap<>();
                        results.put(time, map);
                    }
                    
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return results;
    }


    private NflowOperationExecutedJobWrapper createOperation(ExecutedJob exec) {
        Long id = exec.getExecutionId();
        // TODO Inefficient
        ExecutedJob fullExec = this.jobRepo.findByExecutionId(id.toString());
        return new NflowOperationExecutedJobWrapper(fullExec);
    }
      */


    private NflowOperationBatchJobExecutionJobWrapper createOperation(BatchJobExecution exec) {
        return new NflowOperationBatchJobExecutionJobWrapper(exec);
    }


    private class Criteria extends AbstractMetadataCriteria<NflowOperationCriteria> implements NflowOperationCriteria, Predicate<ExecutedJob> {

        private Set<State> states = new HashSet<>();
        private Set<Nflow.ID> nflowIds = new HashSet<>();
        private DateTime startedBefore;
        private DateTime startedSince;
        private DateTime stoppedBefore;
        private DateTime stoppedSince;

        // TODO This is a temporary filtering solution.  Replace with an implementation that uses
        // the criteria to create SQL.
        @Override
        public boolean test(ExecutedJob job) {
            Nflow.ID id = null;

            if (!nflowIds.isEmpty()) {
                String[] jobName = job.getJobName().split("\\.");
                Nflow nflow = nflowProvider.findBySystemName(jobName[0], jobName[1]);
                id = nflow != null ? nflow.getId() : null;
            }

            return
                (states.isEmpty() || states.contains(asOperationState(job.getStatus()))) &&
                (nflowIds.isEmpty() || (id != null && nflowIds.contains(id))) &&
                (this.startedBefore == null || this.startedBefore.isAfter(job.getStartTime())) &&
                (this.startedSince == null || this.startedSince.isBefore(job.getStartTime())) &&
                (this.stoppedBefore == null || this.stoppedBefore.isAfter(job.getEndTime())) &&
                (this.stoppedSince == null || this.stoppedSince.isBefore(job.getEndTime()));
        }

        @Override
        public NflowOperationCriteria state(State... states) {
            this.states.addAll(Arrays.asList(states));
            return this;
        }

        @Override
        public NflowOperationCriteria nflow(Nflow.ID... nflowIds) {
            this.nflowIds.addAll(Arrays.asList(nflowIds));
            return this;
        }

        @Override
        public NflowOperationCriteria startedSince(DateTime time) {
            this.startedSince = time;
            return this;
        }

        @Override
        public NflowOperationCriteria startedBefore(DateTime time) {
            this.startedBefore = time;
            return this;
        }

        @Override
        public NflowOperationCriteria startedBetween(DateTime after, DateTime before) {
            startedSince(after);
            startedBefore(before);
            return this;
        }

        @Override
        public NflowOperationCriteria stoppedSince(DateTime time) {
            this.stoppedSince = time;
            return this;
        }

        @Override
        public NflowOperationCriteria stoppedBefore(DateTime time) {
            this.stoppedBefore = time;
            return this;
        }

        @Override
        public NflowOperationCriteria stoppedBetween(DateTime after, DateTime before) {
            stoppedSince(after);
            stoppedBefore(before);
            return this;
        }
    }
}
