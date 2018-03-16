#!/usr/bin/env bash

NOVA_HOME=/opt/nova

if [ $# -eq 1 ]
then
    echo "Setting the NOVA_HOME to $1"
    NOVA_HOME=$1
fi

TARGET=nova-db-update-script.sql
PROPS=$NOVA_HOME/nova-services/conf/application.properties

echo "Reading configuration properties from ${PROPS}"

USERNAME=`grep "^spring.datasource.username=" ${PROPS} | cut -d'=' -f2`
PASSWORD=`grep "^spring.datasource.password=" ${PROPS} | cut -d'=' -f2`
DRIVER=`grep "^spring.datasource.driverClassName=" ${PROPS} | cut -d'=' -f2`
URL=`grep "^spring.datasource.url=" ${PROPS} | cut -d'=' -f2`

CP="$NOVA_HOME/nova-services/lib/liquibase-core-3.5.3.jar.jar:$NOVA_HOME/nova-services/lib/*"
echo "Loading classpath: ${CP}"

echo "Generating ${TARGET} for ${URL}, connecting as ${USERNAME}"

java -cp ${CP} \
    liquibase.integration.commandline.Main \
     --changeLogFile=com/onescorpin/db/master.xml \
     --driver=${DRIVER} \
     --url=${URL} \
     --username=${USERNAME} \
     --password=${PASSWORD} \
     updateSQL > ${TARGET}

echo "Replacing delimiter placeholders"
sed -i.bac "s/-- delimiter placeholder //g" ${TARGET}
