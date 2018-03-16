Hadoop Sentry Plugin
====
====
This module allows you to configure Nova to query security groups from Sentry when creating a new category and a new nflow. If you assign Sentry security groups at the category level, the nflow level groups will be defaulted with those values. You will have the option to modify the groups as part of the nflow creation process.

To use this you need to do the following
* Include this jar in the /opt/nova/nova-services/plugin folder
* Copy the authorization.sentry.properties to the /opt/nova/nova-services/conf folder
* Configure the authorization.sentry.properties file
* Make sure the template you are using includes the PutNflowMetadata processor to register the 3 required
metadata attributes. See the HadoopAuthorizationService class to review the property names.

This plugin is not installed by default as part of the RPM install

Sentry authorization.sentry.properties
===
Below is an example properties file for Non-Kerberos environment:

```
beeline.connection.url=jdbc:hive2://localhost:10000/default
beeline.drive.name=org.apache.hive.jdbc.HiveDriver
beeline.userName=nifi
beeline.password=
hdfs.hadoop.configuration=/etc/hadoop/conf/hdfs-site.xml,/etc/hadoop/conf/core-site.xml
authorization.sentry.groups=hadoop,sentry1,sentry2,sentry3,nifi,nova
```

Below is an example properties file for Kerberos environment:

```
beeline.connection.url=jdbc:hive2://localhost:10000/default;principal=hive/quickstart.cloudera@CLOUDERA
beeline.drive.name=org.apache.hive.jdbc.HiveDriver
beeline.userName=nifi
beeline.password=
hdfs.hadoop.configuration=/etc/hadoop/conf/hdfs-site.xml,/etc/hadoop/conf/core-site.xml
authorization.sentry.groups=hadoop,sentry1,sentry2,sentry3,nifi,nova
sentry.kerberos.principal=nifi@CLOUDERA
sentry.kerberos.KeytabLocation=/etc/security/nifi.headless.keytab
sentry.IsKerberosEnabled=true
```


PutNflowMetadata Processor Required Values
===
```
Namespace: registration
hdfsFolders: <list of folders seperated by newline>
hiveSchema: <name of hive schema>
hiveTableNames: <list of hive tables seperated by newline>

```

Development
===
To test the plugin in your IDE you need to add the below two maven modules to the nova-services app

* hadoop-authorization-sentry
* sentry-client
