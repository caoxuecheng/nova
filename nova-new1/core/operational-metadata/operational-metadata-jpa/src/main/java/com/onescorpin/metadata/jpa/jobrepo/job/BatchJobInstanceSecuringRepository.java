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

import com.onescorpin.metadata.jpa.nflow.AugmentableQueryRepositoryImpl;
import com.onescorpin.metadata.jpa.nflow.NflowSecuringQueryAugmentor;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import javax.persistence.EntityManager;

/**
 * Repository which ensures that access to nflow health view is secured by NflowAclIndex table
 */
public class BatchJobInstanceSecuringRepository extends AugmentableQueryRepositoryImpl {

    public BatchJobInstanceSecuringRepository(JpaEntityInformation entityInformation, EntityManager em) {
        super(entityInformation, em);
        this.setAugmentor(new BatchJobInstanceSecuringQueryAugmentor());
    }
}
