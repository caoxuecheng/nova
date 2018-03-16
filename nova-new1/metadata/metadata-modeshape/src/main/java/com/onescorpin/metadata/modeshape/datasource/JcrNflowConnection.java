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
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowConnection;
import com.onescorpin.metadata.modeshape.common.JcrObject;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;

/**
 */
public abstract class JcrNflowConnection extends JcrObject implements NflowConnection {

    private static String DATASOURCE_NAME = "tba:datasource";

    public JcrNflowConnection(Node node) {
        super(node);
    }

    public JcrNflowConnection(Node node, JcrDatasource datasource) {
        this(node);
        this.setProperty(DATASOURCE_NAME, datasource);
    }

    public Datasource getDatasource() {

        try {
            PropertyIterator itr = this.node.getProperties();
            while (itr.hasNext()) {
                Property p = itr.nextProperty();
                Value v = p.getValue();
            }
        } catch (Exception e) {

        }

        return getProperty(DATASOURCE_NAME, JcrDatasource.class);
    }


    @Override
    public Nflow getNflow() {
        return null;
    }
}
