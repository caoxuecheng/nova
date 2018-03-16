/**
 *
 */
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

import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasource;

import javax.jcr.Node;

/**
 *
 */
public class JcrNflowDestination extends JcrNflowConnection implements NflowDestination {

    public static final String NODE_TYPE = "tba:nflowDestination";

    /**
     * @param node
     */
    public JcrNflowDestination(Node node) {
        super(node);
    }

    /**
     * @param node
     * @param datasource
     */
    public JcrNflowDestination(Node node, JcrDatasource datasource) {
        super(node, datasource);
        datasource.addDestinationNode(this.node);
    }
}
