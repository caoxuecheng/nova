package com.onescorpin.servicemonitor.impala.util;

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
public class MyException {

    private int id;

    private String message;

    private String description;

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
    public String toString(){
        return this.getMessage()+";"+this.getDescription();
    }

    //定义几个模式，错误类型是有限个的

    //无法获取配置文件数据流
    public static MyException build_properties_not_find(String propertiesPath){
        return new MyException().setId(1).setMessage("配置文件读取失败").setDescription("当前获取的配置文件位置为:"+propertiesPath);
    }

    //配置文件中必须字段为空
    public static MyException build_properties_not_field(String fieldName){
        return new MyException().setId(1).setMessage("配置文件中必填字段为空").setDescription("为空字段:"+fieldName);
    }

    public static MyException build(){
        return new MyException().setId(0);
    }

    public static MyException build_impala_service_not_find(){
        return new MyException().setId(2).setMessage("impala sql执行失败").setDescription("impala服务异常");
    }

    public static MyException build_impala_properties_deserialization_error(){
        return new MyException().setId(1).setMessage("配置文件信息反序列化失败").setDescription("配置文件填写的有问题，如port指定了非数字");
    }

    public static MyException build_impala_host_port_not_connect(String host,int port){
        return new MyException().setId(2).setMessage("impala服务主机:"+host+",端口:"+port).setDescription("服务关闭状态");
    }


}
