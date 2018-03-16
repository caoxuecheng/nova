package com.onescorpin.servicemonitor.impala.check;
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


import com.onescorpin.servicemonitor.check.ServiceStatusCheck;
import com.onescorpin.servicemonitor.impala.common.DataServiceContext;
import com.onescorpin.servicemonitor.impala.core.ImpalaServiceProcess;
import com.onescorpin.servicemonitor.impala.util.MyException;
import com.onescorpin.servicemonitor.impala.util.ReadProperties;
import com.onescorpin.servicemonitor.model.DefaultServiceComponent;
import com.onescorpin.servicemonitor.model.DefaultServiceStatusResponse;
import com.onescorpin.servicemonitor.model.ServiceComponent;
import com.onescorpin.servicemonitor.model.ServiceStatusResponse;

import java.util.ArrayList;

/**
 * Created by xuchuang on 2017/12/6.
 * 数据服务状态检查逻辑处理和封装类
 */
public class ImpalaStatusCheck implements ServiceStatusCheck {
    private ImpalaServiceProcess process=new ImpalaServiceProcess();
    private ReadProperties properties=new ReadProperties();
    @Override
    //当发起状态检查时调用的方法，里面包含所有状态获得和封装的处理方法
    public ServiceStatusResponse healthCheck() {
        String serviceName="Impala";

        return new DefaultServiceStatusResponse(serviceName, dataStatus());
    }

    //impala一共三个服务impala catalog server ,impal daemon ,impala statestore，定义三个component
    private ArrayList<ServiceComponent> dataStatus(){
        String[] componentNames={"Impala_Catalog_Server","Impala_Daemon","Impala_StateStore"};
        ArrayList<ServiceComponent> components=new ArrayList<>();
        MyException readPropertiesExc=properties.getContext();
        if(readPropertiesExc.getId()==0){
            DataServiceContext context=DataServiceContext.build(); //读取配置文件没问题就获取上下文对象
            process.getState(context.getCatalogHost(),context.getCatalogPort(),components,componentNames[0]);
            process.getState(context.getStatestoreHost(),context.getStatestorePort(),components,componentNames[2]);
            process.getState(context.getDaemonHost(),context.getDaemonPort(),components,componentNames[1]);
            if(components.size()==0){
               for(String temp:componentNames){
                   components.add(new DefaultServiceComponent.Builder(temp, ServiceComponent.STATE.UNKNOWN).message("配置文件没指定任何信息").build());
               }
            }
        }else{
            for(String temp:componentNames){
                components.add(new DefaultServiceComponent.Builder(temp, ServiceComponent.STATE.DOWN).message(readPropertiesExc.toString()).build());
            }
        }

        return components;
    }
}
