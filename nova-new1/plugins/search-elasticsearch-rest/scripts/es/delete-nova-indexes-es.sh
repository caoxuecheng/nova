#!/bin/bash

#############################################################
# Use this script to delete Nova indexes from Elasticsearch #
#############################################################

ELASTIC_SEARCH_HOST=$1
ELASTIC_SEARCH_REST_PORT=$2

if [ $# -eq 2 ]
then
    echo "Deleting nova indexes in Elasticsearch: host="$ELASTIC_SEARCH_HOST", port="$ELASTIC_SEARCH_REST_PORT
else
    echo "Usage: <command> <host> <rest-port>"
    echo "Examples values:"
    echo " host: localhost"
    echo " rest-port: 9200"
    exit 1
fi

nova_indexes=nova-data,nova-datasources,nova-categories-metadata,nova-categories-default,nova-nflows-metadata,nova-nflows-default

for NOVA_INDEX in $(echo $nova_indexes | sed "s/,/ /g")
do
    curl -XDELETE $ELASTIC_SEARCH_HOST:$ELASTIC_SEARCH_REST_PORT/$NOVA_INDEX?pretty
done

echo "Done"
