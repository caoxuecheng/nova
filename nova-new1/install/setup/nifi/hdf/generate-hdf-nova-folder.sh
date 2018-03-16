#!/bin/bash

NOVA_SETUP_FOLDER=/opt/nova/setup

if [ $# -eq 1 ]
then
    NOVA_SETUP_FOLDER=$1
else
    echo "Unknown arguments. You must pass in the nova setup folder location. For example: /opt/nova/setup "
    exit 1
fi

mkdir ./nifi-nova

mkdir -p ./nifi-nova/lib/app

echo "Copying required files for HDF"
cp $NOVA_SETUP_FOLDER/nifi/*.nar ./nifi-nova/lib
cp $NOVA_SETUP_FOLDER/nifi/nova-spark-*.jar ./nifi-nova/lib/app/

mkdir ./nifi-nova/activemq
cp $NOVA_SETUP_FOLDER/nifi/activemq/*.jar ./nifi-nova/activemq/

mkdir ./nifi-nova/h2
mkdir ./nifi-nova/ext-config
cp $NOVA_SETUP_FOLDER/nifi/config.properties ./nifi-nova/ext-config/

mkdir ./nifi-nova/nflow_flowfile_cache

cp $NOVA_SETUP_FOLDER/nifi/hdf/install-nova-hdf-components.sh ./nifi-nova

tar -cvf nifi-nova.tar ./nifi-nova

rm -rf ./nifi-nova

echo "The NiFi Nova folder has been generated"
