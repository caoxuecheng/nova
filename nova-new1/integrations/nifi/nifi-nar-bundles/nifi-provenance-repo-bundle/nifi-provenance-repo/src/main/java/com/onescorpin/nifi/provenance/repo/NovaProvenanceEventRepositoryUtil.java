package com.onescorpin.nifi.provenance.repo;
/*-
 * #%L
 * onescorpin-nifi-provenance-repo
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
import com.onescorpin.nifi.provenance.util.SpringApplicationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

/**
 * Created by sr186054 on 6/21/17.
 */
public class NovaProvenanceEventRepositoryUtil {

    private static final Logger log = LoggerFactory.getLogger(NovaProvenanceEventRepositoryUtil.class);


    public final void persistNflowEventStatisticsToDisk() {
        log.info("onShutdown: Attempting to persist any active flow files to disk");
        try {
            //persist running flowfile metadata to disk
            boolean success = NflowEventStatistics.getInstance().backup();
            if (success) {
                log.info("onShutdown: Successfully Finished persisting Nova Flow processing data to {}", new Object[]{NflowEventStatistics.getInstance().getBackupLocation()});
            } else {
                log.info("onShutdown: FAILED Finished persisting Nova Flow processing data.");
            }
        } catch (Exception e) {
            //ok to swallow exception here.  this is called when NiFi is shutting down
        }
    }

    public void initializeNflowEventStatistics() {
        String backupLocation = ConfigurationProperties.getInstance().getNflowEventStatisticsBackupLocation();
        if (backupLocation != null) {
            NflowEventStatistics.getInstance().setBackupLocation(backupLocation);
        }
        boolean success = NflowEventStatistics.getInstance().loadBackup();
        if (success) {
            log.info("Successfully loaded backup from {} ", NflowEventStatistics.getInstance().getBackupLocation());
        } else {
            log.error("Error loading backup");
        }
    }

    public void init() {
        loadSpring();
        initializeNflowEventStatistics();

    }

    void loadSpring() {
        try {
            SpringApplicationContext.getInstance().initializeSpring("classpath:provenance-application-context.xml");
        } catch (BeansException | IllegalStateException e) {
            log.error("Failed to load spring configurations", e);
        }
    }


}
