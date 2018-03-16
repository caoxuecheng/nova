#!/bin/bash

if [ "$#" -ne 3 ]; then
    echo "[Correct usage] Three parameters are needed: host, user, password"
    exit -1
fi
MYSQL_DIR=$(dirname $0)
echo "*** Performing migration of schema from onescorpin to nova via two step-process ***"
echo "*** Step 1: Setting up nova schema ***"
$MYSQL_DIR/setup-mysql.sh $1 $2 $3
echo
echo "*** Step 2: Migrating onescorpin schema to nova schema ***"
$MYSQL_DIR/nova/migration/migrate_from_onescorpin_schema.sh $1 $2 $3
echo
echo "NOTE: After verifying nova schema, onescorpin schema can be dropped."
echo "Done"
