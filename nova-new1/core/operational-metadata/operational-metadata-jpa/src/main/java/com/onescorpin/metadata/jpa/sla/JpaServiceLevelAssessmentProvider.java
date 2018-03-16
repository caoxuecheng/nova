/**
 *
 */
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

import com.google.common.collect.ImmutableMap;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstantImpl;
import com.querydsl.core.types.ExpressionUtils;
import com.onescorpin.metadata.jpa.nflow.NflowAclIndexQueryAugmentor;
import com.onescorpin.metadata.jpa.nflow.QJpaOpsManagerNflow;
import com.onescorpin.metadata.jpa.support.CommonFilterTranslations;
import com.onescorpin.metadata.jpa.support.GenericQueryDslFilter;
import com.onescorpin.metadata.jpa.support.QueryDslFetchJoin;
import com.onescorpin.metadata.jpa.support.QueryDslPagingSupport;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAssessment;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.metadata.sla.spi.ServiceLevelAssessmentProvider;
import com.onescorpin.security.AccessController;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

/**
 * Provider accessing the {@link JpaServiceLevelAssessment}
 */
@Service
public class JpaServiceLevelAssessmentProvider extends QueryDslPagingSupport<JpaServiceLevelAssessment> implements ServiceLevelAssessmentProvider {

    private static final Logger log = LoggerFactory.getLogger(JpaServiceLevelAssessmentProvider.class);

    private JpaServiceLevelAssessmentRepository serviceLevelAssessmentRepository;

    private JpaServiceLevelAgreementDescriptionRepository serviceLevelAgreementDescriptionRepository;

    @Inject
    private ServiceLevelAgreementProvider slaProvider;

    @Inject
    private AccessController accessController;

    public static final ImmutableMap<String, String> slaAssessmentFilters =
        new ImmutableMap.Builder<String, String>()
            .put("sla", "serviceLevelAgreementDescription.name")
            .put("slaId", "serviceLevelAgreementDescription.slaId.uuid")
            .put("slaDescription", "serviceLevelAgreementDescription.description")
            .put("assessmentId", "id.uuid")
            .put("result", "result").build();

    @Autowired
    public JpaServiceLevelAssessmentProvider(JpaServiceLevelAssessmentRepository serviceLevelAssessmentRepository,
                                             JpaServiceLevelAgreementDescriptionRepository serviceLevelAgreementDescriptionRepository) {
        super(JpaServiceLevelAssessment.class);
        this.serviceLevelAssessmentRepository = serviceLevelAssessmentRepository;
        this.serviceLevelAgreementDescriptionRepository = serviceLevelAgreementDescriptionRepository;
        CommonFilterTranslations.addFilterTranslations(QJpaServiceLevelAssessment.class, slaAssessmentFilters);
    }


    public ServiceLevelAssessment.ID resolveId(Serializable ser) {
        if (ser instanceof JpaServiceLevelAssessment.ID) {
            return (JpaServiceLevelAssessment.ID) ser;
        } else {
            return new JpaServiceLevelAssessment.SlaAssessmentId(ser);
        }
    }

    /**
     * save an sla assessment to the database
     *
     * @param assessment the assessment to save
     * @return the saved assessment
     */
    public ServiceLevelAssessment save(ServiceLevelAssessment assessment) {
        return this.serviceLevelAssessmentRepository.save((JpaServiceLevelAssessment) assessment);
    }

    /**
     * Find all SLA assessments
     *
     * @return find all the SLA assessments
     */
    @Override
    public List<? extends ServiceLevelAssessment> getAssessments() {
        return serviceLevelAssessmentRepository.findAll();
    }


    /**
     * find the latest assessment
     *
     * @param slaId the service level agreement id
     * @return the latest assessment for the sla
     */
    public ServiceLevelAssessment findLatestAssessment(ServiceLevelAgreement.ID slaId) {

        ServiceLevelAgreementDescriptionId jpaId = null;
        if (slaId instanceof ServiceLevelAgreementDescriptionId) {
            jpaId = (ServiceLevelAgreementDescriptionId) slaId;
        } else {
            jpaId = new ServiceLevelAgreementDescriptionId(slaId.toString());
        }

        List<? extends ServiceLevelAssessment> latestAssessments = serviceLevelAssessmentRepository.findLatestAssessments(jpaId);
        if (latestAssessments != null) {
            JpaServiceLevelAssessment jpaServiceLevelAssessment = (JpaServiceLevelAssessment) latestAssessments.get(0);
            ensureServiceLevelAgreementOnAssessment(jpaServiceLevelAssessment);
            return jpaServiceLevelAssessment;
        }
        return null;
    }


    /**
     * Find the latest SLA that doesn't match the incoming {@code assessmentId}
     *
     * @param slaId        the id to reference
     * @param assessmentId the assessment id to cehck
     * @return the latest SLA that doesn't match the incoming {@code assessmentId}
     */
    @Override
    public ServiceLevelAssessment findLatestAssessmentNotEqualTo(ServiceLevelAgreement.ID slaId, ServiceLevelAssessment.ID assessmentId) {
        if (assessmentId != null) {
            ServiceLevelAgreementDescriptionId jpaId = null;
            if (!(slaId instanceof ServiceLevelAgreementDescriptionId)) {
                jpaId = new ServiceLevelAgreementDescriptionId(slaId.toString());
            } else {
                jpaId = (ServiceLevelAgreementDescriptionId) slaId;
            }
            List<? extends ServiceLevelAssessment> latestAssessments = serviceLevelAssessmentRepository.findLatestAssessmentsNotEqualTo(jpaId, assessmentId);
            if (latestAssessments != null && !latestAssessments.isEmpty()) {
                return latestAssessments.get(0);
            } else {
                return null;
            }
        } else {
            return findLatestAssessment(slaId);
        }
    }

    /**
     * Find a ServiceLevelAssessment by its id
     *
     * @param id the id of the sla assessment
     * @return the matching ServiceLevelAssement
     */
    @Override
    public ServiceLevelAssessment findServiceLevelAssessment(ServiceLevelAssessment.ID id) {
        if (accessController.isEntityAccessControlled()) {
            return serviceLevelAssessmentRepository.findAssessmentWithAcl(id);
        } else {
            return serviceLevelAssessmentRepository.findAssessmentWithoutAcl(id);
        }
    }


    /**
     * Makes sure the Service Level Assessment object has its respective SLA attached to it for reference lookups
     *
     * @param assessment the assessment to check and ensure it has its SLA attached
     * @return {@code true} if it was able to attach the SLA to the assessment, {@code false} if it was not about to attach and find the SLA
     */
    public boolean ensureServiceLevelAgreementOnAssessment(ServiceLevelAssessment assessment) {
        if (assessment != null && assessment.getAgreement() != null) {
            return true;
        }
        if (assessment.getAgreement() == null && assessment.getServiceLevelAgreementId() != null) {
            ServiceLevelAgreement agreement = slaProvider.getAgreement(slaProvider.resolve(assessment.getServiceLevelAgreementId().toString()));
            ((JpaServiceLevelAssessment) assessment).setAgreement(agreement);
        }
        return assessment != null && assessment.getAgreement() != null;
    }


    @Override
    public Page<? extends ServiceLevelAssessment> findAll(String filter, Pageable pageable) {
        QJpaServiceLevelAssessment serviceLevelAssessment = QJpaServiceLevelAssessment.jpaServiceLevelAssessment;

        pageable = CommonFilterTranslations.resolveSortFilters(serviceLevelAssessment, pageable);

        QJpaObligationAssessment obligationAssessment = new QJpaObligationAssessment("obligationAssessment");
        QJpaMetricAssessment metricAssessment = new QJpaMetricAssessment("metricAssessment");
        QJpaServiceLevelAgreementDescription serviceLevelAgreementDescription = new QJpaServiceLevelAgreementDescription("slaDescription");
        QJpaOpsManagerNflow nflow = new QJpaOpsManagerNflow("nflow");
        BooleanBuilder assessmentPredicate = GenericQueryDslFilter.buildFilter(serviceLevelAssessment, filter);
        BooleanBuilder nflowPredicate = GenericQueryDslFilter.buildFilter(nflow, filter);
        boolean invalidQuery = false;
        //if there is not predicate found for the supplied 'filter' then fail the query
        if (StringUtils.isNotBlank(filter) && (!assessmentPredicate.hasValue() && !nflowPredicate.hasValue())) {
            invalidQuery = true;
        }
        BooleanBuilder predicate = assessmentPredicate.and(nflowPredicate);
        if (invalidQuery) {
            predicate.and(ExpressionUtils.eq(ConstantImpl.create("1"), ConstantImpl.create("2")));
        }
        predicate.and(nflow.isNull().or(nflow.isNotNull().and(NflowAclIndexQueryAugmentor.generateExistsExpression(nflow.id, accessController.isEntityAccessControlled()))));

        return findAllWithFetch(serviceLevelAssessment,
                                predicate,
                                true,
                                pageable,
                                QueryDslFetchJoin.innerJoin(serviceLevelAssessment.serviceLevelAgreementDescription, serviceLevelAgreementDescription),
                                QueryDslFetchJoin.leftJoin(serviceLevelAgreementDescription.nflows, nflow),
                                QueryDslFetchJoin.leftJoin(serviceLevelAssessment.obligationAssessments, obligationAssessment),
                                QueryDslFetchJoin.leftJoin(obligationAssessment.metricAssessments, metricAssessment));


    }


}
