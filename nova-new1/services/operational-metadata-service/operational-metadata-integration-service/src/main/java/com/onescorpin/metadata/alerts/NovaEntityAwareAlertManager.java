/**
 *
 */
package com.onescorpin.metadata.alerts;

/*-
 * #%L
 * onescorpin-alerts-default
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

import com.google.common.collect.ImmutableMap;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.onescorpin.alerts.api.Alert;
import com.onescorpin.alerts.api.AlertCriteria;
import com.onescorpin.alerts.api.EntityAlert;
import com.onescorpin.alerts.spi.AlertManager;
import com.onescorpin.alerts.spi.AlertSource;
import com.onescorpin.alerts.spi.EntityIdentificationAlertContent;
import com.onescorpin.alerts.spi.defaults.DefaultAlertCriteria;
import com.onescorpin.alerts.spi.defaults.DefaultAlertManager;
import com.onescorpin.alerts.spi.defaults.NovaEntityAwareAlertCriteria;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataChange;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowChangeEvent;
import com.onescorpin.metadata.api.event.sla.ServiceLevelAgreementEvent;
import com.onescorpin.metadata.jpa.alerts.JpaAlert;
import com.onescorpin.metadata.jpa.alerts.JpaAlertChangeEvent;
import com.onescorpin.metadata.jpa.alerts.JpaAlertRepository;
import com.onescorpin.metadata.jpa.sla.QJpaServiceLevelAgreementDescription;
import com.onescorpin.metadata.jpa.support.CommonFilterTranslations;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.role.SecurityRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 *
 */
public class NovaEntityAwareAlertManager extends DefaultAlertManager {

    private static final Logger log = LoggerFactory.getLogger(NovaEntityAwareAlertManager.class);

    @Inject
    private JPAQueryFactory queryFactory;

    @Inject
    private AccessController controller;


    private NflowDeletedListener nflowDeletedListener = new NflowDeletedListener();

    private SlaDeletedListener slaDeletedListener = new SlaDeletedListener();

    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private MetadataEventService metadataEventService;

    private JpaAlertRepository repository;

    private AlertSource.ID identifier = new NovaEntityAwareAlertManagerAlertManagerId();

    public static final ImmutableMap<String, String> alertSlaFilters =
        new ImmutableMap.Builder<String, String>()
            .put("sla", "name")
            .put("slaId", "slaId.uuid")
            .put("slaDescription", "description").build();


    public NovaEntityAwareAlertManager(JpaAlertRepository repo) {
        super(repo);
        CommonFilterTranslations.addFilterTranslations(QJpaServiceLevelAgreementDescription.class, alertSlaFilters);
    }

    @PostConstruct
    private void setupListeners() {
        metadataEventService.addListener(nflowDeletedListener);
        metadataEventService.addListener(slaDeletedListener);
    }

    @Override
    public ID getId() {
        return identifier;
    }

    public NovaEntityAwareAlertCriteria criteria() {
        return new NovaEntityAwareAlertCriteria(queryFactory, controller);
    }


    protected NovaEntityAwareAlertCriteria ensureAlertCriteriaType(AlertCriteria criteria) {
        if (criteria == null) {
            return criteria();
        } else if (criteria instanceof DefaultAlertCriteria && !(criteria instanceof NovaEntityAwareAlertCriteria)) {
            NovaEntityAwareAlertCriteria novaEntityAwareAlertCriteria = criteria();
            ((DefaultAlertCriteria) criteria).transfer(novaEntityAwareAlertCriteria);
            return novaEntityAwareAlertCriteria;
        }
        return (NovaEntityAwareAlertCriteria) criteria;

    }

    @Override
    public Optional<Alert> getAlert(Alert.ID id) {
        return super.getAlert(id);
    }

    @Override
    protected Alert asValue(Alert alert) {
        return new ImmutableEntityAlert(alert, this);
    }


    public Iterator<Alert> entityDeleted(AlertCriteria criteria, String message) {

        log.info("Query for Entity Alerts data");
        List<Alert> handledAlerts = this.metadataAccess.commit(() -> {
            List<Alert> alerts = new ArrayList<>();
            criteria.state(Alert.State.UNHANDLED);
            NovaEntityAwareAlertCriteria critImpl = ensureAlertCriteriaType(criteria);
            critImpl.createEntityQuery().fetch().stream().forEach(jpaAlert -> {
                JpaAlertChangeEvent event = new JpaAlertChangeEvent(Alert.State.HANDLED, MetadataAccess.SERVICE, message, null);
                jpaAlert.addEvent(event);
                //hide it
                jpaAlert.setCleared(true);
                alerts.add(asValue(jpaAlert));
            });
            return alerts;
        }, MetadataAccess.SERVICE);
        return handledAlerts.iterator();
    }

    public static class NovaEntityAwareAlertManagerAlertManagerId implements ID {


        private static final long serialVersionUID = 7691516770322504702L;

        private String idValue = NovaEntityAwareAlertManager.class.getSimpleName().hashCode() + "";


        public NovaEntityAwareAlertManagerAlertManagerId() {
        }


        public String getIdValue() {
            return idValue;
        }

        @Override
        public String toString() {
            return idValue;
        }

    }

    protected static class ImmutableEntityAlert extends ImmutableAlert implements EntityAlert {

        private String entityId;
        private String entityType;

        public ImmutableEntityAlert(Alert alert, AlertManager mgr) {
            super(alert, mgr);
            if (alert instanceof JpaAlert) {
                entityId = ((JpaAlert) alert).getEntityId() != null ? ((JpaAlert) alert).getEntityId().toString() : null;
                entityType = ((JpaAlert) alert).getEntityType();
            }

        }

        @Override
        public Serializable getEntityId() {
            return entityId;
        }

        @Override
        public String getEntityType() {
            return entityType;
        }
    }

    private class NflowDeletedListener implements MetadataEventListener<NflowChangeEvent> {

        @Override
        public void notify(NflowChangeEvent event) {
            if (event.getData().getChange() == MetadataChange.ChangeType.DELETE) {
                NovaEntityAwareAlertCriteria criteria = criteria().entityCriteria(new EntityIdentificationAlertContent(event.getData().getNflowId().toString(), SecurityRole.ENTITY_TYPE.NFLOW));
                entityDeleted(criteria, "The Nflow " + event.getData().getNflowName() + " has been deleted");
                updateLastUpdatedTime();
            }
        }
    }

    private class SlaDeletedListener implements MetadataEventListener<ServiceLevelAgreementEvent> {

        @Override
        public void notify(ServiceLevelAgreementEvent event) {
            if (event.getData().getChange() == MetadataChange.ChangeType.DELETE) {
                NovaEntityAwareAlertCriteria criteria = criteria().entityCriteria(new EntityIdentificationAlertContent(event.getData().getId().toString(), SecurityRole.ENTITY_TYPE.SLA));
                entityDeleted(criteria, "The SLA " + event.getData().getName() + " has been deleted");
                updateLastUpdatedTime();
            }
        }
    }


}
