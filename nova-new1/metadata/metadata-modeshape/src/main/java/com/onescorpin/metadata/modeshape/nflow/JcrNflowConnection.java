package com.onescorpin.metadata.modeshape.nflow;

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
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowConnection;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.common.JcrObject;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasource;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasourceProvider;
import com.onescorpin.metadata.modeshape.support.JcrUtil;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 */
public abstract class JcrNflowConnection extends JcrObject implements NflowConnection {

    public static final String DATASOURCE = "tba:datasource";

    public JcrNflowConnection(Node node) {
        super(node);
    }

    public JcrNflowConnection(Node node, JcrDatasource datasource) {
        this(node);
        this.setProperty(DATASOURCE, datasource);
    }

    public Datasource getDatasource() {
        return JcrUtil.getReferencedObject(this.node, DATASOURCE, JcrDatasourceProvider.TYPE_RESOLVER);
    }


    @Override
    public Nflow getNflow() {
        try {
            //this.getParent == tba:details.
            //this.getParent.getParent == tba:summary
            //this.getParent.getParent.getParent == tba:nflow
            return JcrUtil.createJcrObject(this.node.getParent().getParent().getParent(), JcrNflow.class);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to access nflow", e);
        }
    }
}
