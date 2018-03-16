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

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowPrecondition;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.common.JcrObject;
import com.onescorpin.metadata.modeshape.sla.JcrServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAssessment;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 */
public class JcrNflowPrecondition extends JcrObject implements NflowPrecondition {

    public static final String NODE_TYPE = "tba:nflowPrecondition";

    //  public static final String LAST_ASSESSMENT = "tba:lastAssessment";
    public static final String SLA_REF = "tba:slaRef";
    public static final String SLA = "tba:sla";

    public static final String SLA_TYPE = "tba:sla";

    private JcrNflow nflow;

    /**
     *
     */
    public JcrNflowPrecondition(Node node, JcrNflow nflow) {
        super(node);
        this.nflow = nflow;
    }

    public void clear() {
        try {
            if (this.node.hasProperty(SLA_REF)) {
                this.node.getProperty(SLA_REF).remove();
            }
            if (this.node.hasNode(SLA)) {
                this.node.getNode(SLA).remove();
            }

        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to cler the precondition", e);
        }
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.NflowPrecondition#getNflow()
     */
    @Override
    public Nflow getNflow() {
        return this.nflow;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.NflowPrecondition#getAgreement()
     */
    @Override
    public ServiceLevelAgreement getAgreement() {
        try {
            if (this.node.hasNode(SLA)) {
                return new JcrServiceLevelAgreement(this.node.getNode(SLA));
            } else if (this.node.hasProperty(SLA_REF)) {
                return new JcrServiceLevelAgreement(this.node.getProperty(SLA_REF).getNode());
            } else {
                return null;
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the precondition SLA", e);
        }
    }

    @Override
    public ServiceLevelAssessment getLastAssessment() {
        return null;
    }

    @Override
    public void setLastAssessment(ServiceLevelAssessment assmnt) {
        // TODO create assessment
    }

}
