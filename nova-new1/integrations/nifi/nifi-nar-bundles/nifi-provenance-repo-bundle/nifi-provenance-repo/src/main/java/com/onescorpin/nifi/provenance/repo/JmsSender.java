package com.onescorpin.nifi.provenance.repo;

/*-
 * #%L
 * onescorpin-nifi-provenance-repo
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

import com.onescorpin.nifi.provenance.jms.ProvenanceEventJmsWriter;
import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTO;
import com.onescorpin.nifi.provenance.model.ProvenanceEventRecordDTOHolder;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatistics;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatisticsHolder;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatisticsHolderV2;
import com.onescorpin.nifi.provenance.model.stats.AggregatedNflowProcessorStatisticsHolderV3;
import com.onescorpin.nifi.provenance.util.SpringApplicationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Send data to Ops Manager
 */
public class JmsSender {

    private static final Logger log = LoggerFactory.getLogger(JmsSender.class);

    List<ProvenanceEventRecordDTO> eventsToSend = null;
    List<AggregatedNflowProcessorStatistics> statsToSend = null;
    Map<String,Long> processorIdRunningFlows = new HashMap<>();

    public JmsSender(List<ProvenanceEventRecordDTO> eventsToSend, Collection<AggregatedNflowProcessorStatistics> statsToSend, Map<String,Long> processorIdRunningFlows) {
        this.eventsToSend = eventsToSend;
        if (statsToSend != null) {
            this.statsToSend = new ArrayList<>(statsToSend);
        }
        if(processorIdRunningFlows != null){
            this.processorIdRunningFlows = processorIdRunningFlows;
        }
    }

    public void run() {

        try {
            if (eventsToSend != null && !eventsToSend.isEmpty()) {
                ProvenanceEventRecordDTOHolder eventRecordDTOHolder = new ProvenanceEventRecordDTOHolder();
                eventRecordDTOHolder.setEvents(eventsToSend);
                getProvenanceEventActiveMqWriter().writeBatchEvents(eventRecordDTOHolder);
            }

            if (statsToSend != null && !statsToSend.isEmpty()) {
                AggregatedNflowProcessorStatisticsHolderV3 statsHolder = new AggregatedNflowProcessorStatisticsHolderV3();
                statsHolder.setProcessorIdRunningFlows(processorIdRunningFlows);
                statsHolder.setCollectionId(statsToSend.get(0).getCollectionId());
                statsHolder.setNflowStatistics(statsToSend);
                getProvenanceEventActiveMqWriter().writeStats(statsHolder);
            }

            //if there are no events to send then send off the running flows map
            if(eventsToSend == null && statsToSend == null )  {
                log.info("Sending Running Flow counts statistics for nflows to JMS");
                AggregatedNflowProcessorStatisticsHolderV3 statsHolder = new AggregatedNflowProcessorStatisticsHolderV3();
                statsHolder.setProcessorIdRunningFlows(processorIdRunningFlows);
                statsHolder.setCollectionId(UUID.randomUUID().toString());
                statsHolder.setNflowStatistics(statsToSend);
                getProvenanceEventActiveMqWriter().writeStats(statsHolder);
            }


        } catch (Exception e) {
            log.error("Error writing provenance events to JMS", e);
        }
    }

    public ProvenanceEventJmsWriter getProvenanceEventActiveMqWriter() {
        ProvenanceEventJmsWriter provenanceEventJmsWriter = SpringApplicationContext.getInstance().getBean(ProvenanceEventJmsWriter.class);
        if (provenanceEventJmsWriter == null) {
            log.error("!!!!!!!ProvenanceEventJmsWriter is NULL !!!!!!");
        }
        return provenanceEventJmsWriter;
    }
}
