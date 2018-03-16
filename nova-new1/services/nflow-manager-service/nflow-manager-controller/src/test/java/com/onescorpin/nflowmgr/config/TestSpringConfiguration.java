package com.onescorpin.nflowmgr.config;

/*-
 * #%L
 * onescorpin-nflow-manager-controller
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

import com.onescorpin.app.ServicesApplicationStartup;
import com.onescorpin.cluster.ClusterService;
import com.onescorpin.cluster.JGroupsClusterService;
import com.onescorpin.common.velocity.service.InMemoryVelocityTemplateProvider;
import com.onescorpin.common.velocity.service.VelocityTemplateProvider;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementActionTemplateProvider;
import com.onescorpin.nifi.rest.NiFiObjectCache;
import com.onescorpin.nflowmgr.nifi.NifiConnectionService;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCache;
import com.onescorpin.nflowmgr.nifi.PropertyExpressionResolver;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCacheClusterManager;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCacheImpl;
import com.onescorpin.nflowmgr.rest.Model;
import com.onescorpin.nflowmgr.service.MetadataService;
import com.onescorpin.nflowmgr.service.category.CategoryModelTransform;
import com.onescorpin.nflowmgr.service.category.NflowManagerCategoryService;
import com.onescorpin.nflowmgr.service.category.InMemoryNflowManagerCategoryService;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerNflowService;
import com.onescorpin.nflowmgr.service.nflow.NflowModelTransform;
import com.onescorpin.nflowmgr.service.nflow.InMemoryNflowManagerNflowService;
import com.onescorpin.nflowmgr.service.template.NflowManagerTemplateService;
import com.onescorpin.nflowmgr.service.template.InMemoryNflowManagerTemplateService;
import com.onescorpin.nflowmgr.service.template.NiFiTemplateCache;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateCache;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateService;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateUtil;
import com.onescorpin.nflowmgr.service.template.TemplateModelTransform;
import com.onescorpin.nflowmgr.sla.DefaultServiceLevelAgreementService;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementModelTransform;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementService;
import com.onescorpin.hive.service.HiveService;
import com.onescorpin.kerberos.KerberosTicketConfiguration;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.MetadataAction;
import com.onescorpin.metadata.api.MetadataCommand;
import com.onescorpin.metadata.api.MetadataExecutionException;
import com.onescorpin.metadata.api.MetadataRollbackAction;
import com.onescorpin.metadata.api.MetadataRollbackCommand;
import com.onescorpin.metadata.api.app.NovaVersionProvider;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.security.HadoopSecurityGroupProvider;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreementProvider;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementDescriptionProvider;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.core.dataset.InMemoryDatasourceProvider;
import com.onescorpin.metadata.core.nflow.InMemoryNflowProvider;
import com.onescorpin.metadata.jpa.cluster.NiFiFlowCacheClusterUpdateProvider;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.MetadataJcrConfigurator;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementScheduler;
import com.onescorpin.metadata.sla.spi.core.InMemorySLAProvider;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.nifi.rest.client.NiFiRestClient;
import com.onescorpin.nifi.rest.client.NifiRestClientConfig;
import com.onescorpin.nifi.rest.model.NiFiPropertyDescriptorTransform;
import com.onescorpin.nifi.v1.rest.client.NiFiRestClientV1;
import com.onescorpin.nifi.v1.rest.model.NiFiPropertyDescriptorTransformV1;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.core.encrypt.EncryptionService;
import com.onescorpin.security.rest.controller.SecurityModelTransform;
import com.onescorpin.security.service.user.UserService;
import com.onescorpin.spring.SpringEnvironmentProperties;

import org.mockito.Mockito;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.api.txn.TransactionManagerLookup;
import org.springframework.cloud.config.server.encryption.EncryptionController;
import org.springframework.cloud.config.server.encryption.SingleTextEncryptorLocator;
import org.springframework.cloud.config.server.encryption.TextEncryptorLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.security.Principal;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.Repository;

/**
 */
@Configuration
public class TestSpringConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() throws Exception {
        final PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        Properties properties = new Properties();
        properties.setProperty("nifi.remove.inactive.versioned.nflows", "true");
        propertySourcesPlaceholderConfigurer.setProperties(properties);
        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    public AccessController accessController() {
        return Mockito.mock(AccessController.class);
    }

    @Bean
    public NflowServiceLevelAgreementProvider nflowServiceLevelAgreementProvider() {
        return Mockito.mock(NflowServiceLevelAgreementProvider.class);
    }

    @Bean
    public ServiceLevelAgreementService serviceLevelAgreementService() {
        return new DefaultServiceLevelAgreementService();
    }

    @Bean
    public ServiceLevelAgreementProvider serviceLevelAgreementProvider() {
        return new InMemorySLAProvider();
    }

    @Bean
    public NifiFlowCache nifiFlowCache() {
        return new NifiFlowCacheImpl();
    }

    @Bean
    public ModeShapeEngine modeShapeEngine() {
        return Mockito.mock(ModeShapeEngine.class);
    }

    @Bean
    public MetadataJcrConfigurator metadataJcrConfigurator() {
        return Mockito.mock(MetadataJcrConfigurator.class);
    }

    @Bean
    public MetadataService metadataService() {
        return Mockito.mock(MetadataService.class);
    }

    @Bean
    public NifiConnectionService nifiConnectionService() {
        return new NifiConnectionService();
    }

    @Bean
    public ServiceLevelAgreementScheduler serviceLevelAgreementScheduler() {
        return new ServiceLevelAgreementScheduler() {
            @Override
            public void scheduleServiceLevelAgreement(ServiceLevelAgreement sla) {

            }

            @Override
            public void enableServiceLevelAgreement(ServiceLevelAgreement sla) {

            }

            @Override
            public void disableServiceLevelAgreement(ServiceLevelAgreement sla) {

            }

            @Override
            public boolean unscheduleServiceLevelAgreement(ServiceLevelAgreement.ID slaId) {
                return false;
            }

            @Override
            public boolean unscheduleServiceLevelAgreement(ServiceLevelAgreement sla) {
                return false;
            }
        };
    }

    @Bean
    NflowProvider nflowProvider() {
        return new InMemoryNflowProvider();
    }

    @Bean(name = "metadataJcrRepository")
    public Repository repository() {
        return Mockito.mock(Repository.class);
    }

    @Bean
    public TransactionManagerLookup txnLookup() {
        return Mockito.mock(TransactionManagerLookup.class);
    }

    @Bean
    public JcrMetadataAccess jcrMetadataAccess() {
        // Transaction behavior not enforced in memory-only mode;
        return new JcrMetadataAccess() {
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
            public <R> R commit(Credentials creds, MetadataCommand<R> cmd) {
                try {
                    return cmd.execute();
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
            public <R> R read(Credentials creds, MetadataCommand<R> cmd) {
                try {
                    return cmd.execute();
                } catch (Exception e) {
                    throw new MetadataExecutionException(e);
                }
            }
        };
    }

    @Bean
    MetadataAccess metadataAccess() {
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
    public DatasourceProvider datasetProvider() {
        return new InMemoryDatasourceProvider();
    }

    @Bean
    public NflowManagerNflowService nflowManagerNflowService() {
        return new InMemoryNflowManagerNflowService();
    }

    @Bean
    public NflowManagerCategoryService nflowManagerCategoryService() {
        return new InMemoryNflowManagerCategoryService();
    }

    @Bean
    NflowManagerTemplateService nflowManagerTemplateService() {
        return new InMemoryNflowManagerTemplateService();
    }

    @Bean
    NifiRestClientConfig nifiRestClientConfig() {
        return new NifiRestClientConfig();
    }

    @Bean
    PropertyExpressionResolver propertyExpressionResolver() {
        return new PropertyExpressionResolver();
    }

    @Bean
    SpringEnvironmentProperties springEnvironmentProperties() {
        return new SpringEnvironmentProperties();
    }

    @Bean
    public LegacyNifiRestClient legacyNifiRestClient() {
        return new LegacyNifiRestClient();
    }

    @Bean
    NiFiRestClient niFiRestClient() {
        return new NiFiRestClientV1(nifiRestClientConfig());
    }

    @Bean
    NiFiPropertyDescriptorTransform propertyDescriptorTransform() {
        return new NiFiPropertyDescriptorTransformV1();
    }


    @Bean
    RegisteredTemplateUtil registeredTemplateUtil() {
        return new RegisteredTemplateUtil();
    }

    @Bean
    RegisteredTemplateService registeredTemplateService() {
        return new RegisteredTemplateService();
    }

    @Bean
    public NiFiTemplateCache niFiTemplateCache() {
        return new NiFiTemplateCache();
    }

    @Bean
    NflowModelTransform nflowModelTransform() {
        return new NflowModelTransform();
    }

    @Bean
    CategoryModelTransform categoryModelTransform() {
        return new CategoryModelTransform();
    }

    @Bean
    CategoryProvider nflowManagerCategoryProvider() {
        return new Mockito().mock(CategoryProvider.class);
    }

    @Bean
    NflowManagerTemplateProvider nflowManagerTemplateProvider() {
        return new Mockito().mock(NflowManagerTemplateProvider.class);
    }

    @Bean(name = "hiveJdbcTemplate")
    JdbcTemplate hiveJdbcTemplate() {
        return new Mockito().mock(JdbcTemplate.class);
    }

    @Bean(name = "kerberosHiveConfiguration")
    KerberosTicketConfiguration kerberosHiveConfiguration() {
        return new KerberosTicketConfiguration();
    }

    @Bean
    HadoopSecurityGroupProvider hadoopSecurityGroupProvider() {
        return new Mockito().mock(HadoopSecurityGroupProvider.class);
    }

    @Bean
    HiveService hiveService() {
        return new Mockito().mock(HiveService.class);
    }

    @Bean
    TemplateModelTransform templateModelTransform() {
        return new TemplateModelTransform();
    }

    @Bean
    EncryptionService encryptionService() {
        return new EncryptionService();
    }

    @Bean
    TextEncryptor textEncryptor(){
        return textEncryptorLocator().locate(null);
    }

    @Bean
    TextEncryptorLocator textEncryptorLocator() {
        return new SingleTextEncryptorLocator(null);
    }

    @Bean
    EncryptionController encryptionController() {
        return new EncryptionController(textEncryptorLocator());
    }

    @Bean
    ServiceLevelAgreementModelTransform serviceLevelAgreementModelTransform() {
        return new ServiceLevelAgreementModelTransform(Mockito.mock(Model.class));
    }

    @Bean
    SecurityModelTransform actionsTransform() {
        return Mockito.mock(SecurityModelTransform.class);
    }

    @Bean
    UserService userService() {
        return Mockito.mock(UserService.class);
    }

    @Bean
    NovaVersionProvider novaVersionProvider() {
        return Mockito.mock(NovaVersionProvider.class);
    }

    @Bean
    ClusterService clusterService() {
        return new JGroupsClusterService();
    }

    @Bean
    NifiFlowCacheClusterManager nifiFlowCacheClusterManager() {
        return Mockito.mock(NifiFlowCacheClusterManager.class);
    }
    @Bean
    NiFiFlowCacheClusterUpdateProvider niFiFlowCacheClusterUpdateProvider(){
        return Mockito.mock(NiFiFlowCacheClusterUpdateProvider.class);
    }

    @Bean
    ServiceLevelAgreementDescriptionProvider serviceLevelAgreementDescriptionProvider(){
        return Mockito.mock(ServiceLevelAgreementDescriptionProvider.class);
    }


    @Bean
    public NiFiObjectCache createNflowBuilderCache(){
        return new NiFiObjectCache();
    }

    @Bean
    public RegisteredTemplateCache registeredTemplateCache() {
        return new RegisteredTemplateCache();
    }

    @Bean
    public ServicesApplicationStartup servicesApplicationStartup(){
        return Mockito.mock(ServicesApplicationStartup.class);
    }

    @Bean
    public VelocityTemplateProvider velocityTemplateProvider() {
        return new InMemoryVelocityTemplateProvider();
    }

    @Bean
    public ServiceLevelAgreementActionTemplateProvider serviceLevelAgreementActionTemplateProvider() {
        return Mockito.mock(ServiceLevelAgreementActionTemplateProvider.class);
    }

}
