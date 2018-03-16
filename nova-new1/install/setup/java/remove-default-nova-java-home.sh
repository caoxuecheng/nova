#!/bin/bash

if [ $# -ne 1 ]
then
    echo "Wrong number of arguments. You must pass in the nova home location"
    exit 1
fi

NOVA_HOME=$1

echo "Removing custom JAVA_HOME from nova-ui and nova-services"
sed -i '/export JAVA_HOME=\/opt\/java\/current/d' $NOVA_HOME/nova-services/bin/run-nova-services.sh
sed -i '/export PATH=\$JAVA_HOME\/bin\:\$PATH/d' $NOVA_HOME/nova-services/bin/run-nova-services.sh
sed -i '/export JAVA_HOME=\/opt\/java\/current/d' $NOVA_HOME/nova-services/bin/run-nova-services-with-debug.sh
sed -i '/export PATH=\$JAVA_HOME\/bin\:\$PATH/d' $NOVA_HOME/nova-services/bin/run-nova-services-with-debug.sh
sed -i '/export JAVA_HOME=\/opt\/java\/current/d' $NOVA_HOME/nova-ui/bin/run-nova-ui.sh
sed -i '/export PATH=\$JAVA_HOME\/bin\:\$PATH/d' $NOVA_HOME/nova-ui/bin/run-nova-ui.sh
sed -i '/export JAVA_HOME=\/opt\/java\/current/d' $NOVA_HOME/nova-ui/bin/run-nova-ui-with-debug.sh
sed -i '/export PATH=\$JAVA_HOME\/bin\:\$PATH/d' $NOVA_HOME/nova-ui/bin/run-nova-ui-with-debug.sh
