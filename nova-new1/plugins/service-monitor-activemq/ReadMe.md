Activemq Service Monitor Plugin
====
This module allows you to monitor Activemq service and status is show on Nova UI under Service tab. Activemq URL is obtained from Nova configuration file. 

Pre-requisite : Make sure jms.activemq.broker.url is configured in nova-services/conf/activemq.properties.

To use this you need to do the following

* Copy jar from /opt/nova/setup/plugins/nova-service-monitor-activemq-<Nova-Version>.jar to the /opt/nova/nova-services/plugin/nova-service-monitor-activemq-<Nova-Version>.jar  folder
* Make sure jar file is owned by Nova service user.
* Restart Nova Services

```
service nova-services stop
service nova-services start
```

