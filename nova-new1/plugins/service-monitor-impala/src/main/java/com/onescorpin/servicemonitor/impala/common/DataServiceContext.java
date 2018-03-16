package com.onescorpin.servicemonitor.impala.common;

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


//从本地配置文件中读取数据服务请求需要的数据封装成对象
public class DataServiceContext {

    private String[] catalogHost=null;
    private String[] daemonHost=null;
    private String[] statestoreHost=null;
    private int catalogPort=0;
    private int daemonPort=0;
    private int statestorePort=0;



    private DataServiceContext(){}
    private static DataServiceContext context=new DataServiceContext();

    public static DataServiceContext build(String[] catalogHost,String[] daemonHost,String[] statestoreHost,int catalogPort,int daemonPort,int statestorePort){
        return context.setCatalogHost(catalogHost)
                .setCatalogPort(catalogPort)
                .setDaemonHost(daemonHost)
                .setDaemonPort(daemonPort)
                .setStatestoreHost(statestoreHost)
                .setStatestorePort(statestorePort);

    }
    public static DataServiceContext build(){
        return context;
    }


    public String[] getCatalogHost() {
        return catalogHost;
    }

    public DataServiceContext setCatalogHost(String[] catalogHost) {
        this.catalogHost = catalogHost;
        return this;
    }

    public String[] getDaemonHost() {
        return daemonHost;
    }

    public DataServiceContext setDaemonHost(String[] daemonHost) {
        this.daemonHost = daemonHost;
        return this;
    }

    public String[] getStatestoreHost() {
        return statestoreHost;
    }

    public DataServiceContext setStatestoreHost(String[] statestoreHost) {
        this.statestoreHost = statestoreHost;
        return this;
    }

    public int getCatalogPort() {
        return catalogPort;
    }

    public DataServiceContext setCatalogPort(int catalogPort) {
        this.catalogPort = catalogPort;
        return this;
    }

    public int getDaemonPort() {
        return daemonPort;
    }

    public DataServiceContext setDaemonPort(int daemonPort) {
        this.daemonPort = daemonPort;
        return this;
    }

    public int getStatestorePort() {
        return statestorePort;
    }

    public DataServiceContext setStatestorePort(int statestorePort) {
        this.statestorePort = statestorePort;
        return this;
    }
}
