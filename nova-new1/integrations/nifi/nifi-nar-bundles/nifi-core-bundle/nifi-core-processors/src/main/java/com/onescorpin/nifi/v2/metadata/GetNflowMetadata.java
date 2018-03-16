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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.onescorpin.json.ObjectMapperSerializer;
import com.onescorpin.nifi.core.api.metadata.MetadataProviderService;
import com.onescorpin.nifi.processor.AbstractNiFiProcessor;

import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Adds nflow metadata as {@link FlowFile} attributes.
 */
@CapabilityDescription("Adds nflow metadata json as 'nflowJson' flow file attribute. "
                       + "It is then possible to create new Nifi attributes which refer to "
                       + "nflow metadata fields using Nifi's json expressions, "
                       + "e.g. ${nflowJson:jsonPath('$.category.systemName')}. "
                       + "This processor will cache nflow metadata for configurable duration "
                       + "to avoid making unnecessary calls to remote Metadata service if "
                       + "this processor is scheduled to run continuously")
@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@Tags({"nflow", "metadata", "onescorpin"})
public class GetNflowMetadata extends AbstractNiFiProcessor {

    /**
     * Property for the nflow system name
     */
    public static final PropertyDescriptor NFLOW_NAME = new PropertyDescriptor.Builder()
        .name("System nflow name")
        .description("The system name of this nflow. The default is to have this name automatically set when the nflow is created. Normally you do not need to change the default value.")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .defaultValue("${nflow}")
        .expressionLanguageSupported(true)
        .required(true)
        .build();

    /**
     * Property for the category system name
     */
    public static final PropertyDescriptor CATEGORY_NAME = new PropertyDescriptor.Builder()
        .name("System nflow category")
        .description("The category name of this nflow. The default is to have this name automatically set when the nflow is created. Normally you do not need to change the default value.")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .defaultValue("${category}")
        .expressionLanguageSupported(true)
        .required(true)
        .build();

    /**
     * Property for the metadata provider service
     */
    public static final PropertyDescriptor METADATA_SERVICE = new PropertyDescriptor.Builder()
        .name("Metadata Provider Service")
        .description("Service supplying the implementations of the various metadata providers.")
        .identifiesControllerService(MetadataProviderService.class)
        .required(true)
        .build();

    public static final PropertyDescriptor CACHE_EXPIRE_DURATION = new PropertyDescriptor.Builder()
        .name("Cache Expiry Duration")
        .description("The length of time after which this processor will request updated version of nflow metadata from remote Metadata service")
        .required(true)
        .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
        .expressionLanguageSupported(false)
        .defaultValue("1 secs")
        .build();

    /**
     * Relationship for transferring {@code FlowFile}s generated from events
     */
    public static final Relationship REL_SUCCESS = new Relationship.Builder()
        .name("Success")
        .description("Relationship followed on successful precondition event.")
        .build();

    /**
     * List of property descriptors
     */
    private static final List<PropertyDescriptor> properties = ImmutableList.of(METADATA_SERVICE, CATEGORY_NAME, NFLOW_NAME, CACHE_EXPIRE_DURATION);

    /**
     * List of relationships
     */
    private static final Set<Relationship> relationships = ImmutableSet.of(REL_SUCCESS);

    private static final int CACHE_SIZE = 1;

    private LoadingCache<NflowKey, String> cachedNflow;

    private static class NflowKey {
        String categoryName;
        String nflowName;

        NflowKey(String categoryName, String nflowName) {
            this.categoryName = categoryName;
            this.nflowName = nflowName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            NflowKey other = (NflowKey) o;

            if (!categoryName.equals(other.categoryName)) {
                return false;
            }
            return nflowName.equals(other.nflowName);
        }

        @Override
        public int hashCode() {
            int result = categoryName.hashCode();
            result = 31 * result + nflowName.hashCode();
            return result;
        }
    }

    /**
     * Initializes resources required to trigger this processor.
     *
     * @param context the process context
     */
    @OnScheduled
    public void onScheduled(@Nonnull final ProcessContext context) {
        getLog().debug("Scheduled");

        TimeUnit timeUnit = TimeUnit.NANOSECONDS;
        Long nanos = context.getProperty(CACHE_EXPIRE_DURATION).asTimePeriod(timeUnit);
        cachedNflow = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(nanos, timeUnit)
            .build(
                new CacheLoader<NflowKey, String>() {
                    public String load(@Nonnull NflowKey key) {
                        return ObjectMapperSerializer.serialize(getMetadataService(context).getProvider().getNflow(key.categoryName, key.nflowName));
                    }
                });
    }

    @Override
    public void onTrigger(@Nonnull final ProcessContext context, @Nonnull final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if (flowFile == null) {
            return;
        }

        String categoryName = context.getProperty(CATEGORY_NAME).evaluateAttributeExpressions(flowFile).getValue();
        String nflowName = context.getProperty(NFLOW_NAME).evaluateAttributeExpressions(flowFile).getValue();
        getLog().debug("Triggered for {}.{}", new Object[]{categoryName, nflowName});

        String nflowJson;
        try {
            nflowJson = cachedNflow.get(new NflowKey(categoryName, nflowName));
        } catch (Exception e) {
            getLog().error("Failure retrieving metadata for nflow: {}.{}", new Object[]{categoryName, nflowName}, e);
            throw new IllegalStateException("Failed to retrieve nflow metadata", e);
        }

        if (nflowJson == null) {
            throw new IllegalStateException(String.format("Failed to retrieve nflow metadata for nflow %s:%s", categoryName, nflowName));
        }

        // Create attributes for FlowFile
        Map<String, String> attributes = Maps.newHashMap();
        attributes.put("nflowJson", nflowJson);

        // Create a FlowFile from the event
        flowFile = session.putAllAttributes(flowFile, attributes);

        getLog().trace("Transferring flow file to Success relationship");

        session.transfer(flowFile, REL_SUCCESS);
    }

    /**
     * Gets the metadata service for the specified context.
     *
     * @param context the process context
     * @return the metadata service
     */
    @Nonnull
    private MetadataProviderService getMetadataService(@Nonnull final ProcessContext context) {
        return context.getProperty(METADATA_SERVICE).asControllerService(MetadataProviderService.class);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

}
