package com.onescorpin.metadata.jpa.nflow.security;

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

import com.onescorpin.metadata.jpa.nflow.NflowHealthSecuringRepository;
import com.onescorpin.metadata.jpa.nflow.RepositoryType;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for nflow access control lists.
 */
public interface NflowOpsAccessControlRepository extends JpaRepository<JpaNflowOpsAclEntry, JpaNflowOpsAclEntry.EntryId> {

    /**
     * Predicate for selecting matching principals in WHERE clause.
     */
    String WHERE_PRINCIPALS_MATCH = " ((acl.principalType = 'USER' AND acl.principalName = :#{user.name}) "
                    + " OR (acl.principalType = 'GROUP' AND acl.principalName in :#{user.groups})) ";


    /**
     * Join statement for selecting only nflows accessible to the current principal.
     */
    String JOIN_ACL_TO_NFLOW = " join JpaNflowOpsAclEntry as acl on nflow.id = acl.nflowId ";

    /**
     * Join statement for selecting only jobs accessible to the current principal.
     */
    String JOIN_ACL_TO_JOB =" join JpaNflowOpsAclEntry as acl on job.jobInstance.nflow.id = acl.nflowId ";

    /**
     * Join statement for selecting only jobs executions accessible to the current principal.
     */
    String JOIN_ACL_TO_JOB_EXECUTION =" join JpaNflowOpsAclEntry as acl on jobExecution.nflow.id = acl.nflowId ";


    @Query("select entry from JpaNflowOpsAclEntry as entry where entry.nflowId = :id")
    List<JpaNflowOpsAclEntry> findForNflow(@Param("id") UUID nflowId);

   // @Query("select distinct entry.nflowId from JpaNflowOpsAclEntry as entry where entry.principalName in (:names)")
  //  Set<UUID> findNflowIdsForPrincipals(@Param("names") Set<String> principalNames);

    @Query("select entry from JpaNflowOpsAclEntry as entry where entry.principalName in (:names)")
    Set<JpaNflowOpsAclEntry> findForPrincipals(@Param("names") Set<String> principalNames);

    @Modifying
    @Query("delete from JpaNflowOpsAclEntry as entry where entry.principalName in (:names)")
    int deleteForPrincipals(@Param("names") Set<String> principalNames);

    @Modifying
    @Query("delete from JpaNflowOpsAclEntry as entry where entry.nflowId = :id")
    int deleteForNflow(@Param("id") UUID nflowId);


}
