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


import org.apache.nifi.authorization.Authorizer;
import org.apache.nifi.events.EventReporter;
import org.apache.nifi.provenance.IdentifierLookup;
import org.apache.nifi.provenance.ProvenanceAuthorizableFactory;
import org.apache.nifi.provenance.ProvenanceEventRecord;
import org.apache.nifi.provenance.ProvenanceRepository;
import org.apache.nifi.provenance.VolatileProvenanceRepository;
import org.apache.nifi.util.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Nova Provenance Event Repository This will intercept NiFi Provenance Events via the NovaRecordWriterDelegate and send them to Ops Manager
 */
public class NovaVolatileProvenanceEventRepository extends VolatileProvenanceRepository implements ProvenanceRepository {

    private static final Logger log = LoggerFactory.getLogger(NovaVolatileProvenanceEventRepository.class);

    private NovaProvenanceEventRepositoryUtil provenanceEventRepositoryUtil = new NovaProvenanceEventRepositoryUtil();

    public NovaVolatileProvenanceEventRepository() {
        super();
    }

    private final AtomicLong idGenerator = new AtomicLong(0L);


    public NovaVolatileProvenanceEventRepository(NiFiProperties nifiProperties) throws IOException {
        super(nifiProperties);
        init();
    }


    private void init() {
        log.info("Initializing NovaVolatileProvenanceEventRepository");
        provenanceEventRepositoryUtil.init();
        //initialize the manager to gather and send the statistics
        NflowStatisticsManager.getInstance();
    }

    public void registerEvent(ProvenanceEventRecord event) {
        super.registerEvent(event);
        NflowStatisticsManager.getInstance().addEvent(event, idGenerator.getAndIncrement());
    }


    @Override
    public synchronized void close() throws IOException {
        super.close();
        provenanceEventRepositoryUtil.persistNflowEventStatisticsToDisk();
    }


    @Override
    public void initialize(EventReporter eventReporter, Authorizer authorizer, ProvenanceAuthorizableFactory resourceFactory, IdentifierLookup idLookup) throws IOException {
        super.initialize(eventReporter, authorizer, resourceFactory, idLookup);
    }
}
