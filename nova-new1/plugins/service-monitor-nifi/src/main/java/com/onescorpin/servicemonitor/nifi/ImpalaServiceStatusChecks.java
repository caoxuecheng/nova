package com.onescorpin.servicemonitor.nifi;

/*-
 * #%L
 * onescorpin-service-monitor-nifi
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


import com.onescorpin.servicemonitor.check.ServiceStatusCheck;
import com.onescorpin.servicemonitor.model.DefaultServiceComponent;
import com.onescorpin.servicemonitor.model.DefaultServiceStatusResponse;
import com.onescorpin.servicemonitor.model.ServiceComponent;
import com.onescorpin.servicemonitor.model.ServiceStatusResponse;


import java.util.Arrays;
/**
 * Check the status of NiFi and the required Nova Reporting Task
 */
public class ImpalaServiceStatusChecks implements ServiceStatusCheck {

    @Override
    public ServiceStatusResponse healthCheck() {

        String serviceName = "aaaaaa";

        return new DefaultServiceStatusResponse(serviceName, Arrays.asList(nifiStatus()));
    }


    /**
     * Check to see if NiFi is running
     *
     * @return the status of NiFi
     */
    private ServiceComponent nifiStatus() {

        String componentName = "aaaaa";
        ServiceComponent component = null;
        component = new DefaultServiceComponent.Builder(componentName, ServiceComponent.STATE.DOWN).message("aaaaaaaaa").build();
        return component;

    }
}
