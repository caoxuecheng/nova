package com.onescorpin.metadata.event.jms;

/*-
 * #%L
 * onescorpin-metadata-rest-controller
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

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.CleanupTriggerEvent;
import com.onescorpin.metadata.api.event.nflow.PreconditionTriggerEvent;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.rest.model.event.NflowCleanupTriggerEvent;
import com.onescorpin.metadata.rest.model.event.NflowPreconditionTriggerEvent;

import org.springframework.jms.core.JmsMessagingTemplate;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.Queue;

/**
 * Listens for metadata events that should be transferred to a JMS topic.
 */
public class JmsChangeEventDispatcher {

    /**
     * Event listener for cleanup events
     */
    private final MetadataEventListener<CleanupTriggerEvent> cleanupListener = new CleanupTriggerDispatcher();

    /**
     * Event listener for precondition events
     */
    private final MetadataEventListener<PreconditionTriggerEvent> preconditionListener = new PreconditionTriggerDispatcher();

    /**
     * JMS topic for triggering nflows for cleanup
     */
    @Inject
    @Named("cleanupTriggerQueue")
    private Queue cleanupTriggerQueue;
    /**
     * Metadata event bus
     */
    @Inject
    private MetadataEventService eventService;
    /**
     * Nflow object provider
     */
    @Inject
    private NflowProvider nflowProvider;
    /**
     * Spring JMS messaging template
     */
    @Inject
    private JmsMessagingTemplate jmsMessagingTemplate;
    /**
     * Metadata transaction wrapper
     */
    @Inject
    private MetadataAccess metadata;
    /**
     * JMS topic for triggering nflows based on preconditions
     */
    @Inject
    @Named("preconditionTriggerQueue")
    private Queue preconditionTriggerQueue;

    /**
     * Adds listeners for transferring events.
     */
    @PostConstruct
    public void addEventListener() {
        eventService.addListener(cleanupListener);
        eventService.addListener(preconditionListener);
    }

    /**
     * Removes listeners and stops transferring events.
     */
    @PreDestroy
    public void removeEventListener() {
        eventService.removeListener(cleanupListener);
        eventService.removeListener(preconditionListener);
    }

    /**
     * Transfers cleanup events to JMS.
     */
    private class CleanupTriggerDispatcher implements MetadataEventListener<CleanupTriggerEvent> {

        @Override
        public void notify(@Nonnull final CleanupTriggerEvent metadataEvent) {
            NflowCleanupTriggerEvent jmsEvent = new NflowCleanupTriggerEvent(metadataEvent.getData().toString());

            metadata.read(() -> {
                Nflow nflow = nflowProvider.getNflow(metadataEvent.getData());
                jmsEvent.setNflowName(nflow.getName());
                jmsEvent.setCategoryName(nflow.getCategory().getSystemName());
                return jmsEvent;
            }, MetadataAccess.SERVICE);

            jmsMessagingTemplate.convertAndSend(cleanupTriggerQueue, jmsEvent);
        }
    }

    /**
     * Transfers precondition events to JMS.
     */
    private class PreconditionTriggerDispatcher implements MetadataEventListener<PreconditionTriggerEvent> {

        @Override
        public void notify(@Nonnull final PreconditionTriggerEvent event) {
            NflowPreconditionTriggerEvent triggerEv = new NflowPreconditionTriggerEvent(event.getData().toString());

            metadata.read(() -> {
                Nflow nflow = nflowProvider.getNflow(event.getData());
                triggerEv.setNflowName(nflow.getName());
                triggerEv.setCategory(nflow.getCategory().getSystemName());
                return triggerEv;
            }, MetadataAccess.SERVICE);

            jmsMessagingTemplate.convertAndSend(preconditionTriggerQueue, triggerEv);
        }
    }
}
