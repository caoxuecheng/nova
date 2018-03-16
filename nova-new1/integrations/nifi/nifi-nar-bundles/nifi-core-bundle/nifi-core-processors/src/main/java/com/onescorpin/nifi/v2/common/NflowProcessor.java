/**
 *
 */
package com.onescorpin.nifi.v2.common;

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

import com.onescorpin.nifi.core.api.metadata.MetadataProvider;
import com.onescorpin.nifi.core.api.metadata.MetadataProviderService;
import com.onescorpin.nifi.core.api.metadata.MetadataRecorder;

import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.onescorpin.nifi.v2.common.CommonProperties.NFLOW_CATEGORY;
import static com.onescorpin.nifi.v2.common.CommonProperties.NFLOW_NAME;

/**
 * An abstract processor that can be configured with the nflow canteory and name and
 * which will look up the nflow's ID.
 */
public abstract class NflowProcessor extends BaseProcessor {

    /**
     * The attribute in the flow file containing nflow ID
     */
    public static final String NFLOW_ID_ATTR = "nflowId";

    private static final Logger log = LoggerFactory.getLogger(NflowProcessor.class);

    private transient MetadataProviderService providerService;

    @OnScheduled
    public void scheduled(ProcessContext context) {
        this.providerService = context.getProperty(CommonProperties.METADATA_SERVICE).asControllerService(MetadataProviderService.class);
    }

    public FlowFile initialize(ProcessContext context, ProcessSession session, FlowFile flowFile) {
        return ensureNflowId(context, session, flowFile);
    }

    @Override
    protected void addProperties(List<PropertyDescriptor> list) {
        super.addProperties(list);
        list.add(CommonProperties.METADATA_SERVICE);
        list.add(CommonProperties.NFLOW_CATEGORY);
        list.add(CommonProperties.NFLOW_NAME);
    }

    protected MetadataProvider getMetadataProvider() {
        return this.providerService.getProvider();
    }

    protected MetadataRecorder getMetadataRecorder() {
        return this.providerService.getRecorder();
    }

    protected String getNflowId(ProcessContext context, FlowFile flowFile) {
        String nflowId = flowFile.getAttribute(NFLOW_ID_ATTR);

        if (nflowId == null) {
            final String category = context.getProperty(NFLOW_CATEGORY).evaluateAttributeExpressions(flowFile).getValue();
            final String nflowName = context.getProperty(NFLOW_NAME).evaluateAttributeExpressions(flowFile).getValue();

            try {
                log.info("Resolving ID for nflow {}/{}", category, nflowName);
                nflowId = getMetadataProvider().getNflowId(category, nflowName);

                if (nflowId != null) {
                    log.info("Resolving id {} for nflow {}/{}", nflowId, category, nflowName);
                    return nflowId;
                } else {
                    log.warn("ID for nflow {}/{} could not be located", category, nflowName);
                    throw new NflowIdNotFoundException(category, nflowName);
                }
            } catch (Exception e) {
                log.error("Failed to retrieve nflow ID", e);
                throw e;
            }
        } else {
            return nflowId;
        }
    }

    protected FlowFile ensureNflowId(ProcessContext context, ProcessSession session, FlowFile flowFile) {
        String nflowId = flowFile.getAttribute(NFLOW_ID_ATTR);

        if (nflowId == null) {
            final String category = context.getProperty(NFLOW_CATEGORY).evaluateAttributeExpressions(flowFile).getValue();
            final String nflowName = context.getProperty(NFLOW_NAME).evaluateAttributeExpressions(flowFile).getValue();

            try {
                log.info("Resolving ID for nflow {}/{}", category, nflowName);
                nflowId = getMetadataProvider().getNflowId(category, nflowName);

                if (nflowId != null) {
                    log.info("Resolving id {} for nflow {}/{}", nflowId, category, nflowName);
                    return session.putAttribute(flowFile, NFLOW_ID_ATTR, nflowId);
                } else {
                    log.warn("ID for nflow {}/{} could not be located", category, nflowName);
                    throw new NflowIdNotFoundException(category, nflowName);
                }
            } catch (Exception e) {
                log.error("Failed to retrieve nflow ID", e);
                throw e;
            }
        } else {
            return flowFile;
        }
    }

}
