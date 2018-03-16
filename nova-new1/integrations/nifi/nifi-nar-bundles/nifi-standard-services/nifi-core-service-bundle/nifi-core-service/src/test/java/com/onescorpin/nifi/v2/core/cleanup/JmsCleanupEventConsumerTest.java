package com.onescorpin.nifi.v2.core.cleanup;

/*-
 * #%L
 * onescorpin-nifi-core-service
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

import com.onescorpin.metadata.rest.model.event.NflowCleanupTriggerEvent;
import com.onescorpin.nifi.core.api.cleanup.CleanupListener;

import org.junit.Test;
import org.mockito.Mockito;

public class JmsCleanupEventConsumerTest {

    /**
     * Test consuming and dispatching a cleanup trigger event.
     */
    @Test
    public void test() {
        // Mock event and listener
        final NflowCleanupTriggerEvent event = new NflowCleanupTriggerEvent("NFLOWID");
        event.setCategoryName("cat");
        event.setNflowName("nflow");

        final CleanupListener listener = Mockito.mock(CleanupListener.class);

        // Test receiving and triggering event
        final JmsCleanupEventConsumer consumer = new JmsCleanupEventConsumer();
        consumer.addListener("cat", "nflow", listener);
        consumer.receiveEvent(event);
        Mockito.verify(listener).triggered(event);

        // Test removing listener
        consumer.removeListener(listener);
        consumer.receiveEvent(event);
        Mockito.verifyNoMoreInteractions(listener);
    }
}
