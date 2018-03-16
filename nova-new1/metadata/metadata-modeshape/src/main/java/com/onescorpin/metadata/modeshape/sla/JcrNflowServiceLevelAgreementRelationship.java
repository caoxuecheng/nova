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

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreementRelationship;
import com.onescorpin.metadata.modeshape.extension.JcrExtensibleEntity;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;

import java.util.Set;

import javax.jcr.Node;

/**
 * @see JcrNflowServiceLevelAgreementProvider#relate(ServiceLevelAgreement, Set)
 */
public class JcrNflowServiceLevelAgreementRelationship extends JcrExtensibleEntity implements NflowServiceLevelAgreementRelationship {


    public static final String TYPE_NAME = "nflowSla";
    public static final String NODE_TYPE = "tba:" + TYPE_NAME;
    public static final String NFLOWS = "nflows"; /// list of nflow references on the SLA
    public static final String SLA = "sla"; // a ref to the SLA


    public JcrNflowServiceLevelAgreementRelationship(Node node) {
        super(node);
    }

    public JcrNflowServiceLevelAgreementRelationship(JcrExtensibleEntity extensibleEntity) {
        super(extensibleEntity.getNode());
    }

    @Override
    public ServiceLevelAgreement getAgreement() {
        Node node = (Node) this.getProperty(SLA);
        return new JcrServiceLevelAgreement(node);
    }

    @Override
    public Set<? extends Nflow> getNflows() {
        return getPropertyAsSet(NFLOWS, JcrNflow.class);
    }

    @Override
    public boolean removeNflowRelationships(ServiceLevelAgreement.ID id) {
        @SuppressWarnings("unchecked")
        final Set<JcrNflow> nflows = (Set<JcrNflow>) getNflows();
        if (nflows != null && !nflows.isEmpty()) {
            nflows.stream()
                .filter(nflow -> nflow != null)
                .forEach(nflow -> nflow.removeServiceLevelAgreement(id));
        }
        setProperty(NFLOWS, null);
        return true;


    }


}
