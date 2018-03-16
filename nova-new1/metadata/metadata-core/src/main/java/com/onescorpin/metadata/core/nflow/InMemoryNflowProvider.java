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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.datasource.Datasource;
import com.onescorpin.metadata.api.datasource.Datasource.ID;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.extension.UserFieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowCriteria;
import com.onescorpin.metadata.api.nflow.NflowDestination;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.api.nflow.PreconditionBuilder;
import com.onescorpin.metadata.api.versioning.EntityVersion;
import com.onescorpin.metadata.core.AbstractMetadataCriteria;
import com.onescorpin.metadata.core.nflow.BaseNflow.NflowId;
import com.onescorpin.metadata.core.nflow.BaseNflow.NflowPreconditionImpl;
import com.onescorpin.metadata.sla.api.Metric;
import com.onescorpin.metadata.sla.api.Obligation;
import com.onescorpin.metadata.sla.api.ObligationGroup.Condition;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreement;
import com.onescorpin.metadata.sla.api.ServiceLevelAgreementActionConfiguration;
import com.onescorpin.metadata.sla.spi.ObligationBuilder;
import com.onescorpin.metadata.sla.spi.ObligationGroupBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementBuilder;
import com.onescorpin.metadata.sla.spi.ServiceLevelAgreementProvider;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * A provider of {@link Nflow} objects that stores everything in memory.
 */
public class InMemoryNflowProvider implements NflowProvider {

    private static final Criteria ALL = new Criteria() {
        public boolean apply(BaseNflow input) {
            return true;
        }

        ;
    };

    @Inject
    private DatasourceProvider datasetProvider;

    @Inject
    private ServiceLevelAgreementProvider slaProvider;

    private Map<Nflow.ID, Nflow> nflows = new ConcurrentHashMap<>();

    public InMemoryNflowProvider() {
        super();
    }

    public InMemoryNflowProvider(DatasourceProvider datasetProvider) {
        super();
        this.datasetProvider = datasetProvider;
    }

    @Inject
    public void setDatasourceProvider(DatasourceProvider datasetProvider) {
        this.datasetProvider = datasetProvider;
    }

    @Override
    public Nflow.ID resolveNflow(Serializable fid) {
        if (fid instanceof NflowId) {
            return (NflowId) fid;
        } else {
            return new NflowId(fid);
        }
    }

    @Override
    public NflowSource ensureNflowSource(Nflow.ID nflowId, ID dsId) {
        return ensureNflowSource(nflowId, dsId, null);
    }

    @Override
    public NflowSource ensureNflowSource(Nflow.ID nflowId, Datasource.ID dsId, ServiceLevelAgreement.ID slaId) {
        BaseNflow nflow = (BaseNflow) this.nflows.get(nflowId);
        Datasource ds = this.datasetProvider.getDatasource(dsId);

        if (nflow == null) {
            throw new NflowCreateException("A nflow with the given ID does not exists: " + nflowId);
        }

        if (ds == null) {
            throw new NflowCreateException("A dataset with the given ID does not exists: " + dsId);
        }

        return ensureNflowSource(nflow, ds, slaId);
    }

    @Override
    public NflowDestination ensureNflowDestination(Nflow.ID nflowId, Datasource.ID dsId) {
        BaseNflow nflow = (BaseNflow) this.nflows.get(nflowId);
        Datasource ds = this.datasetProvider.getDatasource(dsId);

        if (nflow == null) {
            throw new NflowCreateException("A nflow with the given ID does not exists: " + nflowId);
        }

        if (ds == null) {
            throw new NflowCreateException("A dataset with the given ID does not exists: " + dsId);
        }

        return ensureNflowDestination(nflow, ds);
    }

    @Override
    public Nflow ensureNflow(Category.ID categoryId, String nflowSystemName) {
        throw new UnsupportedOperationException("Unable to ensure nflow by categoryId with InMemoryProvider");
    }

    @Override
    public Nflow ensureNflow(String categorySystemName, String nflowSystemName) {
        return ensureNflow(categorySystemName, nflowSystemName, null);
    }

    @Override
    public Nflow ensureNflow(String categorySystemName, String name, String descr, ID destId) {
        Datasource dds = this.datasetProvider.getDatasource(destId);

        if (dds == null) {
            throw new NflowCreateException("A dataset with the given ID does not exists: " + destId);
        }

        BaseNflow nflow = (BaseNflow) ensureNflow(categorySystemName, name, descr);

        ensureNflowDestination(nflow, dds);
        return nflow;
    }

    @Override
    public Nflow ensureNflow(String categorySystemName, String name, String descr, ID srcId, ID destId) {
        Datasource sds = this.datasetProvider.getDatasource(srcId);
        Datasource dds = this.datasetProvider.getDatasource(destId);

        if (sds == null) {
            throw new NflowCreateException("A dataset with the given ID does not exists: " + srcId);
        }

        if (dds == null) {
            throw new NflowCreateException("A dataset with the given ID does not exists: " + destId);
        }

        BaseNflow nflow = (BaseNflow) ensureNflow(categorySystemName, name, descr);

        ensureNflowSource(nflow, sds, null);
        ensureNflowDestination(nflow, dds);

        return nflow;
    }

    @Override
    public Nflow ensureNflow(String categorySystemName, String name, String descr) {
        synchronized (this.nflows) {
            for (Nflow nflow : this.nflows.values()) {
                if (nflow.getName().equals(name)) {
                    return nflow;
                }
            }
        }

        BaseNflow newNflow = new BaseNflow(name, descr);
        this.nflows.put(newNflow.getId(), newNflow);
        return newNflow;
    }


    @Override
    public void removeNflowSources(Nflow.ID nflowId) {

    }

    @Override
    public void removeNflowSource(Nflow.ID nflowId, ID dsId) {

    }

    @Override
    public void removeNflowDestination(Nflow.ID nflowId, ID dsId) {

    }

    @Override
    public void removeNflowDestinations(Nflow.ID nflowId) {

    }

    @Override
    public Nflow createPrecondition(Nflow.ID nflowId, String descr, List<Metric> metrics) {
        BaseNflow nflow = (BaseNflow) this.nflows.get(nflowId);

        if (nflow != null) {
            // Remove the old one if any
            NflowPreconditionImpl precond = (NflowPreconditionImpl) nflow.getPrecondition();
            if (precond != null) {
                this.slaProvider.removeAgreement(precond.getAgreement().getId());
            }

            ServiceLevelAgreement sla = this.slaProvider.builder()
                .name("Precondition for nflow " + nflow.getName() + " (" + nflow.getId() + ")")
                .description(descr)
                .obligationBuilder(Condition.REQUIRED)
                .metric(metrics)
                .build()
                .build();

            return setupPrecondition(nflow, sla);
        } else {
            throw new NflowCreateException("A nflow with the given ID does not exists: " + nflowId);
        }
    }

    @Override
    public PreconditionBuilder buildPrecondition(final Nflow.ID nflowId) {
        BaseNflow nflow = (BaseNflow) this.nflows.get(nflowId);

        if (nflow != null) {
            ServiceLevelAgreementBuilder slaBldr = this.slaProvider.builder();
            return new PreconditionbuilderImpl(slaBldr, nflow);
        } else {
            throw new NflowCreateException("A nflow with the given ID does not exists: " + nflowId);
        }

    }

    @Override
    public Nflow addDependent(com.onescorpin.metadata.api.nflow.Nflow.ID targetId, com.onescorpin.metadata.api.nflow.Nflow.ID dependentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Nflow removeDependent(com.onescorpin.metadata.api.nflow.Nflow.ID nflowId, com.onescorpin.metadata.api.nflow.Nflow.ID depId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NflowCriteria nflowCriteria() {
        return new Criteria();
    }

    @Override
    public Nflow getNflow(Nflow.ID id) {
        return this.nflows.get(id);
    }

    @Override
    public List<Nflow> getNflows() {
        return getNflows(ALL);
    }

    @Override
    public Nflow findBySystemName(String systemName) {
        return findBySystemName(null, systemName);
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
    public List<Nflow> getNflows(NflowCriteria criteria) {
        Criteria critImpl = (Criteria) criteria;
        Iterator<Nflow> filtered = Iterators.filter(this.nflows.values().iterator(), critImpl);
        Iterator<Nflow> limited = Iterators.limit(filtered, critImpl.getLimit());

        return ImmutableList.copyOf(limited);
    }


    @Override
    public boolean enableNflow(Nflow.ID id) {
        BaseNflow nflow = (BaseNflow) getNflow(id);
        if (nflow != null) {
            nflow.setState(Nflow.State.ENABLED);
            return true;
        }
        return false;

    }

    @Override
    public boolean disableNflow(Nflow.ID id) {
        BaseNflow nflow = (BaseNflow) getNflow(id);
        if (nflow != null) {
            nflow.setState(Nflow.State.DISABLED);
            return true;
        }
        return false;
    }

    @Override
    public void deleteNflow(@Nonnull final Nflow.ID nflowId) {
        nflows.remove(nflowId);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#create(java.lang.Object)
     */
    @Override
    public Nflow create(Nflow t) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#findById(java.io.Serializable)
     */
    @Override
    public Nflow findById(com.onescorpin.metadata.api.nflow.Nflow.ID id) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#findAll()
     */
    @Override
    public List<Nflow> findAll() {
        // TODO Auto-generated method stub
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#findAll(org.springframework.data.domain.Pageable)
     */
    @Override
    public Page<Nflow> findPage(Pageable page, String filter) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#update(java.lang.Object)
     */
    @Override
    public Nflow update(Nflow t) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#delete(java.lang.Object)
     */
    @Override
    public void delete(Nflow t) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#deleteById(java.io.Serializable)
     */
    @Override
    public void deleteById(com.onescorpin.metadata.api.nflow.Nflow.ID id) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.BaseProvider#resolveId(java.io.Serializable)
     */
    @Override
    public com.onescorpin.metadata.api.nflow.Nflow.ID resolveId(Serializable fid) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.NflowProvider#findByTemplateId(com.onescorpin.metadata.api.template.NflowManagerTemplate.ID)
     */
    @Override
    public List<? extends Nflow> findByTemplateId(com.onescorpin.metadata.api.template.NflowManagerTemplate.ID templateId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.nflow.NflowProvider#findByCategoryId(com.onescorpin.metadata.api.category.Category.ID)
     */
    @Override
    public List<? extends Nflow> findByCategoryId(com.onescorpin.metadata.api.category.Category.ID categoryId) {
        // TODO Auto-generated method stub
        return null;
    }

    private NflowSource ensureNflowSource(BaseNflow nflow, Datasource ds, ServiceLevelAgreement.ID slaId) {
        Map<Datasource.ID, NflowSource> srcIds = new HashMap<>();
        for (NflowSource src : nflow.getSources()) {
            srcIds.put(src.getDatasource().getId(), src);
        }

        if (srcIds.containsKey(ds.getId())) {
            return srcIds.get(ds.getId());
        } else {
            ServiceLevelAgreement sla = this.slaProvider.getAgreement(slaId);
            NflowSource src = nflow.addSource(ds, sla);
            return src;
        }
    }

    private NflowDestination ensureNflowDestination(BaseNflow nflow, Datasource ds) {
        NflowDestination dest = nflow.getDestination(ds.getId());

        if (dest != null) {
            return dest;
        } else {
            dest = nflow.addDestination(ds);
            return dest;
        }
    }

    private Nflow setupPrecondition(BaseNflow nflow, ServiceLevelAgreement sla) {
        nflow.setPrecondition(sla);
        return nflow;
    }

    @Override
    public Nflow updateNflowServiceLevelAgreement(Nflow.ID nflowId, ServiceLevelAgreement sla) {
        return null;
    }

    @Override
    public Map<String, Object> mergeNflowProperties(Nflow.ID nflowId, Map<String, Object> properties) {
        return null;
    }

    @Override
    public Map<String, Object> replaceProperties(Nflow.ID nflowId, Map<String, Object> properties) {
        return null;
    }

    @Nonnull
    @Override
    public Set<UserFieldDescriptor> getUserFields() {
        return Collections.emptySet();
    }

    @Override
    public void setUserFields(@Nonnull Set<UserFieldDescriptor> userFields) {
    }

    @Override
    public void populateInverseNflowDependencies() {

    }

    private static class Criteria extends AbstractMetadataCriteria<NflowCriteria> implements NflowCriteria, Predicate<Nflow> {

        private String name;
        private Set<Datasource.ID> sourceIds = new HashSet<>();
        private Set<Datasource.ID> destIds = new HashSet<>();
        private String category;

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
        public NflowCriteria sourceDatasource(ID id, ID... others) {
            this.sourceIds.add(id);
            for (ID other : others) {
                this.sourceIds.add(other);
            }
            return this;
        }

        @Override
        public NflowCriteria destinationDatasource(ID id, ID... others) {
            this.destIds.add(id);
            for (ID other : others) {
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

    private class PreconditionbuilderImpl implements PreconditionBuilder {

        private final ServiceLevelAgreementBuilder slaBuilder;
        private final BaseNflow nflow;

        public PreconditionbuilderImpl(ServiceLevelAgreementBuilder slaBuilder, BaseNflow nflow) {
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

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.versioning.EntityVersionProvider#findVersions(java.io.Serializable, boolean)
     */
    @Override
    public Optional<List<EntityVersion<Nflow>>> findVersions(com.onescorpin.metadata.api.nflow.Nflow.ID id, boolean includeEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.versioning.EntityVersionProvider#findVersion(java.io.Serializable, com.onescorpin.metadata.api.versioning.EntityVersion.ID, boolean)
     */
    @Override
    public Optional<EntityVersion<Nflow>> findVersion(com.onescorpin.metadata.api.nflow.Nflow.ID entityId, com.onescorpin.metadata.api.versioning.EntityVersion.ID versionId,
                                                     boolean includeEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.versioning.EntityVersionProvider#findLatestVersion(java.io.Serializable, boolean)
     */
    @Override
    public Optional<EntityVersion<Nflow>> findLatestVersion(com.onescorpin.metadata.api.nflow.Nflow.ID entityId, boolean includeEntity) {
        // TODO Auto-generated method stub
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.versioning.EntityVersionProvider#resolveVersion(java.io.Serializable)
     */
    @Override
    public com.onescorpin.metadata.api.versioning.EntityVersion.ID resolveVersion(Serializable ser) {
        // TODO Auto-generated method stub
        return null;
    }
}
