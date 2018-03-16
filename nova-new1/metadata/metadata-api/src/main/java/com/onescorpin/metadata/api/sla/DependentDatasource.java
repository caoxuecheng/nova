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

import com.onescorpin.metadata.sla.api.Metric;

/**
 *
 */
public abstract class DependentDatasource implements Metric {

    private String datasourceName;
    private String nflowName;

    public DependentDatasource() {
    }

    public DependentDatasource(String datasetName) {
        this(null, datasetName);
    }

    public DependentDatasource(String nflowName, String datasetName) {
        super();
        this.nflowName = nflowName;
        this.datasourceName = datasetName;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public String getNflowName() {
        return nflowName;
    }

    public void setNflowName(String nflowName) {
        this.nflowName = nflowName;
    }

}
