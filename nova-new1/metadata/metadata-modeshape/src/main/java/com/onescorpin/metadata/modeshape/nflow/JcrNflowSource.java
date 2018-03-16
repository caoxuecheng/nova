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

import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasource;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

import javax.jcr.Node;

/**
 *
 */
public class JcrNflowSource extends JcrNflowConnection implements NflowSource {

    public static final String NODE_TYPE = "tba:nflowSource";

    public JcrNflowSource(Node node) {
        super(node);
    }

    public JcrNflowSource(Node node, JcrDatasource datasource) {
        super(node, datasource);
        datasource.addSourceNode(this.node);
    }


    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.NflowSource#getAgreement()
     */
    @Override
    public ServiceLevelAgreement getAgreement() {
        return null;
    }
}
