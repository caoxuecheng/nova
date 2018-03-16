package com.onescorpin.nflowmgr.service.domaintype;

/*-
 * #%L
 * nova-nflow-manager-controller
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

import com.onescorpin.discovery.model.DefaultField;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.metadata.api.domaintype.DomainTypeProvider;
import com.onescorpin.policy.rest.model.FieldPolicy;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Transform domain types objects between REST and domain models.
 */
public class DomainTypeTransform {

    /**
     * Provides access to domain type objects.
     */
    @Inject
    private DomainTypeProvider domainTypeProvider;

    /**
     * Transforms the specified REST model to a domain model.
     */
    @Nonnull
    public com.onescorpin.metadata.api.domaintype.DomainType toDomainModel(@Nonnull final com.onescorpin.nflowmgr.rest.model.DomainType restModel) {
        com.onescorpin.metadata.api.domaintype.DomainType domainModel = null;
        if (restModel.getId() != null) {
            domainModel = domainTypeProvider.findById(domainTypeProvider.resolveId(restModel.getId()));
        }
        if (domainModel == null) {
            domainModel = domainTypeProvider.create();
        }

        domainModel.setDescription(restModel.getDescription());
        domainModel.setFieldJson(ObjectMapperSerializer.serialize(restModel.getField()));
        domainModel.setFieldPolicyJson(ObjectMapperSerializer.serialize(restModel.getFieldPolicy()));
        domainModel.setIcon(restModel.getIcon());
        domainModel.setIconColor(restModel.getIconColor());
        domainModel.setRegexFlags(restModel.getRegexFlags());
        domainModel.setRegexPattern(restModel.getRegexPattern());
        domainModel.setTitle(restModel.getTitle());
        return domainModel;
    }

    /**
     * Transforms the specified domain model to a REST model.
     */
    @Nonnull
    public com.onescorpin.nflowmgr.rest.model.DomainType toRestModel(@Nonnull final com.onescorpin.metadata.api.domaintype.DomainType domainModel) {
        final com.onescorpin.nflowmgr.rest.model.DomainType restModel = new com.onescorpin.nflowmgr.rest.model.DomainType();
        restModel.setDescription(domainModel.getDescription());
        restModel.setField((domainModel.getFieldJson() != null) ? ObjectMapperSerializer.deserialize(domainModel.getFieldJson(), DefaultField.class) : null);
        restModel.setFieldPolicy((domainModel.getFieldPolicyJson() != null) ? ObjectMapperSerializer.deserialize(domainModel.getFieldPolicyJson(), FieldPolicy.class) : null);
        restModel.setIcon(domainModel.getIcon());
        restModel.setIconColor(domainModel.getIconColor());
        restModel.setId(domainModel.getId().toString());
        restModel.setRegexFlags(domainModel.getRegexFlags());
        restModel.setRegexPattern(domainModel.getRegexPattern());
        restModel.setTitle(domainModel.getTitle());
        return restModel;
    }
}
