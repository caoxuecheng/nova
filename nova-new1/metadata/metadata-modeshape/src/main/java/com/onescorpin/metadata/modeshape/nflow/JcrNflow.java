package com.onescorpin.metadata.modeshape.nflow;

import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowPrecondition;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.api.nflow.InitializationStatus;
import com.onescorpin.metadata.api.nflow.security.NflowOpsAccessControlProvider;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.security.RoleMembership;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.modeshape.MetadataRepositoryException;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.common.AbstractJcrAuditableSystemEntity;
import com.onescorpin.metadata.modeshape.common.JcrEntity;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasource;
import com.onescorpin.metadata.modeshape.nflow.security.JcrNflowAllowedActions;
import com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions;
import com.onescorpin.metadata.modeshape.security.mixin.AccessControlledMixin;
import com.onescorpin.metadata.modeshape.security.role.JcrAbstractRoleMembership;
import com.onescorpin.metadata.modeshape.sla.JcrServiceLevelAgreement;
import com.onescorpin.metadata.modeshape.support.JcrPropertyUtil;
import com.onescorpin.metadata.modeshape.support.JcrUtil;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.security.role.SecurityRole;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

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

/**
 * An implementation of {@link Nflow} backed by a JCR repository.
 */
public class JcrNflow extends AbstractJcrAuditableSystemEntity implements Nflow, AccessControlledMixin {

    public static final String PRECONDITION_TYPE = "tba:nflowPrecondition";

    public static final String NODE_TYPE = "tba:nflow";

    public static final String SUMMARY = "tba:summary";
    public static final String DATA = "tba:data";

    private Node summaryNode;
    private NflowSummary summary;
    private NflowData data;

    // TODO: Referencing the ops access provider is kind of ugly but is needed so that 
    // a change to the nflow's allowed accessions for ops access get's propagated to the JPA table.
    private volatile Optional<NflowOpsAccessControlProvider> opsAccessProvider = Optional.empty();

    public JcrNflow(Node node) {
        super(node);
    }
    
    public JcrNflow(Node nflowNode, Node summaryNode, NflowOpsAccessControlProvider opsAccessProvider) {
        this(nflowNode, opsAccessProvider);
        // The summary node will be different (not a child of the nflow node) if this is a past version,
        // so it must be supplied at construction.
        this.summary = JcrUtil.getJcrObject(summaryNode, NflowSummary.class, this);
    }

    public JcrNflow(Node node, NflowOpsAccessControlProvider opsAccessProvider) {
        super(node);
        setOpsAccessProvider(opsAccessProvider);
    }

    public JcrNflow(Node node, JcrCategory category) {
        this(node, (NflowOpsAccessControlProvider) null);
        if (category != null) {
            getNflowSummary().ifPresent(s -> s.setProperty(NflowSummary.CATEGORY, category));
        }
    }

    public JcrNflow(Node node, JcrCategory category, NflowOpsAccessControlProvider opsAccessProvider) {
        this(node, opsAccessProvider);
        if (category != null) {
            getNflowSummary().ifPresent(s -> s.setProperty(NflowSummary.CATEGORY, category));
        }
    }


    /**
     * This should be set after an instance of this type is created to allow the change
     * of a nflow's operations access control.
     *
     * @param opsAccessProvider the opsAccessProvider to set
     */
    public void setOpsAccessProvider(NflowOpsAccessControlProvider opsAccessProvider) {
        this.opsAccessProvider = Optional.ofNullable(opsAccessProvider);
    }

    public Optional<NflowOpsAccessControlProvider> getOpsAccessProvider() {
        return this.opsAccessProvider;
    }
    
    /* (non-Javadoc)
     * @see com.onescorpin.metadata.modeshape.security.mixin.AccessControlledMixin#enableAccessControl(com.onescorpin.metadata.modeshape.security.action.JcrAllowedActions, java.security.Principal, java.util.List)
     */
    @Override
    public void enableAccessControl(JcrAllowedActions prototype, Principal owner, List<SecurityRole> roles) {
        AccessControlledMixin.super.enableAccessControl(prototype, owner, roles);
        
        JcrAbstractRoleMembership.enableOnlyForAll(getCategory().getNflowRoleMemberships().stream(), this.getAllowedActions());
    }

    // -=-=--=-=- Delegate Propertied methods to data -=-=-=-=-=-

    @Override
    public Map<String, Object> getProperties() {
        return getNflowData().map(d -> d.getProperties()).orElse(Collections.emptyMap());
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        getNflowData().ifPresent(d -> d.setProperties(properties));
    }

    @Override
    public void setProperty(String name, Object value) {
        getNflowData().ifPresent(d -> d.setProperty(name, value));
    }

    @Override
    public void removeProperty(String key) {
        getNflowData().ifPresent(d -> d.removeProperty(key));
    }

    @Override
    public Map<String, Object> mergeProperties(Map<String, Object> props) {
        return getNflowData().map(d -> d.mergeProperties(props)).orElse(Collections.emptyMap());
    }

    @Override
    public Map<String, Object> replaceProperties(Map<String, Object> props) {
        return getNflowData().map(d -> d.replaceProperties(props)).orElse(Collections.emptyMap());
    }

    // -=-=--=-=- Delegate taggable methods to summary -=-=-=-=-=-

    @Override
    public Set<String> addTag(String tag) {
        return getNflowSummary().map(s -> s.addTag(tag)).orElse(Collections.emptySet());
    }

    @Override
    public Set<String> getTags() {
        return getNflowSummary().map(s -> s.getTags()).orElse(Collections.emptySet());
    }

    @Override
    public void setTags(Set<String> tags) {
        getNflowSummary().ifPresent(s -> s.setTags(tags));
    }

    @Override
    public boolean hasTag(String tag) {
        return getNflowSummary().map(s -> s.hasTag(tag)).orElse(false);
    }

    // -=-=--=-=- Delegate AbstractJcrAuditableSystemEntity methods to summary -=-=-=-=-=-

    @Override
    public DateTime getCreatedTime() {
        return getNflowSummary().map(s -> s.getCreatedTime()).orElse(null);
    }

    @Override
    public void setCreatedTime(DateTime createdTime) {
        getNflowSummary().ifPresent(s -> s.setCreatedTime(createdTime));
    }

    @Override
    public DateTime getModifiedTime() {
        return getNflowSummary().map(s -> s.getModifiedTime()).orElse(null);
    }

    @Override
    public void setModifiedTime(DateTime modifiedTime) {
        getNflowSummary().ifPresent(s -> s.setModifiedTime(modifiedTime));
    }

    @Override
    public String getCreatedBy() {
        return getNflowSummary().map(s -> s.getCreatedBy()).orElse(null);
    }

    @Override
    public String getModifiedBy() {
        return getNflowSummary().map(s -> s.getModifiedBy()).orElse(null);
    }

    // -=-=--=-=- Delegate AbstractJcrSystemEntity methods to summary -=-=-=-=-=-

    @Override
    public String getDescription() {
        return getNflowSummary().map(s -> s.getDescription()).orElse(null);
    }

    @Override
    public void setDescription(String description) {
        getNflowSummary().ifPresent(s -> s.setDescription(description));
    }

    @Override
    public String getSystemName() {
        return getNflowSummary().map(s -> s.getSystemName()).orElse(null);
    }

    @Override
    public void setSystemName(String systemName) {
        getNflowSummary().ifPresent(s -> s.setSystemName(systemName));
    }

    @Override
    public String getTitle() {
        return getNflowSummary().map(s -> s.getTitle()).orElse(null);
    }

    @Override
    public void setTitle(String title) {
        getNflowSummary().ifPresent(s -> s.setTitle(title));
    }


    public Category getCategory() {
        return getNflowSummary().map(s -> s.getCategory(JcrCategory.class)).orElse(null);
    }

    public NflowManagerTemplate getTemplate() {
        return getNflowDetails().map(d -> d.getTemplate()).orElse(null);
    }

    public void setTemplate(NflowManagerTemplate template) {
        getNflowDetails().ifPresent(d -> d.setTemplate(template));
    }

    public List<? extends NflowSource> getSources() {
        return getNflowDetails().map(d -> d.getSources()).orElse(Collections.emptyList());
    }

    public List<? extends NflowDestination> getDestinations() {
        return getNflowDetails().map(d -> d.getDestinations()).orElse(Collections.emptyList());
    }

    @Override
    public String getName() {
        return getSystemName();
    }

    @Override
    public String getQualifiedName() {
        return getCategory().getSystemName() + "." + getName();
    }

    @Override
    public String getDisplayName() {
        return getTitle();
    }

    @Override
    public void setDisplayName(String name) {
        setTitle(name);
    }

    @Override
    public Nflow.State getState() {
        return getNflowData().map(d -> d.getState()).orElse(null);
    }

    @Override
    public void setState(State state) {
        getNflowData().ifPresent(d -> d.setState(state));
    }

    @Override
    public boolean isInitialized() {
        return getNflowData().map(d -> d.isInitialized()).orElse(null);
    }

    @Override
    public InitializationStatus getCurrentInitStatus() {
        return getNflowData().map(d -> d.getCurrentInitStatus()).orElse(null);
    }

    @Override
    public void updateInitStatus(InitializationStatus status) {
        getNflowData().ifPresent(d -> d.updateInitStatus(status));
    }

    @Override
    public List<InitializationStatus> getInitHistory() {
        return getNflowData().map(d -> d.getInitHistory()).orElse(Collections.emptyList());
    }

    @Override
    public NflowPrecondition getPrecondition() {
        return getNflowDetails().map(d -> d.getPrecondition()).orElse(null);
    }

    public void setPrecondition(JcrServiceLevelAgreement sla) {
//        Node precondNode
    }

    @Override
    public Set<String> getWaterMarkNames() {
        return getNflowData().map(d -> d.getWaterMarkNames()).orElse(Collections.emptySet());
    }

    @Override
    public Optional<String> getWaterMarkValue(String waterMarkName) {
        return getNflowData().flatMap(d -> d.getWaterMarkValue(waterMarkName));
    }

    @Override
    public void setWaterMarkValue(String waterMarkName, String value) {
        getNflowData().ifPresent(d -> d.setWaterMarkValue(waterMarkName, value));
    }

    @Override
    public List<Nflow> getDependentNflows() {
        return getNflowDetails().map(d -> d.getDependentNflows()).orElse(Collections.emptyList());
    }

    @Override
    public boolean addDependentNflow(Nflow nflow) {
        return getNflowDetails().map(d -> d.addDependentNflow(nflow)).orElse(false);
    }

    @Override
    public boolean removeDependentNflow(Nflow nflow) {
        return getNflowDetails().map(d -> d.removeDependentNflow(nflow)).orElse(false);
    }

    @Override
    public List<Nflow> getUsedByNflows() {
        return getNflowDetails().map(d -> d.getUsedByNflows()).orElse(Collections.emptyList());
    }

    @Override
    public boolean addUsedByNflow(Nflow nflow) {
        return getNflowDetails().map(d -> d.addUsedByNflow(nflow)).orElse(false);
    }

    @Override
    public boolean removeUsedByNflow(Nflow nflow) {
        return getNflowDetails().map(d -> d.removeUsedByNflow(nflow)).orElse(false);
    }

    @Override
    public NflowSource getSource(final Datasource.ID id) {
        return getNflowDetails().map(d -> d.getSource(id)).orElse(null);
    }

    @Override
    public NflowDestination getDestination(final Datasource.ID id) {
        return getNflowDetails().map(d -> d.getDestination(id)).orElse(null);
    }

    public String getSchedulePeriod() {
        return getNflowData().map(d -> d.getSchedulePeriod()).orElse(null);
    }

    public void setSchedulePeriod(String schedulePeriod) {
        getNflowData().ifPresent(d -> d.setSchedulePeriod(schedulePeriod));
    }

    public String getScheduleStrategy() {
        return getNflowData().map(d -> d.getScheduleStrategy()).orElse(null);
    }

    public void setScheduleStrategy(String scheduleStrategy) {
        getNflowData().ifPresent(d -> d.setScheduleStrategy(scheduleStrategy));
    }

    public List<ServiceLevelAgreement> getServiceLevelAgreements() {
        return getNflowDetails().map(d -> d.getServiceLevelAgreements()).orElse(Collections.emptyList());
    }

    public void setServiceLevelAgreements(List<? extends ServiceLevelAgreement> serviceLevelAgreements) {
        getNflowDetails().ifPresent(d -> d.setServiceLevelAgreements(serviceLevelAgreements));
    }

    public List<? extends HadoopSecurityGroup> getSecurityGroups() {
        return getNflowData().map(d -> d.getSecurityGroups()).orElse(Collections.emptyList());
    }

    public void setSecurityGroups(List<? extends HadoopSecurityGroup> hadoopSecurityGroups) {
        getNflowData().ifPresent(d -> d.setSecurityGroups(hadoopSecurityGroups));
    }

    public void removeServiceLevelAgreement(ServiceLevelAgreement.ID id) {
        getNflowDetails().ifPresent(d -> d.removeServiceLevelAgreement(id));
    }

    public boolean addServiceLevelAgreement(ServiceLevelAgreement sla) {
        return getNflowDetails().map(d -> d.addServiceLevelAgreement(sla)).orElse(false);
    }

    @Nonnull
    @Override
    public Map<String, String> getUserProperties() {
        return JcrPropertyUtil.getUserProperties(node);
    }

    @Override
    public void setUserProperties(@Nonnull final Map<String, String> userProperties, @Nonnull final Set<UserFieldDescriptor> userFields) {
        JcrPropertyUtil.setUserProperties(node, userFields, userProperties);
    }

    @Override
    public String getJson() {
        return getNflowDetails().map(d -> d.getJson()).orElse(null);
    }

    @Override
    public void setJson(String json) {
        getNflowDetails().ifPresent(d -> d.setJson(json));

    }

    @Override
    public String getNifiProcessGroupId() {
        return getNflowDetails().map(d -> d.getNifiProcessGroupId()).orElse(null);
    }

    @Override
    public void setNifiProcessGroupId(String id) {
        getNflowDetails().ifPresent(d -> d.setNifiProcessGroupId(id));
    }
    
    public Set<RoleMembership> getInheritedRoleMemberships() {
        return getCategory().getNflowRoleMemberships();
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.modeshape.security.mixin.AccessControlledMixin#getJcrAllowedActions()
     */
    @Override
    public JcrAllowedActions getJcrAllowedActions() {
        Node allowedNode = JcrUtil.getNode(getNode(), JcrAllowedActions.NODE_NAME);
        return JcrUtil.createJcrObject(allowedNode, getJcrAllowedActionsType(), this.opsAccessProvider.orElse(null));
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.modeshape.security.mixin.AccessControlledMixin#getJcrAllowedActionsType()
     */
    @Override
    public Class<JcrNflowAllowedActions> getJcrAllowedActionsType() {
        return JcrNflowAllowedActions.class;
    }

    public Optional<NflowSummary> getNflowSummary() {
        if (this.summary == null) {
            if (JcrUtil.hasNode(getNode(), SUMMARY)) {
                this.summary = JcrUtil.getJcrObject(getNode(), SUMMARY, NflowSummary.class, this);
                return Optional.of(this.summary);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(this.summary);
        }
    }

    public Optional<NflowDetails> getNflowDetails() {
        Optional<NflowSummary> summary = getNflowSummary();

        if (summary.isPresent()) {
            return summary.get().getNflowDetails();
        } else {
            return Optional.empty();
        }
    }

    public Optional<NflowData> getNflowData() {
        if (this.data == null) {
            if (JcrUtil.hasNode(getNode(), DATA)) {
                this.data = JcrUtil.getJcrObject(getNode(), DATA, NflowData.class);
                return Optional.of(this.data);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.of(this.data);
        }
    }

    @Override
    public NflowId getId() {
        try {
            return new JcrNflow.NflowId(getObjectId());
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to retrieve the entity id", e);
        }
    }

    protected JcrNflowSource ensureNflowSource(JcrDatasource datasource) {
        return getNflowDetails().map(d -> d.ensureNflowSource(datasource)).orElse(null);
    }

    protected JcrNflowDestination ensureNflowDestination(JcrDatasource datasource) {
        return getNflowDetails().map(d -> d.ensureNflowDestination(datasource)).orElse(null);
    }

    protected void removeNflowSource(JcrNflowSource source) {
        getNflowDetails().ifPresent(d -> d.removeNflowSource(source));
    }

    protected void removeNflowDestination(JcrNflowDestination dest) {
        getNflowDetails().ifPresent(d -> d.removeNflowDestination(dest));
    }

    protected void removeNflowSources() {
        getNflowDetails().ifPresent(d -> d.removeNflowSources());
    }

    protected void removeNflowDestinations() {
        getNflowDetails().ifPresent(d -> d.removeNflowDestinations());
    }

    protected Node createNewPrecondition() {
        return getNflowDetails().map(d -> d.createNewPrecondition()).orElse(null);
    }

    public void clearSourcesAndDestinations(){
        removeNflowSources();
        removeNflowDestinations();
    }

    public static class NflowId extends JcrEntity.EntityId implements Nflow.ID {

        public NflowId(Serializable ser) {
            super(ser);
        }
    }
}
