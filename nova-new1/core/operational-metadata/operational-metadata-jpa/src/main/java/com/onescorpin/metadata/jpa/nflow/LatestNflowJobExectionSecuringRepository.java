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

import com.querydsl.core.types.dsl.ComparablePath;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

/**
 */
public class LatestNflowJobExectionSecuringRepository extends AugmentableQueryRepositoryImpl {

    public LatestNflowJobExectionSecuringRepository(JpaEntityInformation entityInformation, EntityManager em) {
        super(entityInformation, em, new NflowAclIndexQueryAugmentor() {
            @Override
            protected <S, T, ID extends Serializable> Path<Object> getNflowId(JpaEntityInformation<T, ID> entityInformation, Root<S> root) {
                return root.get("nflow").get("id");
            }

            @Override
            protected ComparablePath<UUID> getNflowId() {
                throw new IllegalStateException("Dsl Query API not supported for this repository");
            }

            @Override
            protected QOpsManagerNflowId getOpsManagerNflowId() {
                throw new IllegalStateException("Dsl Query API not supported for this repository");
            }
        });
    }
}
