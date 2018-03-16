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
import com.onescorpin.metadata.jpa.support.GenericQueryDslFilter;
import com.onescorpin.security.AccessController;
import com.onescorpin.spring.CommonsSpringConfiguration;
import com.onescorpin.test.security.WithMockJaasUser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;


@SuppressWarnings("SpringJavaAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:test-application.properties")
@SpringApplicationConfiguration(
    classes = {CommonsSpringConfiguration.class, OperationalMetadataConfig.class, TestJpaConfiguration.class, OpsManagerNflowRepositoryTest.class, NflowOpsAccessControlConfig.class})
@Transactional
@Configuration
public class OpsManagerNflowRepositoryTest {

    @Bean
    public AccessController accessController() {
        AccessController mock = Mockito.mock(AccessController.class);
        Mockito.when(mock.isEntityAccessControlled()).thenReturn(true);
        return mock;
    }

    @Autowired
    TestOpsManagerNflowRepository repo;

    @Autowired
    NflowOpsAccessControlRepository aclRepo;


    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findNflowUsingPrincipalsName_MatchingUserNameAndNflowName() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "dladmin");
        repo.save(nflow);
        List<String> nflowNames = repo.getNflowNamesForCurrentUser();
        Assert.assertEquals(1, nflowNames.size());
        Assert.assertEquals("dladmin", nflowNames.get(0));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findNflowUsingPrincipalsName_NonMatchingUserNameAndNflowName() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "non-matching-nflow-name");
        repo.save(nflow);
        List<String> nflowNames = repo.getNflowNamesForCurrentUser();
        Assert.assertEquals(0, nflowNames.size());
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findNflowNames_MatchingGroupAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry adminGroupAcl = new JpaNflowOpsAclEntry(nflowId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(adminGroupAcl);
        List<String> nflowNames = repo.getNflowNames();
        Assert.assertEquals(1, nflowNames.size());
        Assert.assertEquals("nflow-name", nflowNames.get(0));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findNflowNames_NoMatchingGroupMatchingUserAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);
        List<String> nflowNames = repo.getNflowNames();
        Assert.assertEquals(1, nflowNames.size());
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findNflowNames_NoMatchingGroupAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);
        List<String> nflowNames = repo.getNflowNames();
        Assert.assertEquals(0, nflowNames.size());
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findNflowNames_BothMatchingAndNonMatchingGroupsAreSetInAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);

        JpaNflowOpsAclEntry adminGroupAcl = new JpaNflowOpsAclEntry(nflowId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(adminGroupAcl);
        List<String> nflowNames = repo.getNflowNames();
        Assert.assertEquals(1, nflowNames.size());
        Assert.assertEquals("nflow-name", nflowNames.get(0));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findNflowNames_MultipleMatchingNflowsAndGroups() throws Exception {
        JpaOpsManagerNflow nflow1 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow1-name");
        repo.save(nflow1);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow1.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);

        JpaNflowOpsAclEntry adminGroupAcl = new JpaNflowOpsAclEntry(nflowId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(adminGroupAcl);

        JpaOpsManagerNflow nflow2 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow2-name");
        repo.save(nflow2);

        BaseNflow.NflowId nflowId2 = new BaseNflow.NflowId(nflow2.getId().getUuid());

        JpaNflowOpsAclEntry userGroupAcl = new JpaNflowOpsAclEntry(nflowId2, "user", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(userGroupAcl);

        List<String> nflowNames = repo.getNflowNames();
        Assert.assertEquals(2, nflowNames.size());
        Assert.assertTrue(nflowNames.contains("nflow1-name"));
        Assert.assertTrue(nflowNames.contains("nflow2-name"));
    }


    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAll_NoMatchingGroupMatchingUserAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);
        Iterable<JpaOpsManagerNflow> all = repo.findAll();
        Assert.assertTrue(StreamSupport.stream(all.spliterator(), false)
                              .allMatch(it -> it.getName().equals("nflow-name")));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAll_NoMatchingGroupAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);
        Iterable<JpaOpsManagerNflow> all = repo.findAll();
        Assert.assertFalse(StreamSupport.stream(all.spliterator(), false)
                               .anyMatch(it -> it.getName().equals("nflow-name")));
    }


    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAllFilter_NoMatchingGroupMatchingUserAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);

        QJpaOpsManagerNflow qNflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        Iterable<JpaOpsManagerNflow> all = repo.findAll(GenericQueryDslFilter.buildFilter(qNflow, "name: nflow-name"));
        Assert.assertTrue(StreamSupport.stream(all.spliterator(), false)
                              .allMatch(it -> it.getName().equals("nflow-name")));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAllFilter_NoMatchingGroupAclEntry() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);

        QJpaOpsManagerNflow qNflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        Iterable<JpaOpsManagerNflow> all = repo.findAll(GenericQueryDslFilter.buildFilter(qNflow, "name: nflow-name"));
        Assert.assertFalse(StreamSupport.stream(all.spliterator(), false)
                               .anyMatch(it -> it.getName().equals("nflow-name")));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAllFilter_MatchingGroupButNoMatchingFilter() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry nonMatching = new JpaNflowOpsAclEntry(nflowId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(nonMatching);

        QJpaOpsManagerNflow qNflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        Iterable<JpaOpsManagerNflow> all = repo.findAll(GenericQueryDslFilter.buildFilter(qNflow, "name==non-matching-nflow-name"));

        Assert.assertFalse(StreamSupport.stream(all.spliterator(), false)
                               .anyMatch(it -> it.getName().equals("nflow-name")));

        Assert.assertFalse(StreamSupport.stream(all.spliterator(), false)
                               .anyMatch(it -> it.getName().equals("non-matching-nflow-name")));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAllFilter_MatchingGroupAndMatchingFilter() throws Exception {
        JpaOpsManagerNflow nflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow-name");
        repo.save(nflow);

        BaseNflow.NflowId nflowId = new BaseNflow.NflowId(nflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl = new JpaNflowOpsAclEntry(nflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl);

        JpaNflowOpsAclEntry matchingGroup = new JpaNflowOpsAclEntry(nflowId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(matchingGroup);

        JpaOpsManagerNflow nonMatchingNflow = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "non-matching-nflow-name");
        repo.save(nonMatchingNflow);

        BaseNflow.NflowId nonMatchingNflowId = new BaseNflow.NflowId(nonMatchingNflow.getId().getUuid());

        JpaNflowOpsAclEntry dladminUserAcl1 = new JpaNflowOpsAclEntry(nonMatchingNflowId, "dladmin", JpaNflowOpsAclEntry.PrincipalType.USER);
        aclRepo.save(dladminUserAcl1);

        JpaNflowOpsAclEntry matchingGroup1 = new JpaNflowOpsAclEntry(nonMatchingNflowId, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(matchingGroup1);

        QJpaOpsManagerNflow qNflow = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        Iterable<JpaOpsManagerNflow> all = repo.findAll(GenericQueryDslFilter.buildFilter(qNflow, "name==nflow-name"));

        Assert.assertTrue(StreamSupport.stream(all.spliterator(), false)
                              .anyMatch(it -> it.getName().equals("nflow-name")));

        Assert.assertFalse(StreamSupport.stream(all.spliterator(), false)
                               .anyMatch(it -> it.getName().equals("non-matching-nflow-name")));
    }


    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void count_ShouldCountOnlyPermittedNflows() throws Exception {
        JpaOpsManagerNflow nflow1 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow1-name");
        repo.save(nflow1);

        BaseNflow.NflowId nflow1Id = new BaseNflow.NflowId(nflow1.getId().getUuid());

        JpaNflowOpsAclEntry acl1 = new JpaNflowOpsAclEntry(nflow1Id, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl1);

        JpaOpsManagerNflow nflow2 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow2-name");
        repo.save(nflow2);

        BaseNflow.NflowId nflow2Id = new BaseNflow.NflowId(nflow2.getId().getUuid());

        JpaNflowOpsAclEntry acl2 = new JpaNflowOpsAclEntry(nflow2Id, "user", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl2);

        JpaOpsManagerNflow nflow3 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow3-name");
        repo.save(nflow3);

        BaseNflow.NflowId nflow3Id = new BaseNflow.NflowId(nflow3.getId().getUuid());

        JpaNflowOpsAclEntry acl3 = new JpaNflowOpsAclEntry(nflow3Id, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl3);

        long count = repo.count();
        Assert.assertEquals(2, count);

        List<JpaOpsManagerNflow> nflows = repo.findAll();
        Assert.assertTrue(nflows.stream()
                              .anyMatch(it -> it.getName().equals("nflow1-name")));
        Assert.assertTrue(nflows.stream()
                              .anyMatch(it -> it.getName().equals("nflow2-name")));
    }

    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findOne() throws Exception {
        JpaOpsManagerNflow nflow1 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow1-name");
        repo.save(nflow1);

        BaseNflow.NflowId nflow1Id = new BaseNflow.NflowId(nflow1.getId().getUuid());

        JpaNflowOpsAclEntry acl1 = new JpaNflowOpsAclEntry(nflow1Id, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl1);

        JpaOpsManagerNflow nflow2 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow2-name");
        repo.save(nflow2);

        BaseNflow.NflowId nflow2Id = new BaseNflow.NflowId(nflow2.getId().getUuid());

        JpaNflowOpsAclEntry acl2 = new JpaNflowOpsAclEntry(nflow2Id, "user", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl2);

        JpaOpsManagerNflow nflow3 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow3-name");
        repo.save(nflow3);

        BaseNflow.NflowId nflow3Id = new BaseNflow.NflowId(nflow3.getId().getUuid());

        JpaNflowOpsAclEntry acl3 = new JpaNflowOpsAclEntry(nflow3Id, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl3);
        Assert.assertNotNull(repo.findOne(nflow1.getId()));
        Assert.assertNotNull(repo.findOne(nflow2.getId()));
        Assert.assertNull(repo.findOne(nflow3.getId()));
    }


    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void findAll_TwoPages() throws Exception {
        JpaOpsManagerNflow nflow1 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow1-name");
        repo.save(nflow1);
        BaseNflow.NflowId nflow1Id = new BaseNflow.NflowId(nflow1.getId().getUuid());
        JpaNflowOpsAclEntry acl1 = new JpaNflowOpsAclEntry(nflow1Id, "admin", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl1);

        JpaOpsManagerNflow nflow2 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow2-name");
        repo.save(nflow2);
        BaseNflow.NflowId nflow2Id = new BaseNflow.NflowId(nflow2.getId().getUuid());
        JpaNflowOpsAclEntry acl2 = new JpaNflowOpsAclEntry(nflow2Id, "user", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl2);

        JpaOpsManagerNflow nflow3 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow3-name");
        repo.save(nflow3);
        BaseNflow.NflowId nflow3Id = new BaseNflow.NflowId(nflow3.getId().getUuid());
        JpaNflowOpsAclEntry acl3 = new JpaNflowOpsAclEntry(nflow3Id, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl3);

        JpaOpsManagerNflow nflow4 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow4-name");
        repo.save(nflow4);
        BaseNflow.NflowId nflow4Id = new BaseNflow.NflowId(nflow4.getId().getUuid());
        JpaNflowOpsAclEntry acl4 = new JpaNflowOpsAclEntry(nflow4Id, "user", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl4);

        JpaOpsManagerNflow nflow5 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow5-name");
        repo.save(nflow5);
        BaseNflow.NflowId nflow5Id = new BaseNflow.NflowId(nflow5.getId().getUuid());
        JpaNflowOpsAclEntry acl5 = new JpaNflowOpsAclEntry(nflow5Id, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl5);

        JpaOpsManagerNflow nflow6 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow6-name");
        repo.save(nflow6);
        BaseNflow.NflowId nflow6Id = new BaseNflow.NflowId(nflow6.getId().getUuid());
        JpaNflowOpsAclEntry acl6 = new JpaNflowOpsAclEntry(nflow6Id, "user", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl6);

        Pageable page1Request = new PageRequest(0, 2);
        Page<JpaOpsManagerNflow> page1 = repo.findAll(page1Request);
        Assert.assertEquals(0, page1.getNumber());
        Assert.assertEquals(2, page1.getNumberOfElements());
        Assert.assertEquals(2, page1.getTotalPages());
        Assert.assertEquals(4, page1.getTotalElements());

        Pageable page2Request = new PageRequest(1, 2);
        Page<JpaOpsManagerNflow> page2 = repo.findAll(page2Request);
        Assert.assertEquals(1, page2.getNumber());
        Assert.assertEquals(2, page2.getNumberOfElements());
        Assert.assertEquals(2, page2.getTotalPages());
        Assert.assertEquals(4, page2.getTotalElements());

    }


    @WithMockJaasUser(username = "dladmin",
                      password = "secret",
                      authorities = {"admin", "user"})
    @Test
    public void testCustomMethod_findByName() throws Exception {
        JpaOpsManagerNflow nflow1 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow1-name");
        repo.save(nflow1);
        BaseNflow.NflowId nflow1Id = new BaseNflow.NflowId(nflow1.getId().getUuid());
        JpaNflowOpsAclEntry acl1 = new JpaNflowOpsAclEntry(nflow1Id, "NON_MATCHING", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl1);

        JpaOpsManagerNflow nflow2 = new JpaOpsManagerNflow(OpsManagerNflowId.create(), "nflow2-name");
        repo.save(nflow2);
        BaseNflow.NflowId nflow2Id = new BaseNflow.NflowId(nflow2.getId().getUuid());
        JpaNflowOpsAclEntry acl2 = new JpaNflowOpsAclEntry(nflow2Id, "user", JpaNflowOpsAclEntry.PrincipalType.GROUP);
        aclRepo.save(acl2);

        List<JpaOpsManagerNflow> nflows1 = repo.findByName("nflow1-name");
        Assert.assertTrue(nflows1.isEmpty());

        List<JpaOpsManagerNflow> nflows2 = repo.findByName("nflow2-name");
        Assert.assertEquals(1, nflows2.size());
        Assert.assertEquals("nflow2-name", nflows2.get(0).getName());
    }


}
