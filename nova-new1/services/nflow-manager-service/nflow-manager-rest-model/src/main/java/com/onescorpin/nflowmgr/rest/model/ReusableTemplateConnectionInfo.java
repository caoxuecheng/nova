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

/**
 */
public class ReusableTemplateConnectionInfo {

    //private String reusableTemplateNflowName;
    private String nflowOutputPortName;
    private String reusableTemplateInputPortName;
    private String inputPortDisplayName;


    public String getNflowOutputPortName() {
        return nflowOutputPortName;
    }

    public void setNflowOutputPortName(String nflowOutputPortName) {
        this.nflowOutputPortName = nflowOutputPortName;
    }

    public String getReusableTemplateInputPortName() {
        return reusableTemplateInputPortName;
    }

    public void setReusableTemplateInputPortName(String reusableTemplateInputPortName) {
        this.reusableTemplateInputPortName = reusableTemplateInputPortName;
    }

    // public String getReusableTemplateNflowName() {
    //   return reusableTemplateNflowName;
    // }

//  public void setReusableTemplateNflowName(String reusableTemplateNflowName) {
    //   this.reusableTemplateNflowName = reusableTemplateNflowName;
    // }

    public String getInputPortDisplayName() {
        return inputPortDisplayName;
    }

    public void setInputPortDisplayName(String inputPortDisplayName) {
        this.inputPortDisplayName = inputPortDisplayName;
    }
}
