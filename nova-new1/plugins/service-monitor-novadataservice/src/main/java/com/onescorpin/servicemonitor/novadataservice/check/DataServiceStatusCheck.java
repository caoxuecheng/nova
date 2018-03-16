package com.onescorpin.servicemonitor.novadataservice.check;
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


import com.onescorpin.servicemonitor.check.ServiceStatusCheck;
import com.onescorpin.servicemonitor.model.DefaultServiceStatusResponse;
import com.onescorpin.servicemonitor.model.ServiceComponent;
import com.onescorpin.servicemonitor.model.ServiceStatusResponse;
import com.onescorpin.servicemonitor.novadataservice.client.DataServiceRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by xuchuang on 2017/12/6.
 * 数据服务状态检查逻辑处理和封装类
 */
public class DataServiceStatusCheck implements ServiceStatusCheck {
    private static Logger log = LoggerFactory.getLogger(DataServiceStatusCheck.class);
    private DataServiceRestClient dataServiceRestClient;
    @Override
    //当发起状态检查时调用的方法，里面包含所有状态获得和封装的处理方法
    public ServiceStatusResponse healthCheck() {
        String serviceName="NovaDataService";
        return new DefaultServiceStatusResponse(serviceName, dataStatus());
    }

    private ArrayList<ServiceComponent> dataStatus(){
        dataServiceRestClient=new DataServiceRestClient();
        return dataServiceRestClient.getState();
    }
}
