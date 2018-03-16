package com.onescorpin.nflowmgr.rest.support;

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

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class SystemNamingServiceTest {


    @Test
    public void testSystemName() {
        String name = "MYNflowName";

        String systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("my_nflow_name", systemName);
        name = "myNflowNameTEST";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("my_nflow_name_test", systemName);
        name = "My-TestNflowName";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("my_test_nflow_name", systemName);
        name = "MY_TESTNflowName";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("my_test_nflow_name", systemName);

        name = "ALPHA1";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("alpha1", systemName);

        name = "myALPHA13242";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("my_alpha13242", systemName);

        name = "ALPHA13242";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("alpha13242", systemName);

        name = "ALPHA13242TEST";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("alpha13242_test", systemName);

        name = "m;,y_nflow_name";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("my_nflow_name", systemName);

        name = ";,my nflow name";
        systemName = SystemNamingService.generateSystemName(name);
        Assert.assertEquals("my_nflow_name", systemName);

    }


}
