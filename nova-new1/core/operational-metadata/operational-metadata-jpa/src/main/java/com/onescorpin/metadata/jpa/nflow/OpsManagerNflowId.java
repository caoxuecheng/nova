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

import com.onescorpin.jpa.BaseJpaId;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * The primary key for a {@link OpsManagerNflow}
 */
@Embeddable
public class OpsManagerNflowId extends BaseJpaId implements Serializable, OpsManagerNflow.ID, Nflow.ID {

    private static final long serialVersionUID = 6017751710414995750L;

    @Column(name = "id")
    private UUID uuid;

    public OpsManagerNflowId() {
    }


    public OpsManagerNflowId(Serializable ser) {
        super(ser);
    }

    public static OpsManagerNflowId create() {
        return new OpsManagerNflowId(UUID.randomUUID());
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
