/**
 *
 */
package com.onescorpin.metadata.api.event.nflow;

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

import com.onescorpin.metadata.api.nflow.Nflow;
import com.onescorpin.metadata.api.op.NflowOperation;

import java.io.Serializable;

/**
 *
 */
public class OperationStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Nflow.ID nflowId;
    private final String nflowName; // {category}.{nflowSystemName}
    private final NflowOperation.ID operationId;
    private final NflowOperation.State state;
    private final String status;
    private final NflowOperation.NflowType nflowType;



    public OperationStatus(Nflow.ID id, String nflowName, NflowOperation.NflowType nflowType,NflowOperation.ID opId, NflowOperation.State state, String status) {
        this.nflowId = id;
        this.nflowName = nflowName;
        this.operationId = opId;
        this.state = state;
        this.status = status;
        this.nflowType = nflowType != null ? nflowType : NflowOperation.NflowType.NFLOW;
    }



    public Nflow.ID getNflowId() {
        return nflowId;
    }

    public String getNflowName() {
        return nflowName;
    }

    public NflowOperation.State getState() {
        return state;
    }

    public NflowOperation.ID getOperationId() {
        return operationId;
    }

    public String getStatus() {
        return status;
    }

    public NflowOperation.NflowType getNflowType() {
        return nflowType;
    }
}
