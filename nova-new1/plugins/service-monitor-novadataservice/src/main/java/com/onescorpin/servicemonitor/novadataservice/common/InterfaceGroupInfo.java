package com.onescorpin.servicemonitor.novadataservice.common;
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


//通过sql查询接口所有信息封装成对象，包括接口名，接口请求参数，接口数据源请求参数，接口数据源默认请求参数
public class InterfaceGroupInfo {
    private String interface_name;

    private String interface_request;

    private String table_request;

    private String table_default_request;

    private String interface_request_default;


    public String getInterface_name() {
        return interface_name;
    }

    public void setInterface_name(String interface_name) {
        this.interface_name = interface_name;
    }

    public String getInterface_request() {
        return interface_request;
    }

    public void setInterface_request(String interface_request) {
        this.interface_request = interface_request;
    }

    public String getTable_request() {
        return table_request;
    }

    public void setTable_request(String table_request) {
        this.table_request = table_request;
    }

    public String getTable_default_request() {
        return table_default_request;
    }

    public void setTable_default_request(String table_default_request) {this.table_default_request = table_default_request;}


    @Override
    public String toString() {
        return "InterfaceGroupInfo{" +
                "interface_name='" + interface_name + '\'' +
                ", interface_request='" + interface_request + '\'' +
                ", table_request='" + table_request + '\'' +
                ", table_default_request='" + table_default_request + '\'' +
                '}';
    }

    public InterfaceGroupInfo(String interface_name, String interface_request, String table_request, String table_default_request) {
        this.interface_name = interface_name;
        this.interface_request = interface_request;
        this.table_request = table_request;
        this.table_default_request = table_default_request;
    }

    public String getInterface_request_default() {
        return interface_request_default;
    }

    public void setInterface_request_default(String interface_request_default) {
        this.interface_request_default = interface_request_default;
    }
}
