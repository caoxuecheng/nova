package com.onescorpin.metadata.jpa.app;

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

import com.onescorpin.NovaVersionUtil;
import com.onescorpin.NovaVersion;
import com.onescorpin.metadata.api.app.NovaVersionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

/**
 * Provider for accessing and updating the Nova version
 */
public class JpaNovaVersionProvider implements NovaVersionProvider {

    private static final Logger log = LoggerFactory.getLogger(JpaNovaVersionProvider.class);
    
    private static final Sort SORT_ORDER = new Sort(new Sort.Order(Direction.DESC, "majorVersion"),
                                                    new Sort.Order(Direction.DESC, "minorVersion"),
                                                    new Sort.Order(Direction.DESC, "pointVersion"),
                                                    new Sort.Order(Direction.DESC, "tag") );


    private NovaVersionRepository novaVersionRepository;

    private String currentVersion;

    private String buildTimestamp;


    @Autowired
    public JpaNovaVersionProvider(NovaVersionRepository novaVersionRepository) {
        this.novaVersionRepository = novaVersionRepository;
    }

    @Override
    public boolean isUpToDate() {
        NovaVersion buildVer = NovaVersionUtil.getBuildVersion();
        NovaVersion currentVer = getCurrentVersion();
        return currentVer != null && buildVer.matches(currentVer.getMajorVersion(),
                                                        currentVer.getMinorVersion(),
                                                        currentVer.getPointVersion());
    }

    @Override
    public NovaVersion getCurrentVersion() {
        List<JpaNovaVersion> versions = novaVersionRepository.findAll(SORT_ORDER);
        if (versions != null && !versions.isEmpty()) {
            return versions.get(0);
        }
        return null;
    }
    
    /* (non-Javadoc)
     * @see com.onescorpin.metadata.api.app.NovaVersionProvider#setCurrentVersion(com.onescorpin.NovaVersion)
     */
    @Override
    public void setCurrentVersion(NovaVersion version) {
        JpaNovaVersion update = new JpaNovaVersion(version.getMajorVersion(), 
                                                   version.getMinorVersion(), 
                                                   version.getPointVersion(),
                                                   version.getTag());
        novaVersionRepository.save(update);
    }
    
    @Override
    public NovaVersion getBuildVersion() {
        return NovaVersionUtil.getBuildVersion();
    }




    @PostConstruct
    private void init() {
        getBuildVersion();
    }



}
