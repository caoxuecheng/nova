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

import java.io.Serializable;

/**
 * Created by sr186054 on 10/9/17.
 */
public class NflowOperationBatchStatusChange implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String CLUSTER_MESSAGE_TYPE = "NflowOperationBatchStatusChange";

    private  Nflow.ID nflowId;
    private  String nflowName;
    private Long jobExecutionId;

    public static enum BatchType {
        BATCH,STREAM
    }


    private  BatchType batchType;

    public NflowOperationBatchStatusChange() {

    }
    public NflowOperationBatchStatusChange(Nflow.ID nflowId, String nflowName, Long jobExecutionId,BatchType batchType) {
        this.nflowId = nflowId;
        this.nflowName = nflowName;
        this.jobExecutionId = jobExecutionId;
        this.batchType = batchType;
    }

    public Nflow.ID getNflowId() {
        return nflowId;
    }

    public String getNflowName() {
        return nflowName;
    }

    public BatchType getBatchType() {
        return batchType;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }
}
