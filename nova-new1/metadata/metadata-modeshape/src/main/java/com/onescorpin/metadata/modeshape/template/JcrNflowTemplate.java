package com.onescorpin.metadata.modeshape.template;

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.common.AbstractJcrAuditableSystemEntity;
import com.onescorpin.metadata.modeshape.common.JcrEntity;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.security.mixin.AccessControlledMixin;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.template.security.JcrTemplateAllowedActions;

/**
 */
public class JcrNflowTemplate extends AbstractJcrAuditableSystemEntity implements NflowManagerTemplate, AccessControlledMixin {

    public static final String NODE_TYPE = "tba:nflowTemplate";

    public static final String STATE = "tba:state";
    public static final String DEFINE_TABLE = "tba:defineTable";
    public static final String DATA_TRANSFORMATION = "tba:dataTransformation";
    public static final String ALLOW_PRECONDITIONS = "tba:allowPreconditions";
    public static final String ICON = "tba:icon";
    public static final String ICON_COLOR = "tba:iconColor";
    public static final String NIFI_TEMPLATE_ID = "tba:nifiTemplateId";
    public static final String NFLOWS = "tba:nflows";
    public static final String ORDER = "tba:order";

    public static final String JSON = "tba:json";

    public static final String IS_STREAM = "tba:isStream";

    public static final String TEMPLATE_TABLE_OPTION = "tba:templateTableOption";


    public JcrNflowTemplate(Node node) {
        super(node);
    }

    @Override
    public NflowTemplateId getId() {
        try {
            return new JcrNflowTemplate.NflowTemplateId(this.node.getIdentifier());
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the entity id", e);
        }
    }

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public void setName(String name) {
        setTitle(name);
    }

    @Override
    public String getNifiTemplateId() {

        return getProperty(NIFI_TEMPLATE_ID, String.class);
    }

    @Override
    public void setNifiTemplateId(String nifiTemplateId) {
        setProperty(NIFI_TEMPLATE_ID, nifiTemplateId);
    }

    @Override
    public boolean isDefineTable() {
        return getProperty(DEFINE_TABLE, Boolean.class);
    }

    @Override
    public void setDefineTable(boolean defineTable) {
        setProperty(DEFINE_TABLE, defineTable);
    }

    @Override
    public boolean isDataTransformation() {
        return getProperty(DATA_TRANSFORMATION, Boolean.class);
    }

    @Override
    public void setDataTransformation(boolean dataTransformation) {
        setProperty(DATA_TRANSFORMATION, dataTransformation);
    }

    @Override
    public boolean isAllowPreconditions() {
        return getProperty(ALLOW_PRECONDITIONS, Boolean.class);
    }

    @Override
    public void setAllowPreconditions(boolean allowedPreconditions) {
        setProperty(ALLOW_PRECONDITIONS, allowedPreconditions);
    }

    @Override
    public String getIcon() {
        return getProperty(ICON, String.class);
    }

    @Override
    public void setIcon(String icon) {
        setProperty(ICON, icon);
    }

    @Override
    public String getIconColor() {
        return getProperty(ICON_COLOR, String.class);
    }

    @Override
    public void setIconColor(String iconColor) {
        setProperty(ICON_COLOR, iconColor);
    }

    @Override
    public String getJson() {
        return getProperty(JSON, String.class);
    }

    @Override
    public void setJson(String json) {
        setProperty(JSON, json);
    }

    public State getState() {
        return getProperty(STATE, NflowManagerTemplate.State.ENABLED);
    }

    @Override
    public void setState(State state) {
        setProperty(STATE, state);
    }

    public boolean isEnabled() {
        return State.ENABLED.equals(getState());
    }

    public void enable() {
        setProperty(STATE, NflowManagerTemplate.State.ENABLED);
    }

    public void disable() {
        setProperty(STATE, State.DISABLED);
    }

    public List<Nflow> getNflows() {
        List<Nflow> nflows = new ArrayList<>();
        Set<Node> nflowNodes = JcrPropertyUtil.getSetProperty(this.node, NFLOWS);

        for (Node depNode : nflowNodes) {
            // TODO: note that nflows instances returned here will not be able to update nflow ops 
            // access through permission changes to their allowed actions.
            nflows.add(new JcrNflow(depNode, (NflowOpsAccessControlProvider) null));
        }

        return nflows;
    }

    public boolean addNflow(Nflow nflow) {
        JcrNflow jcrNflow = (JcrNflow) nflow;
        Node nflowNode = jcrNflow.getNode();

        return JcrPropertyUtil.addToSetProperty(this.node, NFLOWS, nflowNode);
    }

    public boolean removeNflow(Nflow nflow) {
        JcrNflow jcrNflow = (JcrNflow) nflow;
        Node nflowNode = jcrNflow.getNode();

        return JcrPropertyUtil.removeFromSetProperty(this.node, NFLOWS, nflowNode);
    }

    public Long getOrder() {
        return getProperty(ORDER, Long.class);
    }

    public void setOrder(Long order) {
        setProperty(ORDER, order);
    }

    @Override
    public boolean isStream() {
        if (!hasProperty(IS_STREAM)) {
            setStream(false);
        }
        return getProperty(IS_STREAM, Boolean.class, true);
    }

    @Override
    public void setStream(boolean isStream) {
        setProperty(IS_STREAM, isStream);
    }
    
    @Override
    public Class<? extends JcrAllowedActions> getJcrAllowedActionsType() {
        return JcrTemplateAllowedActions.class;
    }

    @Override
    public String getTemplateTableOption() {
        return getProperty(TEMPLATE_TABLE_OPTION, String.class);
    }

    @Override
    public void setTemplateTableOption(final String templateTableOption) {
        setProperty(TEMPLATE_TABLE_OPTION, templateTableOption);
    }

    public static class NflowTemplateId extends JcrEntity.EntityId implements NflowManagerTemplate.ID {

        public NflowTemplateId(Serializable ser) {
            super(ser);
        }
    }

}

