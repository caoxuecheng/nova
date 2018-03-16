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

import com.onescorpin.metadata.api.nflow.NflowProperties;
import com.onescorpin.nifi.core.api.metadata.NovaNiFiFlowProvider;
import com.onescorpin.nifi.core.api.metadata.MetadataProvider;
import com.onescorpin.nifi.core.api.metadata.MetadataProviderService;
import com.onescorpin.nifi.core.api.metadata.MetadataRecorder;

import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Properties;

/**
 */
public class PutNflowMetadataTest {

    private static final String METADATA_SERVICE_IDENTIFIER = "MockMetadataProviderService";

    private final TestRunner runner = TestRunners.newTestRunner(PutNflowMetadata.class);


    @Before
    public void setUp() throws Exception {
        // Setup services
        final MetadataProviderService metadataService = new MockMetadataProviderService();

        // Setup test runner
        runner.addControllerService(METADATA_SERVICE_IDENTIFIER, metadataService);
        runner.enableControllerService(metadataService);
        runner.setProperty(TriggerCleanup.METADATA_SERVICE, METADATA_SERVICE_IDENTIFIER);
    }

    @Test
    public void testValidationPasses() {
        runner.setProperty(PutNflowMetadata.CATEGORY_NAME, "cat");
        runner.setProperty(PutNflowMetadata.NFLOW_NAME, "nflow");
        runner.setProperty(PutNflowMetadata.NAMESPACE, "registration");
        runner.setProperty("testProperty1", "myValue1");
        runner.setProperty("testProperty2", "myValue2");
        runner.run();

    }

    @Test(expected = AssertionError.class)
    public void testValidationFailsForInvalidCharacterInFieldName() {
        runner.setProperty(PutNflowMetadata.CATEGORY_NAME, "cat");
        runner.setProperty(PutNflowMetadata.NFLOW_NAME, "nflow");
        runner.setProperty(PutNflowMetadata.NAMESPACE, "registration");
        runner.setProperty("$testProperty1", "myValue1");
        runner.run();

    }

    private static class MockMetadataProviderService extends AbstractControllerService implements MetadataProviderService {

        @Override
        public MetadataProvider getProvider() {
            final MetadataProvider provider = Mockito.mock(MetadataProvider.class);
            Mockito.when(provider.getNflowId(Mockito.anyString(), Mockito.anyString())).then(invocation -> {
                if ("invalid".equals(invocation.getArgumentAt(0, String.class))) {
                    throw new IllegalArgumentException();
                }
                return invocation.getArgumentAt(1, String.class);
            });
            Mockito.when(provider.updateNflowProperties(Mockito.anyString(), Mockito.any(Properties.class))).then(invocation -> {
                Properties properties = new Properties();
                properties.setProperty("TestUpdate", "worked");
                return properties;
            });
            Mockito.when(provider.getNflowProperties(Mockito.anyString())).then(invocation -> {
                final String nflowId = invocation.getArgumentAt(0, String.class);
                if ("disabled".equals(nflowId)) {
                    return new Properties();
                }
                if ("unavailable".equals(nflowId)) {
                    return null;
                }
                Properties properties = new Properties();
                properties.setProperty(NflowProperties.CLEANUP_ENABLED, "true");
                return properties;
            });
            return provider;
        }

        @Override
        public NovaNiFiFlowProvider getNovaNiFiFlowProvider() {
            return null;
        }

        @Override
        public MetadataRecorder getRecorder() {
            return null;
        }
    }

}
