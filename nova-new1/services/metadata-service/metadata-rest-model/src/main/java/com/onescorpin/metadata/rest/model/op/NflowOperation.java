/**
 *
 */
package com.onescorpin.metadata.rest.model.op;

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

import org.joda.time.DateTime;

import java.util.Map;

/**
 *
 */
@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NflowOperation {

    private String operationId;
    private String nflowId;
    private State state;
    private String status;
    private DateTime startTime;
    private DateTime stopTiime;
    private Map<String, String> results;

    public NflowOperation() {
        super();
    }

    public NflowOperation(String operationId, String nflowId, State state, String status, DateTime startTime, DateTime stopTiime) {
        super();
        this.operationId = operationId;
        this.nflowId = nflowId;
        this.state = state;
        this.status = status;
        this.startTime = startTime;
        this.stopTiime = stopTiime;
    }

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String id) {
        this.operationId = id;
    }

    public String getNflowId() {
        return nflowId;
    }

    public void setNflowId(String nflowId) {
        this.nflowId = nflowId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public DateTime getStopTiime() {
        return stopTiime;
    }

    public void setStopTime(DateTime stopTiime) {
        this.stopTiime = stopTiime;
    }

    public enum State {
        STARTED, SUCCESS, FAILURE, CANCELED
    }

}
