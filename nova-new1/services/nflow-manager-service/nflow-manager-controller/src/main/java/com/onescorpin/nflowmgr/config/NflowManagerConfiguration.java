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


import com.onescorpin.nflowmgr.nifi.PropertyExpressionResolver;
import com.onescorpin.nflowmgr.nifi.SpringCloudContextEnvironmentChangedListener;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCache;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCacheClusterManager;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCacheImpl;
import com.onescorpin.nflowmgr.rest.Model;
import com.onescorpin.nflowmgr.service.DefaultJobService;
import com.onescorpin.nflowmgr.service.NflowManagerMetadataService;
import com.onescorpin.nflowmgr.service.MetadataModelTransform;
import com.onescorpin.nflowmgr.service.MetadataService;
import com.onescorpin.nflowmgr.service.UploadProgressService;
import com.onescorpin.nflowmgr.service.category.CategoryModelTransform;
import com.onescorpin.nflowmgr.service.category.DefaultNflowManagerCategoryService;
import com.onescorpin.nflowmgr.service.category.NflowManagerCategoryService;
import com.onescorpin.nflowmgr.service.datasource.DatasourceModelTransform;
import com.onescorpin.nflowmgr.service.datasource.DatasourceService;
import com.onescorpin.nflowmgr.service.domaintype.DomainTypeTransform;
import com.onescorpin.nflowmgr.service.nflow.DefaultNflowManagerNflowService;
import com.onescorpin.nflowmgr.service.nflow.ExportImportNflowService;
import com.onescorpin.nflowmgr.service.nflow.NflowHiveTableService;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerNflowService;
import com.onescorpin.nflowmgr.service.nflow.NflowManagerPreconditionService;
import com.onescorpin.nflowmgr.service.nflow.NflowModelTransform;
import com.onescorpin.nflowmgr.service.nflow.datasource.DerivedDatasourceFactory;
import com.onescorpin.nflowmgr.service.security.DefaultSecurityService;
import com.onescorpin.nflowmgr.service.security.SecurityService;
import com.onescorpin.nflowmgr.service.template.DefaultNflowManagerTemplateService;
import com.onescorpin.nflowmgr.service.template.ExportImportTemplateService;
import com.onescorpin.nflowmgr.service.template.NflowManagerTemplateService;
import com.onescorpin.nflowmgr.service.template.NiFiTemplateCache;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateCache;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateService;
import com.onescorpin.nflowmgr.service.template.RegisteredTemplateUtil;
import com.onescorpin.nflowmgr.service.template.TemplateModelTransform;
import com.onescorpin.nflowmgr.sla.DefaultServiceLevelAgreementService;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementModelTransform;
import com.onescorpin.nflowmgr.sla.ServiceLevelAgreementService;
import com.onescorpin.hive.service.HiveService;
import com.onescorpin.jobrepo.service.JobService;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.core.nflow.NflowPreconditionService;
import com.onescorpin.nifi.rest.client.NiFiRestClient;
import com.onescorpin.security.core.encrypt.EncryptionService;
import com.onescorpin.spring.SpringEnvironmentProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Spring Bean configuration for nflow manager
 */
@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = {"com.onescorpin"})
public class NflowManagerConfiguration {

    @Inject
    private Environment env;

    public NflowManagerConfiguration() {
    }

    @Bean
    public NflowManagerNflowService nflowManagerNflowService() {
        return new DefaultNflowManagerNflowService();
    }

    @Bean
    public NflowManagerCategoryService nflowManagerCategoryService() {
        return new DefaultNflowManagerCategoryService();
    }


    @Bean
    public NflowManagerTemplateService nflowManagerTemplateService() {
        return new DefaultNflowManagerTemplateService();
    }

    @Bean(name = "metadataModelTransform")
    public MetadataModelTransform metadataTransform() {
        return new MetadataModelTransform();
    }

    @Bean
    public NflowModelTransform nflowModelTransformer() {
        return new NflowModelTransform();
    }


    @Bean
    public TemplateModelTransform templateModelTransform() {
        return new TemplateModelTransform();
    }


    @Bean
    public CategoryModelTransform categoryModelTransform() {
        return new CategoryModelTransform();
    }


    @Bean
    public NflowManagerPreconditionService nflowManagerPreconditionService() {
        return new NflowManagerPreconditionService();
    }

    @Bean
    public SpringEnvironmentProperties springEnvironmentProperties() {
        return new SpringEnvironmentProperties();
    }

    @Bean
    public SpringCloudContextEnvironmentChangedListener springEnvironmentChangedListener() {
        return new SpringCloudContextEnvironmentChangedListener();
    }

    @Bean
    public MetadataService metadataService() {
        return new NflowManagerMetadataService();
    }

    @Bean
    public SecurityService securityService() {
        return new DefaultSecurityService();
    }

    @Bean
    public ExportImportTemplateService exportImportTemplateService() {
        return new ExportImportTemplateService();
    }

    @Bean
    public ExportImportNflowService exportImportNflowService() {
        return new ExportImportNflowService();
    }

    @Bean
    public PropertyExpressionResolver propertyExpressionResolver() {
        return new PropertyExpressionResolver();
    }

    @Bean
    public NifiFlowCache nifiFlowCache() {
        return new NifiFlowCacheImpl();
    }


    @Bean
    public ServiceLevelAgreementService serviceLevelAgreementService() {
        return new DefaultServiceLevelAgreementService();
    }


    @Bean
    public NflowPreconditionService nflowPreconditionService() {
        return new NflowPreconditionService();
    }


    @Bean
    public DatasourceService datasourceService() {
        return new DatasourceService();
    }

    @Bean
    public DerivedDatasourceFactory derivedDatasourceFactory() {
        return new DerivedDatasourceFactory();
    }

    @Bean
    public JobService jobService() {
        return new DefaultJobService();
    }

    @Bean
    public EncryptionService encryptionService() {
        return new EncryptionService();
    }

    @Bean
    public RegisteredTemplateService registeredTemplateService() {
        return new RegisteredTemplateService();
    }

    @Bean
    public RegisteredTemplateUtil registeredTemplateUtil() {
        return new RegisteredTemplateUtil();
    }

    @Bean
    public NiFiTemplateCache niFiTemplateCache() {
        return new NiFiTemplateCache();
    }

    @Bean
    public UploadProgressService uploadProgressService() {
        return new UploadProgressService();
    }

    /**
     * Transforms objects between {@link com.onescorpin.metadata.rest.model.data.Datasource} and {@link com.onescorpin.metadata.api.datasource.Datasource}.
     *
     * @param datasourceProvider the {@link com.onescorpin.metadata.api.datasource.Datasource} provider
     * @param textEncryptor      the encryption provider
     * @param niFiRestClient     the NiFi REST client
     * @param securityService    the security service
     * @return the model transformer
     */
    @Bean
    @Nonnull
    public DatasourceModelTransform datasourceModelTransform(@Nonnull final DatasourceProvider datasourceProvider, @Nonnull final TextEncryptor textEncryptor,
                                                             @Nonnull final NiFiRestClient niFiRestClient, @Nonnull final SecurityService securityService) {
        return new DatasourceModelTransform(datasourceProvider, textEncryptor, niFiRestClient, securityService);
    }

    /**
     * Transforms objects between different nflow models and domain objects.
     *
     * @param datasourceTransform the {@code Datasource} object transformer
     * @return the model transformer
     */
    @Bean
    @Nonnull
    public Model model(@Nonnull final DatasourceModelTransform datasourceTransform) {
        return new Model(datasourceTransform);
    }

    /**
     * Transforms SLA objects between the REST model and the domain object.
     *
     * @param model the model transformer
     * @return the SLA transformer
     */
    @Bean
    @Nonnull
    public ServiceLevelAgreementModelTransform serviceLevelAgreementModelTransform(@Nonnull final Model model) {
        return new ServiceLevelAgreementModelTransform(model);
    }


    @Bean
    public NifiFlowCacheClusterManager nifiFlowCacheClusterManager() {
        return new NifiFlowCacheClusterManager();
    }

    /**
     * Transforms domain type objects between the REST and domain models.
     */
    @Bean
    public DomainTypeTransform domainTypeTransform() {
        return new DomainTypeTransform();
    }

    @Bean
    public NflowHiveTableService nflowHiveTableService(@Nonnull final HiveService hiveService) {
        return new NflowHiveTableService(hiveService);
    }

    @Bean
    public RegisteredTemplateCache registeredTemplateCache() {
        return new RegisteredTemplateCache();
    }
}
