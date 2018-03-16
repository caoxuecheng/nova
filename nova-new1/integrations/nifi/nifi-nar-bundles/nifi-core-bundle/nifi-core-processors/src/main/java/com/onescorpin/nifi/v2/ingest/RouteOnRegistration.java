package com.onescorpin.nifi.v2.ingest;

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


import com.onescorpin.nifi.processor.AbstractNiFiProcessor;

import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.onescorpin.nifi.v2.ingest.IngestProperties.NFLOW_CATEGORY;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.NFLOW_NAME;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.METADATA_SERVICE;
import static com.onescorpin.nifi.v2.ingest.IngestProperties.REL_SUCCESS;

@EventDriven
@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({"onescorpin", "registration", "route"})
@CapabilityDescription("Routes depending on whether a registration is required.  Registration is typically one-time setup such as creating permanent tables.")

public class RouteOnRegistration extends AbstractNiFiProcessor {

    public static final Relationship REL_REGISTRATION_REQ = new Relationship.Builder()
        .name("registration_required")
        .description("Registration is required.")
        .build();

    // Relationships
    private final Set<Relationship> relationships;
    private final List<PropertyDescriptor> propDescriptors;

    public RouteOnRegistration() {
        Set<Relationship> r = new HashSet<>();
        r.add(REL_SUCCESS);
        r.add(REL_REGISTRATION_REQ);
        this.relationships = Collections.unmodifiableSet(r);

        List<PropertyDescriptor> pds = new ArrayList<>();
        pds.add(METADATA_SERVICE);
        pds.add(NFLOW_CATEGORY);
        pds.add(NFLOW_NAME);
        this.propDescriptors = Collections.unmodifiableList(pds);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propDescriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        session.transfer(flowFile, REL_REGISTRATION_REQ);
    }

}
