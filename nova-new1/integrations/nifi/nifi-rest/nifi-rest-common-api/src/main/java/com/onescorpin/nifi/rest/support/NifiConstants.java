package com.onescorpin.nifi.rest.support;

/*-
 * #%L
 * onescorpin-nifi-rest-common-api
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
 * Defines NiFi enumerations.
 */
public class NifiConstants {

    /**
     * Indicates a type of NiFi port.
     */
    public enum NIFI_PORT_TYPE {
        OUTPUT_PORT, INPUT_PORT
    }

    /**
     * Indicates a type of NiFi processor.
     */
    public enum NIFI_PROCESSOR_TYPE {
        PROCESSOR
    }

    /**
     * Indicates a type of NiFi component.
     */
    public enum NIFI_COMPONENT_TYPE {
        OUTPUT_PORT, INPUT_PORT, PROCESSOR, PROCESS_GROUP, REMOTE_PROCESS_GROUP,CONNECTION, TEMPLATE, CONTROLLER_SERVICE
    }
}
