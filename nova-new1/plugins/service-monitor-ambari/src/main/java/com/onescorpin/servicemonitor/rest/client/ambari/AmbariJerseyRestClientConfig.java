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

import com.onescorpin.rest.JerseyClientConfig;
import com.onescorpin.security.core.encrypt.EncryptionService;

/**
 * Configuration for Ambari REST client
 */
public class AmbariJerseyRestClientConfig extends JerseyClientConfig {

    private String apiPath = "/api/v1";

    public AmbariJerseyRestClientConfig(EncryptionService encryptionService) {
        super(encryptionService);
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }


}
