package com.onescorpin.metadata.config;

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

import com.onescorpin.alerts.api.AlertProvider;
import com.onescorpin.metadata.cache.AlertsCache;
import com.onescorpin.metadata.cache.CacheService;
import com.onescorpin.metadata.cache.CategoryNflowService;
import com.onescorpin.metadata.cache.DataConfidenceJobsCache;
import com.onescorpin.metadata.cache.NflowHealthSummaryCache;
import com.onescorpin.metadata.cache.RunningJobsCache;
import com.onescorpin.metadata.cache.ServiceStatusCache;
import com.onescorpin.metadata.jobrepo.StreamingNflowService;
import com.onescorpin.metadata.jobrepo.nifi.provenance.NifiBulletinExceptionExtractor;
import com.onescorpin.metadata.jobrepo.nifi.provenance.NifiStatsJmsReceiver;
import com.onescorpin.metadata.jobrepo.nifi.provenance.ProvenanceEventNflowUtil;
import com.onescorpin.metadata.jobrepo.nifi.provenance.ProvenanceEventReceiver;
import com.onescorpin.metadata.jobrepo.nifi.provenance.RetryProvenanceEventWithDelay;
import com.onescorpin.metadata.sla.DefaultServiceLevelAgreementScheduler;
import com.onescorpin.metadata.sla.JpaJcrServiceLevelAgreementChecker;
import com.onescorpin.metadata.sla.ServiceLevelAgreementActionAlertResponderFactory;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementChecker;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementScheduler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring configuration class for the beans in the current module
 */
@Configuration
public class OperationalMetadataServiceSpringConfiguration {



    @Bean
    public ServiceLevelAgreementScheduler serviceLevelAgreementScheduler() {
        return new DefaultServiceLevelAgreementScheduler();
    }

    @Bean
    public ServiceLevelAgreementChecker serviceLevelAgreementChecker() {
        return new JpaJcrServiceLevelAgreementChecker();
    }

    @Bean(name = "slaActionAlertResponder")
    public ServiceLevelAgreementActionAlertResponderFactory slaActionResponder(@Qualifier("alertProvider") AlertProvider alertProvider) {
        ServiceLevelAgreementActionAlertResponderFactory responder = new ServiceLevelAgreementActionAlertResponderFactory();
        alertProvider.addResponder(responder);
        return responder;
    }

    @Bean
    public ProvenanceEventNflowUtil provenanceEventNflowUtil(){
        return new ProvenanceEventNflowUtil();
    }

    @Bean
    @Profile("!novaUpgrade")
    public RetryProvenanceEventWithDelay retryProvenanceEventWithDelay() {
            return new RetryProvenanceEventWithDelay();
    }

    @Bean
    @Profile("!novaUpgrade")
    public ProvenanceEventReceiver provenanceEventReceiver(){
        return new ProvenanceEventReceiver();
    }

    @Bean
    @Profile("!novaUpgrade")
    public NifiStatsJmsReceiver nifiStatsJmsReceiver() {
        return new NifiStatsJmsReceiver();
    }

    @Bean
    public NifiBulletinExceptionExtractor nifiBulletinExceptionExtractor(){
        return new NifiBulletinExceptionExtractor();
    }



    @Bean
    public CacheService cacheService(){
        return new CacheService();
    }
    @Bean
    public AlertsCache alertsCache(){
        return new AlertsCache();
    }
    @Bean
    public DataConfidenceJobsCache dataConfidenceJobsCache(){
        return new DataConfidenceJobsCache();
    }
    @Bean
    public NflowHealthSummaryCache nflowHealthSummaryCache(){
        return new NflowHealthSummaryCache();
    }
    @Bean
    public RunningJobsCache runningJobsCache(){
        return new RunningJobsCache();
    }
    @Bean
    public ServiceStatusCache serviceStatusCache(){
        return new ServiceStatusCache();
    }

    @Bean
    public CategoryNflowService categoryNflowService() {
        return new CategoryNflowService();
    }

}
