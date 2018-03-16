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

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryNotFoundException;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.datasource.DatasourceNotFoundException;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.event.MetadataChange.ChangeType;
import com.onescorpin.metadata.api.event.MetadataEventService;
import com.onescorpin.metadata.api.event.nflow.NflowChange;
import com.onescorpin.metadata.api.event.nflow.NflowChangeEvent;
import com.onescorpin.metadata.api.event.nflow.NflowPropertyChangeEvent;
import com.onescorpin.metadata.api.event.nflow.PropertyChange;
import com.onescorpin.metadata.api.extension.ExtensibleTypeProvider;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.Nflow.ID;
import com.onescorpin.metadata.api.nflow.NflowCriteria;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowNotFoundExcepton;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.api.nflow.PreconditionBuilder;
import com.onescorpin.metadata.api.nflow.security.NflowAccessControl;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.modeshape.AbstractMetadataCriteria;
import com.onescorpin.metadata.modeshape.BaseJcrProvider;
import com.onescorpin.metadata.modeshape.JcrMetadataAccess;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.common.EntityUtil;
import com.onescorpin.metadata.modeshape.common.JcrEntity;
import com.onescorpin.metadata.modeshape.common.JcrObject;
import com.onescorpin.metadata.modeshape.common.mixin.VersionProviderMixin;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasource;
import com.onescorpin.metadata.modeshape.extension.ExtensionsConstants;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedEntityActionsProvider;
import com.onescorpin.metadata.modeshape.sla.JcrServiceLevelAgreement;
import com.onescorpin.metadata.modeshape.sla.JcrServiceLevelAgreementProvider;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.support.JcrQueryUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.metadata.modeshape.support.JcrVersionUtil;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.Obligation;
import com.onescorpin.metadata.sla.api.ObligationGroup.Condition;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementActionConfiguration;
import com.onescorpin.metadata.sla.spi.ObligationBuilder;
import com.onescorpin.metadata.sla.spi.ObligationGroupBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;
import com.onescorpin.security.AccessController;
import com.onescorpin.security.action.AllowedActions;
import com.onescorpin.security.role.SecurityRole;
import com.onescorpin.security.role.SecurityRoleProvider;
import com.onescorpin.support.NflowNameUtil;

import org.joda.time.DateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

/**
 * A JCR provider for {@link Nflow} objects.
 */
public class JcrNflowProvider extends BaseJcrProvider<Nflow, Nflow.ID> implements NflowProvider, VersionProviderMixin<Nflow, Nflow.ID> {

    private static final String SORT_NFLOW_NAME = "nflowName";
    private static final String SORT_STATE = "state";
    private static final String SORT_CATEGORY_NAME = "category.name";
    private static final String SORT_TEMPLATE_NAME = "templateName";
    private static final String SORT_UPDATE_DATE = "updateDate";

    private static final Map<String, String> JCR_PROP_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put(SORT_NFLOW_NAME, "fs.[tba:systemName]");
        map.put(SORT_STATE, "fdata.[tba:state]");
        map.put(SORT_CATEGORY_NAME, "c.[tba:systemName]");
//        map.put(SORT_TEMPLATE_NAME, "t.[jcr:title]");         // Not supported
        map.put(SORT_TEMPLATE_NAME, "e.[jcr:lastModified]");    // Ignore template name sorting for now and use updateDate
        map.put(SORT_UPDATE_DATE, "e.[jcr:lastModified]");
        JCR_PROP_MAP = Collections.unmodifiableMap(map);
    }

    @Inject
    private CategoryProvider categoryProvider;

    @Inject
    private ServiceLevelAgreementProvider slaProvider;

    @Inject
    private DatasourceProvider datasourceProvider;

    /**
     * JCR node type manager
     */
    @Inject
    private ExtensibleTypeProvider extensibleTypeProvider;

    /**
     * Transaction support
     */
    @Inject
    private MetadataAccess metadataAccess;

    @Inject
    private SecurityRoleProvider roleProvider;

    @Inject
    private JcrAllowedEntityActionsProvider actionsProvider;

    @Inject
    private AccessController accessController;

    @Inject
    private NflowOpsAccessControlProvider opsAccessProvider;

    @Inject
    private MetadataEventService metadataEventService;

    @Override
    public String getNodeType(Class<? extends JcrEntity> jcrEntityType) {
        return JcrNflow.NODE_TYPE;
    }

    @Override
    public Class<JcrNflow> getEntityClass() {
        return JcrNflow.class;
    }

    @Override
    public Class<? extends JcrEntity> getJcrEntityClass() {
        return JcrNflow.class;
    }
    
    @Override
    public Optional<Node> findVersionableNode(ID id) {
        final JcrNflow nflow = (JcrNflow) findById(id);
        if (nflow != null) {
            return nflow.getNflowSummary().map(s -> s.getNode());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Nflow asEntity(ID id, Node versionable) {
        try {
            // The versionable node argument is the summary node.
            Node nflowNode = versionable.getSession().getNodeByIdentifier(id.toString());
            return JcrUtil.getJcrObject(nflowNode, JcrNflow.class, versionable, null);
        } catch (RepositoryException e) {
            throw new NflowNotFoundExcepton(id);
        }
    }

    /* (non-Javadoc)
         * @see com.onescorpin.metadata.modeshape.BaseJcrProvider#create(java.lang.Object)
         */
    @Override
    public Nflow create(Nflow t) {
        JcrNflow nflow = (JcrNflow) super.create(t);
        nflow.setOpsAccessProvider(this.opsAccessProvider);
        return nflow;
    }

    @Override
    public List<Nflow> findAll() {
        List<Nflow> nflows = super.findAll();
        return nflows.stream()
            .map(JcrNflow.class::cast)
            .map(nflow -> {
                nflow.setOpsAccessProvider(opsAccessProvider);
                return nflow;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Nflow findById(ID id) {
        final JcrNflow nflow = (JcrNflow) super.findById(id);
        if (nflow != null) {
            nflow.setOpsAccessProvider(this.opsAccessProvider);
        }
        return nflow;
    }

    @Override
    public Nflow update(Nflow nflow) {

        //   nflow.getCategory().getAllowedActions().checkPermission(CategoryAccessControl.CREATE_NFLOW);
        return super.update(nflow);
    }

    public void removeNflowSources(Nflow.ID nflowId) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);
        nflow.removeNflowSources();
    }

    public void removeNflowSource(Nflow.ID nflowId, Datasource.ID dsId) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);
        JcrNflowSource source = (JcrNflowSource) nflow.getSource(dsId);

        if (source != null) {
            nflow.removeNflowSource(source);
        }
    }

    public void removeNflowDestination(Nflow.ID nflowId, Datasource.ID dsId) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);
        JcrNflowDestination dest = (JcrNflowDestination) nflow.getDestination(dsId);

        if (dest != null) {
            nflow.removeNflowDestination(dest);
        }
    }

    public void removeNflowDestinations(Nflow.ID nflowId) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);
        nflow.removeNflowDestinations();
    }

    @Override
    public NflowSource ensureNflowSource(Nflow.ID nflowId, Datasource.ID dsId) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);
        NflowSource source = nflow.getSource(dsId);

        if (source == null) {
            JcrDatasource datasource = (JcrDatasource) datasourceProvider.getDatasource(dsId);

            if (datasource != null) {
                JcrNflowSource jcrSrc = nflow.ensureNflowSource(datasource);

                save();
                return jcrSrc;
            } else {
                throw new DatasourceNotFoundException(dsId);
            }
        } else {
            return source;
        }
    }

    @Override
    public NflowSource ensureNflowSource(Nflow.ID nflowId, com.onescorpin.metadata.api.datasource.Datasource.ID id, com.onescorpin.metadata.sla.api.ServiceLevelAgreement.ID slaId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NflowDestination ensureNflowDestination(Nflow.ID nflowId, com.onescorpin.metadata.api.datasource.Datasource.ID dsId) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);
        NflowDestination source = nflow.getDestination(dsId);

        if (source == null) {
            JcrDatasource datasource = (JcrDatasource) datasourceProvider.getDatasource(dsId);

            if (datasource != null) {
                JcrNflowDestination jcrDest = nflow.ensureNflowDestination(datasource);

                save();
                return jcrDest;
            } else {
                throw new DatasourceNotFoundException(dsId);
            }
        } else {
            return source;
        }
    }

    @Override
    public Nflow ensureNflow(Category.ID categoryId, String nflowSystemName) {
        Category category = categoryProvider.findById(categoryId);
        return ensureNflow(category.getSystemName(), nflowSystemName);
    }

    /**
     * Ensure the Nflow, but the Category must exist!
     */
    @Override
    public Nflow ensureNflow(String categorySystemName, String nflowSystemName) {
        JcrCategory category = null;
        try {
            String categoryPath = EntityUtil.pathForCategory(categorySystemName);
            Node categoryNode = getSession().getNode(categoryPath);
            if (categoryNode != null) {
                category = JcrUtil.createJcrObject(categoryNode, JcrCategory.class);
            } else {
                category = (JcrCategory) categoryProvider.findBySystemName(categorySystemName);
            }
        } catch (RepositoryException e) {
            throw new CategoryNotFoundException("Unable to find Category for " + categorySystemName, null);
        }

        String nflowParentPath = category.getNflowParentPath();
        boolean newNflow = !hasEntityNode(nflowParentPath, nflowSystemName);
        Node nflowNode = findOrCreateEntityNode(nflowParentPath, nflowSystemName, getJcrEntityClass());
        boolean versionable = JcrVersionUtil.isVersionable(nflowNode);

        JcrNflow nflow = new JcrNflow(nflowNode, category, this.opsAccessProvider);

        nflow.setSystemName(nflowSystemName);

        if (newNflow) {
            if (this.accessController.isEntityAccessControlled()) {
                List<SecurityRole> roles = this.roleProvider.getEntityRoles(SecurityRole.NFLOW);
                this.actionsProvider.getAvailableActions(AllowedActions.NFLOW)
                    .ifPresent(actions -> nflow.enableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser(), roles));
            } else {
                this.actionsProvider.getAvailableActions(AllowedActions.NFLOW)
                    .ifPresent(actions -> nflow.disableAccessControl((JcrAllowedActions) actions, JcrMetadataAccess.getActiveUser()));
            }

            addPostNflowChangeAction(nflow, ChangeType.CREATE);
        }

        return nflow;
    }

    /**
     * Registers an action that produces a nflow change event upon a successful transaction commit.
     *
     * @param nflow the nflow to being created
     */
    private void addPostNflowChangeAction(Nflow nflow, ChangeType changeType) {
        Nflow.State state = nflow.getState();
        Nflow.ID id = nflow.getId();
        String nflowName = nflow.getQualifiedName();
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        Consumer<Boolean> action = (success) -> {
            if (success) {
                NflowChange change = new NflowChange(changeType, nflowName, nflowName,id, state);
                NflowChangeEvent event = new NflowChangeEvent(change, DateTime.now(), principal);
                metadataEventService.notify(event);
            }
        };

        JcrMetadataAccess.addPostTransactionAction(action);
    }

    @Override
    public Nflow ensureNflow(String categorySystemName, String nflowSystemName, String descr) {
        Nflow nflow = ensureNflow(categorySystemName, nflowSystemName);
        nflow.setDescription(descr);
        return nflow;
    }

    @Override
    public Nflow ensureNflow(String categorySystemName, String nflowSystemName, String descr, Datasource.ID destId) {
        Nflow nflow = ensureNflow(categorySystemName, nflowSystemName, descr);
        //TODO add/find datasources
        return nflow;
    }

    @Override
    public Nflow ensureNflow(String categorySystemName, String nflowSystemName, String descr, Datasource.ID srcId, Datasource.ID destId) {
        Nflow nflow = ensureNflow(categorySystemName, nflowSystemName, descr);
        if (srcId != null) {
            ensureNflowSource(nflow.getId(), srcId);
        }
        return nflow;
    }

    @Override
    public Nflow createPrecondition(Nflow.ID nflowId, String descr, List<Metric> metrics) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);

        ServiceLevelAgreementBuilder slaBldr = buildPrecondition(nflow)
            .name("Precondition for nflow " + nflowId)
            .description(descr);

        slaBldr.obligationGroupBuilder(Condition.REQUIRED)
            .obligationBuilder()
            .metric(metrics)
            .build()
            .build();

        return nflow;
    }

    @Override
    public PreconditionBuilder buildPrecondition(ID nflowId) {
        JcrNflow nflow = (JcrNflow) findById(nflowId);

        return buildPrecondition(nflow);
    }

    private PreconditionBuilder buildPrecondition(JcrNflow nflow) {
        try {
            if (nflow != null) {
                Node slaNode = nflow.createNewPrecondition();
                ServiceLevelAgreementBuilder slaBldr = ((JcrServiceLevelAgreementProvider) this.slaProvider).builder(slaNode);

                return new JcrPreconditionbuilder(slaBldr, nflow);
            } else {
                throw new NflowNotFoundExcepton(nflow.getId());
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to create the precondition for nflow " + nflow.getId(), e);
        }
    }

    @Override
    public Nflow addDependent(ID targetId, ID dependentId) {
        JcrNflow target = (JcrNflow) getNflow(targetId);

        if (target == null) {
            throw new NflowNotFoundExcepton("The target nflow to be assigned the dependent does not exists", targetId);
        }

        JcrNflow dependent = (JcrNflow) getNflow(dependentId);

        if (dependent == null) {
            throw new NflowNotFoundExcepton("The dependent nflow does not exists", dependentId);
        }

        target.addDependentNflow(dependent);
        return target;
    }

    @Override
    public Nflow removeDependent(ID nflowId, ID dependentId) {
        JcrNflow target = (JcrNflow) getNflow(nflowId);

        if (target == null) {
            throw new NflowNotFoundExcepton("The target nflow to be assigned the dependent does not exists", nflowId);
        }

        JcrNflow dependent = (JcrNflow) getNflow(dependentId);

        if (dependent == null) {
            throw new NflowNotFoundExcepton("The dependent nflow does not exists", dependentId);
        }

        target.removeDependentNflow(dependent);
        return target;
    }

    @Override
    public NflowCriteria nflowCriteria() {
        return new Criteria();
    }

    @Override
    public Nflow getNflow(Nflow.ID id) {
        return findById(id);
    }

    @Override
    public List<Nflow> getNflows() {
        return findAll();
    }

    @Override
    public List<Nflow> getNflows(NflowCriteria criteria) {

        if (criteria != null) {
            Criteria criteriaImpl = (Criteria) criteria;
            return criteriaImpl.select(getSession(), JcrNflow.NODE_TYPE, Nflow.class, JcrNflow.class);
        }
        return null;
    }

    @Override
    public Nflow findBySystemName(String systemName) {
        String categorySystemName = NflowNameUtil.category(systemName);
        String nflowSystemName = NflowNameUtil.nflow(systemName);
        return findBySystemName(categorySystemName, nflowSystemName);
    }

    @Override
    public Nflow findBySystemName(String categorySystemName, String systemName) {
        NflowCriteria c = nflowCriteria();
        if (categorySystemName != null) {
            c.category(categorySystemName);
        }
        c.name(systemName);
        List<Nflow> nflows = getNflows(c);
        if (nflows != null && !nflows.isEmpty()) {
            return nflows.get(0);
        }
        return null;
    }


    @Override
    public List<? extends Nflow> findByTemplateId(NflowManagerTemplate.ID templateId) {
        String query = "SELECT * from " + EntityUtil.asQueryProperty(JcrNflow.NODE_TYPE) + " as e WHERE e." + EntityUtil.asQueryProperty(NflowDetails.TEMPLATE) + " = $id";
        Map<String, String> bindParams = new HashMap<>();
        bindParams.put("id", templateId.toString());
        return JcrQueryUtil.find(getSession(), query, JcrNflow.class);
    }

    @Override
    public List<? extends Nflow> findByCategoryId(Category.ID categoryId) {

        String query = "SELECT e.* from " + EntityUtil.asQueryProperty(JcrNflow.NODE_TYPE) + " as e "
                       + "INNER JOIN ['tba:nflowSummary'] as summary on ISCHILDNODE(summary,e)"
                       + "WHERE summary." + EntityUtil.asQueryProperty(NflowSummary.CATEGORY) + " = $id";

        Map<String, String> bindParams = new HashMap<>();
        bindParams.put("id", categoryId.toString());

        try {
            QueryResult result = JcrQueryUtil.query(getSession(), query, bindParams);
            return JcrQueryUtil.queryResultToList(result, JcrNflow.class);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Unable to getNflows for Category ", e);
        }

    }

//
//    @Override
//    public NflowSource getNflowSource(com.onescorpin.metadata.api.nflow.NflowSource.ID id) {
//        try {
//            Node node = getSession().getNodeByIdentifier(id.toString());
//
//            if (node != null) {
//                return JcrUtil.createJcrObject(node, JcrNflowSource.class);
//            }
//        } catch (RepositoryException e) {
//            throw new MetadataRepositoryException("Unable to get Nflow Destination for " + id);
//        }
//        return null;
//    }
//
//    @Override
//    public NflowDestination getNflowDestination(com.onescorpin.metadata.api.nflow.NflowDestination.ID id) {
//        try {
//            Node node = getSession().getNodeByIdentifier(id.toString());
//
//            if (node != null) {
//                return JcrUtil.createJcrObject(node, JcrNflowDestination.class);
//            }
//        } catch (RepositoryException e) {
//            throw new MetadataRepositoryException("Unable to get Nflow Destination for " + id);
//        }
//        return null;
//
//    }

    @Override
    public Nflow.ID resolveNflow(Serializable fid) {
        return resolveId(fid);
    }
//
//    @Override
//    public com.onescorpin.metadata.api.nflow.NflowSource.ID resolveSource(Serializable sid) {
//        return new JcrNflowSource.NflowSourceId((sid));
//    }
//
//    @Override
//    public com.onescorpin.metadata.api.nflow.NflowDestination.ID resolveDestination(Serializable sid) {
//        return new JcrNflowDestination.NflowDestinationId(sid);
//    }

    @Override
    public boolean enableNflow(Nflow.ID id) {
        Nflow nflow = getNflow(id);
        if (accessController.isEntityAccessControlled()) {
            nflow.getAllowedActions().checkPermission(NflowAccessControl.ENABLE_DISABLE);
        }

        if (!nflow.getState().equals(Nflow.State.ENABLED)) {
            nflow.setState(Nflow.State.ENABLED);
            //Enable any SLAs on this nflow
            List<ServiceLevelAgreement> serviceLevelAgreements = nflow.getServiceLevelAgreements();
            if (serviceLevelAgreements != null) {
                for (ServiceLevelAgreement sla : serviceLevelAgreements) {
                    JcrServiceLevelAgreement jcrSla = (JcrServiceLevelAgreement) sla;
                    jcrSla.enable();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean disableNflow(Nflow.ID id) {
        Nflow nflow = getNflow(id);
        if (accessController.isEntityAccessControlled()) {
            nflow.getAllowedActions().checkPermission(NflowAccessControl.ENABLE_DISABLE);
        }

        if (!nflow.getState().equals(Nflow.State.DISABLED)) {
            nflow.setState(Nflow.State.DISABLED);
            //disable any SLAs on this nflow
            List<ServiceLevelAgreement> serviceLevelAgreements = nflow.getServiceLevelAgreements();
            if (serviceLevelAgreements != null) {
                for (ServiceLevelAgreement sla : serviceLevelAgreements) {
                    JcrServiceLevelAgreement jcrSla = (JcrServiceLevelAgreement) sla;
                    jcrSla.disabled();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void deleteNflow(ID nflowId) {
        JcrNflow nflow = (JcrNflow) getNflow(nflowId);
        if (nflow != null) {
            delete(nflow);
        }
    }

    @Override
    public void delete(Nflow nflow) {
        if (accessController.isEntityAccessControlled()) {
            nflow.getAllowedActions().checkPermission(NflowAccessControl.DELETE);
        }

        addPostNflowChangeAction(nflow, ChangeType.DELETE);

        // Remove dependent nflows
        final Node node = ((JcrNflow) nflow).getNode();
        nflow.getDependentNflows().forEach(dep -> nflow.removeDependentNflow((JcrNflow) dep));
        JcrMetadataAccess.getCheckedoutNodes().removeIf(node::equals);

        // Remove destinations and sources
        ((JcrNflow) nflow).removeNflowDestinations();
        ((JcrNflow) nflow).removeNflowSources();

        // Delete nflow
        NflowManagerTemplate template = nflow.getTemplate();
        if (template != null) {
            template.removeNflow(nflow);
        }

        // Remove all Ops access control entries
        this.opsAccessProvider.revokeAllAccess(nflow.getId());

        super.delete(nflow);
    }

    public Nflow.ID resolveId(Serializable fid) {
        return new JcrNflow.NflowId(fid);
    }

    protected JcrNflow setupPrecondition(JcrNflow nflow, ServiceLevelAgreement sla) {
//        this.preconditionService.watchNflow(nflow);
        nflow.setPrecondition((JcrServiceLevelAgreement) sla);
        return nflow;
    }

    public Nflow updateNflowServiceLevelAgreement(Nflow.ID nflowId, ServiceLevelAgreement sla) {
        JcrNflow nflow = (JcrNflow) getNflow(nflowId);
        nflow.addServiceLevelAgreement(sla);
        return nflow;
    }

    @Override
    public Map<String, Object> mergeNflowProperties(ID nflowId, Map<String, Object> properties) {
        JcrNflow nflow = (JcrNflow) getNflow(nflowId);
        List<String> securityGroupNames = new ArrayList<>();
        for (Object o : nflow.getSecurityGroups()) {
            HadoopSecurityGroup securityGroup = (HadoopSecurityGroup) o;
            securityGroupNames.add(securityGroup.getName());
        }

        Map<String, Object> merged = nflow.mergeProperties(properties);

        PropertyChange change = new PropertyChange(nflow.getId().getIdValue(),
                                                   nflow.getCategory().getSystemName(),
                                                   nflow.getSystemName(),
                                                   securityGroupNames,
                                                   nflow.getProperties(),
                                                   properties);
        this.metadataEventService.notify(new NflowPropertyChangeEvent(change));

        return merged;
    }

    public Map<String, Object> replaceProperties(ID nflowId, Map<String, Object> properties) {
        JcrNflow nflow = (JcrNflow) getNflow(nflowId);
        return nflow.replaceProperties(properties);
    }

    @Nonnull
    @Override
    public Set<UserFieldDescriptor> getUserFields() {
        return JcrPropertyUtil.getUserFields(ExtensionsConstants.USER_NFLOW, extensibleTypeProvider);
    }

    @Override
    public void setUserFields(@Nonnull final Set<UserFieldDescriptor> userFields) {
        // TODO service?
        metadataAccess.commit(() -> {
            JcrPropertyUtil.setUserFields(ExtensionsConstants.USER_NFLOW, userFields, extensibleTypeProvider);
            return userFields;
        }, MetadataAccess.SERVICE);
    }

    public void populateInverseNflowDependencies() {
        Map<Nflow.ID, Nflow> map = new HashMap<Nflow.ID, Nflow>();
        List<Nflow> nflows = getNflows();
        if (nflows != null) {
            nflows.stream().forEach(nflow -> map.put(nflow.getId(), nflow));
        }
        nflows.stream().filter(nflow -> nflow.getDependentNflows() != null && !nflow.getDependentNflows().isEmpty()).forEach(nflow1 -> {
            List<Nflow> dependentNflows = nflow1.getDependentNflows();
            dependentNflows.stream().filter(depNflow -> depNflow.getUsedByNflows() == null || !depNflow.getUsedByNflows().contains(nflow1))
                .forEach(depNflow -> depNflow.addUsedByNflow(nflow1));
        });
    }

    @Override
    protected void appendJoins(StringBuilder bldr, String filter) {
        if (!Strings.isNullOrEmpty(filter)) {
            bldr.append("JOIN [tba:categoryDetails] AS cd ON ISCHILDNODE(e, cd) ");
            bldr.append("JOIN [tba:category] AS c ON ISCHILDNODE(cd, c) ");
            bldr.append("JOIN [tba:nflowSummary] AS fs ON ISCHILDNODE(fs, e) ");
            bldr.append("JOIN [tba:nflowDetails] AS fdetail ON ISCHILDNODE(fdetail, fs) ");
            bldr.append("JOIN [tba:nflowData] AS fdata ON ISCHILDNODE(fdata, e) ");
        }
    }

    @Override
    protected void appendJoins(StringBuilder bldr, Pageable pageable, String filter) {
        List<String> sortProps = new ArrayList<>();
        if (pageable.getSort() != null) {
            pageable.getSort().forEach(o -> sortProps.add(o.getProperty()));
        }

        // TODO: template sorting does not currently work because a way hasn't been found yet to join
        // across reference properties, so the template associated with a nflow cannot be joined.
        
        // If there is no filter then just perform the minimal joins needed to sort.
        if (!Strings.isNullOrEmpty(filter)) {
            appendJoins(bldr, filter);
        } else if (sortProps.contains(SORT_NFLOW_NAME)) {
            bldr.append("JOIN [tba:nflowSummary] AS fs ON ISCHILDNODE(fs, e) ");
        } else if (sortProps.contains(SORT_CATEGORY_NAME)) {
            bldr.append("JOIN [tba:categoryDetails] AS cd ON ISCHILDNODE(e, cd) ");
            bldr.append("JOIN [tba:category] AS c ON ISCHILDNODE(cd, c) ");
        } else if (sortProps.contains(SORT_STATE)) {
            bldr.append("JOIN [tba:nflowData] AS fdata ON ISCHILDNODE(fdata, e) ");
        }
    }

    @Override
    protected void appendFilter(StringBuilder bldr, String filter) {
        String filterPattern = Strings.isNullOrEmpty(filter) ? null : "'%" + filter.toLowerCase() + "%'";
        if (filterPattern != null) {
            bldr.append("WHERE LOWER(").append(JCR_PROP_MAP.get(SORT_NFLOW_NAME)).append(") LIKE ").append(filterPattern);
            bldr.append(" OR LOWER(").append(JCR_PROP_MAP.get(SORT_CATEGORY_NAME)).append(") LIKE ").append(filterPattern);
            bldr.append(" OR LOWER(").append(JCR_PROP_MAP.get(SORT_STATE)).append(") LIKE ").append(filterPattern);
            // Must sub-select matching templates for reference comparison because a way to join across reference properties has not been found.
            bldr.append(" OR CAST(fdetail.[tba:nflowTemplate] AS REFERENCE) IN ")
                .append("(SELECT [mode:id] from [tba:nflowTemplate] AS t WHERE LOWER(t.[jcr:title]) LIKE ").append(filterPattern).append(") ");
        }
    }

    @Override
    protected String getEntityQueryStartingPath() {
        return EntityUtil.pathForCategory();
    }

    @Override
    protected String getFindAllFilter() {
       return getFindAllFilter(getEntityQueryStartingPath(),5);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.modeshape.BaseJcrProvider#deriveJcrPropertyName(java.lang.String)
     */
    @Override
    protected String deriveJcrPropertyName(String property) {
        String jcrProp = JCR_PROP_MAP.get(property);

        if (jcrProp == null) {
            throw new IllegalArgumentException("Unknown sort property: " + property);
        } else if (jcrProp.length() == 0) {
            return JCR_PROP_MAP.get("nflowName");
        } else {
            return jcrProp;
        }
    }

    private static class Criteria extends AbstractMetadataCriteria<NflowCriteria> implements NflowCriteria, Predicate<Nflow> {

        private String name;
        private Set<Datasource.ID> sourceIds = new HashSet<>();
        private Set<Datasource.ID> destIds = new HashSet<>();
        private String category;

        /**
         * Selects by navigation rather than SQL
         */
        @Override
        public <E, J extends JcrObject> List<E> select(Session session, String typeName, Class<E> type, Class<J> jcrClass) {
            try {
                // Datasources are not currently used so only name comparison is necessary
                Node nflowsNode = session.getRootNode().getNode("metadata/nflows");
                NodeIterator catItr = null;

                if (this.category != null) {
                    catItr = nflowsNode.getNodes(this.category);
                } else {
                    catItr = nflowsNode.getNodes();
                }

                List<Nflow> list = new ArrayList<Nflow>();

                while (catItr.hasNext()) {
                    Node catNode = (Node) catItr.next();
                    NodeIterator nflowItr = null;

                    if (catNode.hasNode(JcrCategory.DETAILS)) {
                        catNode = catNode.getNode(JcrCategory.DETAILS);
                    }

                    if (this.name != null) {
                        nflowItr = catNode.getNodes(this.name);
                    } else {
                        nflowItr = catNode.getNodes();
                    }

                    while (nflowItr.hasNext()) {
                        Node nflowNode = (Node) nflowItr.next();

                        if (nflowNode.getPrimaryNodeType().getName().equals("tba:nflow")) {
                            list.add(JcrUtil.createJcrObject(nflowNode, JcrNflow.class));
                        }
                    }
                }

                return (List<E>) list;
            } catch (RepositoryException e) {
                throw new MetadataRepositoryException("Failed to select nflows", e);
            }
        }

        @Override
        @Deprecated
        protected void applyFilter(StringBuilder queryStr, HashMap<String, Object> params) {
            StringBuilder cond = new StringBuilder();
            StringBuilder join = new StringBuilder();

            if (this.name != null) {
                cond.append(EntityUtil.asQueryProperty(JcrNflow.SYSTEM_NAME) + " = $name");
                params.put("name", this.name);
            }
            if (this.category != null) {
                //TODO FIX SQL
                join.append(
                    " join [" + JcrCategory.NODE_TYPE + "] as c on e." + EntityUtil.asQueryProperty(NflowSummary.CATEGORY) + "." + EntityUtil.asQueryProperty(JcrCategory.SYSTEM_NAME) + " = c."
                    + EntityUtil
                        .asQueryProperty(JcrCategory.SYSTEM_NAME));
                cond.append(" c." + EntityUtil.asQueryProperty(JcrCategory.SYSTEM_NAME) + " = $category ");
                params.put("category", this.category);
            }

            applyIdFilter(cond, join, this.sourceIds, "sources", params);
            applyIdFilter(cond, join, this.destIds, "destinations", params);

            if (join.length() > 0) {
                queryStr.append(join.toString());
            }

            if (cond.length() > 0) {
                queryStr.append(" where ").append(cond.toString());
            }
        }

        @Deprecated
        private void applyIdFilter(StringBuilder cond, StringBuilder join, Set<Datasource.ID> idSet, String relation, HashMap<String, Object> params) {
            if (!idSet.isEmpty()) {
                if (cond.length() > 0) {
                    cond.append("and ");
                }

                String alias = relation.substring(0, 1);
                join.append("join e.").append(relation).append(" ").append(alias).append(" ");
                cond.append(alias).append(".datasource.id in $").append(relation).append(" ");
                params.put(relation, idSet);
            }
        }

        @Override
        public boolean apply(Nflow input) {
            if (this.name != null && !name.equals(input.getName())) {
                return false;
            }
            if (this.category != null && input.getCategory() != null && !this.category.equals(input.getCategory().getSystemName())) {
                return false;
            }
            if (!this.destIds.isEmpty()) {
                List<? extends NflowDestination> destinations = input.getDestinations();
                for (NflowDestination dest : destinations) {
                    if (this.destIds.contains(dest.getDatasource().getId())) {
                        return true;
                    }
                }
                return false;
            }

            if (!this.sourceIds.isEmpty()) {
                List<? extends NflowSource> sources = input.getSources();
                for (NflowSource src : sources) {
                    if (this.sourceIds.contains(src.getDatasource().getId())) {
                        return true;
                    }
                }
                return false;
            }

            return true;
        }

        @Override
        public NflowCriteria sourceDatasource(Datasource.ID id, Datasource.ID... others) {
            this.sourceIds.add(id);
            for (Datasource.ID other : others) {
                this.sourceIds.add(other);
            }
            return this;
        }

        @Override
        public NflowCriteria destinationDatasource(Datasource.ID id, Datasource.ID... others) {
            this.destIds.add(id);
            for (Datasource.ID other : others) {
                this.destIds.add(other);
            }
            return this;
        }

        @Override
        public NflowCriteria name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public NflowCriteria category(String category) {
            this.category = category;
            return this;
        }
    }

    private class JcrPreconditionbuilder implements PreconditionBuilder {

        private final ServiceLevelAgreementBuilder slaBuilder;
        private final JcrNflow nflow;

        public JcrPreconditionbuilder(ServiceLevelAgreementBuilder slaBuilder, JcrNflow nflow) {
            super();
            this.slaBuilder = slaBuilder;
            this.nflow = nflow;
        }

        public ServiceLevelAgreementBuilder name(String name) {
            return slaBuilder.name(name);
        }

        public ServiceLevelAgreementBuilder description(String description) {
            return slaBuilder.description(description);
        }

        public ServiceLevelAgreementBuilder obligation(Obligation obligation) {
            return slaBuilder.obligation(obligation);
        }

        public ObligationBuilder<ServiceLevelAgreementBuilder> obligationBuilder() {
            return slaBuilder.obligationBuilder();
        }

        public ObligationBuilder<ServiceLevelAgreementBuilder> obligationBuilder(Condition condition) {
            return slaBuilder.obligationBuilder(condition);
        }

        public ObligationGroupBuilder obligationGroupBuilder(Condition condition) {
            return slaBuilder.obligationGroupBuilder(condition);
        }

        public ServiceLevelAgreement build() {
            ServiceLevelAgreement sla = slaBuilder.build();

            setupPrecondition(nflow, sla);
            return sla;
        }

        @Override
        public ServiceLevelAgreementBuilder actionConfigurations(List<? extends ServiceLevelAgreementActionConfiguration> actionConfigurations) {
            return null;
        }
    }
}
