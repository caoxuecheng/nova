package com.onescorpin.metadata.modeshape.datasource;

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

import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.common.AbstractJcrAuditableSystemEntity;
import com.onescorpin.metadata.modeshape.common.JcrEntity;
import com.onescorpin.metadata.modeshape.nflow.JcrNflowDestination;
import com.onescorpin.metadata.modeshape.nflow.JcrNflowSource;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 */
public class JcrDatasource extends AbstractJcrAuditableSystemEntity implements Datasource {


    public static final String NODE_TYPE = "tba:datasource";
    public static final String SOURCE_NAME = "tba:nflowSources";
    public static final String DESTINATION_NAME = "tba:nflowDestinations";

    public static final String TYPE_NAME = "datasourceType";


    public JcrDatasource(Node node) {
        super(node);
    }


    @Override
    public DatasourceId getId() {
        try {
            return new JcrDatasource.DatasourceId(getObjectId());
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the entity id", e);
        }
    }

    public List<JcrNflowSource> getSources() {
        return JcrUtil.getJcrObjects(this.node, SOURCE_NAME, JcrNflowSource.class);
    }

    public void setSources(List<NflowSource> sources) {
        JcrPropertyUtil.setProperty(this.node, SOURCE_NAME, null);

        for (NflowSource src : sources) {
            Node destNode = ((JcrNflowSource) src).getNode();
            addSourceNode(destNode);
        }
    }

    public List<JcrNflowDestination> getDestinations() {
        return JcrUtil.getJcrObjects(this.node, DESTINATION_NAME, JcrNflowDestination.class);
    }

    public void setDestinations(List<NflowDestination> destinations) {
        JcrPropertyUtil.setProperty(this.node, DESTINATION_NAME, null);

        for (NflowDestination dest : destinations) {
            Node destNode = ((JcrNflowSource) dest).getNode();
            addDestinationNode(destNode);
        }
    }

    @Override
    public String getName() {
        return super.getProperty(TITLE, String.class);
    }

    @Override
    public String getDescription() {
        return super.getProperty(DESCRIPTION, String.class);
    }

    @Override
    public Set<? extends NflowSource> getNflowSources() {
        return JcrPropertyUtil.getReferencedNodeSet(this.node, SOURCE_NAME).stream()
            .map(n -> JcrUtil.createJcrObject(n, JcrNflowSource.class))
            .collect(Collectors.toSet());
    }

    @Override
    public Set<? extends NflowDestination> getNflowDestinations() {
        return JcrPropertyUtil.getReferencedNodeSet(this.node, DESTINATION_NAME).stream()
            .map(n -> JcrUtil.createJcrObject(n, JcrNflowDestination.class))
            .collect(Collectors.toSet());
    }

    public void addSourceNode(Node node) {
        JcrPropertyUtil.addToSetProperty(this.node, SOURCE_NAME, node, true);
    }

    public void removeSourceNode(Node node) {
        JcrPropertyUtil.removeFromSetProperty(this.node, SOURCE_NAME, node);
    }

    public void addDestinationNode(Node node) {
        JcrPropertyUtil.addToSetProperty(this.node, DESTINATION_NAME, node, true);
    }

    public void removeDestinationNode(Node node) {
        JcrPropertyUtil.removeFromSetProperty(this.node, DESTINATION_NAME, node);
    }

    public static class DatasourceId extends JcrEntity.EntityId implements Datasource.ID {

        public DatasourceId(Serializable ser) {
            super(ser);
        }
    }


}
