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


//从本地配置文件中读取数据服务请求需要的数据封装成对象
public class DataServiceContext {

    private String dataServiceUrl; //数据服务url
    private String dataServiceUser;//数据服务访问用户名
    private String dataServicePW;//数据服务访问的密码
    private String service_request_separator;

    private String mysqlUrl;
    private String mysqlUser;
    private String mysqlPW;
    private String mysqlPort;
    private String mysqlDatabase;

    private String serviceTable; //测试表名
    private String serviceTestTable; //接口表名

    private String interface_keyword_field;//接口表中的的一个字段名，用来与测试表关联，当前情况下此字段名为接口访问数据库的表名
    private String interface_table_query_param_field;//接口表请求参数字段名
    private String interface_separator;//请求参数字段的值是用什么分隔符
    private String interface_url_field;//接口表中记录接口名的字段名

    private String test_keyword_field;//测试表中的一个字段名，用来与接口表关联，当前情况下此字段名为数据库表名
    private String test_table_default_query_value_field;//测试表中记录默认请求参数的值的字段名
    private String test_table_default_query_key_field;//测试表中记录默认请求参数的字段名（即，这个表的查询条件）
    private String test_separator=";";//测试表的请求字段和默认字段的值的分隔符






    private DataServiceContext(){}
    private static DataServiceContext context=new DataServiceContext();

    public static DataServiceContext build(String dataServiceUrl,
                                           String dataServiceUser,
                                           String dataServicePW,
                                           String mysqlUrl,
                                           String mysqlUser,
                                           String mysqlPW,
                                           String serviceTable,
                                           String serviceTestTable,
                                           String mysqlPort,
                                           String mysqlDatabase,
                                           String interface_keyword_field,
                                           String interface_table_query_param_field,
                                           String interface_separator,
                                           String interface_url_field,
                                           String test_keyword_field,
                                           String test_table_default_query_value_field,
                                           String test_table_default_query_key_field,
                                           String test_separator){
        return context
                .setDataServiceUser(dataServiceUser)
                .setDataServiceUrl(dataServiceUrl)
                .setDataServicePW(dataServicePW)
                .setMysqlPW(mysqlPW)
                .setMysqlUrl(mysqlUrl)
                .setMysqlUser(mysqlUser)
                .setServiceTable(serviceTable)
                .setServiceTestTable(serviceTestTable)
                .setMysqlPort(mysqlPort)
                .setMysqlDatabase(mysqlDatabase)
                .setInterface_keyword_field(interface_keyword_field)
                .setInterface_table_query_param_field(interface_table_query_param_field)
                .setInterface_separator(interface_separator)
                .setInterface_url_field(interface_url_field)
                .setTest_keyword_field(test_keyword_field)
                .setTest_table_default_query_value_field(test_table_default_query_value_field)
                .setTest_table_default_query_key_field(test_table_default_query_key_field)
                .setTest_separator(test_separator);
    }
    public static DataServiceContext build(){
        return context;
    }

    public String getDataServiceUrl() {
        return dataServiceUrl;
    }


    public DataServiceContext setDataServiceUrl(String dataServiceUrl) {
        this.dataServiceUrl = dataServiceUrl;
        return this;
    }

    public String getMysqlPW() {
        return mysqlPW;
    }

    public DataServiceContext setMysqlPW(String mysqlPW) {
        this.mysqlPW = mysqlPW;
        return this;
    }

    public String getMysqlUser() {
        return mysqlUser;
    }

    public DataServiceContext setMysqlUser(String mysqlUser) {
        this.mysqlUser = mysqlUser;
        return this;
    }

    public String getMysqlUrl() {
        return mysqlUrl;
    }

    public DataServiceContext setMysqlUrl(String mysqlUrl) {
        this.mysqlUrl = mysqlUrl;
        return this;
    }

    public String getDataServicePW() {
        return dataServicePW;
    }

    public DataServiceContext setDataServicePW(String dataServicePW) {
        this.dataServicePW = dataServicePW;
        return this;
    }

    public String getDataServiceUser() {
        return dataServiceUser;
    }

    public DataServiceContext setDataServiceUser(String dataServiceUser) {
        this.dataServiceUser = dataServiceUser;
        return this;
    }

    public String getServiceTable() {
        return serviceTable;
    }

    public DataServiceContext setServiceTable(String serviceTable) {
        this.serviceTable = serviceTable;
        return this;
    }

    public String getServiceTestTable() {
        return serviceTestTable;
    }

    public DataServiceContext setServiceTestTable(String serviceTestTable) {
        this.serviceTestTable = serviceTestTable;
        return this;
    }

    public String getMysqlPort() {
        return mysqlPort;
    }

    public DataServiceContext setMysqlPort(String mysqlPort) {
        this.mysqlPort = mysqlPort;
        return this;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public DataServiceContext setMysqlDatabase(String mysqlDatabase) {
        this.mysqlDatabase = mysqlDatabase;
        return this;
    }

    public String getInterface_keyword_field() {
        return interface_keyword_field;
    }

    public DataServiceContext setInterface_keyword_field(String interface_keyword_field) {
        this.interface_keyword_field = interface_keyword_field;
        return this;
    }

    public String getInterface_table_query_param_field() {
        return interface_table_query_param_field;
    }

    public DataServiceContext setInterface_table_query_param_field(String interface_table_query_param_field) {
        this.interface_table_query_param_field = interface_table_query_param_field;
        return this;
    }

    public String getInterface_separator() {
        return interface_separator;
    }

    public DataServiceContext setInterface_separator(String interface_separator) {
        this.interface_separator = interface_separator;
        return this;
    }

    public String getInterface_url_field() {
        return interface_url_field;
    }

    public DataServiceContext setInterface_url_field(String interface_url_field) {
        this.interface_url_field = interface_url_field;
        return this;
    }

    public String getTest_keyword_field() {
        return test_keyword_field;
    }

    public DataServiceContext setTest_keyword_field(String test_keyword_field) {
        this.test_keyword_field = test_keyword_field;
        return this;
    }

    public String getTest_table_default_query_value_field() {
        return test_table_default_query_value_field;
    }

    public DataServiceContext setTest_table_default_query_value_field(String test_table_default_query_value_field) {
        this.test_table_default_query_value_field = test_table_default_query_value_field;
        return this;
    }

    public String getTest_table_default_query_key_field() {
        return test_table_default_query_key_field;
    }

    public DataServiceContext setTest_table_default_query_key_field(String test_table_default_query_key_field) {
        this.test_table_default_query_key_field = test_table_default_query_key_field;
        return this;
    }

    public String getTest_separator() {
        return test_separator;
    }

    public DataServiceContext setTest_separator(String test_separator) {
        this.test_separator = test_separator;
        return this;
    }

    public String getService_request_separator() {
        return service_request_separator;
    }

    public DataServiceContext setService_request_separator(String service_request_separator) {
        this.service_request_separator = service_request_separator;
        return this;
    }


}
