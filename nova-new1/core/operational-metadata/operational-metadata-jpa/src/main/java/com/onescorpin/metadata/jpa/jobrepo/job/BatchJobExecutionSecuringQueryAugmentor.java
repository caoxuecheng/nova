package com.onescorpin.metadata.jpa.jobrepo.job;

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

import com.querydsl.core.types.dsl.ComparablePath;
import com.onescorpin.metadata.jpa.nflow.NflowAclIndexQueryAugmentor;
import com.onescorpin.metadata.jpa.nflow.QJpaOpsManagerNflow;
import com.onescorpin.metadata.jpa.nflow.QOpsManagerNflowId;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

/**
 * Query augmentor which checks whether access to nflow is allowed by ACLs defined in NflowAclIndexEntry table by
 * adding following expression to queries: <br>
 * <code>and exists (select 1 from JpaNflowOpsAclEntry as x where nflow.id = x.nflowId and x.principalName in :#{principal.roleSet})</code>
 */
public class BatchJobExecutionSecuringQueryAugmentor extends NflowAclIndexQueryAugmentor {

    @Override
    protected <S, T, ID extends Serializable> Path<Object> getNflowId(JpaEntityInformation<T, ID> entityInformation, Root<S> root) {
      return root.get("jobInstance").get("nflow").get("id");
    }

    @Override
    protected ComparablePath<UUID> getNflowId() {
        QJpaOpsManagerNflow root = QJpaOpsManagerNflow.jpaOpsManagerNflow;
        return root.id.uuid;
    }

    @Override
    protected QOpsManagerNflowId getOpsManagerNflowId() {
       return QJpaOpsManagerNflow.jpaOpsManagerNflow.id;
    }
}
