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

/**
 */

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.onescorpin.nifi.core.api.metadata.MetadataProvider;
import com.onescorpin.nifi.core.api.metadata.MetadataProviderService;
import com.onescorpin.nifi.processor.AbstractNiFiProcessor;

import org.apache.nifi.annotation.behavior.DynamicProperty;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;
import org.apache.nifi.expression.AttributeExpression;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;


@CapabilityDescription("Allow you to add 1 or more attributes that will added to the nflow metadata in Nova")
@EventDriven
@InputRequirement(InputRequirement.Requirement.INPUT_ALLOWED)
@Tags({"nflow", "metadata", "onescorpin"})
@DynamicProperty(name = "A nflow attribute to add", value = "The value to set it to", supportsExpressionLanguage = true,
                 description = "Updates a nflow attribute specified by the Dynamic Property's key with the value specified by the Dynamic Property's value")
public class PutNflowMetadata extends AbstractNiFiProcessor {

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
        .description("All FlowFiles are routed to this relationship on success").name("success").build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
        .description("All FlowFiles are routed to this relationship on failure").name("failure").build();
    public static final PropertyDescriptor METADATA_SERVICE = new PropertyDescriptor.Builder()
        .name("Metadata Provider Service")
        .description("Service supplying the implementations of the various metadata providers.")
        .identifiesControllerService(MetadataProviderService.class)
        .required(true)
        .build();
    public static final PropertyDescriptor CATEGORY_NAME = new PropertyDescriptor.Builder()
        .name("Nflow Category")
        .description("They category your nflow is created under")
        .required(true)
        .expressionLanguageSupported(true)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();
    public static final PropertyDescriptor NFLOW_NAME = new PropertyDescriptor.Builder()
        .name("Nflow Name")
        .description("They name of the nflow")
        .required(true)
        .expressionLanguageSupported(true)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();
    public static final PropertyDescriptor NAMESPACE = new PropertyDescriptor.Builder()
        .name("Namespace")
        .description("Namespace for the attributes you create. This value will be prepended to the attribute name for storage in the metadata store  ")
        .required(true)
        .expressionLanguageSupported(true)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();
    private static final String METADATA_FIELD_PREFIX = "nifi";
    private static final Pattern DYNAMIC_ATTRIBUTE_NAME_REGEX = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9:_]+");
    public static final Validator ATTRIBUTE_KEY_DYANMIC_PROPERTY_NAME_VALIDATOR = new Validator() {
        @Override
        public ValidationResult validate(final String subject, final String input, final ValidationContext context) {
            final ValidationResult.Builder builder = new ValidationResult.Builder();
            builder.subject("Property Name").input(subject);

            try {
                if (DYNAMIC_ATTRIBUTE_NAME_REGEX.matcher(subject).matches()) {
                    builder.valid(true);
                } else {
                    builder.valid(false).explanation("Invalid character. The field name must start with a letter or number. The remaining characters may also contain a colon and underscore");
                }


            } catch (final IllegalArgumentException e) {
                builder.valid(false).explanation(e.getMessage());
            }

            return builder.build();
        }
    };
    private static final List<String> PROPERTY_LIST_TO_IGNORE = ImmutableList.of(METADATA_SERVICE.getName(), CATEGORY_NAME.getName(), NFLOW_NAME.getName(), NAMESPACE.getName());
    private static final List<PropertyDescriptor> properties = ImmutableList.of(METADATA_SERVICE, CATEGORY_NAME, NFLOW_NAME, NAMESPACE);
    private static final Set<Relationship> relationships = ImmutableSet.of(REL_SUCCESS, REL_FAILURE);

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(final String propertyDescriptorName) {
        return new PropertyDescriptor.Builder()
            .name(propertyDescriptorName)
            .required(false)
            .addValidator(StandardValidators.createAttributeExpressionLanguageValidator(AttributeExpression.ResultType.STRING, true))
            .addValidator(ATTRIBUTE_KEY_DYANMIC_PROPERTY_NAME_VALIDATOR)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .expressionLanguageSupported(true)
            .dynamic(true)
            .build();
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        final ComponentLog logger = getLog();
        FlowFile flowFile = null;

        try {
            if (context.hasIncomingConnection()) {
                flowFile = session.get();

                // If we have no FlowFile, and all incoming connections are self-loops then we can continue on.
                // However, if we have no FlowFile and we have connections coming from other Processors, then
                // we know that we should run only if we have a FlowFile.
                if (flowFile == null && context.hasNonLoopConnection()) {
                    return;
                }
            }

            final FlowFile incoming = flowFile;

            // Get the nflow id
            String category = context.getProperty(CATEGORY_NAME).evaluateAttributeExpressions(flowFile).getValue();
            String nflow = context.getProperty(NFLOW_NAME).evaluateAttributeExpressions(flowFile).getValue();
            String namespace = context.getProperty(NAMESPACE).evaluateAttributeExpressions(flowFile).getValue();

            getLog().debug("The category is: " + category + " and nflow is " + nflow);

            MetadataProvider metadataProvider = getMetadataService(context).getProvider();

            // Ignore the 3 required properties and send the rest to the metadata server
            Map<PropertyDescriptor, String> properties = context.getProperties();
            Set<PropertyDescriptor> propertyKeys = properties.keySet();

            Properties metadataProperties = new Properties();
            for (PropertyDescriptor property : propertyKeys) {
                String propertyName = property.getName();
                String value = context.getProperty(propertyName).evaluateAttributeExpressions(flowFile).getValue();

                if (!PROPERTY_LIST_TO_IGNORE.contains(propertyName)) {
                    metadataProperties.setProperty(METADATA_FIELD_PREFIX + ":" + namespace + ":" + propertyName, value);
                }
            }

            String nflowId = metadataProvider.getNflowId(category, nflow);
            metadataProvider.updateNflowProperties(nflowId, metadataProperties);

            session.transfer(flowFile, REL_SUCCESS);
        } catch (Exception e) {
            logger.error("Error processing custom nflow metadata", e);
            session.transfer(flowFile, REL_FAILURE);
        }
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

}
