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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class GeneralAPIClient {
    private static Logger log = LoggerFactory.getLogger(GeneralAPIClient.class);
    public MyException http_request(String url,String password,String name,String user_id,String inputname,String inputparam) {
        CloseableHttpClient httpclient=null ;
        HttpPost httppost=null;
        String jsonStr=null;
        try {
            log.info("111");
            httpclient = HttpClientBuilder.create().build();
            log.info("222");
            SimpleDateFormat greenwich = new SimpleDateFormat("EEE,d MMM yyyy HH:mm:ss z", Locale.US);
            httppost = new HttpPost(url);//修改url最后的一段为接口名称
            log.info("223");
            int i = 0;
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            if(inputname!=null && inputparam!=null) {
                String[] inputNameArray = inputname.split(",");
                String[] inputParamArray = inputparam.split(",");
                if (inputname != null && inputparam != null && inputname != "" && inputparam != "") {
                    for (String inputName : inputNameArray) {
                        //加入请求参数和请求值，具体传什么跟接口表请求参数有关
                        params.add(new BasicNameValuePair(inputName, URLEncoder.encode(inputParamArray[i], "utf-8")));
                        i++;
                    }
                }
            }
            params.add(new BasicNameValuePair("user_id", user_id));//用户id
            String aa = greenwich.format(new Date());
            String time = URLEncoder.encode(greenwich.format(new Date()), "utf8");
            params.add(new BasicNameValuePair("time", time));
//            System.out.println(greenwich.format(new Date()));

            //Token token1 = new Token();
            //String token = token1.token(user_id, name, aa, password);//秘钥验证，用户id，接口名，请求时间格林威治形式，秘钥（用户秘钥）
            params.add(new BasicNameValuePair("token", password));//用户秘钥
            httppost.setEntity(new UrlEncodedFormEntity(params));
            log.info("333");
            CloseableHttpResponse response = httpclient.execute(httppost);
            log.info("444");
            HttpEntity entity = response.getEntity();
            log.info("555");
            //这里返回的是一个json格式的字符串，对这个字符串转化为对象，取出id和description，封装在MyException中返回
             jsonStr = EntityUtils.toString(entity, "utf-8");
            log.info("http返回结果:"+jsonStr);
            //System.out.println(jsonStr);
            return getResponseIdDesc(jsonStr);
        }catch (Exception e){
            return MyException.build_service_connect_error();
        }finally {
            try {
                httpclient.close();
            }catch (Exception e){
                return MyException.build_service_connect_error();
            }
        }
    }

    public MyException getResponseIdDesc(String str){
        try{
            ObjectMapper mapper=new ObjectMapper();
            JsonNode node=mapper.readTree(str);
            int id=node.findValue("status_id").asInt();
            String desc=node.findValue("description").toString();
          //  System.out.println(id);
          //  System.out.println(desc);
            if(id==0){
                return MyException.build();
            }else{
                return MyException.build().setId(2).setMessage("数据服务异常").setDescription(desc);
            }
        }catch (Exception e){
            return MyException.build_json_parse_error();
        }
    }



}
