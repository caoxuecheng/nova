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
import com.onescorpin.metadata.api.sla.NflowServiceLevelAgreement;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;

import java.util.Set;

import javax.jcr.Node;

/**
 * Resulting Query object that gets all SLAs and their related Nflows
 *
 * @see JcrNflowServiceLevelAgreementProvider
 */
public class JcrNflowServiceLevelAgreement extends JcrServiceLevelAgreement implements NflowServiceLevelAgreement {

    private Set<JcrNflow> nflows;


    public JcrNflowServiceLevelAgreement(Node node, Set<JcrNflow> nflows) {
        super(node);
        this.nflows = nflows;
    }

    @Override
    public Set<? extends Nflow> getNflows() {
        return nflows;
    }
}
