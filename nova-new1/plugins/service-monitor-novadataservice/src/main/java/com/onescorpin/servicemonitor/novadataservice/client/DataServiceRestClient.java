package com.onescorpin.servicemonitor.novadataservice.client;

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



import com.onescorpin.servicemonitor.model.DefaultServiceComponent;
import com.onescorpin.servicemonitor.model.ServiceComponent;
import com.onescorpin.servicemonitor.novadataservice.common.DataServiceContext;
import com.onescorpin.servicemonitor.novadataservice.common.InterfaceGroupInfo;
import com.onescorpin.servicemonitor.novadataservice.connect.DataBasesConnection;
import com.onescorpin.servicemonitor.novadataservice.util.GeneralAPIClient;
import com.onescorpin.servicemonitor.novadataservice.util.MyException;
import com.onescorpin.servicemonitor.novadataservice.util.ReadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

//数据服务客户端，用来请求数据服务并获得服务状态，服务状态交给check
public class DataServiceRestClient {
    private ReadProperties readProperties=new ReadProperties();
    private static Logger log = LoggerFactory.getLogger(DataServiceRestClient.class);


    public ArrayList<ServiceComponent> getState(){
        log.info("DataServiceRestClient.getState start up ");
        ReadProperties readProperties=new ReadProperties();
        MyException exception=readProperties.getContext();
        if(exception.getId()!=0){
            ArrayList<ServiceComponent> list=new ArrayList<>();
            list.add(new DefaultServiceComponent.Builder("Exception", ServiceComponent.STATE.DOWN).message(exception.toString()).build());
            log.error(exception.toString());
            return list;
        }else{
            DataServiceContext context=DataServiceContext.build();
            return run(context);

        }
    }
    //接口表中的表名进行关联（关联接口测试表）
    private ArrayList<ServiceComponent> run(DataServiceContext context){
        log.info("DataServiceRestClient.run start up");
        ArrayList<InterfaceGroupInfo> interfaceObject=new ArrayList<>();
        DataBasesConnection connection=new DataBasesConnection();
        Connection conn=connection.getConnection(context);
        String interface_table=context.getServiceTable(); //接口表名
        String inteface_keyword_field=context.getInterface_keyword_field();//接口表中接口访问的表
        String interface_table_query_param_field=context.getInterface_table_query_param_field();//接口表请求参数的字段名
        String interface_separator=context.getInterface_separator();//接口表的请求参数字段的分隔符
        String interface_url_field=context.getInterface_url_field();//接口表中的接口名

        String test_keyword_field=context.getTest_keyword_field();//测试表中的表名
        String test_table=context.getServiceTestTable();//接口测试表名
        String test_table_default_query_value_field=context.getTest_table_default_query_value_field();//测试表默认请求参数的值的字段名
        String test_table_default_query_key_field=context.getTest_table_default_query_key_field();//测试表请求参数的字段名
        String test_separator=context.getTest_separator();//测试表的请求字段和默认字段的值的分隔符

        String sql="SELECT "+interface_table+"."+interface_url_field+" as url ,"+interface_table+"."+interface_table_query_param_field+" as it_request,"+test_table+"."+test_table_default_query_value_field+" as default_query ,"+test_table+"."+test_table_default_query_key_field+" as table_request"+
                " FROM "+interface_table+" LEFT JOIN "+test_table+" ON "+interface_table+"."+inteface_keyword_field+" = "+test_table+"."+test_keyword_field;
        //注意这里没有限定where条件，有些字段会返回null
        log.info("SQL:"+sql);
        ArrayList<ServiceComponent> list=new ArrayList<>();
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            ResultSet result = preparedStatement.executeQuery();
            while (result.next()) {
                InterfaceGroupInfo obj = getInterfaceGroupInfo(result);
                // System.out.println(obj);
                if (obj == null) {
                    list.add(new DefaultServiceComponent.Builder("Exception", ServiceComponent.STATE.DOWN).message(MyException.build_mysql_extract_error().toString()).build());
                }else{
                    // System.out.println(obj.toString());
                    interfaceObject.add(processInfo(obj,interface_separator));//对接口表的请求参数去除下划线后面的内容
                }
            }
            return getComponents(context, list,interfaceObject);

        }catch (Exception e){
            MyException exception=MyException.build_mysql_query_error();
            log.error(exception.toString());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String strs = sw.toString();
            log.error(strs);
            list.add(new DefaultServiceComponent.Builder("Exception", ServiceComponent.STATE.DOWN).message(exception.toString()).build());
            return list;
        }finally {
            connection.closeConnection();
        }
    }

    private InterfaceGroupInfo getInterfaceGroupInfo(ResultSet result){
        log.info("DataServiceRestClient.getInterfaceGroupInfo start up");
        try {
            String interface_name = result.getString("url");
            String interface_request = result.getString("it_request");
            String table_request = result.getString("table_request");
            String table_default_request = result.getString("default_query");
            return new InterfaceGroupInfo(interface_name,interface_request,table_request,table_default_request);
        }catch (Exception e){
            e.printStackTrace();
            MyException exception=MyException.build_mysql_extract_error();
            log.error(exception.toString());
            return null;
        }
    }

    //对每个接口发起请求，结果封装成component
    private ArrayList<ServiceComponent> getComponents(DataServiceContext context,ArrayList<ServiceComponent> list,ArrayList<InterfaceGroupInfo> interfaceObject){
        log.info("DataServiceRestClient.getComponents start up");
        if(interfaceObject.size()==0){
            log.error("没有找到任何服务信息，监控失败，请检查配置文件");
            list.add(new DefaultServiceComponent.Builder("Exception", ServiceComponent.STATE.DOWN).message("没有找到任何服务信息，监控失败，请检查配置文件").build());
        }else{
            //对每一条请求的信息进行封装，请求接口，判断是否有返回结果
            log.info("interfaceObject number:"+interfaceObject.size());
            try {
                for (int i = 0; i < interfaceObject.size(); i++) {
                    log.info(interfaceObject.get(i).toString());
                    InterfaceGroupInfo this_obj = interfaceObject.get(i);
                    String name = this_obj.getInterface_name(); //接口名
                    String interface_url = context.getDataServiceUrl();
                    if (!interface_url.endsWith("/")) {
                        interface_url += "/";
                    }
                    String url = interface_url + name; //接口url路径，包含接口url和接口名
                    String password = context.getDataServicePW();//用户秘钥
                    String user_id = context.getDataServiceUser();//用户名
                    String inputname = this_obj.getInterface_request();//接口请求参数
                    MyException exception = getInputParam(context, this_obj);
                    log.info(exception.toString());
                    String inputparam = "";
                    ServiceComponent component = null;
                    if (exception.getId() == 0) {
                        log.info("11");
                        inputparam = this_obj.getInterface_request_default();
                        //发送http请求
                        log.info("22");
                        MyException exce = new GeneralAPIClient().http_request(url, password, name, user_id, inputname, inputparam);
                        log.info("33");
                        log.info(exce.toString());
                        // System.out.println(exce.getId()+":"+exce.toString());
                        if (exce.getId() == 0) {
                            component = new DefaultServiceComponent.Builder(name, ServiceComponent.STATE.UP).message(exce.toString()).build();
                        } else {
                            component = new DefaultServiceComponent.Builder(name, ServiceComponent.STATE.DOWN).message(exce.toString()).build();
                        }
                        list.add(component);
                    } else {
                        component = new DefaultServiceComponent.Builder(name, ServiceComponent.STATE.DOWN).message(exception.toString()).build();
                        list.add(component);
                    }
                }
            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw, true));
                String strs = sw.toString();
                log.error(strs);
                log.error("封装component失败");
            }
        }
        log.info("当前返回的component size :"+list.size());
        return list;
    }

    //处理接口表请求参数中有多余字符的问题,例interface_request='AAC002_P'
    private InterfaceGroupInfo processInfo(InterfaceGroupInfo obj,String seq){
        log.info("DataServiceRestClient.processInfo start up");
        String interface_request=obj.getInterface_request();
        if(interface_request!=null && interface_request.contains("_")){
            String[] strSplit=interface_request.split(seq);
            String end="";
            for(String temp:strSplit){
                if(temp.contains("_")) {
                    int sign = temp.indexOf("_");
                    end += temp.substring(0, sign) + seq;
                }else{
                    end+=temp+seq;
                }
            }
            end=end.substring(0,end.length()-1);
            obj.setInterface_request(end);
        }
        return obj;
    }

    private MyException getInputParam(DataServiceContext context,InterfaceGroupInfo obj){
        log.info("DataServiceRestClient.getInputParam start up");
        try{
            if (obj.getInterface_request() != null && !obj.getInterface_request().equals("")) {
                //接口请求参数
                String[] interface_query_param = obj.getInterface_request().split(context.getInterface_separator());
                if (obj.getTable_request() == null && obj.getTable_default_request() == null) {
                    return MyException.build_interface_param_and_defualt_value_not_found_error(obj.getInterface_name());
                }
                //表请求参数
                String[] table_query_param = obj.getTable_request().split(context.getTest_separator());
                log.info("1");
                //表请求参数默认值
                String[] table_query_default = obj.getTable_default_request().split(context.getTest_separator());
                log.info("2");
                if (table_query_default.length != table_query_param.length) {
                    return MyException.build_test_request_field_default_field_error();
                }
                log.info("3");
                String end = "";
                for (int y = 0; y < interface_query_param.length; y++) {
                    String this_query_param = interface_query_param[y];
                    for (int x = 0; x < table_query_param.length; x++) {
                        String temp = table_query_param[x];
                        if (temp.equals(this_query_param)) {
                            end += table_query_default[x] + context.getInterface_separator();
                        }
                        log.info("4");
                    }
                    log.info("5");
                }
                log.info("6");
                if (end.length() > 0) {
                    end = end.substring(0, end.length() - 1);
                }
                //有可能接口请求参数和表默认请求参数不同的情况，那最后的input_request_param和input_request_default的个数不匹配，会导致程序异常，所以默认只要不匹配即忽略
                int sign = 0;
                if (end.length() != 0) {
                    sign = end.split(context.getInterface_separator()).length;
                }
                if (sign != interface_query_param.length) {
                    return MyException.build_request_interface_param_and_defualt_value_num_error();
                }
//                log.info("请求参数" + obj.getInterface_request() + "默认分隔符" + context.getInterface_separator());
//                log.info("默认参数" + obj.getTable_request() + "默认分隔符" + context.getTest_separator());
//                log.info("默认参数值" + obj.getTable_default_request() + "默认分隔符" + context.getTest_separator());
//                log.info("处理后请求参数值" + end);
                obj.setInterface_request_default(end);
            }
            }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            String strs = sw.toString();
            log.error(strs);
        }

        return MyException.build();

    }


}
