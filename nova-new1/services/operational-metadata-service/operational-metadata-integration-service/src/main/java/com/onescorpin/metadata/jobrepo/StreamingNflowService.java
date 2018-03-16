package com.onescorpin.metadata.jobrepo;


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


import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowChangeEvent;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.jobrepo.ExecutionConstants;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecution;
import com.onescorpin.metadata.api.jobrepo.job.BatchJobExecutionProvider;
import com.onescorpin.metadata.jobrepo.nifi.provenance.ProvenanceEventNflowUtil;

import org.joda.time.DateTime;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Created by sr186054 on 6/17/17.
 * This is not used anymore.
 * The NiFiStatsJmsReceiver takes care of setting the streaming job as started/stopped
 */
@Deprecated
public class StreamingNflowService {

    @Inject
    private MetadataEventService metadataEventService;

    @Inject
    protected MetadataAccess metadataAccess;

    @Inject
    private BatchJobExecutionProvider batchJobExecutionProvider;

    @Inject
    OpsManagerNflowProvider opsManagerNflowProvider;

    @Inject
    ProvenanceEventNflowUtil provenanceEventNflowUtil;

    /**
     * Event listener for precondition events
     */
    private final MetadataEventListener<NflowChangeEvent> nflowPropertyChangeListener = new NflowChangeEventDispatcher();


    @PostConstruct
    public void addEventListener() {
        metadataEventService.addListener(nflowPropertyChangeListener);
    }


    private class NflowChangeEventDispatcher implements MetadataEventListener<NflowChangeEvent> {


        @Override
        public void notify(@Nonnull final NflowChangeEvent metadataEvent) {
            Optional<String> nflowName = metadataEvent.getData().getNflowName();
            Nflow.State state = metadataEvent.getData().getNflowState();
            if (nflowName.isPresent()) {
                metadataAccess.commit(() -> {
                    OpsManagerNflow nflow = opsManagerNflowProvider.findByNameWithoutAcl(nflowName.get());
                    if (nflow != null && nflow.isStream()) {
                        //update the job status
                        BatchJobExecution jobExecution = batchJobExecutionProvider.findLatestJobForNflow(nflowName.get());
                        if(jobExecution != null) {
                            if (state.equals(Nflow.State.ENABLED)) {
                                jobExecution.setStatus(BatchJobExecution.JobStatus.STARTED);
                                jobExecution.setExitCode(ExecutionConstants.ExitCode.EXECUTING);
                                jobExecution.setStartTime(DateTime.now());
                            } else {
                                jobExecution.setStatus(BatchJobExecution.JobStatus.STOPPED);
                                jobExecution.setExitCode(ExecutionConstants.ExitCode.COMPLETED);
                                jobExecution.setEndTime(DateTime.now());
                            }
                            batchJobExecutionProvider.save(jobExecution);
                        }
                    }
                }, MetadataAccess.SERVICE);
            }
        }

    }

}
