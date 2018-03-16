package com.onescorpin.nflowmgr.rest.model.schema;

/*-
 * #%L
 * onescorpin-nflow-manager-rest-model
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onescorpin.discovery.model.DefaultField;
import com.onescorpin.discovery.model.DefaultTableSchema;
import com.onescorpin.nflowmgr.rest.model.NflowCategory;
import com.onescorpin.nflowmgr.rest.model.NflowMetadata;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class TableSetupTest {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        NflowMetadata nflowMetadata = new NflowMetadata();
        nflowMetadata.setCategory(new NflowCategory());
        nflowMetadata.setTable(new TableSetup());
        nflowMetadata.getTable().setTableSchema(new DefaultTableSchema());
        nflowMetadata.getTable().getTableSchema().setName("test");
        DefaultField f1 = new DefaultField();
        f1.setName("field1");
        nflowMetadata.getTable().getTableSchema().getFields().add(f1);

        String json = mapper.writeValueAsString(nflowMetadata);
        NflowMetadata nflowMetadata2 = mapper.readValue(json, NflowMetadata.class);
        assertEquals(nflowMetadata2.getTable().getTableSchema().getName(), nflowMetadata.getTable().getTableSchema().getName());

    }

}
