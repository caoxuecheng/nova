package com.onescorpin.nifi.provenance;

/*-
 * #%L
 * onescorpin-nifi-provenance-constants
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
 * Provenance constants needed for Nova to check create controller services, reporting task, and check its status in NiFi
 */
public interface NiFiProvenanceConstants {


    String NiFiMetadataServiceName = "Nova Metadata Service";

    String NiFiMetadataControllerServiceType = "com.onescorpin.nifi.v2.core.metadata.MetadataProviderSelectorService";

    String NiFiNovaProvenanceEventReportingTaskType = "com.onescorpin.nifi.provenance.reporting.NovaProvenanceEventReportingTask";

}
