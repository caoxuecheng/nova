package com.onescorpin.servicemonitor.impala.util;

import java.net.Socket;

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
//通过socket查看指定主机的端口是否启动
public class SocketPortMonitor {

    public MyException monitor(String host,int port){
        Socket theTcpSocket=null;
        try {
            theTcpSocket = new Socket(host, port);
            return MyException.build();
        }catch (Exception e){
            return MyException.build_impala_host_port_not_connect(host,port);
        }finally {
            try {
                theTcpSocket.close();
            }catch (Exception e){
                //没连通也没法关，这会有异常
            }
        }
    }


}
