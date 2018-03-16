package com.onescorpin.nflowmgr.rest.model;

/*-
 * #%L
 * onescorpin-nflow-manager-rest-model
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.onescorpin.nifi.rest.model.NifiProcessGroup;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class NifiNflow {

    private NflowMetadata nflowMetadata;
    private NifiProcessGroup nflowProcessGroup;
    private boolean success = false;
    private boolean enableAfterSave = false;
    private List<String> errorMessages;


    public NifiNflow() {}

    public NifiNflow(NflowMetadata nflowMetadata, NifiProcessGroup nflowProcessGroup) {
        this.nflowMetadata = nflowMetadata;
        this.nflowProcessGroup = nflowProcessGroup;
    }

    public NflowMetadata getNflowMetadata() {
        return nflowMetadata;
    }

    public void setNflowMetadata(NflowMetadata nflowMetadata) {
        this.nflowMetadata = nflowMetadata;
    }

    public NifiProcessGroup getNflowProcessGroup() {
        return nflowProcessGroup;
    }

    public void setNflowProcessGroup(NifiProcessGroup nflowProcessGroup) {
        this.nflowProcessGroup = nflowProcessGroup;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public boolean isEnableAfterSave() {
        return enableAfterSave;
    }

    public void setEnableAfterSave(boolean enableAfterSave) {
        this.enableAfterSave = enableAfterSave;
    }

    @JsonIgnore
    public void addErrorMessage(Exception e) {
        addErrorMessage(e.getMessage());
    }

    public void addErrorMessage(String msg) {
        if (errorMessages == null) {
            errorMessages = new ArrayList<>();
        }
        errorMessages.add(msg);
    }
}
