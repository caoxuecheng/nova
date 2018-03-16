package com.onescorpin.discovery.rest.controller;

/*-
 * #%L
 * onescorpin-schema-discovery-controller
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

import com.onescorpin.discovery.model.SchemaParserDescriptor;
import com.onescorpin.discovery.parser.FileSchemaParser;
import com.onescorpin.discovery.parser.SchemaParser;
import com.onescorpin.policy.BasePolicyAnnotationTransformer;
import com.onescorpin.policy.rest.model.FieldRuleProperty;

import java.util.List;

/**
 * Transforms the schema parser UI model to/from
 */
public class SchemaParserAnnotationTransformer extends BasePolicyAnnotationTransformer<SchemaParserDescriptor, FileSchemaParser, SchemaParser> {

    @Override
    public SchemaParserDescriptor buildUiModel(SchemaParser annotation, FileSchemaParser policy, List<FieldRuleProperty> properties) {
        SchemaParserDescriptor descriptor = new SchemaParserDescriptor();
        descriptor.setProperties(properties);
        descriptor.setName(annotation.name());
        descriptor.setDescription(annotation.description());
        descriptor.setProperties(properties);
        descriptor.setObjectClassType(policy.getClass().getTypeName());
        descriptor.setTags(annotation.tags());
        descriptor.setGeneratesHiveSerde(annotation.generatesHiveSerde());
        descriptor.setSupportsBinary(annotation.supportsBinary());
        descriptor.setAllowSkipHeader(annotation.allowSkipHeader());
        return descriptor;
    }

    @Override
    public Class<SchemaParser> getAnnotationClass() {
        return SchemaParser.class;
    }
}

