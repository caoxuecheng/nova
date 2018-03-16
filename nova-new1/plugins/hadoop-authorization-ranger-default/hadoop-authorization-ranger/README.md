Hadoop Ranger Authorization Plugin
====
This module allows you to configure Nova to query security groups from Ranger when creating a new category and a new nflow. If you
assign Ranger security groups at the category level, the nflow level groups will be defaulted with those values. You will have the option to modify the groups
as part of the nflow creation process.

In addition there is a Ranger NiFi processor that can create Ranger policies based on the needs of your template. You can
see an example of this in the data ingest sample template.

To use this you need to do the following
* Include this jar in the /opt/nova/nova-services/plugin folder
* Copy the authorization.ranger.properties to the /opt/nova/nova-services/conf folder
* Configure the authorization.ranger.properties file
* Make sure the template you are using includes the PutNflowMetadat processor to register the 3 required
metadata attributes. See the HadoopAuthorizationService class to review the property names.

This plugin is not installed by default as part of the RPM install

Ranger authorization.ranger.properties
===
Below is an example properties file:

```
ranger.hostName=localhost
ranger.port=6080
ranger.userName=admin
ranger.password=admin
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

* hadoop-authorization-ranger
* ranger-rest-client
