package com.onescorpin.jobrepo.query.model;

/*-
 * #%L
 * onescorpin-job-repository-core
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Nflow Status built from the transform class
 *
 * @see com.onescorpin.jobrepo.query.model.transform.NflowModelTransform
 */
public class DefaultNflowStatus implements NflowStatus {

    private List<NflowHealth> nflows;
    private Integer healthyCount = 0;
    private Integer failedCount = 0;
    private float percent;
    private List<NflowHealth> healthyNflows;
    private List<NflowHealth> failedNflows;

    private List<NflowSummary> nflowSummary;

    public DefaultNflowStatus(List<NflowHealth> nflows) {
        this.nflows = nflows;
        this.populate();

    }
    public void populate(List<NflowSummary> nflows) {
        if(nflows != null && !nflows.isEmpty()) {
            this.nflows = nflows.stream().map(f -> f.getNflowHealth()).collect(Collectors.toList());
        }
        populate();

    }


    public DefaultNflowStatus() {

    }

    @Override
    public void populate() {
        this.healthyNflows = new ArrayList<NflowHealth>();
        this.failedNflows = new ArrayList<NflowHealth>();
        this.nflowSummary = new ArrayList<>();
        healthyCount = 0;
        failedCount = 0;
        percent = 0f;
        if (nflows != null && !nflows.isEmpty()) {
            for (NflowHealth nflowHealth : nflows) {
                if (nflowHealth.isHealthy()) {
                    healthyCount++;
                    healthyNflows.add(nflowHealth);
                } else {
                    failedCount++;
                    failedNflows.add(nflowHealth);
                }
                this.nflowSummary.add(new DefaultNflowSummary(nflowHealth));
            }
            percent = (float) healthyCount / nflows.size();
        }
        if (percent > 0f) {
            DecimalFormat twoDForm = new DecimalFormat("##.##");
            this.percent = Float.valueOf(twoDForm.format(this.percent)) * 100;
        }

    }

    @Override
    public List<NflowHealth> getNflows() {
        return nflows;
    }

    @Override
    public void setNflows(List<NflowHealth> nflows) {
        this.nflows = nflows;
    }

    @Override
    public Integer getHealthyCount() {
        return healthyCount;
    }

    @Override
    public void setHealthyCount(Integer healthyCount) {
        this.healthyCount = healthyCount;
    }

    @Override
    public Integer getFailedCount() {
        return failedCount;
    }

    @Override
    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    @Override
    public Float getPercent() {
        return percent;
    }

    @Override
    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    @Override
    public List<NflowHealth> getHealthyNflows() {
        return healthyNflows;
    }

    @Override
    public void setHealthyNflows(List<NflowHealth> healthyNflows) {
        this.healthyNflows = healthyNflows;
    }

    @Override
    public List<NflowHealth> getFailedNflows() {
        return failedNflows;
    }

    @Override
    public void setFailedNflows(List<NflowHealth> failedNflows) {
        this.failedNflows = failedNflows;
    }

    @Override
    public List<NflowSummary> getNflowSummary() {
        return nflowSummary;
    }

    @Override
    public void setNflowSummary(List<NflowSummary> nflowSummary) {
        this.nflowSummary = nflowSummary;
    }
}
