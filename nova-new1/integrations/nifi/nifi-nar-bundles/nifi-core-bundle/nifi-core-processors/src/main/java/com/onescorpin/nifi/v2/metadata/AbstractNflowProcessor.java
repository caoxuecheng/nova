/**
 *
 */
package com.onescorpin.nifi.v2.metadata;

/*-
 * #%L
 * onescorpin-nifi-core-processors
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

import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.nifi.core.api.metadata.MetadataProvider;
import com.onescorpin.nifi.core.api.metadata.MetadataProviderService;
import com.onescorpin.nifi.v2.common.BaseProcessor;
import com.onescorpin.nifi.v2.common.CommonProperties;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.Relationship;

import java.util.List;
import java.util.Set;

/**
 */
public abstract class AbstractNflowProcessor extends BaseProcessor {

    protected void addProperties(List<PropertyDescriptor> props) {
        props.add(CommonProperties.METADATA_SERVICE);
    }

    protected void addRelationships(Set<Relationship> relationships2) {
    }

    protected MetadataProviderService getProviderService(ProcessContext context) {
        return context.getProperty(CommonProperties.METADATA_SERVICE).asControllerService(MetadataProviderService.class);
    }

    protected Datasource findDatasource(ProcessContext context, String dsName) {
        MetadataProvider datasetProvider = getProviderService(context).getProvider();
        return datasetProvider.getDatasourceByName(dsName);
    }
}
