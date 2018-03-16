package com.onescorpin.metadata.jpa.sla;

/*-
 * #%L
 * onescorpin-operational-metadata-jpa
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

import com.onescorpin.common.velocity.model.VelocityTemplate;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.event.MetadataChange;
import com.onescorpin.metadata.api.event.MetadataEventListener;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.sla.ServiceLevelAgreementEvent;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflow;
import com.onescorpin.metadata.api.nflow.OpsManagerNflowProvider;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementActionTemplate;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementActionTemplateProvider;
import com.onescorpin.metadata.api.sla.ServiceLevelAgreementDescriptionProvider;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementDescription;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Service
public class JpaServiceLevelAgreementDescriptionProvider implements ServiceLevelAgreementDescriptionProvider {

    private static final Logger log = LoggerFactory.getLogger(JpaServiceLevelAgreementDescriptionProvider.class);
    private JpaServiceLevelAgreementDescriptionRepository serviceLevelAgreementDescriptionRepository;

    @Inject
    private ServiceLevelAgreementProvider slaProvider;

    @Inject
    private OpsManagerNflowProvider nflowProvider;

    @Inject
    private ServiceLevelAgreementActionTemplateProvider serviceLevelAgreementActionTemplateProvider;

    @Inject
    MetadataAccess metadataAccess;

    @Inject
    private MetadataEventService metadataEventService;

    @Inject
    private ServiceLevelAgreementDescriptionCache serviceLevelAgreementDescriptionCache;

    private SlaDeletedListener slaDeletedListener = new SlaDeletedListener();

    @Autowired
    public JpaServiceLevelAgreementDescriptionProvider(JpaServiceLevelAgreementDescriptionRepository serviceLevelAgreementDescriptionRepository) {
        this.serviceLevelAgreementDescriptionRepository = serviceLevelAgreementDescriptionRepository;
    }

    @PostConstruct
    private void init() {
        serviceLevelAgreementDescriptionCache.populateCache();
        metadataEventService.addListener(slaDeletedListener);
    }


    @Override
    public ServiceLevelAgreementDescription findOne(ServiceLevelAgreement.ID id) {
        return serviceLevelAgreementDescriptionRepository.findByIdFetchNflows((ServiceLevelAgreementDescriptionId) resolveId(id.toString()));
    }

    @Override
    public List<ServiceLevelAgreementDescription> findAll() {
        return new ArrayList<>(serviceLevelAgreementDescriptionRepository.findAll());
    }

    /**
     * Updates the Service Level Agreement (SLA) JPA mapping and its relationship to Nflows
     * Called from ModeShape when an SLA is saved/updated
     *
     * @param slaId       the SLA id
     * @param name        the SLA Name
     * @param description the SLA Description
     * @param nflows       a set of Nflow Ids related to this SLA
     */
    @Override
    public void updateServiceLevelAgreement(ServiceLevelAgreement.ID slaId, String name, String description, Set<Nflow.ID> nflows, Set<VelocityTemplate.ID> velocityTemplates) {
        ServiceLevelAgreementDescriptionId id = null;
        if (!(slaId instanceof ServiceLevelAgreementDescriptionId)) {
            id = new ServiceLevelAgreementDescriptionId(slaId.toString());
        } else {
            id = (ServiceLevelAgreementDescriptionId) slaId;
        }
        JpaServiceLevelAgreementDescription serviceLevelAgreementDescription = serviceLevelAgreementDescriptionRepository.findOne(id);
        if (serviceLevelAgreementDescription == null) {
            serviceLevelAgreementDescription = new JpaServiceLevelAgreementDescription();
            serviceLevelAgreementDescription.setSlaId(id);
        }
        serviceLevelAgreementDescription.setName(name);
        serviceLevelAgreementDescription.setDescription(description);
        List<OpsManagerNflow> jpaNflows = null;
        if (nflows != null) {
            List<OpsManagerNflow.ID> nflowIds = nflows.stream().map(f -> nflowProvider.resolveId(f.toString())).collect(Collectors.toList());
            jpaNflows = (List<OpsManagerNflow>) nflowProvider.findByNflowIds(nflowIds);
        }
        if (jpaNflows != null) {
            serviceLevelAgreementDescription.setNflows(new HashSet<>(jpaNflows));
        } else {
            serviceLevelAgreementDescription.setNflows(null);
        }
        serviceLevelAgreementDescriptionRepository.save(serviceLevelAgreementDescription);
        //update the cache
        serviceLevelAgreementDescriptionCache.save(serviceLevelAgreementDescription);
        //save the velocity template relationships
        serviceLevelAgreementActionTemplateProvider.assignTemplateByIds(serviceLevelAgreementDescription, velocityTemplates);
    }

    public ServiceLevelAgreement.ID resolveId(Serializable ser) {
        if (ser instanceof ServiceLevelAgreementDescriptionId) {
            return (ServiceLevelAgreementDescriptionId) ser;
        } else {
            return new ServiceLevelAgreementDescriptionId(ser);
        }
    }

    public List<? extends ServiceLevelAgreementDescription> findForNflow(OpsManagerNflow.ID nflowId) {
        return serviceLevelAgreementDescriptionRepository.findForNflow(nflowId);
    }

    /**
     * Listen for when SLAs are deleted from ModeShape
     */
    private class SlaDeletedListener implements MetadataEventListener<ServiceLevelAgreementEvent> {

        @Override
        public void notify(ServiceLevelAgreementEvent event) {
            if (event.getData().getChange() == MetadataChange.ChangeType.DELETE) {
                serviceLevelAgreementDescriptionCache.deleteByDtoId(event.getData().getId().toString());
                try {
                    ServiceLevelAgreementDescriptionId serviceLevelAgreementDescriptionId = (ServiceLevelAgreementDescriptionId) resolveId(event.getData().getId().toString());
                    ServiceLevelAgreementDescription serviceLevelAgreementDescription = serviceLevelAgreementDescriptionRepository.findOne(serviceLevelAgreementDescriptionId);

                    List<? extends ServiceLevelAgreementActionTemplate> slaTemplates = serviceLevelAgreementActionTemplateProvider.deleteForSlaId(serviceLevelAgreementDescriptionId);

                    if (serviceLevelAgreementDescription != null) {
                        serviceLevelAgreementDescriptionRepository.delete((JpaServiceLevelAgreementDescription) serviceLevelAgreementDescription);
                    }
                } catch (Exception e) {
                    log.error("Unable to delete the Service Level Description for " + event.getData().getName() + " ( " + event.getData().getId());
                }
            }
        }
    }


}
