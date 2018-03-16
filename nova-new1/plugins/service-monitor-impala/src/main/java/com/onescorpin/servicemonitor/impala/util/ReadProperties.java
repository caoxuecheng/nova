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




import com.onescorpin.servicemonitor.impala.common.DataServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Properties;

//读取配置文件生成对象
public class ReadProperties {
    private static Logger log = LoggerFactory.getLogger(ReadProperties.class);
    private String separator=";";   //配置文件中同key多value的分隔符
    public MyException getContext() {
        Properties properties = new Properties();
        String jarPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        String propertiesPath = jarPath.substring(0, jarPath.indexOf("plugin")) + "conf/impalaService.properties";
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(propertiesPath);
            properties.load(fileInputStream);
        } catch (Exception e) {
            MyException exception = MyException.build_properties_not_find(propertiesPath);
            log.error(exception.toString());//错误信息在日志里也打印一份
            return exception;
        } finally {
            try {
                fileInputStream.close();
            } catch (Exception e) {
            }
        }
        String impalaCatalogServerHost = properties.getProperty(PropertiesObject.impalaCatalogServerHost, null);
        String impalaCatalogServerPort = properties.getProperty(PropertiesObject.impalaCatalogServerPort, null);
        String impalaDaemonHost = properties.getProperty(PropertiesObject.impalaDaemonHost, null);
        String impalaDaemonPort = properties.getProperty(PropertiesObject.impalaDaemonPort, null);
        String impalaStateStoreHost = properties.getProperty(PropertiesObject.impalaStateStoreHost, null);
        String impalaStateStorePort = properties.getProperty(PropertiesObject.impalaStateStorePort, null);
        DataServiceContext context=DataServiceContext.build();
        try {
            if(impalaCatalogServerPort != null) {context.setCatalogPort(Integer.parseInt(impalaCatalogServerPort));}
            if(impalaDaemonPort        !=null){context.setDaemonPort(Integer.parseInt(impalaDaemonPort));}
            if(impalaStateStorePort    !=null){context.setStatestorePort(Integer.parseInt(impalaStateStorePort));}
            if(impalaCatalogServerHost !=null){context.setCatalogHost(impalaCatalogServerHost.split(separator));}
            if(impalaDaemonHost        !=null){context.setDaemonHost(impalaDaemonHost.split(separator));}
            if(impalaStateStoreHost    !=null){context.setStatestoreHost(impalaStateStoreHost.split(separator));}
        }catch (Exception e){
            MyException exception= MyException.build_impala_properties_deserialization_error();
            log.error(exception.toString());
            return exception;
        }
        return MyException.build();
    }

}
