package com.onescorpin.servicemonitor.novadataservice.util;

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
public class MyException {

    private int id;

    private String message="";

    private String description="";

    public int getId() {
        return id;
    }

    public MyException setId(int id) {
        this.id = id;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public MyException setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public MyException setMessage(String message) {
        this.message = message;
        return this;
    }

    //定义几个模式，错误类型是有限个的

    //无法获取配置文件数据流
    public static MyException build_properties_not_find(String propertiesPath){return new MyException().setId(1).setMessage("配置文件读取失败").setDescription("当前获取的配置文件位置为:"+propertiesPath);}

    //配置文件中必须字段为空
    public static MyException build_properties_not_field(String fieldName){return new MyException().setId(1).setMessage("配置文件中必填字段为空").setDescription("为空字段:"+fieldName);}
    //mysql 连不上，测试表，接口表都访问不了
    public static MyException build_mysql_not_connect(String mysqlHost,String mysqlDatabase){return new MyException().setId(2).setMessage("mysql连接失败").setDescription("mysqlIP:"+mysqlHost+";mysqlDatabase："+mysqlDatabase);}

    //配置文件中指定的mysql表中的字段有问题，sql查询错误
    public static MyException build_mysql_query_error(){return new MyException().setId(2).setMessage("接口表与接口默认请求字段表查询失败").setDescription("请检查配置文件中的mysql字段");}

    //在mysql返回的数据中提取指定字段返回
    public static MyException build_mysql_extract_error(){return new MyException().setId(2).setMessage("提取mysql数据封装对象时发生错误").setDescription("请检查配置文件的字段名");}

    //数据服务访问失败
    public static MyException build_service_connect_error(){return new MyException().setId(2).setMessage("访问数据服务异常，无法连接").setDescription("请检查配置文件中数据服务访问的相关信息");}

    //数据服务返回的结果json解析失败，
    public static MyException build_json_parse_error(){return new MyException().setId(2).setMessage("数据服务返回结果json解析失败").setDescription("可能是数据服务返回结果封装存在bug，也可能是返回结果定义的格式与解析方式不符");}

   //数据库中，接口测试表的请求字段和请求字段默认值映射关系不匹配
    public static MyException build_test_request_field_default_field_error(){return new MyException().setId(2).setMessage("接口测试表的请求字段和请求字段默认值映射关系不匹配").setDescription("请检查接口测试表的请求字段内容和默认请求值内容是否对应");}

    public static MyException build_request_interface_param_and_defualt_value_num_error(){return new MyException().setId(2).setMessage("处理后接口请求参数和接口请求参数默认值个数不匹配").setDescription("接口测试表中的表的请求字段并没有包含接口请求字段，导致请求字段值为空");}

    public static MyException build_interface_param_and_defualt_value_not_found_error(String interface_name){return new MyException().setId(2).setMessage(interface_name+"没有找到接口默认请求信息").setDescription("接口对应的测试表字段信息为空");}
    public String toString(){return this.getMessage()+";"+getDescription();}

    public static MyException build(){return new MyException().setId(0).setMessage("dataService is up");}

}
