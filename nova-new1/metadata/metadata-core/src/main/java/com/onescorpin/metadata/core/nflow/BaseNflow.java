package com.onescorpin.metadata.core.nflow;

/*-
 * #%L
 * onescorpin-metadata-core
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
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowConnection;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowPrecondition;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.api.nflow.InitializationStatus;
import com.onescorpin.metadata.api.security.HadoopSecurityGroup;
import com.onescorpin.metadata.api.security.RoleMembership;
import com.onescorpin.metadata.api.template.NflowManagerTemplate;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAssessment;
import com.onescorpin.security.action.AllowedActions;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A POJO implementation of {@link Nflow}.
 */
public class BaseNflow implements Nflow {

    private ID Id;
    private String name;
    private String displayName;
    private String description;
    private State state;
    private boolean initialized;
    private DateTime createdTime;
    private Set<Nflow> dependentNflows = new HashSet<>();
    private Set<NflowSource> sources = new HashSet<>();
    private Set<NflowDestination> destinations = new HashSet<>();
    private NflowPreconditionImpl precondition;
    private Map<String, Object> properties;
    private List<ServiceLevelAgreement> nflowServiceLevelAgreements;
    private List<? extends HadoopSecurityGroup> hadoopSecurityGroups;
    private Map<String, String> waterMarkValues = new HashMap<>();
    private String json;
    private NflowManagerTemplate template;
    private String nifiProcessGroupId;
    private Principal owner;

    /**
     * User-defined properties
     */
    private Map<String, String> userProperties;

    public BaseNflow(String name, String description) {
        this.Id = new NflowId();
        this.name = name;
        this.displayName = name;
        this.description = description;
        this.createdTime = DateTime.now();
    }

    @Override
    public String getVersionName() {
        return "";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Nflow> getDependentNflows() {
        return new ArrayList(this.dependentNflows);
    }

    @Override
    public boolean addDependentNflow(Nflow nflow) {
        return this.dependentNflows.add(nflow);
    }

    @Override
    public boolean removeDependentNflow(Nflow nflow) {
        return this.dependentNflows.remove(nflow);
    }


    @Override
    public List<Nflow> getUsedByNflows() {
        return null;
    }

    @Override
    public boolean addUsedByNflow(Nflow nflow) {
        return false;
    }

    @Override
    public boolean removeUsedByNflow(Nflow nflow) {
        return false;
    }

    @Override
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public void setProperties(Map<String, Object> props) {
        this.properties = props;
    }

    @Override
    public Map<String, Object> mergeProperties(Map<String, Object> props) {
        for (Entry<String, Object> entry : props.entrySet()) {
            this.properties.put(entry.getKey(), entry.getValue());
        }
        return this.properties;
    }

    @Override
    public DateTime getCreatedTime() {
        return this.createdTime;
    }

    @Override
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    @Override
    public void removeProperty(String key) {
        this.properties.remove(key);
    }

    public ID getId() {
        return Id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getQualifiedName() {
        return getCategory().getSystemName() + "." + getName();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String descr) {
        this.description = descr;
    }

    @Override
    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public Category getCategory() {
        return null;
    }

    @Override
    public DateTime getModifiedTime() {
        return null;
    }

    public List<NflowSource> getSources() {
        return new ArrayList<>(this.sources);
    }

    public List<NflowDestination> getDestinations() {
        return new ArrayList<>(destinations);
    }

    @Override
    public NflowDestination getDestination(Datasource.ID id) {
        for (NflowDestination dest : this.destinations) {
            if (dest.getDatasource().getId().equals(id)) {
                return dest;
            }
        }

        return null;
    }

    @Override
    public void clearSourcesAndDestinations() {
        this.sources.clear();
        this.destinations.clear();
    }

    @Override
    public NflowPrecondition getPrecondition() {
        return this.precondition;
    }

    public NflowSource addSource(Datasource ds) {
        return addSource(ds, null);
    }

    public NflowSource addSource(Datasource ds, ServiceLevelAgreement agreement) {
        Source src = new Source(ds, agreement);
        this.sources.add(src);
        return src;
    }

    @Override
    public NflowSource getSource(Datasource.ID id) {
        for (NflowSource src : this.sources) {
            if (src.getNflow().getId().equals(id)) {
                return src;
            }
        }

        return null;
    }
//
//    @Override
//    public NflowSource getSource(NflowSource.ID id) {
//        return this.sources.get(id);
//    }

    public NflowDestination addDestination(Datasource ds) {
        NflowDestination dest = new Destination(ds);
        this.destinations.add(dest);
        return dest;
    }
//
//    @Override
//    public NflowDestination getDestination(NflowDestination.ID id) {
//        return this.destinations.get(id);
//    }

    public NflowPrecondition setPrecondition(ServiceLevelAgreement sla) {
        this.precondition = new NflowPreconditionImpl(this, sla);
        return this.precondition;
    }

    @Override
    public List<? extends HadoopSecurityGroup> getSecurityGroups() {
        return this.hadoopSecurityGroups;
    }

    @Override
    public void setSecurityGroups(List<? extends HadoopSecurityGroup> securityGroups) {
        hadoopSecurityGroups = securityGroups;
    }

    @Override
    public List<ServiceLevelAgreement> getServiceLevelAgreements() {
        return nflowServiceLevelAgreements;
    }

    @Nonnull
    @Override
    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    @Override
    public void setUserProperties(@Nonnull Map<String, String> userProperties, @Nonnull Set<UserFieldDescriptor> userFields) {
        this.userProperties = userProperties;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.Nflow#getWaterMarkValue(java.lang.String)
     */
    @Override
    public Optional<String> getWaterMarkValue(String waterMarkName) {
        return Optional.ofNullable(this.waterMarkValues.get(waterMarkName));
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.Nflow#getWaterMarkNames()
     */
    @Override
    public Set<String> getWaterMarkNames() {
        return new HashSet<>(this.waterMarkValues.keySet());
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.Nflow#setWaterMarkValue(java.lang.String, java.lang.String)
     */
    @Override
    public void setWaterMarkValue(String waterMarkName, String value) {
        this.waterMarkValues.put(waterMarkName, waterMarkName);
    }
    
    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.Nflow#getTags()
     */
    @Override
    public Set<String> getTags() {
        return Collections.emptySet();
    }

    @Override
    public void setTags(@Nullable final Set<String> tags) {
        // ignored
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.Nflow#getCurrentInitStatus()
     */
    @Override
    public InitializationStatus getCurrentInitStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.Nflow#updateInitStatus(com.onescorpin.metadata.api.nflow.InitializationStatus)
     */
    @Override
    public void updateInitStatus(InitializationStatus status) {
        // TODO Auto-generated method stub

    }
//
//    protected static class SourceId extends BaseId implements NflowSource.ID {
//        public SourceId() {
//            super();
//        }
//
//        public SourceId(Serializable ser) {
//            super(ser);
//        }
//    }
//
//    protected static class DestinationId extends BaseId implements NflowDestination.ID {
//        public DestinationId() {
//            super();
//        }
//
//        public DestinationId(Serializable ser) {
//            super(ser);
//        }
//    }
//    @Override
//    public String getVersionName() {
//        return null;
//    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.Nflow#getInitHistory()
     */
    @Override
    public List<InitializationStatus> getInitHistory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AllowedActions getAllowedActions() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.security.AccessControlled#getRoleAssignments()
     */
    @Override
    public Set<RoleMembership> getRoleMemberships() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<RoleMembership> getRoleMembership(String roleName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.security.AccessControlled#getInheritedRoleMemberships()
     */
    @Override
    public Set<RoleMembership> getInheritedRoleMemberships() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.security.AccessControlled#getInheritedRoleMembership(java.lang.String)
     */
    @Override
    public Optional<RoleMembership> getInheritedRoleMembership(String roleName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJson() {
        return json;
    }

    @Override
    public void setJson(String json) {
        this.json = json;
    }

    public NflowManagerTemplate getTemplate() {
        return template;
    }

    @Override
    public void setTemplate(NflowManagerTemplate template) {
        this.template = template;
    }

    @Override
    public String getNifiProcessGroupId() {
        return nifiProcessGroupId;
    }

    @Override
    public void setNifiProcessGroupId(String nifiProcessGroupId) {
        this.nifiProcessGroupId = nifiProcessGroupId;
    }

    @Override
    public void setVersionName(String version) {
        // TODO Auto-generated method stub

    }

    @Override
    public Principal getOwner() {
        return owner;
    }

    public void setOwner(Principal owner) {
        this.owner = owner;
    }

    public static class BaseId {

        private final UUID uuid;

        public BaseId() {
            this.uuid = UUID.randomUUID();
        }

        public BaseId(Serializable ser) {
            if (ser instanceof String) {
                this.uuid = UUID.fromString((String) ser);
            } else if (ser instanceof UUID) {
                this.uuid = (UUID) ser;
            } else {
                throw new IllegalArgumentException("Unknown ID value: " + ser);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass().isAssignableFrom(obj.getClass())) {
                BaseId that = (BaseId) obj;
                return Objects.equals(this.uuid, that.uuid);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass(), this.uuid);
        }

        @Override
        public String toString() {
            return this.uuid.toString();
        }
    }

    public static class NflowId extends BaseId implements Nflow.ID {

        public NflowId() {
            super();
        }

        public NflowId(Serializable ser) {
            super(ser);
        }
    }

    protected static class NflowPreconditionImpl implements NflowPrecondition {

        private ServiceLevelAgreement sla;
        private BaseNflow nflow;
        private ServiceLevelAssessment lastAssessment;

        public NflowPreconditionImpl(BaseNflow nflow, ServiceLevelAgreement sla) {
            this.sla = sla;
            this.nflow = nflow;
        }

        @Override
        public Nflow getNflow() {
            return this.nflow;
        }

        @Override
        public ServiceLevelAgreement getAgreement() {
            return sla;
        }

        @Override
        public ServiceLevelAssessment getLastAssessment() {
            return lastAssessment;
        }

        @Override
        public void setLastAssessment(ServiceLevelAssessment assmnt) {
            this.lastAssessment = assmnt;
        }
    }

    private abstract class Data implements NflowConnection {

        private Datasource dataset;

        public Data(Datasource ds) {
            this.dataset = ds;
        }

        @Override
        public Nflow getNflow() {
            return BaseNflow.this;
        }

        @Override
        public Datasource getDatasource() {
            return this.dataset;
        }
    }

    private class Source extends Data implements NflowSource {

        private static final long serialVersionUID = -2407190619538717445L;

        //        private SourceId id;
        private ServiceLevelAgreement agreement;

        public Source(Datasource ds, ServiceLevelAgreement agreement) {
            super(ds);
//            this.id = new SourceId();
            this.agreement = agreement;
        }
//
//        @Override
//        public ID getId() {
//            return this.id;
//        }

        @Override
        public ServiceLevelAgreement getAgreement() {
            return this.agreement;
        }
    }

    private class Destination extends Data implements NflowDestination {

        private static final long serialVersionUID = -6990911423133789381L;

//        private DestinationId id;

        public Destination(Datasource ds) {
            super(ds);
//            this.id = new DestinationId();
        }
//
//        @Override
//        public ID getId() {
//            return this.id;
//        }
    }
}
