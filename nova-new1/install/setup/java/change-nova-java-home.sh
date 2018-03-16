#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "You must pass in a JAVA_HOME path followed by the nova home path"
    exit 1
fi

JAVA_HOME=$1
NOVA_HOME=$2

echo "Adding Custom Java home to nova-ui and nova-services"
sed -i "/\#\!\/bin\/bash/a export JAVA_HOME=$JAVA_HOME\nexport PATH=\$JAVA_HOME\/bin\:\$PATH" $NOVA_HOME/nova-services/bin/run-nova-services.sh
sed -i "/\#\!\/bin\/bash/a export JAVA_HOME=$JAVA_HOME\nexport PATH=\$JAVA_HOME\/bin\:\$PATH" $NOVA_HOME/nova-services/bin/run-nova-services-with-debug.sh
sed -i "/\#\!\/bin\/bash/a export JAVA_HOME=$JAVA_HOME\nexport PATH=\$JAVA_HOME\/bin\:\$PATH" $NOVA_HOME/nova-ui/bin/run-nova-ui.sh
sed -i "/\#\!\/bin\/bash/a export JAVA_HOME=$JAVA_HOME\nexport PATH=\$JAVA_HOME\/bin\:\$PATH" $NOVA_HOME/nova-ui/bin/run-nova-ui-with-debug.sh
