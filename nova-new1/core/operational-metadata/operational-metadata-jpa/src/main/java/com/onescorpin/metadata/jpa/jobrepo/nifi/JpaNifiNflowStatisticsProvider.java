package com.onescorpin.metadata.jpa.jobrepo.nifi;

/*-
 * #%L
 * onescorpin-operational-metadata-jpa
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

import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStatisticsProvider;
import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowStats;
import com.onescorpin.security.AccessController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import javax.inject.Inject;

/**
 * Created by sr186054 on 7/25/17.
 */
@Service
public class JpaNifiNflowStatisticsProvider implements NifiNflowStatisticsProvider {

    private NifiNflowStatisticsRepository nflowStatisticsRepository;

    @Inject
    private AccessController accessController;

    @Autowired
    public JpaNifiNflowStatisticsProvider(NifiNflowStatisticsRepository nflowStatisticsRepository) {
        this.nflowStatisticsRepository = nflowStatisticsRepository;
    }

    /**
     * Marks these are the latest nflow stats
     *
     * @param nflowStatsList the stats to save
     */
    @Override
    public void saveLatestNflowStats(List<NifiNflowStats> nflowStatsList) {
        if (nflowStatsList != null) {
            nflowStatsList.stream().forEach(nifiNflowStats -> {
                nflowStatisticsRepository.save(((JpaNifiNflowStats) nifiNflowStats));
            });
        }
    }

    public void deleteNflowStats(String nflowName){
        List<JpaNifiNflowStats> stats = nflowStatisticsRepository.findForNflowWithoutAcl(nflowName);
        if(stats != null && !stats.isEmpty()){
            nflowStatisticsRepository.delete(stats);
        }
    }

    @Override
    public NifiNflowStats findLatestStatsForNflow(String nflowName) {
        return accessController.isEntityAccessControlled() ? nflowStatisticsRepository.findLatestForNflowWithAcl(nflowName) : nflowStatisticsRepository.findLatestForNflowWithoutAcl(nflowName);
    }

    public NifiNflowStats findLatestStatsForNflowWithoutAccessControl(String nflowName) {
        return nflowStatisticsRepository.findLatestForNflowWithoutAcl(nflowName);
    }

    public List<? extends NifiNflowStats> findNflowStats(boolean streamingOnly) {
        if (streamingOnly) {
            return accessController.isEntityAccessControlled() ? nflowStatisticsRepository.findStreamingNflowStatsWithAcl() : nflowStatisticsRepository.findStreamingNflowStatsWithoutAcl();
        } else {
            return accessController.isEntityAccessControlled() ? nflowStatisticsRepository.findNflowStatsWithAcl() : nflowStatisticsRepository.findNflowStatsWithoutAcl();
        }
    }
}
