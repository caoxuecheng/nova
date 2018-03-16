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

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAclEntry;
import com.onescorpin.metadata.config.RoleSetExposingSecurityExpressionRoot;
import com.onescorpin.metadata.jpa.nflow.security.JpaNflowOpsAclEntry;
import com.onescorpin.metadata.jpa.nflow.security.QJpaNflowOpsAclEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

/**
 * Secures queries by checking whether access to them is allowed by having matching roles for current
 * user principal in NflowAclIndex table
 */
public abstract class NflowAclIndexQueryAugmentor implements QueryAugmentor {

    private static final Logger LOG = LoggerFactory.getLogger(NflowAclIndexQueryAugmentor.class);
    private static final StringTemplate CONSTANT_ONE = Expressions.stringTemplate("1");
    private static final BooleanExpression ONE_EQUALS_ONE = CONSTANT_ONE.eq(CONSTANT_ONE);

    protected abstract <S, T, ID extends Serializable> Path<Object> getNflowId(JpaEntityInformation<T, ID> entityInformation, Root<S> root);

    protected abstract ComparablePath<UUID> getNflowId();

    protected abstract QOpsManagerNflowId getOpsManagerNflowId();

    @Override
    public <S, T, ID extends Serializable> Specification<S> augment(Specification<S> spec, Class<S> domainClass,
                                                                    JpaEntityInformation<T, ID> entityInformation) {
        LOG.debug("QueryAugmentor.augment");

        return (root, query, criteriaBuilder) -> {
            Root<JpaNflowOpsAclEntry> fromAcl = query.from(JpaNflowOpsAclEntry.class);
            query.distinct(true);
            if (query.getSelection() == null) {
                query.select((Selection) root);
            }

            Path<Object> nflowId = getNflowId(entityInformation, root);
            javax.persistence.criteria.Predicate rootNflowIdEqualToAclNflowId = criteriaBuilder.equal(nflowId, fromAcl.get("nflowId"));

            RoleSetExposingSecurityExpressionRoot userCxt = getUserContext();
            javax.persistence.criteria.Predicate aclPrincipalInGroups = fromAcl.get("principalName").in(userCxt.getGroups());
            javax.persistence.criteria.Predicate aclPrincipalTypeIsGroup = criteriaBuilder.equal(fromAcl.get("principalType"), NflowOpsAclEntry.PrincipalType.GROUP);
            javax.persistence.criteria.Predicate acePrincipalGroupMatch = criteriaBuilder.and(aclPrincipalInGroups, aclPrincipalTypeIsGroup);
            javax.persistence.criteria.Predicate aclPrincipalEqUser = criteriaBuilder.equal(fromAcl.get("principalName"), userCxt.getName());
            javax.persistence.criteria.Predicate aclPrincipalTypeIsUser = criteriaBuilder.equal(fromAcl.get("principalType"), NflowOpsAclEntry.PrincipalType.USER);
            javax.persistence.criteria.Predicate acePrincipalUserMatch = criteriaBuilder.and(aclPrincipalEqUser, aclPrincipalTypeIsUser);
            javax.persistence.criteria.Predicate acePrincipalMatch = criteriaBuilder.or(acePrincipalGroupMatch, acePrincipalUserMatch);

            javax.persistence.criteria.Predicate nflowIdEqualsAndPrincipalMatch = criteriaBuilder.and(rootNflowIdEqualToAclNflowId, acePrincipalMatch);

            if (spec != null) {
                javax.persistence.criteria.Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);
                return criteriaBuilder.and(predicate, nflowIdEqualsAndPrincipalMatch);
            } else {
                return nflowIdEqualsAndPrincipalMatch;
            }
        };
    }

    @Override
    public List<Predicate> augment(Predicate[] predicate) {
        LOG.debug("NflowAclIndexQueryAugmentor.augment(Predicate[])");
        QOpsManagerNflowId nflow = getOpsManagerNflowId();

        BooleanExpression exists = generateExistsExpression(nflow);

        List<Predicate> predicates = new ArrayList<>();
        predicates.addAll(Arrays.asList(predicate));
        predicates.add(exists);

        return predicates;
    }

    /**
     * Generates the Exist expression for the nflow to nflowacl table
     *
     * @return exists expression
     */
    private static BooleanExpression generateExistsExpression(QOpsManagerNflowId nflowId) {
        LOG.debug("NflowAclIndexQueryAugmentor.generateExistsExpression(QOpsManagerNflowId)");

        RoleSetExposingSecurityExpressionRoot userCxt = getUserContext();
        QJpaNflowOpsAclEntry aclEntry = QJpaNflowOpsAclEntry.jpaNflowOpsAclEntry;
        JPQLQuery<JpaNflowOpsAclEntry> subquery = JPAExpressions.selectFrom(aclEntry)
            .where(aclEntry.nflow.id.eq(nflowId)
                       .and(aclEntry.principalName.in(userCxt.getGroups()).and(aclEntry.principalType.eq(NflowOpsAclEntry.PrincipalType.GROUP))
                                .or(aclEntry.principalName.eq(userCxt.getName()).and(aclEntry.principalType.eq(NflowOpsAclEntry.PrincipalType.USER))))
        );
        return subquery.exists();
    }

    public static BooleanExpression generateExistsExpression(QOpsManagerNflowId id, boolean entityAccessControlled) {
        if (entityAccessControlled) {
            return generateExistsExpression(id);
        } else {
            return ONE_EQUALS_ONE;
        }
    }

    @Override
    public <S, T, ID extends Serializable> CriteriaQuery<Long> getCountQuery(EntityManager entityManager, JpaEntityInformation<T, ID> entityInformation, Specification<S> spec, Class<S> domainClass) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<S> root = query.from(domainClass);

        if (query.isDistinct()) {
            query.select(builder.countDistinct(root));
        } else {
            query.select(builder.count(root));
        }

        Specification<S> secured = this.augment(spec, domainClass, entityInformation);
        query.where(secured.toPredicate(root, query, builder));

        return query;
    }

    private static RoleSetExposingSecurityExpressionRoot getUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new RoleSetExposingSecurityExpressionRoot(authentication);
    }
}
