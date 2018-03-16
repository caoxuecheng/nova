/**
 *
 */
package com.onescorpin.metadata.modeshape.nflow;

/*-
 * #%L
 * nova-metadata-modeshape
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

import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowPrecondition;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.common.JcrPropertiesEntity;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasource;
import com.onescorpin.metadata.modeshape.sla.JcrServiceLevelAgreement;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.metadata.modeshape.support.JcrVersionUtil;
import com.onescorpin.metadata.modeshape.template.JcrNflowTemplate;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 *
 */
public class NflowDetails extends JcrPropertiesEntity {

    private static final Logger log = LoggerFactory.getLogger(NflowDetails.class);

    public static final String NODE_TYPE = "tba:nflowDetails";

    public static final String NFLOW_JSON = "tba:json";
    public static final String PROCESS_GROUP_ID = "tba:processGroupId";
    public static final String NFLOW_TEMPLATE = "tba:nflowTemplate";

    public static final String PRECONDITION = "tba:precondition";
    public static final String DEPENDENTS = "tba:dependentNflows";
    public static final String USED_BY_NFLOWS = "tba:usedByNflows";
    public static final String SOURCE_NAME = "tba:sources";
    public static final String DESTINATION_NAME = "tba:destinations";

    public static final String TEMPLATE = "tba:nflowTemplate";
    public static final String SLA = "tba:slas";

    private NflowSummary summary;

    /**
     * @param node
     */
    public NflowDetails(Node node, NflowSummary summary) {
        super(node);
        this.summary = summary;
    }

    protected JcrNflow getParentNflow() {
        return this.summary.getParentNflow();
    }

    protected NflowSummary getParentSummary() {
        return this.summary;
    }

    public List<? extends NflowSource> getSources() {
        return JcrUtil.getJcrObjects(this.node, SOURCE_NAME, JcrNflowSource.class);
    }

    public List<? extends NflowDestination> getDestinations() {
        return JcrUtil.getJcrObjects(this.node, DESTINATION_NAME, JcrNflowDestination.class);
    }

    public <C extends Category> List<Nflow> getDependentNflows() {
        List<Nflow> deps = new ArrayList<>();
        Set<Node> depNodes = JcrPropertyUtil.getSetProperty(this.node, DEPENDENTS);

        for (Node depNode : depNodes) {
            deps.add(new JcrNflow(depNode, this.summary.getParentNflow().getOpsAccessProvider().orElse(null)));
        }

        return deps;
    }

    public boolean addDependentNflow(Nflow nflow) {
        JcrNflow dependent = (JcrNflow) nflow;
        Node depNode = dependent.getNode();
        nflow.addUsedByNflow(getParentNflow());

        return JcrPropertyUtil.addToSetProperty(this.node, DEPENDENTS, depNode, true);
    }

    public boolean removeDependentNflow(Nflow nflow) {
        JcrNflow dependent = (JcrNflow) nflow;
        Node depNode = dependent.getNode();
        nflow.removeUsedByNflow(getParentNflow());

        boolean weakRef = false;
        Optional<Property> prop = JcrPropertyUtil.findProperty(this.node, DEPENDENTS);
        if (prop.isPresent()) {
            try {
                weakRef = PropertyType.WEAKREFERENCE == prop.get().getType();
            } catch (RepositoryException e) {
                log.error("Error removeDependentNflow for {}.  Unable to identify if the property is a Weak Reference or not {} ", nflow.getName(), e.getMessage(), e);
            }
        }
        return JcrPropertyUtil.removeFromSetProperty(this.node, DEPENDENTS, depNode, weakRef);
    }

    public boolean addUsedByNflow(Nflow nflow) {
        JcrNflow dependent = (JcrNflow) nflow;
        Node depNode = dependent.getNode();

        return JcrPropertyUtil.addToSetProperty(this.node, USED_BY_NFLOWS, depNode, true);
    }

    public List<Nflow> getUsedByNflows() {
        List<Nflow> deps = new ArrayList<>();
        Set<Node> depNodes = JcrPropertyUtil.getSetProperty(this.node, USED_BY_NFLOWS);

        for (Node depNode : depNodes) {
            deps.add(new JcrNflow(depNode, this.summary.getParentNflow().getOpsAccessProvider().orElse(null)));
        }

        return deps;
    }

    public boolean removeUsedByNflow(Nflow nflow) {
        JcrNflow dependent = (JcrNflow) nflow;
        Node depNode = dependent.getNode();
        boolean weakRef = false;
        Optional<Property> prop = JcrPropertyUtil.findProperty(this.node, USED_BY_NFLOWS);
        if (prop.isPresent()) {
            try {
                weakRef = PropertyType.WEAKREFERENCE == prop.get().getType();
            } catch (RepositoryException e) {
                log.error("Error removeUsedByNflow for {}.  Unable to identify if the property is a Weak Reference or not {} ", nflow.getName(), e.getMessage(), e);
            }
        }
        return JcrPropertyUtil.removeFromSetProperty(this.node, USED_BY_NFLOWS, depNode, weakRef);
    }

    public NflowSource getSource(final Datasource.ID id) {
        List<? extends NflowSource> sources = getSources();
        if (sources != null) {
            return sources.stream().filter(nflowSource -> nflowSource.getDatasource().getId().equals(id)).findFirst().orElse(null);
        }
        return null;
    }

    public NflowDestination getDestination(final Datasource.ID id) {
        List<? extends NflowDestination> destinations = getDestinations();
        if (destinations != null) {
            return destinations.stream().filter(nflowDestination -> nflowDestination.getDatasource().getId().equals(id)).findFirst().orElse(null);
        }
        return null;
    }

    public NflowPrecondition getPrecondition() {
        try {
            if (this.node.hasNode(PRECONDITION)) {
                return new JcrNflowPrecondition(this.node.getNode(PRECONDITION), getParentNflow());
            } else {
                return null;
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the nflow precondition", e);
        }
    }

    public NflowManagerTemplate getTemplate() {
        return getProperty(TEMPLATE, JcrNflowTemplate.class);
    }

    public void setTemplate(NflowManagerTemplate template) {
        setProperty(TEMPLATE, template);
        template.addNflow(getParentNflow());
    }

    public List<ServiceLevelAgreement> getServiceLevelAgreements() {
        Set<Node> list = JcrPropertyUtil.getReferencedNodeSet(this.node, SLA);
        List<ServiceLevelAgreement> serviceLevelAgreements = new ArrayList<>();
        if (list != null) {
            for (Node n : list) {
                serviceLevelAgreements.add(JcrUtil.createJcrObject(n, JcrServiceLevelAgreement.class));
            }
        }
        return serviceLevelAgreements;
    }

    public void setServiceLevelAgreements(List<? extends ServiceLevelAgreement> serviceLevelAgreements) {
        setProperty(SLA, serviceLevelAgreements);
    }

    public void removeServiceLevelAgreement(ServiceLevelAgreement.ID id) {
        try {
            Set<Node> nodes = JcrPropertyUtil.getSetProperty(this.node, SLA);
            Set<Value> updatedSet = new HashSet<>();
            for (Node node : nodes) {
                if (!node.getIdentifier().equalsIgnoreCase(id.toString())) {
                    Value value = this.node.getSession().getValueFactory().createValue(node, true);
                    updatedSet.add(value);
                }
            }
            node.setProperty(SLA, (Value[]) updatedSet.stream().toArray(size -> new Value[size]));
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Unable to remove reference to SLA " + id + "from nflow " + this.getId());
        }

    }

    public boolean addServiceLevelAgreement(ServiceLevelAgreement sla) {
        JcrServiceLevelAgreement jcrServiceLevelAgreement = (JcrServiceLevelAgreement) sla;
        Node node = jcrServiceLevelAgreement.getNode();
        //add a ref to this node
        return JcrPropertyUtil.addToSetProperty(this.node, SLA, node, true);
    }

    public String getJson() {
        return getProperty(NflowDetails.NFLOW_JSON, String.class);
    }

    public void setJson(String json) {
        setProperty(NflowDetails.NFLOW_JSON, json);
    }

    public String getNifiProcessGroupId() {
        return getProperty(NflowDetails.PROCESS_GROUP_ID, String.class);
    }

    public void setNifiProcessGroupId(String id) {
        setProperty(NflowDetails.PROCESS_GROUP_ID, id);
    }

    protected JcrNflowSource ensureNflowSource(JcrDatasource datasource) {
        Node nflowSrcNode = JcrUtil.addNode(getNode(), NflowDetails.SOURCE_NAME, JcrNflowSource.NODE_TYPE);
        return new JcrNflowSource(nflowSrcNode, datasource);
    }

    protected JcrNflowDestination ensureNflowDestination(JcrDatasource datasource) {
        Node nflowDestNode = JcrUtil.addNode(getNode(), NflowDetails.DESTINATION_NAME, JcrNflowDestination.NODE_TYPE);
        return new JcrNflowDestination(nflowDestNode, datasource);
    }

    protected void removeNflowSource(JcrNflowSource source) {
        try {
            source.getNode().remove();
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("nable to remove nflow source for nflow " + getParentSummary().getSystemName(), e);
        }
    }

    protected void removeNflowDestination(JcrNflowDestination dest) {
        try {
            dest.getNode().remove();
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("nable to remove nflow destination for nflow " + getParentSummary().getSystemName(), e);
        }
    }

    protected void removeNflowSources() {
        List<? extends NflowSource> sources = getSources();
        if (sources != null && !sources.isEmpty()) {
            //checkout the nflow
            sources.stream().forEach(source -> {
                try {
                    Node sourceNode = ((JcrNflowSource) source).getNode();
                    ((JcrDatasource) ((JcrNflowSource) source).getDatasource()).removeSourceNode(sourceNode);
                    sourceNode.remove();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    protected void removeNflowDestinations() {
        List<? extends NflowDestination> destinations = getDestinations();
        
        if (destinations != null && !destinations.isEmpty()) {
            destinations.stream().forEach(dest -> {
                try {
                    // Remove the connection nodes
                    Node destNode = ((JcrNflowDestination) dest).getNode();
                    JcrDatasource datasource = (JcrDatasource) dest.getDatasource();
                    datasource.removeDestinationNode(destNode);
                    destNode.remove();
                    
                    // Remove the datasource if there are no referencing nflows
                    if (datasource.getNflowDestinations().isEmpty() && datasource.getNflowSources().isEmpty()) {
                        datasource.remove();
                    }
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
            });
            
            
        }
    }

    protected Node createNewPrecondition() {
        try {
            Node nflowNode = getNode();
            Node precondNode = JcrUtil.getOrCreateNode(nflowNode, NflowDetails.PRECONDITION, JcrNflow.PRECONDITION_TYPE);

            if (precondNode.hasProperty(JcrNflowPrecondition.SLA_REF)) {
                precondNode.getProperty(JcrNflowPrecondition.SLA_REF).remove();
            }
            if (precondNode.hasNode(JcrNflowPrecondition.SLA)) {
                precondNode.getNode(JcrNflowPrecondition.SLA).remove();
            }

            return precondNode.addNode(JcrNflowPrecondition.SLA, JcrNflowPrecondition.SLA_TYPE);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to create the precondition for nflow " + getParentNflow().getId(), e);
        }
    }
}
