package com.onescorpin.nifi.rest;

/*-
 * #%L
 * onescorpin-service-app
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

import com.onescorpin.nflowmgr.nifi.CreateNflowBuilder;
import com.onescorpin.nflowmgr.nifi.PropertyExpressionResolver;
import com.onescorpin.nflowmgr.nifi.cache.DefaultNiFiFlowCompletionCallback;
import com.onescorpin.nflowmgr.nifi.cache.NiFiFlowInspectorManager;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCache;
import com.onescorpin.nflowmgr.nifi.cache.NifiFlowCacheImpl;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;
import com.onescorpin.nifi.nflowmgr.InputOutputPort;
import com.onescorpin.nifi.rest.client.LegacyNifiRestClient;
import com.onescorpin.nifi.rest.client.NiFiRestClient;
import com.onescorpin.nifi.rest.client.NifiRestClientConfig;
import com.onescorpin.nifi.rest.model.NifiProcessorSchedule;
import com.onescorpin.nifi.rest.model.NifiProperty;
import com.onescorpin.nifi.rest.model.flow.NifiFlowProcessGroup;
import com.onescorpin.nifi.rest.model.visitor.NifiFlowBuilder;
import com.onescorpin.nifi.rest.model.visitor.NifiVisitableProcessGroup;
import com.onescorpin.nifi.rest.support.NifiPropertyUtil;
import com.onescorpin.nifi.v1.rest.client.NiFiRestClientV1;
import com.onescorpin.nifi.v1.rest.model.NiFiPropertyDescriptorTransformV1;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.web.api.dto.ProcessGroupDTO;
import org.apache.nifi.web.api.dto.TemplateDTO;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class NifiRestTest {

    private static final Logger log = LoggerFactory.getLogger(NifiRestTest.class);
    private NiFiObjectCache createNflowBuilderCache;
    private LegacyNifiRestClient restClient;
    private NifiFlowCache nifiFlowCache;
    private NiFiPropertyDescriptorTransformV1 propertyDescriptorTransform;

    @Before
    public void setupRestClient() {
        restClient = new LegacyNifiRestClient();
        NifiRestClientConfig clientConfig = new NifiRestClientConfig();
        //clientConfig.setHost("localhost");
        clientConfig.setHost("34.208.236.190");
        clientConfig.setPort(8079);
        NiFiRestClient c = new NiFiRestClientV1(clientConfig);
        restClient.setClient(c);
        nifiFlowCache = new NifiFlowCacheImpl();
        propertyDescriptorTransform = new NiFiPropertyDescriptorTransformV1();
        createNflowBuilderCache = new NiFiObjectCache();
        createNflowBuilderCache.setRestClient(restClient);

    }


    //@Test
    public void testCreateNflow1() {
        TemplateDTO templateDTO = restClient.getTemplateByName("New Data Ingest");
        String inputType = "org.apache.nifi.processors.standard.GetFile";

        NifiProcessorSchedule schedule = new NifiProcessorSchedule();
        schedule.setSchedulingStrategy("TIMER_DRIVEN");
        schedule.setSchedulingPeriod("10 sec");
        String inputPortName = "From Data Ingest Nflow";

        String nflowOutputPortName = "To Data Ingest";

        NflowMetadata nflowMetadata = new NflowMetadata();
        nflowMetadata.setCategory(new NflowCategory());
        nflowMetadata.getCategory().setSystemName("online");
        nflowMetadata.setSystemNflowName("Scotts Nflow");

        CreateNflowBuilder.newNflow(restClient, nifiFlowCache, nflowMetadata, templateDTO.getId(), new PropertyExpressionResolver(), propertyDescriptorTransform, createNflowBuilderCache)
            .inputProcessorType(inputType)
            .nflowSchedule(schedule).addInputOutputPort(new InputOutputPort(inputPortName, nflowOutputPortName)).build();
    }


    //@Test
    public void testLoad() {
        //setup constants for the test
        String templateName = "Data Ingest";
        int num = 10;
        String processGroupName = "LoadTest";
        String nflowPrefix = "LT_";
        String inputType = "org.apache.nifi.processors.standard.GetFile";
        List<NifiProperty> templateProperties = new ArrayList<>();

        String schedulePeriod = "10 sec";

        String GET_FILE_PROCESSOR_NAME = "Poll filesystem";
        String UPDATE_PARAMETERS_PROCESSOR_NAME = "Update flow parameters";

        String INPUT_DIRECTORY_PROPERTY = "Input Directory";
        String SOURCE_PROPERTY = "source";
        String ENTITY_PROPERTY = "entity";

        try {
            TemplateDTO template = restClient.getTemplateByName(templateName);

            List<NifiProperty> propertyList = restClient.getPropertiesForTemplate(template.getId(), true);
            NifiProperty inputDirectory = NifiPropertyUtil
                .getProperty(GET_FILE_PROCESSOR_NAME, INPUT_DIRECTORY_PROPERTY, propertyList);
            NifiProperty entity = NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, SOURCE_PROPERTY, propertyList);
            NifiProperty source = NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, ENTITY_PROPERTY, propertyList);
            templateProperties.add(inputDirectory);
            templateProperties.add(entity);
            templateProperties.add(source);

            NifiProcessorSchedule schedule = new NifiProcessorSchedule();
            schedule.setSchedulingStrategy("TIMER_DRIVEN");
            schedule.setSchedulingPeriod(schedulePeriod);
            for (int i = 0; i < num; i++) {
                String nflowName = nflowPrefix + i;

                List<NifiProperty> instanceProperties = NifiPropertyUtil.copyProperties(templateProperties);
                //update the properties
                NifiPropertyUtil.getProperty(GET_FILE_PROCESSOR_NAME, INPUT_DIRECTORY_PROPERTY, instanceProperties).setValue("/tmp/" + nflowName);
                NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, SOURCE_PROPERTY, instanceProperties).setValue(processGroupName);
                NifiPropertyUtil.getProperty(UPDATE_PARAMETERS_PROCESSOR_NAME, ENTITY_PROPERTY, instanceProperties).setValue(nflowName);

                NflowMetadata nflowMetadata = new NflowMetadata();
                nflowMetadata.setCategory(new NflowCategory());
                nflowMetadata.getCategory().setSystemName(processGroupName);
                nflowMetadata.setSystemNflowName("nflowPrefix + i");

                CreateNflowBuilder.newNflow(restClient, nifiFlowCache, nflowMetadata, template.getId(), new PropertyExpressionResolver(), propertyDescriptorTransform, createNflowBuilderCache)
                    .inputProcessorType(inputType)
                    .nflowSchedule(schedule).properties(instanceProperties).build();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //@Test
    public void testCreateNflow() throws Exception {
        TemplateDTO templateDTO = restClient.getTemplateByName("New Data Ingest");
        String inputType = "org.apache.nifi.processors.standard.GetFile";

        NifiProcessorSchedule schedule = new NifiProcessorSchedule();
        schedule.setSchedulingStrategy("TIMER_DRIVEN");
        schedule.setSchedulingPeriod("10 sec");
        String inputPortName = "From Data Ingest Nflow";

        String nflowOutputPortName = "To Data Ingest";

        NflowMetadata nflowMetadata = new NflowMetadata();
        nflowMetadata.setCategory(new NflowCategory());
        nflowMetadata.getCategory().setSystemName("online");
        nflowMetadata.setSystemNflowName("Scotts Nflow");

        CreateNflowBuilder.newNflow(restClient, nifiFlowCache, nflowMetadata, templateDTO.getId(), new PropertyExpressionResolver(), propertyDescriptorTransform, createNflowBuilderCache)
            .inputProcessorType(inputType)
            .nflowSchedule(schedule).addInputOutputPort(new InputOutputPort(inputPortName, nflowOutputPortName)).build();
    }

    //@Test
    public void testInspection() {
        DefaultNiFiFlowCompletionCallback completionCallback = new DefaultNiFiFlowCompletionCallback();
        NiFiFlowInspectorManager flowInspectorManager = new NiFiFlowInspectorManager.NiFiFlowInspectorManagerBuilder(restClient.getNiFiRestClient())
            .startingProcessGroupId("root")
            .completionCallback(completionCallback)
            .threads(50)
            .waitUntilComplete(true)
            .buildAndInspect();

        log.info("NiFi Flow Inspection took {} ms with {} threads for {} nflows, {} processors and {} connections ", flowInspectorManager.getTotalTime(), flowInspectorManager.getThreadCount(),completionCallback.getNflowNames().size(),
                 completionCallback.getProcessorIdToProcessorName().size(), completionCallback.getConnectionIdCacheNameMap().size());

        int i = 0;
    }

    // @Test
    public void testOrder() throws Exception {

        NifiVisitableProcessGroup g = restClient.getFlowOrder("63de0732-015e-1000-f198-dcd76ac2942e", null);
        NifiFlowProcessGroup flow = new NifiFlowBuilder().build(g);

        // NifiFlowProcessGroup flow2 = restClient.getNflowFlow("27ab143a-0159-1000-4f6a-30f3746a341e");

        // List<String> nflows = Lists.newArrayList();
        //   nflows.add("sample.new_nflow_three");
        //   List<NifiFlowProcessGroup> flows3 = restClient.getNiFiRestClient().flows().getNflowFlows(nflows);

//        List<NifiFlowProcessGroup> nflowFlows = restClient.getNflowFlows();
        int i = 0;

    }

    //  @Test
    public void findNflowProcessGroup() throws Exception {
        String nflowName = "cat_62_nflow_282";
        String categoryGroupId = "66e65266-015e-1000-703b-cb619274da55";
        long start = System.currentTimeMillis();
        ProcessGroupDTO nflowGroup = restClient.getProcessGroupByName(categoryGroupId, nflowName);
        long stop = System.currentTimeMillis();
        long time = stop - start;
        int i = 0;
    }


    // @Test
    public void testUpdateProcessor() {
        NifiProperty p = new NifiProperty();
        p.setProcessGroupId("0b013850-d6bb-44e4-87c2-1784858e60ab");
        p.setProcessorId("795509d5-1433-4e64-b7bd-d05c6adfb95a");
        p.setKey("Source Database Connection");
        p.setValue("4688ee71-262c-46bc-af35-9e9825507160");
        restClient.updateProcessorProperty(p.getProcessGroupId(), p.getProcessorId(), p);
        int i = 0;
    }

    // @Test
    public void testFile() throws IOException {
        InputStream in = NifiRestTest.class
            .getResourceAsStream("/template.xml");
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, "UTF-8");
        String theString = writer.toString();

        restClient.importTemplate("test", theString);
    }

}
