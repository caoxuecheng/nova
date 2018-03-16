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
echo "Couldn't recognize linux version, after removal you need to turn off autostart of nova services
(nova-ui, nova-services and nova-spark-shell)"
}

linux_type=$(get_linux_type)
echo "Type of init scripts management tool determined as $linux_type"

rpmInstallDir=/opt/nova

echo "     2. Stopping Applications ... "
service nova-ui stop
service nova-services stop
service nova-spark-shell stop

echo "     3. Removing service configuration "

if [ "$linux_type" == "chkonfig" ]; then
    chkconfig --del nova-ui
    chkconfig --del nova-spark-shell
    chkconfig --del nova-services
elif [ "$linux_type" == "update-rc.d" ]; then
    update-rc.d -f nova-ui remove
    update-rc.d -f nova-shell remove
    update-rc.d -f nova-spark-services remove
fi
rm -rf /etc/init.d/nova-ui
echo "         - Removed nova-ui script '/etc/init.d/nova-ui'"
rm -rf /etc/init.d/nova-services
echo "         - Removed nova-services script '/etc/init.d/nova-services'"
rm -rf /etc/init.d/nova-spark-shell
echo "         - Removed nova-spark-shell script '/etc/init.d/nova-spark-shell'"

rm -rf $rpmInstallDir/setup

echo "     4. Deleting application folders "
rm -rf $rpmInstallDir/nova-ui
echo "         - Removed nova-ui"
rm -rf $rpmInstallDir/nova-services
echo "         - Removed nova-services"

echo "     5. Deleting log folders "
rm -rf /var/log/nova-ui
rm -rf /var/log/nova-services
rm -rf /var/log/nova-spark-shell

echo "     6. Deleting nova-service "
rm -rf /usr/bin/nova-service

echo "    REMOVAL COMPLETE! "
