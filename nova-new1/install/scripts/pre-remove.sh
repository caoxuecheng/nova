#!/bin/bash

###
# #%L
# install
# %%
# Copyright (C) 2017 Onescorpin
# %%
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #L%
###

# function for determining way to handle startup scripts
function get_linux_type {
# redhat
which chkconfig > /dev/null && echo "chkonfig" && return 0
# ubuntu sysv
which update-rc.d > /dev/null && echo "update-rc.d" && return 0
echo "Couldn't recognize linux version, not saving version of installed Nova package"
}

linux_type=$(get_linux_type)
echo "Type of init scripts management tool determined as $linux_type"

rpmInstallDir=/opt/nova

echo "   REMOVING Nova ... "
##copy any conf and plugins before installing
{
time_stamp=$(date +%Y_%m_%d_%H_%M_%s)
mkdir -p $rpmInstallDir/bkup-config/$time_stamp
bkupDir=$rpmInstallDir/bkup-config/$time_stamp
###find the rpm that is installed
if [ "$linux_type" == "chkonfig" ]; then
    lastRpm=$(rpm -q --last nova)
elif [ "$linux_type" == "update-rc.d" ]; then
    lastRpm=$(dpkg -s nova | grep ^Version)
fi
touch ${bkupDir}/README.txt
readme=${bkupDir}/README.txt

echo "    1. Backup Configuration "
echo "        - Backing up previous configuration files "
echo "        - Copying previous /conf folder"
echo "        - Contents in this directory is for nova RPM: $lastRpm " > $readme
cp -r $rpmInstallDir/nova-ui/conf $bkupDir/nova-ui
cp -r $rpmInstallDir/nova-services/conf $bkupDir/nova-services
echo "        - BACKUP COMPLETE!! "
echo "        - Backup Configuration is located at : $bkupDir "
echo "        - A README.txt file will be included in the backup directory indicating what RPM these files came from "
}
