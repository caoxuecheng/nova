package com.onescorpin.jobrepo.query.support;

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

import com.onescorpin.jobrepo.query.model.DefaultNflowHealth;
import com.onescorpin.jobrepo.query.model.ExecutedNflow;
import com.onescorpin.jobrepo.query.model.NflowHealth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to convert ExecutedNflows to NflowHealth objects
 */
public class NflowHealthUtil {


    public static List<NflowHealth> parseToList(List<ExecutedNflow> latestOpNflows, Map<String, Long> avgRunTimes) {
        List<NflowHealth> list = new ArrayList<NflowHealth>();
        Map<String, NflowHealth> map = new HashMap<String, NflowHealth>();

        if (latestOpNflows != null) {
            for (ExecutedNflow nflow : latestOpNflows) {
                String nflowName = nflow.getName();
                NflowHealth nflowHealth = map.get(nflowName);
                if (nflowHealth == null) {
                    nflowHealth = new DefaultNflowHealth();
                    nflowHealth.setNflow(nflowName);
                    if (avgRunTimes != null) {
                        nflowHealth.setAvgRuntime(avgRunTimes.get(nflowName));
                    }
                    list.add(nflowHealth);
                    map.put(nflowName, nflowHealth);
                }
                nflowHealth.setLastOpNflow(nflow);
            }
        }
        return list;

    }
}
