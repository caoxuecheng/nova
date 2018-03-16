/**
 *
 */
package com.onescorpin.metadata.rest.model.nflow;

/*-
 * #%L
 * onescorpin-metadata-rest-model
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.onescorpin.metadata.rest.model.data.Datasource;
import com.onescorpin.metadata.rest.model.nflow.transform.FieldsPolicy;

import java.io.Serializable;

/**
 *
 */
@SuppressWarnings("serial")
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NflowDestination implements Serializable {

    private String id;
    private FieldsPolicy fieldsPolicy;
    private String nflowId;
    private String datasourceId;
    private Datasource datasource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNflowId() {
        return nflowId;
    }

    public void setNflowId(String nflowId) {
        this.nflowId = nflowId;
    }

    public FieldsPolicy getFieldsPolicy() {
        return fieldsPolicy;
    }

    public void setFieldsPolicy(FieldsPolicy fieldsPolicy) {
        this.fieldsPolicy = fieldsPolicy;
    }

    public String getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }
}
