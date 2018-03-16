package com.onescorpin.servicemonitor.novadataservice.config;

/*-
 * #%L
 * onescorpin-service-monitor-novadatabase
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

import com.onescorpin.servicemonitor.novadataservice.check.DataServiceStatusCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by xuchuang on 2017/12/6.
 * 貌似用用来跟前台沟通的
 */
@Configuration
public class DataServiceStatusCheckConfiguration {
    public DataServiceStatusCheckConfiguration(){}
    private static Logger log = LoggerFactory.getLogger(DataServiceStatusCheckConfiguration.class);
    @Bean(name="NovaDataServiceStatus")
    public DataServiceStatusCheck dataServiceStatusCheck(){
        log.info("DataServiceStatusCheckConfiguration start up");
        return new DataServiceStatusCheck();}
}
