package com.onescorpin.servicemonitor.impala.config;
/*-
 * #%L
 * onescorpin-service-monitor-impala
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


import com.onescorpin.servicemonitor.impala.check.ImpalaStatusCheck;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by xuchuang on 2017/12/6.
 * 貌似用用来跟前台沟通的
 */
@Configuration
public class ImpalaStatusCheckConfiguration {
    public ImpalaStatusCheckConfiguration(){}

    @Bean(name="ImpalaServiceStatus")
    public ImpalaStatusCheck dataServiceStatusCheck(){return new ImpalaStatusCheck();}
}
