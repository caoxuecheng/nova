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
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.security.core.context.SecurityContextHolder;

import com.onescorpin.metadata.api.event.MetadataChange.ChangeType;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.template.TemplateChange;
import com.onescorpin.metadata.api.event.template.TemplateChangeEvent;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.api.template.NflowManagerTemplateProvider;
import com.onescorpin.metadata.api.template.TemplateDeletionException;
import com.onescorpin.metadata.api.template.security.TemplateAccessControl;
import com.onescorpin.metadata.modeshape.BaseJcrProvider;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.common.EntityUtil;
import com.onescorpin.metadata.modeshape.common.JcrEntity;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedEntityActionsProvider;
import com.onescorpin.metadata.modeshape.support.JcrQueryUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;

/**
 */
public class JcrNflowTemplateProvider extends BaseJcrProvider<NflowManagerTemplate, NflowManagerTemplate.ID> implements NflowManagerTemplateProvider {

    @Inject
    private MetadataEventService metadataEventService;
    
    @Inject
    private SecurityRoleProvider roleProvider;

    @Inject
    private JcrAllowedEntityActionsProvider actionsProvider;
    
    @Inject
    private AccessController accessController;

    @Override
    public Class<? extends NflowManagerTemplate> getEntityClass() {
        return JcrNflowTemplate.class;
    }

    @Override
    public Class<? extends JcrEntity> getJcrEntityClass() {
        return JcrNflowTemplate.class;
    }

    @Override
    public String getNodeType(Class<? extends JcrEntity> jcrEntityType) {
        return JcrNflowTemplate.NODE_TYPE;
    }

    @Override
    protected String getEntityQueryStartingPath() {
        return EntityUtil.pathForTemplates();
    }


    public NflowManagerTemplate ensureTemplate(String systemName) {
        String sanitizedName = sanitizeSystemName(systemName);
        String path = EntityUtil.pathForTemplates();
        Map<String, Object> props = new HashMap<>();
        props.put(JcrNflowTemplate.TITLE, sanitizedName);
        boolean newTemplate = !JcrUtil.hasNode(getSession(), path, sanitizedName);
        JcrNflowTemplate template = (JcrNflowTemplate) findOrCreateEntity(path, sanitizedName, props);

        if (newTemplate) {
            if (this.accessController.isEntityAccessControlled()) {
                List<SecurityRole> roles = this.roleProvider.getEntityRoles(SecurityRole.TEMPLATE);
                this.actionsProvider.getAvailableActions(AllowedActions.TEMPLATE)
                    .ifPresent(actions -> template.enableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser(), roles));
            } else {
                this.actionsProvider.getAvailableActions(AllowedActions.TEMPLATE)
                .ifPresent(actions -> template.disableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser()));
            }
            
            addPostNflowChangeAction(template, ChangeType.CREATE);
        }

        return template;
    }



    @Override
    public NflowManagerTemplate findByName(String name) {
        if (StringUtils.isNotBlank(name)) {
            String sanitizedTitle = sanitizeTitle(name);
            String query = "SELECT * from " + EntityUtil.asQueryProperty(JcrNflowTemplate.NODE_TYPE) + " as e where e." + EntityUtil.asQueryProperty(JcrNflowTemplate.TITLE) + " = $title ";
            query = applyFindAllFilter(query,EntityUtil.pathForTemplates());
            Map<String, String> bindParams = new HashMap<>();
            bindParams.put("title", sanitizedTitle);
            return JcrQueryUtil.findFirst(getSession(), query, bindParams, JcrNflowTemplate.class);
        } else {
            return null;
        }
    }

    @Override
    public NflowManagerTemplate findByNifiTemplateId(String nifiTemplateId) {
        String
            query =
            "SELECT * from " + EntityUtil.asQueryProperty(JcrNflowTemplate.NODE_TYPE) + " as e where e." + EntityUtil.asQueryProperty(JcrNflowTemplate.NIFI_TEMPLATE_ID) + " = $nifiTemplateId ";
        query = applyFindAllFilter(query,EntityUtil.pathForTemplates());
        Map<String, String> bindParams = new HashMap<>();
        bindParams.put("nifiTemplateId", nifiTemplateId);
        return JcrQueryUtil.findFirst(getSession(), query, bindParams, JcrNflowTemplate.class);

    }

    public NflowManagerTemplate.ID resolveId(Serializable fid) {
        return new JcrNflowTemplate.NflowTemplateId(fid);
    }


    @Override
    public NflowManagerTemplate enable(NflowManagerTemplate.ID id) {
        JcrNflowTemplate template = (JcrNflowTemplate) findById(id);
        if (template != null) {
            if (!template.isEnabled()) {
                template.enable();
                addPostNflowChangeAction(template, ChangeType.UPDATE);
                return update(template);
            }
            return template;
        } else {
            throw new MetadataRepositoryException("Unable to find template with id" + id);
        }
    }

    @Override
    public NflowManagerTemplate disable(NflowManagerTemplate.ID id) {
        JcrNflowTemplate template = (JcrNflowTemplate) findById(id);
        if (template != null) {
            if (template.isEnabled()) {
                template.disable();
                addPostNflowChangeAction(template, ChangeType.UPDATE);
                return update(template);
            }
            return template;
        } else {
            throw new MetadataRepositoryException("Unable to find template with id" + id);
        }
    }

    @Override
    public boolean deleteTemplate(NflowManagerTemplate.ID id) throws TemplateDeletionException {
        NflowManagerTemplate item = findById(id);
        return deleteTemplate(item);
    }

    public boolean deleteTemplate(NflowManagerTemplate nflowManagerTemplate) throws TemplateDeletionException {
        if (nflowManagerTemplate != null && (nflowManagerTemplate.getNflows() == null || nflowManagerTemplate.getNflows().size() == 0)) {
            nflowManagerTemplate.getAllowedActions().checkPermission(TemplateAccessControl.DELETE);
            addPostNflowChangeAction(nflowManagerTemplate, ChangeType.DELETE);
            super.delete(nflowManagerTemplate);
            return true;
        } else {
            throw new TemplateDeletionException(nflowManagerTemplate.getName(), nflowManagerTemplate.getId().toString(), "There are still nflows assigned to this template.");
        }
    }

    @Override
    public void delete(NflowManagerTemplate nflowManagerTemplate) {
        deleteTemplate(nflowManagerTemplate);
    }

    @Override
    public void deleteById(NflowManagerTemplate.ID id) {
        deleteTemplate(id);
    }

    /**
     * Registers an action that produces a template change event upon a successful transaction commit.
     *
     * @param template the nflow to being created
     */
    private void addPostNflowChangeAction(NflowManagerTemplate template, ChangeType changeType) {
        NflowManagerTemplate.State state = template.getState();
        NflowManagerTemplate.ID id = template.getId();
        String desc = template.getName();
        DateTime createTime = template.getCreatedTime();
        final Principal principal = SecurityContextHolder.getContext().getAuthentication() != null
                                    ? SecurityContextHolder.getContext().getAuthentication()
                                    : null;

        Consumer<Boolean> action = (success) -> {
            if (success) {
                TemplateChange change = new TemplateChange(changeType, desc, id, state);
                TemplateChangeEvent event = new TemplateChangeEvent(change, createTime, principal);
                metadataEventService.notify(event);
            }
        };

        JcrMetadataAccess.addPostTransactionAction(action);
    }

}
