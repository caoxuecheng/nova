/**
 *
 */
package com.onescorpin.metadata.api.sla;

/*-
 * #%L
 * onescorpin-metadata-api
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

import java.beans.Transient;

/**
 *
 */
public class DatasourceUpdatedSinceNflowExecuted extends DependentDatasource {

    public DatasourceUpdatedSinceNflowExecuted() {
    }

    public DatasourceUpdatedSinceNflowExecuted(String datasourceName, String nflowName) {
        super(nflowName, datasourceName);
    }

    /* (non-Javadoc)
     * @see com.onescorpin.metadata.sla.api.Metric#getDescription()
     */
    @Override
    @Transient
    public String getDescription() {
        return "Datasource " + getDatasourceName() + " has been updated since nflow " + getNflowName() + " was last executed";
    }
}
