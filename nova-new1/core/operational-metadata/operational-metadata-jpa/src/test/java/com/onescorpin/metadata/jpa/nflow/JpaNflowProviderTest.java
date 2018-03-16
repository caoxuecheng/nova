package com.onescorpin.metadata.jpa.nflow;

/*-
 * #%L
 * onescorpin-operational-metadata-jpa
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

import com.onescorpin.DateTimeUtil;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.jobrepo.job.JobStatusCount;
import com.onescorpin.metadata.config.OperationalMetadataConfig;
import com.onescorpin.metadata.core.nflow.BaseNflow;
import com.onescorpin.metadata.jpa.TestJpaConfiguration;
import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;
import com.onescorpin.metadata.jpa.nflow.security.JpaNflowOpsAclEntry;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.GroupPrincipal;
import com.onescorpin.security.UsernamePrincipal;
import com.onescorpin.spring.CommonsSpringConfiguration;
import com.onescorpin.test.security.WithMockJaasUser;

import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:test-application.properties")
@SpringApplicationConfiguration(classes = {CommonsSpringConfiguration.class, OperationalMetadataConfig.class, TestJpaConfiguration.class, JpaNflowProviderTest.class})
public class JpaNflowProviderTest {

    @Inject
    private NflowOpsAccessControlProvider aclProvider;

    @Inject
    private OpsNflowManagerNflowProvider nflowProvider;

    @Inject
    private MetadataAccess metadataAccess;

    @Bean
    public AccessController accessController() {
        AccessController mock = Mockito.mock(AccessController.class);
        Mockito.when(mock.isEntityAccessControlled()).thenReturn(true);
        return mock;
    }
    

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin"})
    @Test
    public void testFindNflowUsingGenericFilter() {
        // Create nflow
        final String name = "testCategory.testNflow";
        final String id = metadataAccess.commit(() -> {
            final OpsManagerNflow.ID nflowId = nflowProvider.resolveId(UUID.randomUUID().toString());
            nflowProvider.save(nflowId, name,false,1000L);
            return nflowId.toString();
        });

        // Add ACL entries
        final BaseNflow.NflowId nflowId = new BaseNflow.NflowId(id);
        final JpaNflowOpsAclEntry userAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        final JpaNflowOpsAclEntry adminAcl = new JpaNflowOpsAclEntry(nflowId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclProvider.grantAccess(nflowId,new UsernamePrincipal("dladmin"), new GroupPrincipal("admin"));

        // Verify access to nflows
        metadataAccess.read(() -> {
            List<OpsManagerNflow> nflows = nflowProvider.findAll("name:" + name);
            Assert.assertTrue(nflows != null && !nflows.isEmpty());

            List<String> nflowNames = nflowProvider.getNflowNames();
            Assert.assertTrue(nflowNames != null && !nflowNames.isEmpty());

            return nflows;
        });
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin"})
    @Test
    public void testNflowHealth() {
        metadataAccess.read(() -> {
            List<? extends com.onescorpin.metadata.api.nflow.NflowHealth> health = nflowProvider.getNflowHealth();

            return null;
        });
    }


    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin"})
    @Test
    public void testJobStatusCountFromNow() {
        String nflowName = "movies.new_releases";
        metadataAccess.read(() -> {
            Period period = DateTimeUtil.period("10W");
            List<JobStatusCount> counts = nflowProvider.getJobStatusCountByDateFromNow(nflowName, period);
            return counts;
        });

    }

//    @Test
//    public void testAbandonNflowJobs() {
//
//        try (AbandonNflowJobsStoredProcedureMock storedProcedureMock = new AbandonNflowJobsStoredProcedureMock()) {
//
//            Assert.assertTrue(storedProcedureMock.getInvocationParameters().isEmpty());
//
//            String nflowName = "movies.new_releases";
//            metadataAccess.commit(() -> {
//                nflowProvider.abandonNflowJobs(nflowName);
//            });
//
//            Assert.assertFalse(storedProcedureMock.getInvocationParameters().isEmpty());
//
//            Assert.assertEquals(1, storedProcedureMock.getInvocationParameters().size());
//
//            AbandonNflowJobsStoredProcedureMock.InvocationParameters parameters =
//                    storedProcedureMock.getInvocationParameters().get(0);
//
//            Assert.assertEquals(nflowName, parameters.nflow);
//
//            String expectedExitMessagePrefix = String.format("Job manually abandoned @ %s",
//                    DateTimeFormat.forPattern("yyyy-MM-dd").print(DateTime.now()));
//
//            Assert.assertTrue(parameters.exitMessage.startsWith(expectedExitMessagePrefix));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

}
