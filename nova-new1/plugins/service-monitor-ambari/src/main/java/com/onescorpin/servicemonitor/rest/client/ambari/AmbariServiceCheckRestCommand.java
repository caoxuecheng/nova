package com.onescorpin.servicemonitor.rest.client.ambari;

/*-
 * #%L
 * onescorpin-service-monitor-ambari
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


import com.onescorpin.servicemonitor.rest.client.RestCommand;

/**
 * Superclass for checking status of Ambari services.
 */
public class AmbariServiceCheckRestCommand<T> extends RestCommand<T> {

    private String clusterName;
    private String services;

    AmbariServiceCheckRestCommand(String clusterName, String services) {
        super();
        this.clusterName = clusterName;
        this.services = services;
    }


    @Override
    public String payload() {
        return null;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }


    String getClusterName() {
        return clusterName;
    }

}
