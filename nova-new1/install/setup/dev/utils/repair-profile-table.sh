#!/bin/bash

# Show purpose of utility
echo
echo "============ Repair Utility (Nova Profiler) ============"
echo "This utility is for fixing profile tables that show the "
echo "same column name being repeated twice (one all in lower "
echo "case, and other with upper case letters.)  This utility "
echo "will repair the profile table  by converting all column " 
echo "names to lowercase."
echo
echo "NOTE 1: "
echo "The utility assumes that standard Nova directory names "
echo "and table names are used as specified in standard "
echo "ingest template."
echo
echo "NOTE 2: "
echo "If your profile data is substantial, consider running "
echo "each step of this utility script manually and verifying "
echo "success of each step."
echo "======================================================="
echo

# Check command-line arguments
if [ "$#" -ne 6 ]; then
    echo "[Profile Table Repair Utility: correct usage]" 
    echo "$ repair-profile-table.sh <[1] hiveserver2-hostname> <[2] hiveserver2-port (usually 10000)> <[3] hiveserver2-username> <[4] hiveserver2-password> <[5] nova-category-name> <[6] nova-nflow-name>"
    exit -1
fi

# Check user running the script
USER_RUNNING_SCRIPT=$(whoami)

if [[ ${USER_RUNNING_SCRIPT} != "nifi" ]]; then
    echo "[Profile Table Repair Utility: user check] This utility must be run as nifi user. (Current user is ${USER_RUNNING_SCRIPT})"
    exit -1
fi 

# Ask user confirmation to move ahead
read -p "Do you wish to proceed (y/n)? " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]
then

HS2_HOSTNAME=$1
HS2_PORT=$2
HS2_USERNAME=$3
HS2_PASSWORD=$4
NOVA_CATEGORYNAME=$5
NOVA_NFLOWNAME=$6

# Derive values for profile and profile_fix table
NOVA_PROFILE_TABLENAME="${NOVA_NFLOWNAME}_profile"
NOVA_PROFILE_FIX_TABLENAME="${NOVA_PROFILE_TABLENAME}_fix"

#echo "[Profile Table Repair Utility] Opening beeline"
beeline << EOF

!connect jdbc:hive2://${HS2_HOSTNAME}:${HS2_PORT} ${HS2_USERNAME} ${HS2_PASSWORD}

set hive.exec.dynamic.partition.mode=nonstrict;

create table if not exists ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_FIX_TABLENAME}
like ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_TABLENAME}
LOCATION 'hdfs:///model.db/${NOVA_CATEGORYNAME}/${NOVA_NFLOWNAME}/profile_fix';

insert overwrite table ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_FIX_TABLENAME} partition (processing_dttm)
select columnname, metrictype, metricvalue, processing_dttm
from ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_TABLENAME};

truncate table ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_TABLENAME};

insert overwrite table ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_TABLENAME} partition (processing_dttm)
select lower(columnname), metrictype, metricvalue, processing_dttm
from ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_FIX_TABLENAME}
where columnname <> '(ALL)'
UNION
select columnname, metrictype, metricvalue, processing_dttm
from ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_FIX_TABLENAME}
where columnname = '(ALL)';

drop table ${NOVA_CATEGORYNAME}.${NOVA_PROFILE_FIX_TABLENAME};

EOF
echo "============ Repair complete ============"
exit

fi

echo "Repair aborted."
