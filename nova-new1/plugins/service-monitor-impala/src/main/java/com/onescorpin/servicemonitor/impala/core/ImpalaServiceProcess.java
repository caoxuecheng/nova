package com.onescorpin.servicemonitor.impala.core;

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



import com.onescorpin.servicemonitor.impala.util.MyException;
import com.onescorpin.servicemonitor.impala.util.SocketPortMonitor;
import com.onescorpin.servicemonitor.model.DefaultServiceAlert;
import com.onescorpin.servicemonitor.model.DefaultServiceComponent;
import com.onescorpin.servicemonitor.model.ServiceAlert;
import com.onescorpin.servicemonitor.model.ServiceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;

//数据服务客户端，用来请求数据服务并获得服务状态，服务状态交给check，废弃，不采用sql验证服务状态
public class ImpalaServiceProcess {
    private static Logger log = LoggerFactory.getLogger(ImpalaServiceProcess.class);
    private SocketPortMonitor socketUtil=new SocketPortMonitor();



    //    private ImpalaConnection conn=new ImpalaConnection();
//    private ReadProperties properties=new ReadProperties();
//    // /show database sql查询impala，这个暂时不用，因为仿造CDH监控impala要监控三个服务，不能简单的像nifi发送请求接收不报错就任务状态良好
//    public MyException getState(){
//        MyException exception=properties.getContext();
//        if(exception.getId()==0){                  //读取配置文件没任何错误
//            DataServiceContext context=DataServiceContext.build();
//            Statement statement=conn.getStatement(context);
//            String sql="show databases";
//            try {
//                statement.execute(sql);
//            }catch (Exception e){
//                return MyException.build_impala_service_not_find();
//            }finally {
//                conn.closeConnection();//别忘了关闭连接
//            }
//            return MyException.build();
//        }else {
//            return exception;
//        }
//    }

    //查看端口确定服务是否启动
    private MyException getImpalaThreeServiceState(String host, int port){
        return socketUtil.monitor(host,port);
    }
    //根据查询结果封装component对象
    public void getState(String[] hosts, int port, ArrayList<ServiceComponent> components, String componentName){
        if(hosts!=null && port !=0){
            for(String host:hosts){
                MyException exception=getImpalaThreeServiceState(host,port);
                if(exception.getId()==0){
                    components.add(new DefaultServiceComponent.Builder(componentName, ServiceComponent.STATE.UP).message(host+" is up").build());
                }else{
                    ServiceAlert alert = null;
                    alert = new DefaultServiceAlert();
                    alert.setLabel(componentName);
                    alert.setServiceName("test");
                    alert.setComponentName(componentName);
                    alert.setMessage("alert");
                    alert.setState(ServiceAlert.STATE.CRITICAL);
                    Date first=new Date();
                    long times=first.getTime()+(31536000000l*100);
                    Date later=new Date(times);
                    alert.setFirstTimestamp(later);
//                    alert.setLatestTimestamp(later);

                    components.add(new DefaultServiceComponent.Builder(componentName, ServiceComponent.STATE.DOWN).message(exception.toString()).addAlert(alert).build());
                }
            }
        }
    }
}

