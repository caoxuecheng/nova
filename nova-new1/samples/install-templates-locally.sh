#!/bin/bash

##########
#
# installs the sample templates in the Nova sandbox
#
##########

curl -i -X POST -u dladmin:onescorpin -H "Content-Type: multipart/form-data" \
    -F "overwrite=false" \
    -F "categorySystemName=" \
    -F "importConnectingReusableFlow=NOT_SET" \
    -F "file=@/opt/nova/setup/data/nflows/nifi-1.3/index_text_service_v2.nflow.zip" \
     http://localhost:8400/proxy/v1/nflowmgr/admin/import-nflow

curl -i -X POST -u dladmin:onescorpin -H "Content-Type: multipart/form-data" \
    -F "file=@/opt/nova/setup/data/templates/nifi-1.0/data_ingest.zip" \
    -F "overwrite=false" \
    -F "createReusableFlow=false" \
    -F "importConnectingReusableFlow=YES" \
    http://localhost:8400/proxy/v1/nflowmgr/admin/import-template

curl -i -X POST -u dladmin:onescorpin -H "Content-Type: multipart/form-data" \
    -F "file=@/opt/nova/setup/data/templates/nifi-1.0/data_transformation.zip" \
    -F "overwrite=false" \
    -F "createReusableFlow=false" \
    -F "importConnectingReusableFlow=YES" \
    http://localhost:8400/proxy/v1/nflowmgr/admin/import-template

curl -i -X POST -u dladmin:onescorpin -H "Content-Type: multipart/form-data" \
    -F "file=@/opt/nova/setup/data/templates/nifi-1.0/data_confidence_invalid_records.zip" \
    -F "overwrite=true" \
    -F "createReusableFlow=false" \
    -F "importConnectingReusableFlow=NOT_SET" \
    http://localhost:8400/proxy/v1/nflowmgr/admin/import-template
