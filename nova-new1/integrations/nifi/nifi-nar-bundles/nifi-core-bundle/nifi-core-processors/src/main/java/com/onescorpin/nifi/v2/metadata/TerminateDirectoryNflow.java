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
import com.onescorpin.metadata.rest.model.data.DirectoryDatasource;
import com.onescorpin.metadata.rest.model.op.DataOperation;
import com.onescorpin.metadata.rest.model.op.DataOperation.State;
import com.onescorpin.metadata.rest.model.op.Dataset;
import com.onescorpin.nifi.core.api.metadata.MetadataProvider;

import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 */
@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"onescorpin", "registration", "route"})
@CapabilityDescription("Routes depending on whether a registration is required.  Registration is typically one-time setup such as creating permanent tables.")
public class TerminateDirectoryNflow extends AbstractTerminateNflow {

    @Override
    protected void addProperties(List<PropertyDescriptor> props) {
        super.addProperties(props);
        props.add(DirectoryProperties.DIRECTORY_PATH);
    }

    @Override
    protected Datasource createDestinationDatasource(ProcessContext context, String datasetName, String descr) {
        MetadataProvider provider = getProviderService(context).getProvider();
        String path = context.getProperty(DirectoryProperties.DIRECTORY_PATH).getValue();

        return provider.ensureDirectoryDatasource(datasetName, "", Paths.get(path));
    }

    @Override
    protected DataOperation completeOperation(ProcessContext context,
                                              FlowFile flowFile,
                                              Datasource datasource,
                                              DataOperation op,
                                              DataOperation.State state) {
        MetadataProvider provider = getProviderService(context).getProvider();
        DirectoryDatasource dds = (DirectoryDatasource) datasource;

        if (state == State.SUCCESS) {
            ArrayList<Path> paths = new ArrayList<>();
            // TODO Extract file paths from flow file
            Dataset dataset = provider.createDataset(dds, paths);

            return provider.completeOperation(op.getId(), "", dataset);
        } else {
            return provider.completeOperation(op.getId(), "", state);
        }

    }
}
