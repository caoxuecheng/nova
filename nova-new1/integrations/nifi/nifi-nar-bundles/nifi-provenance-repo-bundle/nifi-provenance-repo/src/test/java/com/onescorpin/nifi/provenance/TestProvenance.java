package com.onescorpin.nifi.provenance;/*-
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

import com.onescorpin.nifi.provenance.repo.NflowEventStatistics;
import com.onescorpin.nifi.provenance.repo.NflowStatisticsManager;

import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by sr186054 on 6/8/17.
 */
public class TestProvenance {

    private static final Logger log = LoggerFactory.getLogger(TestProvenance.class);

    private NflowStatisticsManager nflowStatisticsManager;
    SimulateNiFiFlow nifiFlow = null;


    public TestProvenance() {
        init();
    }

    private void init() {
        nflowStatisticsManager = NflowStatisticsManager.getInstance();


        this.nifiFlow = new SimulateNiFiFlow(this.nflowStatisticsManager);
    }


    public void run() {

        int maxNflowFlows = 2000;
        AtomicInteger count = new AtomicInteger(maxNflowFlows);
        DateTime startTime = DateTime.now();
        while (count.get() > 0) {
            this.nifiFlow.createSplit();
            count.decrementAndGet();
        }
        log.info("Flows processed: {}, TotalTime: {}, skippedEvents:{} ", maxNflowFlows - count.get(), (DateTime.now().getMillis() - startTime.getMillis()), nifiFlow.getSkippedCount());


    }

    @Test
    public void testIt() {
        run();
        //nflowStatisticsManager.send();
        Double avgTime = nifiFlow.averageTime();
        NflowEventStatistics s = NflowEventStatistics.getInstance();
        int i = 0;
    }


}
