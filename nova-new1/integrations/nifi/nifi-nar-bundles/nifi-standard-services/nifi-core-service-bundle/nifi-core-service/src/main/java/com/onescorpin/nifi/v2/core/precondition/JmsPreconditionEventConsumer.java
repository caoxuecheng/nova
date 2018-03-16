package com.onescorpin.nifi.v2.core.precondition;

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

import com.onescorpin.jms.JmsConstants;
import com.onescorpin.metadata.event.jms.MetadataQueues;
import com.onescorpin.metadata.rest.model.event.NflowPreconditionTriggerEvent;
import com.onescorpin.nifi.core.api.precondition.PreconditionEventConsumer;
import com.onescorpin.nifi.core.api.precondition.PreconditionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Consumes the precondition events in JMS
 */
public class JmsPreconditionEventConsumer implements PreconditionEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(JmsPreconditionEventConsumer.class);

    private ConcurrentMap<String, PreconditionListener> listeners = new ConcurrentHashMap<>();

    /**
     * default constructor
     */
    public JmsPreconditionEventConsumer() {
        LOG.debug("New JmsPreconditionEventConsumer {}", this);
    }

    @JmsListener(destination = MetadataQueues.PRECONDITION_TRIGGER, containerFactory = JmsConstants.JMS_CONTAINER_FACTORY)
    public void receiveEvent(NflowPreconditionTriggerEvent event) {
        LOG.debug("{} Received JMS message - topic: {}, message: {}", this, MetadataQueues.PRECONDITION_TRIGGER, event);
        LOG.info("{} Received nflow precondition trigger event: {}", this, event);

        String key = generateKey(event.getCategory(), event.getNflowName());
        LOG.debug("{} Looking up precondition listener for key '{}'", this, key);

        PreconditionListener listener = this.listeners.get(key);

        if (listener != null) {
            LOG.debug("{} Found precondition listener for key '{}'", this, key);
            listener.triggered(event);
        } else {
            LOG.debug("{} No precondition listeners found for key '{}'", this, key);
        }

    }

    @Override
    public void addListener(String category, String nflowName, PreconditionListener listener) {
        LOG.info("{} Adding listener for '{}.{}'", this, category, nflowName);
        this.listeners.put(generateKey(category, nflowName), listener);
    }

    @Override
    public void removeListener(PreconditionListener listener) {
        LOG.info("{} Removing listener {}", this, listener);
        this.listeners.values().remove(listener);
    }

    private String generateKey(String category, String nflowName) {
        return category + "." + nflowName;
    }
}
