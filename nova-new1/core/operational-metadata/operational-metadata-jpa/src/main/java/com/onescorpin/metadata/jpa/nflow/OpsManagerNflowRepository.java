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

import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * Spring data repository for accessing {@link JpaOpsManagerNflow}
 */
@RepositoryType(NflowSecuringRepository.class)
public interface OpsManagerNflowRepository extends JpaRepository<JpaOpsManagerNflow, JpaOpsManagerNflow.ID>, QueryDslPredicateExecutor<JpaOpsManagerNflow> {

    JpaOpsManagerNflow findByName(String name);


    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + " where nflow.name = :nflowName"
           + " and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    JpaOpsManagerNflow findByNameWithAcl(@Param("nflowName") String nflowName);

    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + " where nflow.name = :nflowName ")
    JpaOpsManagerNflow findByNameWithoutAcl(@Param("nflowName") String nflowName);

    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + " where nflow.name = :nflowName ")
    List<JpaOpsManagerNflow> findNflowsByNameWithoutAcl(@Param("nflowName") String nflowName);

    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + " where nflow.name in (:nflowNames) ")
    List<JpaOpsManagerNflow> findNflowsByNameWithoutAcl(@Param("nflowNames") List<String> nflowNames);


    @Query("select new com.onescorpin.metadata.jpa.nflow.JpaNflowNameCount(nflow.name ,count(nflow.name) ) "
           + "from JpaOpsManagerNflow as nflow "
           + " group by nflow.name"
           + " having count(nflow.name) >1")
    List<JpaNflowNameCount> findNflowsWithSameName();


    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + " where nflow.id = :id "
           + " and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    JpaOpsManagerNflow findByIdWithAcl(@Param("id") OpsManagerNflow.ID id);

    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + " where nflow.id = :id")
    JpaOpsManagerNflow findByIdWithoutAcl(@Param("id") OpsManagerNflow.ID id);


    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + " where nflow.id in(:ids)"
           + " and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<JpaOpsManagerNflow> findByNflowIdsWithAcl(@Param("ids") List<OpsManagerNflow.ID> ids);

    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + " where nflow.id in(:ids)")
    List<JpaOpsManagerNflow> findByNflowIdsWithoutAcl(@Param("ids") List<OpsManagerNflow.ID> ids);

    @Query("SELECT distinct nflow.name FROM JpaOpsManagerNflow AS nflow "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + "where " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<String> getNflowNamesWithAcl();

    @Query("SELECT distinct nflow.name FROM JpaOpsManagerNflow AS nflow ")
    List<String> getNflowNamesWithoutAcl();

    @Procedure(name = "OpsManagerNflow.deleteNflowJobs")
    Integer deleteNflowJobs(@Param("category") String category, @Param("nflow") String nflow);

    @Procedure(name = "OpsManagerNflow.abandonNflowJobs")
    Integer abandonNflowJobs(@Param("nflow") String nflow, @Param("exitMessage") String exitMessage, @Param("username") String username);


    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + " where nflow.name in(:nflowNames)"
           + " and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<JpaOpsManagerNflow> findByNamesWithAcl(@Param("nflowNames") Set<String> nflowName);


    @Query("select nflow from JpaOpsManagerNflow as nflow "
           + " where nflow.name in(:nflowNames)")
    List<JpaOpsManagerNflow> findByNamesWithoutAcl(@Param("nflowNames") Set<String> nflowName);


    @Query("select distinct nflow from JpaOpsManagerNflow as nflow "
           + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW
           + " where " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH)
    List<JpaOpsManagerNflow> findAllWithAcl();

    @Query("select distinct nflow from JpaOpsManagerNflow as nflow ")
    List<JpaOpsManagerNflow> findAllWithoutAcl();
}
