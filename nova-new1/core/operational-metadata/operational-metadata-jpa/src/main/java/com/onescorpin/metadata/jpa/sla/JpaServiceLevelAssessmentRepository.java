package com.onescorpin.metadata.jpa.sla;

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

import com.onescorpin.metadata.jpa.nflow.security.NflowOpsAccessControlRepository;
import com.onescorpin.metadata.sla.api.ServiceLevelAssessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 */
public interface JpaServiceLevelAssessmentRepository extends JpaRepository<JpaServiceLevelAssessment, JpaServiceLevelAssessment.ID> {

    @Query(" select assessment from JpaServiceLevelAssessment assessment where assessment.slaId = :slaId "
           + "and assessment.createdTime = (select max(assessment2.createdTime) "
           + "                              from JpaServiceLevelAssessment as assessment2 "
           + "                              where assessment2.slaId = :slaId)")
    List<JpaServiceLevelAssessment> findLatestAssessments(@Param("slaId") ServiceLevelAgreementDescriptionId slaId);

    @Query(" select assessment from JpaServiceLevelAssessment assessment where assessment.slaId = :slaId "
           + "and assessment.createdTime = (select max(assessment2.createdTime) "
           + "                              from JpaServiceLevelAssessment as assessment2 "
           + "                              where assessment2.slaId = :slaId"
           + "                              and assessment2.id != :assessmentId)")
    List<JpaServiceLevelAssessment> findLatestAssessmentsNotEqualTo(@Param("slaId") ServiceLevelAgreementDescriptionId slaId, @Param("assessmentId") ServiceLevelAssessment.ID assessmentId);

    @Query(" select distinct assessment from JpaServiceLevelAssessment assessment " +
           " left join JpaServiceLevelAgreementDescription slaDescription on slaDescription.slaId = assessment.slaId " +
           "left join slaDescription.nflows as nflow " +
           " where assessment.id = :id ")
    JpaServiceLevelAssessment findAssessmentWithoutAcl(@Param("id") ServiceLevelAssessment.ID id);

    @Query(" select distinct assessment from JpaServiceLevelAssessment assessment " +
           " left join JpaServiceLevelAgreementDescription slaDescription on slaDescription.slaId = assessment.slaId " +
           "left join slaDescription.nflows as nflow " +
           "left " + NflowOpsAccessControlRepository.JOIN_ACL_TO_NFLOW + " " +
           " where assessment.id = :id  and (nflow.id is null or (nflow.id is not null and " + NflowOpsAccessControlRepository.WHERE_PRINCIPALS_MATCH + " ))")
    JpaServiceLevelAssessment findAssessmentWithAcl(@Param("id") ServiceLevelAssessment.ID id);

}
