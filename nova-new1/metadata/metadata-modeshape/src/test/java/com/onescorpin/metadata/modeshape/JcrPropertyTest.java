package com.onescorpin.metadata.modeshape;

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

import com.onescorpin.metadata.api.MetadataAccess;
import com.onescorpin.metadata.api.category.Category;
import com.onescorpin.metadata.api.category.CategoryProvider;
import com.onescorpin.metadata.api.datasource.DatasourceProvider;
import com.onescorpin.metadata.api.extension.ExtensibleType;
import com.onescorpin.metadata.api.extension.FieldDescriptor;
import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.nflow.NflowProvider;
import com.onescorpin.metadata.api.nflow.NflowSource;
import com.onescorpin.metadata.modeshape.category.JcrCategory;
import com.onescorpin.metadata.modeshape.common.JcrObject;
import com.onescorpin.metadata.modeshape.datasource.JcrDatasource;
import com.onescorpin.metadata.modeshape.datasource.JcrDerivedDatasource;
import com.onescorpin.metadata.modeshape.extension.JcrExtensibleTypeProvider;
import com.onescorpin.metadata.modeshape.nflow.JcrNflow;
import com.onescorpin.metadata.modeshape.nflow.JcrNflowProvider;
import com.onescorpin.metadata.modeshape.support.JcrVersionUtil;
import com.onescorpin.metadata.modeshape.tag.TagProvider;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testng.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ModeShapeEngineConfig.class, JcrTestConfig.class, JcrPropertyTestConfig.class})
@ComponentScan(basePackages = {"com.onescorpin.metadata.modeshape.op"})
public class JcrPropertyTest {

    private static final Logger log = LoggerFactory.getLogger(JcrPropertyTest.class);
    @Inject
    CategoryProvider categoryProvider;
    @Inject
    DatasourceProvider datasourceProvider;
    @Inject
    NflowProvider nflowProvider;
    @Inject
    TagProvider tagProvider;
    @Inject
    private JcrExtensibleTypeProvider provider;
    @Inject
    private JcrMetadataAccess metadata;

    @Test
    public void testGetPropertyTypes() throws RepositoryException {
        Map<String, FieldDescriptor.Type> propertyTypeMap = metadata.commit(() -> {
            provider.ensureTypeDescriptors();
            
            ExtensibleType nflowType = provider.getType("tba:nflow");
            Set<FieldDescriptor> fields = nflowType.getFieldDescriptors();
            Map<String, FieldDescriptor.Type> map = new HashMap<>();

            for (FieldDescriptor field : fields) {
                map.put(field.getName(), field.getType());
            }

            return map;
        }, MetadataAccess.SERVICE);
        log.info("Property Types {} ", propertyTypeMap);

    }


    /**
     * Creates a new Category Creates a new Nflow Updates the Nflow Get Nflow and its versions Validates Nflow is versioned along with its properties and can successfully return a version
     */
    @Test
    public void testNflow() {
        String categorySystemName = "my_category";
        Category category = metadata.commit(() -> {
            JcrCategory cat = (JcrCategory) categoryProvider.ensureCategory(categorySystemName);
            cat.setDescription("my category desc");
            cat.setTitle("my category");
            categoryProvider.update(cat);
            return cat;
        }, MetadataAccess.SERVICE);

        final JcrNflow.NflowId createdNflowId = metadata.commit(() -> {

            String sysName = "my_category";

            JcrCategory cat = (JcrCategory) categoryProvider.ensureCategory(sysName);
            cat.setDescription("my category desc");
            cat.setTitle("my category");
            categoryProvider.update(cat);

            JcrDerivedDatasource datasource1 = (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HiveDatasource", "mysql.table1", "mysql.table1", "mysql table source 1", null);
            JcrDerivedDatasource datasource2 = (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HiveDatasource", "mysql.table2", "mysql.table2", "mysql table source 2", null);

            JcrDerivedDatasource emptySource1 = (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HDFSDatasource", "", "", "empty hdfs source", null);
            JcrDerivedDatasource emptySource2 = (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HDFSDatasource", "", "", "empty hdfs source", null);
            Assert.assertEquals(emptySource1.getId(), emptySource2.getId());
            JcrDerivedDatasource emptySource3 = (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HDFSDatasource", null, null, "empty hdfs source", null);
            JcrDerivedDatasource emptySource4 = (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HDFSDatasource", null, null, "empty hdfs source", null);
            Assert.assertEquals(emptySource3.getId(), emptySource4.getId());
            JcrDerivedDatasource
                datasource3 =
                (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HDFSDatasource", "/etl/customers/swert/012320342", "/etl/customers/swert/012320342", "mysql hdfs source", null);
            JcrDerivedDatasource
                datasource4 =
                (JcrDerivedDatasource) datasourceProvider.ensureDerivedDatasource("HDFSDatasource", "/etl/customers/swert/012320342", "/etl/customers/swert/012320342", "mysql hdfs source", null);
            Assert.assertEquals(datasource3.getId(), datasource4.getId());

            String nflowSystemName = "my_nflow_" + UUID.randomUUID();
//                JcrNflow nflow = (JcrNflow) nflowProvider.ensureNflow(categorySystemName, nflowSystemName, "my nflow desc", datasource1.getId(), null);
            JcrNflow nflow = (JcrNflow) nflowProvider.ensureNflow(sysName, nflowSystemName, "my nflow desc");
            nflowProvider.ensureNflowSource(nflow.getId(), datasource1.getId());
            nflowProvider.ensureNflowSource(nflow.getId(), datasource2.getId());
            nflow.setTitle("my nflow");
            nflow.addTag("my tag");
            nflow.addTag("my second tag");
            nflow.addTag("nflowTag");

            Map<String, Object> otherProperties = new HashMap<String, Object>();
            otherProperties.put("prop1", "my prop1");
            otherProperties.put("prop2", "my prop2");
            nflow.setProperties(otherProperties);

            return nflow.getId();
        }, MetadataAccess.SERVICE);

        //read and find nflow verisons and ensure props

        JcrNflow.NflowId readNflowId = metadata.read(() -> {
            Session s = null;
            JcrNflow f = (JcrNflow) ((JcrNflowProvider) nflowProvider).findById(createdNflowId);
            // TODO: Nflow vesioning disabled for Nova v0.5.0
//            int versions = printVersions(f);
//            Assert.assertTrue(versions > 1, "Expecting more than 1 version: jcr:rootVersion, 1.0");

            @SuppressWarnings("unchecked")
            List<? extends NflowSource> sources = f.getSources();

            Assert.assertTrue(sources.size() > 0);

            if (sources != null) {
                for (NflowSource source : sources) {
                    Map<String, Object> dataSourceProperties = ((JcrDatasource) source.getDatasource()).getAllProperties();
                    String type = (String) dataSourceProperties.get(JcrDerivedDatasource.TYPE_NAME);
                    Assert.assertEquals(type, "HiveDatasource");
                }
            }
            List<JcrObject> taggedObjects = tagProvider.findByTag("my tag");
            //assert we got 1 nflow back
            Assert.assertTrue(taggedObjects.size() >= 1);
            return f.getId();
        }, MetadataAccess.SERVICE);

        //update the nflow again

        JcrNflow.NflowId updatedNflow = metadata.commit(() -> {
            JcrNflow f = (JcrNflow) ((JcrNflowProvider) nflowProvider).findById(createdNflowId);
            f.setDescription("My Nflow Updated Description");

            Map<String, Object> otherProperties = new HashMap<String, Object>();
            otherProperties.put("prop1", "my updated prop1");
            f.setProperties(otherProperties);

            ((JcrNflowProvider) nflowProvider).update(f);
            return f.getId();
        }, MetadataAccess.SERVICE);

        //read it again and find the versions
        readNflowId = metadata.read(() -> {
            JcrNflow f = (JcrNflow) ((JcrNflowProvider) nflowProvider).findById(updatedNflow);
            // TODO: Nflow vesioning disabled for Nova v0.5.0
//            int versions = printVersions(f);
//            Assert.assertTrue(versions > 2, "Expecting more than 2 versions: jcr:rootVersion, 1.0, 1.1");
//            JcrNflow v1 = JcrVersionUtil.getVersionedNode(f, "1.0", JcrNflow.class);
//            JcrNflow v11 = JcrVersionUtil.getVersionedNode(f, "1.1", JcrNflow.class);
//            String v1Prop1 = v1.getProperty("prop1", String.class);
//            String v11Prop1 = v11.getProperty("prop1", String.class);
//            JcrNflow baseVersion = JcrVersionUtil.getVersionedNode(JcrVersionUtil.getBaseVersion(f.getNode()), JcrNflow.class);

            //Assert the Props get versioned

//            Assert.assertEquals(v1Prop1, "my prop1");
//            Assert.assertEquals(v11Prop1, "my updated prop1");
//            Assert.assertEquals(v1.getDescription(), "my nflow desc");
//            Assert.assertEquals(v11.getDescription(), "My Nflow Updated Description");
//            String v = v11.getVersionName();
//            Nflow.ID v1Id = v1.getId();
//            Nflow.ID v11Id = v11.getId();
//            Nflow.ID baseId = baseVersion.getId();
            //assert all ids are equal
//            Assert.assertEquals(v1Id, v11Id);
//            Assert.assertEquals(v1Id, baseId);
            return f.getId();
        }, MetadataAccess.SERVICE);


    }

    private int printVersions(JcrObject o) {
        List<Version> versions = JcrVersionUtil.getVersions(o.getNode());
        int versionCount = versions.size();
        log.info(" {}. Version count: {}", o.getNodeName(), versionCount);
        for (Version v : versions) {
            try {
                log.info("Version: {}", v.getName());

            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        return versionCount;
    }

    @Test
    public void queryTest() {

        //final String query = "select e.* from [tba:nflow] as e  join [tba:category] c on e.[tba:category].[tba:systemName] = c.[tba:systemName] where  c.[tba:systemName] = $category ";
        final String query = "select e.* from [tba:nflow] as e join [tba:category] as c on e.[tba:category] = c.[jcr:uuid]";

        metadata.read(() -> {
            List<Node> nflows = ((JcrNflowProvider) nflowProvider).findNodes(query);
        }, MetadataAccess.SERVICE);

        metadata.read(() -> {
            List<Category> c = categoryProvider.findAll();
            if (c != null) {
                for (Category cat : c) {
                    Category jcrCategory = (Category) cat;
                    List<? extends Nflow> categoryNflows = jcrCategory.getNflows();
                    if (categoryNflows != null) {
                        for (Nflow nflow : categoryNflows) {
                            log.info("Nflow for category {} is {}", cat.getSystemName(), nflow.getName());
                        }
                    }

                }
            }
        }, MetadataAccess.SERVICE);

    }

    @Test
    public void testNflowManager() {
        Nflow nflow = metadata.read(() -> {
            List<Nflow> nflows = nflowProvider.findAll();
            if (nflows != null) {
                return nflows.get(0);
            }
            return null;
        }, MetadataAccess.SERVICE);

    }

    @Test
    public void testMergeProps() {
        testNflow();
        Map<String, Object> props = new HashMap<>();
        props.put("name", "An Old User");
        props.put("age", 140);

        Map<String, Object> props2 = metadata.commit(() -> {
            List<? extends Nflow> nflows = nflowProvider.getNflows();
            Nflow nflow = null;
            //grab the first nflow
            if (nflows != null) {
                nflow = nflows.get(0);
            }
            nflowProvider.mergeNflowProperties(nflow.getId(), props);
            return nflow.getProperties();
        }, MetadataAccess.SERVICE);
        props.put("address", "Some road");
        props2 = metadata.commit(() -> {
            List<? extends Nflow> nflows = nflowProvider.getNflows();
            Nflow nflow = null;
            //grab the first nflow
            if (nflows != null) {
                nflow = nflows.get(0);
            }

            nflowProvider.mergeNflowProperties(nflow.getId(), props);
            return nflow.getProperties();
        }, MetadataAccess.SERVICE);
        org.junit.Assert.assertEquals("Some road", props2.get("address"));

        //Now Replace
        props.remove("address");
        props2 = metadata.commit(() -> {
            List<? extends Nflow> nflows = nflowProvider.getNflows();
            Nflow nflow = null;
            //grab the first nflow
            if (nflows != null) {
                nflow = nflows.get(0);
            }
            nflowProvider.replaceProperties(nflow.getId(), props);
            return nflow.getProperties();
        }, MetadataAccess.SERVICE);
        Assert.assertNull(props2.get("address"));


    }
}






