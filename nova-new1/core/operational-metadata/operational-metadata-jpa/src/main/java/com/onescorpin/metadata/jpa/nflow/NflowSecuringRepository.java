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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

/**
 * Repository which ensures that access to nflows is secured by NflowAclIndex table
 */
public class NflowSecuringRepository extends AugmentableQueryRepositoryImpl {

    private static final Logger LOG = LoggerFactory.getLogger(NflowSecuringRepository.class);

    public NflowSecuringRepository(JpaEntityInformation entityInformation, EntityManager em) {
        super(entityInformation, em);
        this.setAugmentor(new NflowSecuringQueryAugmentor());
    }

    public List<JpaOpsManagerNflow> findByName(String name) {
        LOG.debug("NflowSecuringRepository.findByName");

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<JpaOpsManagerNflow> query = builder.createQuery(JpaOpsManagerNflow.class);
        Root<JpaOpsManagerNflow> root = query.from(JpaOpsManagerNflow.class);

        Expression<Boolean> nameIsEqualToParam = builder.equal(root.get("name"), name);

        Specification<JpaOpsManagerNflow> spec = null;
        Specification<JpaOpsManagerNflow> secured = augmentor.augment(spec, JpaOpsManagerNflow.class, entityInformation);
        query.where(builder.and(nameIsEqualToParam, secured.toPredicate(root, query, builder)));

        return entityManager.createQuery(query).getResultList();
    }

}
