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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.concurrent.TimeUnit;

/**
 * Nflow summary data built via the model transform class.
 *
 * @see com.onescorpin.jobrepo.query.model.transform.NflowModelTransform
 */
public class DefaultNflowSummary implements NflowSummary {

    private NflowHealth nflowHealth;


    public DefaultNflowSummary(NflowHealth nflowHealth) {
        this.nflowHealth = nflowHealth;
    }

    public DefaultNflowSummary() {

    }


    @Override
    public String getNflow() {
        return nflowHealth.getNflow();
    }

    @Override
    public String getState() {

        String state = nflowHealth.getLastOpNflowState();

        return state;
    }


    @Override
    public String getLastStatus() {
        if (nflowHealth.getLastOpNflow() != null && isWaiting()) {
            return nflowHealth.getLastOpNflow().getStatus().name();
        } else {
            return "N/A";
        }
    }

    @Override
    public boolean isWaiting() {
        return DefaultNflowHealth.STATE.WAITING.equals(DefaultNflowHealth.STATE.valueOf(getState()));
    }

    @Override
    public boolean isRunning() {
        return DefaultNflowHealth.STATE.RUNNING.equals(DefaultNflowHealth.STATE.valueOf(getState()));
    }

    @Override
    public Long getTimeSinceEndTime() {
        if (nflowHealth.getLastOpNflow() != null) {
            return nflowHealth.getLastOpNflow().getTimeSinceEndTime();
        } else {
            return null;
        }
    }

    @Override
    @JsonIgnore
    public String formatTimeMinSec(Long millis) {
        if (millis == null) {
            return null;
        }

        Long hours = TimeUnit.MILLISECONDS.toHours(millis);
        Long min = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        Long sec = TimeUnit.MILLISECONDS.toSeconds(millis) -
                   TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        String str = String.format("%d hr %d min %d sec",
                                   hours, min, sec);
        if (hours == 0L) {

            if (min == 0L) {
                str = String.format("%d sec",
                                    sec);
            } else {
                str = String.format("%d min %d sec",
                                    min, sec);
            }

        }

        return str;
    }

    @Override
    public String getTimeSinceEndTimeString() {
        return formatTimeMinSec(getTimeSinceEndTime());
    }

    @Override
    public Long getRunTime() {
        if (nflowHealth.getLastOpNflow() != null) {
            return nflowHealth.getLastOpNflow().getRunTime();
        } else {
            return null;
        }
    }

    @Override
    public String getRunTimeString() {
        return formatTimeMinSec(getRunTime());
    }

    @Override
    public Long getAvgCompleteTime() {
        return nflowHealth.getAvgRuntime();
    }

    @Override
    public String getAvgCompleteTimeString() {
        Long avgRunTime = nflowHealth.getAvgRuntime();
        if (avgRunTime != null) {
            avgRunTime *= 1000;  //convert to millis
        }

        return formatTimeMinSec(avgRunTime);
    }

    @Override
    public boolean isHealthy() {
        return nflowHealth.isHealthy();
    }


    @Override
    public String getLastExitCode() {
        if (nflowHealth.getLastOpNflow() != null) {
            return nflowHealth.getLastOpNflow().getExitCode();
        } else {
            return null;
        }
    }

    @Override
    public NflowHealth getNflowHealth() {
        return nflowHealth;
    }

    @Override
    public boolean isStream() {
        return nflowHealth.isStream();
    }


}
