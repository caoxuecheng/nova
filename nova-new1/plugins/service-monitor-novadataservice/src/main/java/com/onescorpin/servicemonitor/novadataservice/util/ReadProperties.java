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




import com.onescorpin.servicemonitor.novadataservice.common.DataServiceContext;

import java.io.FileInputStream;
import java.util.Properties;

//读取配置文件生成对象
public class ReadProperties {

    public MyException getContext(){
        Properties properties=new Properties();
        String jarPath=this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        String propertiesPath=jarPath.substring(0,jarPath.indexOf("plugin"))+"conf/dataService.properties";
        FileInputStream fileInputStream=null;
        try {
            fileInputStream = new FileInputStream(propertiesPath);
            properties.load(fileInputStream);
        }catch (Exception e){
            return MyException.build_properties_not_find(propertiesPath);
        }finally {
            try {
                fileInputStream.close();
            }catch (Exception e){}
        }
        String dataServiceUrl=properties.getProperty(PropertiesObject.dataServiceUrl,null);
        String dataServiceUser=properties.getProperty(PropertiesObject.dataServiceUser,null);
        String dataServicePW=properties.getProperty(PropertiesObject.dataServicePW,null);
        String mysqlUrl=properties.getProperty(PropertiesObject.mysqlUrl,null);
        String mysqlUser=properties.getProperty(PropertiesObject.mysqlUser,null);
        String mysqlPW=properties.getProperty(PropertiesObject.mysqlPW,null);
        String serviceTable=properties.getProperty(PropertiesObject.serviceTable,null);
        String serviceTestTable=properties.getProperty(PropertiesObject.serviceTestTable,null);
        String mysqlPort=properties.getProperty(PropertiesObject.mysqlPort,null);
        String mysqlDatabase=properties.getProperty(PropertiesObject.mysqlDatabase,null);

        String interface_keyword_field=properties.getProperty(PropertiesObject.interface_keyword_field);
        String interface_table_query_param_field=properties.getProperty(PropertiesObject.interface_table_query_param_field);
        String interface_separator=properties.getProperty(PropertiesObject.interface_separator);
        String interface_url_field=properties.getProperty(PropertiesObject.interface_url_field);
        String test_keyword_field=properties.getProperty(PropertiesObject.test_keyword_field);
        String test_table_default_query_value_field=properties.getProperty(PropertiesObject.test_table_default_query_value_field);
        String test_table_default_query_key_field=properties.getProperty(PropertiesObject.test_table_default_query_key_field);
        String test_separator=properties.getProperty(PropertiesObject.test_separator);

        if(dataServicePW !=null &&
                dataServiceUrl !=null &&
                dataServiceUser!=null &&
                mysqlUrl!=null &&
                mysqlPW !=null &&
                mysqlUser!=null &&
                serviceTable !=null &&
                serviceTestTable!=null &&
                mysqlPort!=null &&
                mysqlDatabase!=null &&
                interface_keyword_field !=null &&
                interface_table_query_param_field!=null &&
                interface_separator!=null &&
                interface_url_field!=null &&
                test_keyword_field!=null &&
                test_table_default_query_value_field!=null &&
                test_table_default_query_key_field!=null &&
                test_separator!=null){
            DataServiceContext.build(
                    dataServiceUrl,
                    dataServiceUser,
                    dataServicePW,
                    mysqlUrl,
                    mysqlUser,
                    mysqlPW,
                    serviceTable,
                    serviceTestTable,
                    mysqlPort,
                    mysqlDatabase,
                    interface_keyword_field,
                    interface_table_query_param_field,
                    interface_separator,
                    interface_url_field,
                    test_keyword_field,
                    test_table_default_query_value_field,
                    test_table_default_query_key_field,
                    test_separator);
            return MyException.build();
        }else{
            String key_none="";
            if(dataServicePW==null){key_none=PropertiesObject.dataServicePW;
            }else if(dataServiceUrl==null){key_none=PropertiesObject.dataServiceUrl;
            }else if(dataServiceUser==null){key_none=PropertiesObject.dataServiceUser;
            }else if(mysqlPW==null){key_none=PropertiesObject.mysqlPW;
            }else if(mysqlUrl==null){key_none=PropertiesObject.mysqlUrl;
            }else if(mysqlUser==null){key_none=PropertiesObject.mysqlUser;
            }else if(serviceTable==null){key_none=PropertiesObject.serviceTable;
            }else if(serviceTestTable==null){key_none=PropertiesObject.serviceTestTable;
            }else if(mysqlPort==null){key_none=PropertiesObject.mysqlPort;
            }else if( mysqlDatabase==null){key_none=PropertiesObject.mysqlDatabase;
            }else if(interface_keyword_field==null){key_none=PropertiesObject.interface_keyword_field;
            }else if(interface_table_query_param_field==null){key_none=PropertiesObject.interface_table_query_param_field;
            }else if(interface_separator==null){key_none=PropertiesObject.interface_separator;
            }else if(interface_url_field==null){key_none=PropertiesObject.interface_url_field;
            }else if(test_keyword_field==null){key_none=PropertiesObject.test_keyword_field;
            }else if(test_table_default_query_value_field==null){key_none=PropertiesObject.test_table_default_query_value_field;
            }else if(test_table_default_query_key_field==null){key_none=PropertiesObject.test_table_default_query_key_field;
            }else if(test_separator==null){key_none=PropertiesObject.test_separator;}
            return MyException.build_properties_not_field(key_none);
        }
    }
}
