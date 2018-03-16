package com.onescorpin.metadata.server;

/*-
 * #%L
 * onescorpin-metadata-rest-controller
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
import com.onescorpin.metadata.api.MetadataAction;
import com.onescorpin.metadata.api.MetadataCommand;
import com.onescorpin.metadata.api.MetadataExecutionException;
import com.onescorpin.metadata.api.MetadataRollbackAction;
import com.onescorpin.metadata.api.MetadataRollbackCommand;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.core.dataset.InMemoryDatasourceProvider;
import com.onescorpin.metadata.core.nflow.InMemoryNflowProvider;
import com.onescorpin.metadata.core.sla.TestMetricAssessor;
import com.onescorpin.metadata.core.sla.WithinScheduleAssessor;
import com.onescorpin.metadata.core.sla.nflow.DatasourceUpdatedSinceAssessor;
import com.onescorpin.metadata.core.sla.nflow.DatasourceUpdatedSinceNflowExecutedAssessor;
import com.onescorpin.metadata.core.sla.nflow.NflowExecutedSinceNflowAssessor;
import com.onescorpin.metadata.core.sla.nflow.NflowExecutedSinceScheduleAssessor;
import com.onescorpin.metadata.event.jms.JmsChangeEventDispatcher;
import com.onescorpin.metadata.event.jms.MetadataJmsConfig;
import com.onescorpin.metadata.event.reactor.ReactorConfiguration;
import com.onescorpin.metadata.modeshape.ModeShapeEngineConfig;
import com.onescorpin.metadata.sla.spi.MetricAssessor;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.metadata.sla.spi.ServiceLevelAssessor;
import com.onescorpin.metadata.sla.spi.core.InMemorySLAProvider;
import com.onescorpin.metadata.sla.spi.core.SimpleServiceLevelAssessor;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.security.Principal;

@Configuration
@EnableAutoConfiguration
@Import({ReactorConfiguration.class, MetadataJmsConfig.class, ModeShapeEngineConfig.class})
public class ServerConfiguration {

    @Bean
    @Profile("metadata.memory-only")
    public NflowProvider nflowProvider() {
        return new InMemoryNflowProvider();
    }

    @Bean
    @Profile("metadata.memory-only")
    public DatasourceProvider datasetProvider() {
        return new InMemoryDatasourceProvider();
    }

    @Bean
    @Profile("metadata.memory-only")
    public MetadataAccess metadataAccess() {
        // Transaction behavior not enforced in memory-only mode;
        return new MetadataAccess() {
            @Override
            public <R> R commit(MetadataCommand<R> cmd, Principal... principals) {
                try {
                    return cmd.execute();
                } catch (Exception e) {
                    throw new MetadataExecutionException(e);
                }
            }

            @Override
            public <R> R read(MetadataCommand<R> cmd, Principal... principals) {
                try {
                    return cmd.execute();
                } catch (Exception e) {
                    throw new MetadataExecutionException(e);
                }
            }

            @Override
            public void commit(MetadataAction action, Principal... principals) {
                try {
                    action.execute();
                } catch (Exception e) {
                    throw new MetadataExecutionException(e);
                }
            }

            @Override
            public <R> R commit(MetadataCommand<R> cmd, MetadataRollbackCommand rollbackCmd, Principal... principals) {
                return commit(cmd, principals);
            }

            @Override
            public void commit(MetadataAction action, MetadataRollbackAction rollbackAction, Principal... principals) {
                commit(action, principals);
            }

            @Override
            public void read(MetadataAction cmd, Principal... principals) {
                try {
                    cmd.execute();
                } catch (Exception e) {
                    throw new MetadataExecutionException(e);
                }
            }
        };
    }

    @Bean
    public JmsChangeEventDispatcher changeEventDispatcher() {
        return new JmsChangeEventDispatcher();
    }


    @Profile("metadata.memory-only")
    public ServiceLevelAgreementProvider slaProvider() {
        return new InMemorySLAProvider();
    }

    @Bean
    @Profile("metadata.memory-only")
    public ServiceLevelAssessor slaAssessor() {
        SimpleServiceLevelAssessor assr = new SimpleServiceLevelAssessor();
        assr.registerMetricAssessor(datasetUpdatedSinceMetricAssessor());
        assr.registerMetricAssessor(nflowExecutedSinceNflowMetricAssessor());
        assr.registerMetricAssessor(nflowExecutedSinceScheduleMetricAssessor());
        assr.registerMetricAssessor(datasourceUpdatedSinceNflowExecutedAssessor());
        assr.registerMetricAssessor(withinScheduleAssessor());
        return assr;
    }

    @Bean
    public ServerConfigurationInitialization serverConfigurationInitialization() {
        return new ServerConfigurationInitialization();
    }

    @Bean
    public MetricAssessor<?, ?> nflowExecutedSinceNflowMetricAssessor() {
        return new NflowExecutedSinceNflowAssessor();
    }

    @Bean
    public MetricAssessor<?, ?> datasetUpdatedSinceMetricAssessor() {
        return new DatasourceUpdatedSinceAssessor();
    }

    @Bean
    public MetricAssessor<?, ?> datasourceUpdatedSinceNflowExecutedAssessor() {
        return new DatasourceUpdatedSinceNflowExecutedAssessor();
    }

    @Bean
    public MetricAssessor<?, ?> nflowExecutedSinceScheduleMetricAssessor() {
        return new NflowExecutedSinceScheduleAssessor();
    }

    @Bean
    public MetricAssessor<?, ?> withinScheduleAssessor() {
        return new WithinScheduleAssessor();
    }

    @Bean
    public MetricAssessor<?, ?> testMetricAssessor() {
        return new TestMetricAssessor();
    }
}
