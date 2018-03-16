package com.onescorpin.servicemonitor.impala.connect;//package com.thinkbiganalytics.impala.connect;
//
//import com.thinkbiganalytics.impala.common.DataServiceContext;
//
//import java.sql.*;
//
///*-
// * #%L
// * thinkbig-service-monitor-nifi
// * %%
// * Copyright (C) 2017 ThinkBig Analytics
// * %%
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// * #L%
// */
////获取impala connection 废弃，不通过发送sql验证
//public class ImpalaConnection {
//    private Connection conn=null;
//    public Connection getConnection(DataServiceContext context){
//        try {
//            String url = assembled(context);
//            //加载jdbc驱动
//            Class.forName(context.getImpalaDriver());
//            conn = DriverManager.getConnection(url);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//        return conn;
//    }
//    public Statement getStatement(DataServiceContext context){
//        Connection conn=getConnection(context);
//        try {
//            return conn.createStatement();
//        }catch(Exception e){
//            e.printStackTrace();
//            return null;
//        }
//    }
//    public void closeConnection(){
//        try {
//            if (conn != null) {
//                conn.close();
//            }
//        }catch (Exception e){}
//
//    }
//    //判断数据库是否支持批量处理
//    public  boolean supportBatch(Connection con) {
//        try {
//            // 得到数据库的元数据
//            DatabaseMetaData md = con.getMetaData();
//            return md.supportsBatchUpdates();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//    /** 执行一批SQL语句 事务，只支持增删改，不返回结果的查询 */
//    public Statement goBatch(Connection con, String[] sqls) throws Exception {
//        if (sqls == null) {
//            return null;
//        }
//        Statement sm = null;
//        try {
//            sm = con.createStatement();
//            for (int i = 0; i < sqls.length; i++) {
//                sm.addBatch(sqls[i]);// 将所有的SQL语句添加到Statement中
//            }
//            // 一次执行多条SQL语句
//            return sm;
//        } catch (SQLException e) {
//            e.printStackTrace();
//        } finally {
//            sm.close();
//        }
//        return null;
//    }
//    public  String assembled(DataServiceContext context){
//        return  "jdbc:hive2://" + context.getImpalaHost() + ':' + context.getImpalaJdbcPort() + "/"+context.getImpalaDatabase()+";auth=noSasl";
//
//    }
//}
