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

import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import java.util.List;

/**
 * This is an example repository which shows how to refer to principal roles in @Query annotations and also
 * to how secure repository methods with QueryAugmentor
 */
@RepositoryType(TestNflowSecuringRepository.class)
public interface TestOpsManagerNflowRepository extends JpaRepository<JpaOpsManagerNflow, JpaOpsManagerNflow.ID>, QueryDslPredicateExecutor<JpaOpsManagerNflow> {

    @Query("select nflow.name from JpaOpsManagerNflow as nflow where nflow.name = :#{user.name}")
    List<String> getNflowNamesForCurrentUser();

    @Query("select distinct nflow.name from JpaOpsManagerNflow as nflow "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           +" where "+NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<String> getNflowNames();

    List<JpaOpsManagerNflow> findByName(String name);
}
