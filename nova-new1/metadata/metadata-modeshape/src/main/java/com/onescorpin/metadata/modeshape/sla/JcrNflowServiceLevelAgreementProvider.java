package com.onescorpin.metadata.modeshape.sla;

/*-
 * #%L
 * onescorpin-metadata-modeshape
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
import com.onescorpin.metadata.api.PostMetadataConfigAction;
import com.onescorpin.metadata.api.extension.ExtensibleEntity;
import com.onescorpin.metadata.api.extension.ExtensibleEntityProvider;
import com.onescorpin.metadata.api.extension.ExtensibleType;
import com.onescorpin.metadata.api.extension.ExtensibleTypeProvider;
import com.onescorpin.metadata.api.extension.FieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreementProvider;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreementRelationship;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.common.EntityUtil;
import com.onescorpin.metadata.modeshape.extension.JcrExtensibleEntity;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.support.JcrQueryUtil;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 */
public class JcrNflowServiceLevelAgreementProvider implements NflowServiceLevelAgreementProvider, PostMetadataConfigAction {

    @Inject
    NflowProvider nflowProvider;
    @Inject
    private ExtensibleTypeProvider typeProvider;
    @Inject
    private ExtensibleEntityProvider entityProvider;
    @Inject
    private JcrMetadataAccess metadata;

    private AtomicBoolean typeCreated = new AtomicBoolean(false);

    /**
     * Finds All Service Level Agreements and also adds in an related nflows
     * Filter out the Precondition SLAs as those are not to be managed via the SLA screen.
     */
    @Override
    public List<NflowServiceLevelAgreement> findAllAgreements() {
        String query = "SELECT * from [" + JcrServiceLevelAgreement.NODE_TYPE + "] AS sla "
                       + "JOIN [" + JcrNflowServiceLevelAgreementRelationship.NODE_TYPE + "] AS nflowSla ON nflowSla.[" + JcrNflowServiceLevelAgreementRelationship.SLA + "] = sla.[jcr:uuid] ";
                      // + "WHERE ISDESCENDANTNODE('" + EntityUtil.pathForSla()+"') ";
        return queryToList(query, null);

    }

    @Override
    public void run() {
        createType();
    }

    private List<NflowServiceLevelAgreement> queryToList(String query, Map<String, String> params) {
        QueryResult result = null;
        try {
            result = JcrQueryUtil.query(getSession(), query, params);

            List<NflowServiceLevelAgreement> entities = new ArrayList<>();

            if (result != null) {
                try {
                    RowIterator rowIterator = result.getRows();
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.nextRow();
                        Node slaNode = row.getNode("sla");
                        Set<JcrNflow> nflows = null;

                        Node nflowSlaNode = row.getNode("nflowSla");
                        //Left Join will result in the node being NULL if its not there
                        if (nflowSlaNode != null) {
                            JcrNflowServiceLevelAgreementRelationship nflowServiceLevelAgreementRelationship = new JcrNflowServiceLevelAgreementRelationship(nflowSlaNode);
                            nflows = (Set<JcrNflow>) nflowServiceLevelAgreementRelationship.getNflows();
                        }
                        JcrNflowServiceLevelAgreement entity = new JcrNflowServiceLevelAgreement(slaNode, nflows);
                        entities.add(entity);
                    }
                } catch (RepositoryException e) {
                    throw new MetadataRepositoryException("Unable to parse QueryResult to List for nflowSla", e);

                }
            }
            return entities;


        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Unable to execute Nflow SLA Query.  Query is: " + query, e);
        }
    }

    public NflowServiceLevelAgreement findAgreement(ServiceLevelAgreement.ID id) {
        String query = "SELECT * from [" + JcrServiceLevelAgreement.NODE_TYPE + "] AS sla "
                       + "LEFT JOIN [" + JcrNflowServiceLevelAgreementRelationship.NODE_TYPE + "] AS nflowSla ON nflowSla.[" + JcrNflowServiceLevelAgreementRelationship.SLA + "] = sla.[jcr:uuid] "
                       + "WHERE sla.[jcr:uuid] = $slaId ";
        Map<String, String> bindParams = new HashMap<>();
        bindParams.put("slaId", id.toString());
        List<NflowServiceLevelAgreement> list = queryToList(query, bindParams);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;

    }


    public List<NflowServiceLevelAgreement> findNflowServiceLevelAgreements(Nflow.ID nflowId) {
        String query = "SELECT * from [" + JcrServiceLevelAgreement.NODE_TYPE + "] AS sla "
                       + "JOIN [" + JcrNflowServiceLevelAgreementRelationship.NODE_TYPE + "] AS nflowSla ON nflowSla.[" + JcrNflowServiceLevelAgreementRelationship.SLA + "] = sla.[jcr:uuid] "
                       + "WHERE nflowSla.[" + JcrNflowServiceLevelAgreementRelationship.NFLOWS + "] IN ('" + nflowId.toString() + "')";
        List<NflowServiceLevelAgreement> list = queryToList(query, null);
        return list;
    }


    public List<ExtensibleEntity> findAllRelationships() {
        return entityProvider.getEntities(JcrNflowServiceLevelAgreementRelationship.NODE_TYPE);
    }

    public NflowServiceLevelAgreementRelationship findRelationship(ServiceLevelAgreement.ID id) {
        List<? extends ExtensibleEntity> entities = entityProvider.findEntitiesMatchingProperty(JcrNflowServiceLevelAgreementRelationship.NODE_TYPE, JcrNflowServiceLevelAgreementRelationship.SLA, id);
        if (entities != null && !entities.isEmpty()) {
            return new JcrNflowServiceLevelAgreementRelationship((JcrExtensibleEntity) entities.get(0));
        }
        return null;
    }

    /**
     * Returns the relationship object for SLA to a Set of Nflows
     */
    public NflowServiceLevelAgreementRelationship getRelationship(ExtensibleEntity.ID id) {
        ExtensibleEntity entity = entityProvider.getEntity(id);
        return new JcrNflowServiceLevelAgreementRelationship((JcrExtensibleEntity) entity);
    }


    /**
     * Creates the Extensible Entity Type
     */
    public String createType() {
        // TODO service?
        if (typeCreated.compareAndSet(false, true)) {
            return metadata.commit(() -> {
                ExtensibleType nflowSla = typeProvider.getType(JcrNflowServiceLevelAgreementRelationship.TYPE_NAME);
                if (nflowSla == null) {
                    nflowSla = typeProvider.buildType(JcrNflowServiceLevelAgreementRelationship.TYPE_NAME)
                        // @formatter:off
                         .field(JcrNflowServiceLevelAgreementRelationship.NFLOWS)
                            .type(FieldDescriptor.Type.WEAK_REFERENCE)
                            .displayName("Nflows")
                            .description("The Nflows referenced on this SLA")
                            .required(false)
                            .collection(true)
                            .add()
                        .field(JcrNflowServiceLevelAgreementRelationship.SLA)
                            .type(FieldDescriptor.Type.WEAK_REFERENCE)
                            .displayName("SLA")
                            .description("The SLA")
                            .required(true)
                            .add()
                        .build();
                        // @formatter:on
                }

                return nflowSla.getName();
            }, MetadataAccess.SERVICE);
        }
        return null;
    }


    /**
     * Create/Update the Relationship between a SLA and a Set of Nflows
     */
    @Override
    public NflowServiceLevelAgreementRelationship relate(ServiceLevelAgreement sla, Set<Nflow.ID> nflowIds) {
        JcrNflowServiceLevelAgreementRelationship relationship = null;
        //find if this relationship already exists
        Set<Node> nflowNodes = new HashSet<>();
        Map<String, Object> props = new HashMap<>();
        Set<Nflow> nflows = new HashSet<>();
        for (Nflow.ID nflowId : nflowIds) {
            Nflow nflow = nflowProvider.getNflow(nflowId);
            if (nflow != null) {
                nflows.add(nflow);
            }
        }
        return relateNflows(sla, nflows);
    }

    @Override
    public NflowServiceLevelAgreementRelationship relateNflows(ServiceLevelAgreement sla, Set<Nflow> nflows) {
        JcrNflowServiceLevelAgreementRelationship relationship = null;
        //find if this relationship already exists
        JcrNflowServiceLevelAgreementRelationship nflowSla = (JcrNflowServiceLevelAgreementRelationship) findRelationship(sla.getId());
        Set<Node> nflowNodes = new HashSet<>();
        Map<String, Object> props = new HashMap<>();
        for (Nflow nflow : nflows) {
            if (nflow != null) {
                JcrNflow jcrNflow = (JcrNflow) nflow;
                nflowNodes.add(jcrNflow.getNode());
            }
        }
        props.put(JcrNflowServiceLevelAgreementRelationship.NFLOWS, nflowNodes);
        props.put(JcrNflowServiceLevelAgreementRelationship.SLA, ((JcrServiceLevelAgreement) sla).getNode());

        //remove any existing relationships
        removeNflowRelationships(sla.getId());
        if (nflowSla == null) {

            ExtensibleType type = typeProvider.getType(JcrNflowServiceLevelAgreementRelationship.TYPE_NAME);
            JcrExtensibleEntity entity = (JcrExtensibleEntity) entityProvider.createEntity(type, props);

            relationship = new JcrNflowServiceLevelAgreementRelationship(entity.getNode());
        } else {
            JcrExtensibleEntity entity = (JcrExtensibleEntity) entityProvider.updateEntity(nflowSla, props);
            relationship = new JcrNflowServiceLevelAgreementRelationship(entity.getNode());
        }
        //update the nflow relationships
        for (Nflow nflow : nflows) {
            nflowProvider.updateNflowServiceLevelAgreement(nflow.getId(), sla);
        }

        return relationship;
    }


    @Override
    public boolean removeNflowRelationships(ServiceLevelAgreement.ID id) {
        JcrNflowServiceLevelAgreementRelationship extensibleEntity = (JcrNflowServiceLevelAgreementRelationship) findRelationship(id);
        if (extensibleEntity != null) {
            return extensibleEntity.removeNflowRelationships(id);
        } else {
            return false;
        }
    }

    public boolean removeAllRelationships(ServiceLevelAgreement.ID id) {
        try {
            JcrNflowServiceLevelAgreementRelationship extensibleEntity = (JcrNflowServiceLevelAgreementRelationship) findRelationship(id);
            if (extensibleEntity != null) {
                extensibleEntity.removeNflowRelationships(id);
                extensibleEntity.getNode().remove();
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("unable to remove Nflow SLA relationships for SLA " + id, e);
        }
        return false;
    }


    protected Session getSession() {
        return JcrMetadataAccess.getActiveSession();
    }


}
