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
import com.onescorpin.metadata.api.category.CategoryNotFoundException;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.common.AbstractJcrAuditableSystemEntity;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.metadata.modeshape.support.JcrVersionUtil;

import java.util.Optional;

import javax.jcr.Node;

public class NflowSummary extends AbstractJcrAuditableSystemEntity {

    public static final String NODE_TYPE = "tba:nflowSummary";

    public static final String DETAILS = "tba:details";

    public static final String CATEGORY = "tba:category";

    private NflowDetails details;
    private JcrNflow nflow;

    public NflowSummary(Node node, JcrNflow nflow) {
        super(JcrVersionUtil.createAutoCheckoutProxy(node));
        this.nflow = nflow;
    }

    public NflowSummary(Node node, JcrCategory category, JcrNflow nflow) {
        this(node, nflow);
        if (category != null) {
            setProperty(CATEGORY, category);
        }
    }

    public Optional<NflowDetails> getNflowDetails() {
        if (this.details == null) {
            if (JcrUtil.hasNode(getNode(), DETAILS)) {
                this.details = JcrUtil.getJcrObject(getNode(), DETAILS, NflowDetails.class, this);
                return Optional.of(this.details);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(this.details);
        }
    }

    protected Category getCategory(Class<? extends JcrCategory> categoryClass) {
        Category category = null;
        try {
            category = (Category) getProperty(CATEGORY, categoryClass);
        } catch (Exception e) {
            if (category == null) {
                try {
                    category = (Category) JcrUtil.constructNodeObject(this.nflow.getNode().getParent(), categoryClass, null);
                } catch (Exception e2) {
                    throw new CategoryNotFoundException("Unable to find category on Nflow for category type  " + categoryClass + ". Exception: " + e.getMessage(), null);
                }
            }
        }
        if (category == null) {
            throw new CategoryNotFoundException("Unable to find category on Nflow ", null);
        }
        return category;
    }

    protected JcrNflow getParentNflow() {
        return this.nflow;
    }
}
