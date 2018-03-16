package com.onescorpin.metadata.rest.jobrepo.nifi;

/*-
 * #%L
 * onescorpin-operational-metadata-rest-model
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


import com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorErrors;

import org.apache.commons.beanutils.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NifiNflowProcessorStatsTransform {

    /**
     * Converts the domain model objects to the rest model equivalent
     *
     * @param domains A list of domain objects
     * @return a list of converted objects, or null if the provided list was empty
     */
    public static List<NifiNflowProcessorStats> toModel(List<? extends com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats> domains) {
        if (domains != null && !domains.isEmpty()) {
           return domains.stream().map(domain -> toModel(domain)).collect(Collectors.toList());
        }
        return new ArrayList<>(0);
    }

    /**
     * Converts a domain model object to the rest model equivalent
     *
     * @param domain The domain object
     * @return the rest model object
     */
    public static NifiNflowProcessorStats toModel(com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorStats domain) {
        NifiNflowProcessorStats stats = new NifiNflowProcessorStats();
        try {
            BeanUtils.copyProperties(stats, domain);
        } catch (Exception e) {
            //TODO LOG IT
        }
        return stats;
    }




    /**
     * Converts the domain model objects to the rest model equivalent
     *
     * @param domains A list of domain objects
     * @return a list of converted objects, or null if the provided list was empty
     */
    public static List<NifiNflowProcessorStatsErrors> toErrorsModel(List<? extends com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorErrors> domains) {
        if (domains != null && !domains.isEmpty()) {
            return domains.stream().map(domain -> toErrorsModel(domain)).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * Converts a domain model object to the rest model equivalent
     *
     * @param domain The domain object
     * @return the rest model object
     */
    public static NifiNflowProcessorStatsErrors toErrorsModel(com.onescorpin.metadata.api.jobrepo.nifi.NifiNflowProcessorErrors domain) {
        NifiNflowProcessorStatsErrors errors = new NifiNflowProcessorStatsErrors();
        try {
            BeanUtils.copyProperties(errors, domain);
        } catch (Exception e) {
            //TODO LOG IT
        }
        return errors;
    }







}
