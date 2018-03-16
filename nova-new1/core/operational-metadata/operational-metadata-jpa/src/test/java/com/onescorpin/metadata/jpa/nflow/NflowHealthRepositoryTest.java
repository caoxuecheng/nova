package com.onescorpin.metadata.jpa.nflow;

/*-
 * #%L
 * nova-operational-metadata-jpa
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

import com.onescorpin.metadata.config.OperationalMetadataConfig;
import com.onescorpin.metadata.core.nflow.BaseNflow;
import com.onescorpin.metadata.jpa.TestJpaConfiguration;
import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlConfig;
import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;
import com.onescorpin.metadata.jpa.nflow.security.JpaNflowOpsAclEntry;
import com.onescorpin.security.AccessController;
import com.onescorpin.spring.CommonsSpringConfiguration;
import com.onescorpin.test.security.WithMockJaasUser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

@SuppressWarnings("SpringJavaAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:test-application.properties")
@SpringApplicationConfiguration(classes = {CommonsSpringConfiguration.class,
                                           OperationalMetadataConfig.class,
                                           TestJpaConfiguration.class,
                                           NflowHealthRepositoryTest.class,
                                           NflowOpsAccessControlConfig.class})
@Transactional
@Configuration
public class NflowHealthRepositoryTest {

    @Bean
    public AccessController accessController() {
        AccessController mock = Mockito.mock(AccessController.class);
        Mockito.when(mock.isEntityAccessControlled()).thenReturn(true);
        return mock;
    }

    @Inject
    NflowHealthRepository repo;

    @Inject
    NflowOpsAccessControlRepository aclRepo;

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAll_NoMatchingGroupAclEntry() throws Exception {
        UUID uuid = UUID.randomUUID();

        JpaOpsManagerNflowHealth health = new JpaOpsManagerNflowHealth();
        health.setNflowId(new JpaOpsManagerNflowHealth.OpsManagerNflowHealthNflowId(uuid));

        repo.save(health);

        BaseNflow.NflowId healthId = new BaseNflow.NflowId(uuid);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(healthId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);

        Iterable<JpaOpsManagerNflowHealth> all = repo.findAll();
        Assert.assertFalse(StreamSupport.stream(all.spliterator(), false)
                               .anyMatch(it -> it.getNflowId().getUuid().equals(uuid)));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAll_WithMatchingGroupAclEntry() throws Exception {
        UUID uuid = UUID.randomUUID();

        JpaOpsManagerNflowHealth health = new JpaOpsManagerNflowHealth();
        health.setNflowId(new JpaOpsManagerNflowHealth.OpsManagerNflowHealthNflowId(uuid));

        repo.save(health);

        BaseNflow.NflowId healthId = new BaseNflow.NflowId(uuid);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(healthId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);

        Iterable<JpaOpsManagerNflowHealth> all = repo.findAll();
        Assert.assertTrue(StreamSupport.stream(all.spliterator(), false)
                              .anyMatch(it -> it.getNflowId().getUuid().equals(uuid)));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAll_WithMatchingUserAclEntry() throws Exception {
        UUID uuid = UUID.randomUUID();

        JpaOpsManagerNflowHealth health = new JpaOpsManagerNflowHealth();
        health.setNflowId(new JpaOpsManagerNflowHealth.OpsManagerNflowHealthNflowId(uuid));

        repo.save(health);

        BaseNflow.NflowId healthId = new BaseNflow.NflowId(uuid);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(healthId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(nonMatching);

        Iterable<JpaOpsManagerNflowHealth> all = repo.findAll();
        Assert.assertTrue(StreamSupport.stream(all.spliterator(), false)
                              .anyMatch(it -> it.getNflowId().getUuid().equals(uuid)));
    }

}
