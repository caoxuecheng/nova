package com.onescorpin.metadata.jobrepo.nifi.provenance;

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
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStatisticsProvider;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementChecker;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Map;

import javax.inject.Inject;

/**
 * Quartz Scheduled Job Bean that will call the procedure to compact the statistcs table
 */
public class NiFiStatsCompactionQuartzJobBean extends QuartzJobBean{
    private static final Logger log = LoggerFactory.getLogger(NiFiStatsCompactionQuartzJobBean.class);
    @Inject
    NifiNflowProcessorStatisticsProvider nflowProcessorStatisticsProvider;

    @Inject
    private MetadataAccess metadataAccess;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        final Map<String,   Object> jobDataMap = context.getMergedJobDataMap();

        String results = metadataAccess.commit(() -> {
            return nflowProcessorStatisticsProvider.compactNflowProcessorStatistics();
        }, MetadataAccess.SERVICE);
    }
}
