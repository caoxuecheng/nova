#!/bin/bash
####
# There are two ways to run this script
# No agruments - Assumes it's the RPM process running the script
# ./post-install.sh INSTALL_HOME LINUX_USER LINUX_GROUP
####

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

INSTALL_HOME=/opt/nova
INSTALL_USER=nova
INSTALL_GROUP=users
INSTALL_TYPE="RPM"
LOG_DIRECTORY_LOCATION=/var/log

echo "Installing to $INSTALL_HOME as the user $INSTALL_USER"

if [ $# -eq 3 ]
then
    INSTALL_HOME=$1
    INSTALL_USER=$2
    INSTALL_GROUP=$3
    INSTALL_TYPE="COMMAND_LINE"
elif [ $# -eq 4 ]
then
    INSTALL_HOME=$1
    INSTALL_USER=$2
    INSTALL_GROUP=$3
    LOG_DIRECTORY_LOCATION=$4
    INSTALL_TYPE="COMMAND_LINE"
fi

# function for determining way to handle startup scripts
function get_linux_type {
# redhat
which chkconfig > /dev/null && echo "chkonfig" && return 0
# ubuntu sysv
which update-rc.d > /dev/null && echo "update-rc.d" && return 0
echo "Couldn't recognize linux version, after installation you need to do these steps manually:"
echo " * add proper header to /etc/init.d/{nova-ui,nova-services,nova-spark-shell} files"
echo " * set them to autostart"
}

linux_type=$(get_linux_type)
echo "Type of init scripts management tool determined as $linux_type"

if [ "RPM" = "$INSTALL_TYPE" ]
then
    cd $INSTALL_HOME
    tar -xf nova-*-dependencies.tar.gz
    rm nova-*-dependencies.tar.gz
fi

chown -R $INSTALL_USER:$INSTALL_GROUP $INSTALL_HOME

pgrepMarkerNovaUi=nova-ui-pgrep-marker
pgrepMarkerNovaServices=nova-services-pgrep-marker
pgrepMarkerNovaSparkShell=nova-spark-shell-pgrep-marker
rpmLogDir=$LOG_DIRECTORY_LOCATION

echo "    - Install nova-ui application"

jwtkey=$(head -c 64 /dev/urandom | md5sum |cut -d' ' -f1)
sed -i "s/security\.jwt\.key=<insert-256-bit-secret-key-here>/security\.jwt\.key=${jwtkey}/" $INSTALL_HOME/nova-ui/conf/application.properties
echo "   - Installed nova-ui to '$INSTALL_HOME/nova-ui'"

if ! [ -f $INSTALL_HOME/encrypt.key ]
then
    head -c64 < /dev/urandom | base64 > $INSTALL_HOME/encrypt.key
    chmod 400 $INSTALL_HOME/encrypt.key
    chown $INSTALL_USER:$INSTALL_GROUP $INSTALL_HOME/encrypt.key
fi

cat << EOF > $INSTALL_HOME/nova-ui/bin/run-nova-ui.sh
#!/bin/bash
export JAVA_HOME=/opt/java/current
export PATH=\$JAVA_HOME/bin:\$PATH
export NOVA_UI_OPTS=-Xmx512m
[ -f $INSTALL_HOME/encrypt.key ] && export ENCRYPT_KEY="\$(cat $INSTALL_HOME/encrypt.key)"
java \$NOVA_UI_OPTS -cp $INSTALL_HOME/nova-ui/conf:$INSTALL_HOME/nova-ui/lib/*:$INSTALL_HOME/nova-ui/plugin/* com.onescorpin.NovaUiApplication --pgrep-marker=$pgrepMarkerNovaUi > $LOG_DIRECTORY_LOCATION/nova-ui/std.out 2>$LOG_DIRECTORY_LOCATION/nova-ui/std.err &
EOF
cat << EOF > $INSTALL_HOME/nova-ui/bin/run-nova-ui-with-debug.sh
  #!/bin/bash
export JAVA_HOME=/opt/java/current
export PATH=\$JAVA_HOME/bin:\$PATH
export NOVA_UI_OPTS=-Xmx512m
[ -f $INSTALL_HOME/encrypt.key ] && export ENCRYPT_KEY="\$(cat $INSTALL_HOME/encrypt.key)"
JAVA_DEBUG_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9997
java \$NOVA_UI_OPTS \$JAVA_DEBUG_OPTS -cp $INSTALL_HOME/nova-ui/conf:$INSTALL_HOME/nova-ui/lib/*:$INSTALL_HOME/nova-ui/plugin/* com.onescorpin.NovaUiApplication --pgrep-marker=$pgrepMarkerNovaUi > $LOG_DIRECTORY_LOCATION/nova-ui/std.out 2>$LOG_DIRECTORY_LOCATION/nova-ui/std.err &
EOF
chmod +x $INSTALL_HOME/nova-ui/bin/run-nova-ui.sh
chmod +x $INSTALL_HOME/nova-ui/bin/run-nova-ui-with-debug.sh
echo "   - Created nova-ui script '$INSTALL_HOME/nova-ui/bin/run-nova-ui.sh'"

# header of the service file depends on system used
if [ "$linux_type" == "chkonfig" ]; then
cat << EOF > /etc/init.d/nova-ui
#! /bin/sh
# chkconfig: 345 98 22
# description: nova-ui
# processname: nova-ui
EOF
elif [ "$linux_type" == "update-rc.d" ]; then
cat << EOF > /etc/init.d/nova-ui
#! /bin/sh
### BEGIN INIT INFO
# Provides:          nova-ui
# Required-Start:    $local_fs $network $named $time $syslog
# Required-Stop:     $local_fs $network $named $time $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Description:       nova-ui
### END INIT INFO
EOF
fi

cat << EOF >> /etc/init.d/nova-ui
RUN_AS_USER=$INSTALL_USER

debug() {
    if pgrep -f nova-ui-pgrep-marker >/dev/null 2>&1
      then
        echo Already running.
      else
        echo Starting nova-ui in debug mode...
        grep 'address=' $INSTALL_HOME/nova-ui/bin/run-nova-ui-with-debug.sh
        su - \$RUN_AS_USER -c "$INSTALL_HOME/nova-ui/bin/run-nova-ui-with-debug.sh"
    fi
}

start() {
    if pgrep -f $pgrepMarkerNovaUi >/dev/null 2>&1
      then
        echo Already running.
      else
        echo Starting nova-ui ...
        su - \$RUN_AS_USER -c "$INSTALL_HOME/nova-ui/bin/run-nova-ui.sh"
    fi
}

stop() {
    if pgrep -f $pgrepMarkerNovaUi >/dev/null 2>&1
      then
        echo Stopping nova-ui ...
        pkill -f $pgrepMarkerNovaUi
      else
        echo Already stopped.
    fi
}

status() {
    if pgrep -f $pgrepMarkerNovaUi >/dev/null 2>&1
      then
          echo Running.  Here are the related processes:
          pgrep -lf $pgrepMarkerNovaUi
      else
        echo Stopped.
    fi
}

case "\$1" in
    debug)
        debug
    ;;
    start)
        start
    ;;
    stop)
        stop
    ;;
    status)
        status
    ;;
    restart)
       echo "Restarting nova-ui"
       stop
       sleep 2
       start
       echo "nova-ui started"
    ;;
esac
exit 0
EOF
chmod +x /etc/init.d/nova-ui
echo "   - Created nova-ui script '/etc/init.d/nova-ui'"

mkdir -p $rpmLogDir/nova-ui/
echo "   - Created Log folder $rpmLogDir/nova-ui/"

if [ "$linux_type" == "chkonfig" ]; then
    chkconfig --add nova-ui
    chkconfig nova-ui on
elif [ "$linux_type" == "update-rc.d" ]; then
    update-rc.d nova-ui defaults
fi
echo "   - Added service 'nova-ui'"
echo "    - Completed nova-ui install"

echo "    - Install nova-services application"

rm -f $INSTALL_HOME/nova-services/lib/jetty*
rm -f $INSTALL_HOME/nova-services/lib/servlet-api*
sed -i "s/security\.jwt\.key=<insert-256-bit-secret-key-here>/security\.jwt\.key=${jwtkey}/" $INSTALL_HOME/nova-services/conf/application.properties
echo "   - Installed nova-services to '$INSTALL_HOME/nova-services'"

cat << EOF > $INSTALL_HOME/nova-services/bin/run-nova-services.sh
#!/bin/bash
export JAVA_HOME=/opt/java/current
export PATH=\$JAVA_HOME/bin:\$PATH
export NOVA_SERVICES_OPTS=-Xmx768m
export NOVA_SPRING_PROFILES_OPTS=
[ -f $INSTALL_HOME/encrypt.key ] && export ENCRYPT_KEY="\$(cat $INSTALL_HOME/encrypt.key)"
PROFILES=\$(grep ^spring.profiles. $INSTALL_HOME/nova-services/conf/application.properties)
NOVA_NIFI_PROFILE="nifi-v1"
if [[ \${PROFILES} == *"nifi-v1.1"* ]];
 then
 NOVA_NIFI_PROFILE="nifi-v1.1"
elif [[ \${PROFILES} == *"nifi-v1.2"* ]] || [[ \${PROFILES} == *"nifi-v1.3"* ]] || [[ \${PROFILES} == *"nifi-v1.4"* ]];
then
 NOVA_NIFI_PROFILE="nifi-v1.2"
fi
echo "using NiFi profile: \${NOVA_NIFI_PROFILE}"

java \$NOVA_SERVICES_OPTS \$NOVA_SPRING_PROFILES_OPTS -cp $INSTALL_HOME/nova-services/conf:$INSTALL_HOME/nova-services/lib/*:$INSTALL_HOME/nova-services/lib/\${NOVA_NIFI_PROFILE}/*:$INSTALL_HOME/nova-services/plugin/* com.onescorpin.server.NovaServerApplication --pgrep-marker=$pgrepMarkerNovaServices > $LOG_DIRECTORY_LOCATION/nova-services/std.out 2>$LOG_DIRECTORY_LOCATION/nova-services/std.err &
EOF
cat << EOF > $INSTALL_HOME/nova-services/bin/run-nova-services-with-debug.sh
#!/bin/bash
export JAVA_HOME=/opt/java/current
export PATH=\$JAVA_HOME/bin:\$PATH
export NOVA_SERVICES_OPTS=-Xmx768m
[ -f $INSTALL_HOME/encrypt.key ] && export ENCRYPT_KEY="\$(cat $INSTALL_HOME/encrypt.key)"
JAVA_DEBUG_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9998
PROFILES=\$(grep ^spring.profiles. $INSTALL_HOME/nova-services/conf/application.properties)
NOVA_NIFI_PROFILE="nifi-v1"

if [[ \${PROFILES} == *"nifi-v1.1"* ]];
 then
 NOVA_NIFI_PROFILE="nifi-v1.1"
elif [[ \${PROFILES} == *"nifi-v1.2"* ]] || [[ \${PROFILES} == *"nifi-v1.3"* ]] || [[ \${PROFILES} == *"nifi-v1.4"* ]];
then
 NOVA_NIFI_PROFILE="nifi-v1.2"
fi
echo "using NiFi profile: \${NOVA_NIFI_PROFILE}"
java \$NOVA_SERVICES_OPTS \$JAVA_DEBUG_OPTS -cp $INSTALL_HOME/nova-services/conf:$INSTALL_HOME/nova-services/lib/*:$INSTALL_HOME/nova-services/lib/\${NOVA_NIFI_PROFILE}/*:$INSTALL_HOME/nova-services/plugin/* com.onescorpin.server.NovaServerApplication --pgrep-marker=$pgrepMarkerNovaServices > $LOG_DIRECTORY_LOCATION/nova-services/std.out 2>$LOG_DIRECTORY_LOCATION/nova-services/std.err &
EOF
chmod +x $INSTALL_HOME/nova-services/bin/run-nova-services.sh
chmod +x $INSTALL_HOME/nova-services/bin/run-nova-services-with-debug.sh
echo "   - Created nova-services script '$INSTALL_HOME/nova-services/bin/run-nova-services.sh'"

# header of the service file depends on system used
if [ "$linux_type" == "chkonfig" ]; then
cat << EOF > /etc/init.d/nova-services
#! /bin/sh
# chkconfig: 345 98 21
# description: nova-services
# processname: nova-services
EOF
elif [ "$linux_type" == "update-rc.d" ]; then
cat << EOF > /etc/init.d/nova-services
#! /bin/sh
### BEGIN INIT INFO
# Provides:          nova-services
# Required-Start:    $local_fs $network $named $time $syslog
# Required-Stop:     $local_fs $network $named $time $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Description:       nova-services
### END INIT INFO
EOF
fi

cat << EOF >> /etc/init.d/nova-services
RUN_AS_USER=$INSTALL_USER

debug() {
    if pgrep -f nova-services-pgrep-marker >/dev/null 2>&1
      then
        echo Already running.
      else
        echo Starting nova-services in debug mode...
        grep 'address=' $INSTALL_HOME/nova-services/bin/run-nova-services-with-debug.sh
        su - \$RUN_AS_USER -c "$INSTALL_HOME/nova-services/bin/run-nova-services-with-debug.sh"
    fi
}

start() {
    if pgrep -f $pgrepMarkerNovaServices >/dev/null 2>&1
      then
        echo Already running.
      else
        echo Starting nova-services ...
        su - \$RUN_AS_USER -c "$INSTALL_HOME/nova-services/bin/run-nova-services.sh"
    fi
}

stop() {
    if pgrep -f $pgrepMarkerNovaServices >/dev/null 2>&1
      then
        echo Stopping nova-services ...
        pkill -f $pgrepMarkerNovaServices
      else
        echo Already stopped.
    fi
}

status() {
    if pgrep -f $pgrepMarkerNovaServices >/dev/null 2>&1
      then
          echo Running.  Here are the related processes:
          pgrep -lf $pgrepMarkerNovaServices
      else
        echo Stopped.
    fi
}

case "\$1" in
    debug)
        debug
    ;;
    start)
        start
    ;;
    stop)
        stop
    ;;
    status)
        status
    ;;
    restart)
       echo "Restarting nova-services"
       stop
       sleep 2
       start
       echo "nova-services started"
    ;;
esac
exit 0
EOF
chmod +x /etc/init.d/nova-services
echo "   - Created nova-services script '/etc/init.d/nova-services'"

mkdir -p $rpmLogDir/nova-services/
echo "   - Created Log folder $rpmLogDir/nova-services/"

if [ "$linux_type" == "chkonfig" ]; then
    chkconfig --add nova-services
    chkconfig nova-services on
elif [ "$linux_type" == "update-rc.d" ]; then
    update-rc.d nova-services defaults
fi
echo "   - Added service 'nova-services'"


echo "    - Completed nova-services install"

echo "    - Install nova-spark-shell application"

cat << EOF > $INSTALL_HOME/nova-services/bin/run-nova-spark-shell.sh
#!/bin/bash

if ! which spark-submit >/dev/null 2>&1; then
	>&2 echo "ERROR: spark-submit not on path.  Has spark been installed?"
	exit 1
fi

SPARK_PROFILE="v"\$(spark-submit --version 2>&1 | grep -o "version [0-9]" | grep -o "[0-9]" | head -1)
NOVA_DRIVER_CLASS_PATH=$INSTALL_HOME/nova-spark-shell-pgrep-marker:$INSTALL_HOME/nova-services/conf:$INSTALL_HOME/nova-services/lib/mariadb-java-client-1.5.7.jar
if [[ -n \$SPARK_CONF_DIR ]]; then
        if [ -r \$SPARK_CONF_DIR/spark-defaults.conf ]; then
		CLASSPATH_FROM_SPARK_CONF=\$(grep -E '^spark.driver.extraClassPath' \$SPARK_CONF_DIR/spark-defaults.conf | awk '{print \$2}')
		if [[ -n \$CLASSPATH_FROM_SPARK_CONF ]]; then
			NOVA_DRIVER_CLASS_PATH=\${NOVA_DRIVER_CLASS_PATH}:\$CLASSPATH_FROM_SPARK_CONF
		fi
	fi
fi
spark-submit --master local --conf spark.driver.userClassPathFirst=true --class com.onescorpin.spark.SparkShellApp --driver-class-path \$NOVA_DRIVER_CLASS_PATH --driver-java-options -Dlog4j.configuration=log4j-spark.properties $INSTALL_HOME/nova-services/lib/app/nova-spark-shell-client-\${SPARK_PROFILE}-*.jar --pgrep-marker=nova-spark-shell-pgrep-marker
EOF
cat << EOF > $INSTALL_HOME/nova-services/bin/run-nova-spark-shell-with-debug.sh
#!/bin/bash

if ! which spark-submit >/dev/null 2>&1; then
	>&2 echo "ERROR: spark-submit not on path.  Has spark been installed?"
	exit 1
fi

JAVA_DEBUG_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9998
SPARK_PROFILE="v"\$(spark-submit --version 2>&1 | grep -o "version [0-9]" | grep -o "[0-9]" | head -1)
NOVA_DRIVER_CLASS_PATH=$INSTALL_HOME/nova-spark-shell-pgrep-marker:$INSTALL_HOME/nova-services/conf:$INSTALL_HOME/nova-services/lib/mariadb-java-client-1.5.7.jar
if [[ -n \$SPARK_CONF_DIR ]]; then
        if [ -r \$SPARK_CONF_DIR/spark-defaults.conf ]; then
		CLASSPATH_FROM_SPARK_CONF=\$(grep -E '^spark.driver.extraClassPath' \$SPARK_CONF_DIR/spark-defaults.conf | awk '{print \$2}')
		if [[ -n \$CLASSPATH_FROM_SPARK_CONF ]]; then
			NOVA_DRIVER_CLASS_PATH=\${NOVA_DRIVER_CLASS_PATH}:\$CLASSPATH_FROM_SPARK_CONF
		fi
	fi
fi
spark-submit --master local --conf spark.driver.userClassPathFirst=true --class com.onescorpin.spark.SparkShellApp --driver-class-path \$NOVA_DRIVER_CLASS_PATH --driver-java-options "-Dlog4j.configuration=log4j-spark.properties \$JAVA_DEBUG_OPTS" $INSTALL_HOME/nova-services/lib/app/nova-spark-shell-client-\${SPARK_PROFILE}-*.jar --pgrep-marker=nova-spark-shell-pgrep-marker
EOF
chmod +x $INSTALL_HOME/nova-services/bin/run-nova-spark-shell.sh
chmod +x $INSTALL_HOME/nova-services/bin/run-nova-spark-shell-with-debug.sh
echo "   - Created nova-spark-shell script '$INSTALL_HOME/nova-services/bin/run-nova-spark-shell.sh'"

# header of the service file depends on system used
if [ "$linux_type" == "chkonfig" ]; then
cat << EOF > /etc/init.d/nova-spark-shell
#! /bin/sh
# chkconfig: 345 98 20
# description: nova-spark-shell
# processname: nova-spark-shell
EOF
elif [ "$linux_type" == "update-rc.d" ]; then
cat << EOF > /etc/init.d/nova-spark-shell
#! /bin/sh
### BEGIN INIT INFO
# Provides:          nova-spark-shell
# Required-Start:    $local_fs $network $named $time $syslog
# Required-Stop:     $local_fs $network $named $time $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Description:       nova-spark-shell
### END INIT INFO
EOF
fi

cat << EOF >> /etc/init.d/nova-spark-shell
stdout_log="$LOG_DIRECTORY_LOCATION/nova-spark-shell/std.out"
stderr_log="$LOG_DIRECTORY_LOCATION/nova-spark-shell/std.err"
RUN_AS_USER=$INSTALL_USER

start() {
    if pgrep -f $pgrepMarkerNovaSparkShell >/dev/null 2>&1
      then
        echo Already running.
      else
        echo Starting nova-spark-shell ...
        su - \$RUN_AS_USER -c "$INSTALL_HOME/nova-services/bin/run-nova-spark-shell.sh >> \$stdout_log 2>> \$stderr_log" &
    fi
}

stop() {
    if pgrep -f $pgrepMarkerNovaSparkShell >/dev/null 2>&1
      then
        echo Stopping nova-spark-shell ...
        pkill -f $pgrepMarkerNovaSparkShell
      else
        echo Already stopped.
    fi
}

status() {
    if pgrep -f $pgrepMarkerNovaSparkShell >/dev/null 2>&1
      then
          echo Running.  Here are the related processes:
          pgrep -lf $pgrepMarkerNovaSparkShell
      else
        echo Stopped.
    fi
}

case "\$1" in
    start)
        start
    ;;
    stop)
        stop
    ;;
    status)
        status
    ;;
    restart)
       echo "Restarting nova-spark-shell"
       stop
       sleep 2
       start
       echo "nova-spark-shell started"
    ;;
esac
exit 0
EOF
chmod +x /etc/init.d/nova-spark-shell
echo "   - Created nova-spark-shell script '/etc/init.d/nova-spark-shell'"

if [ "$linux_type" == "chkonfig" ]; then
    chkconfig --add nova-spark-shell
    chkconfig nova-spark-shell on
elif [ "$linux_type" == "update-rc.d" ]; then
    update-rc.d nova-spark-shell defaults
fi
echo "   - Added service 'nova-spark-shell'"

mkdir -p $rpmLogDir/nova-spark-shell/
echo "   - Created Log folder $rpmLogDir/nova-spark-shell/"


echo "    - Completed nova-spark-shell install"

{
echo "    - Create an RPM Removal script at: $INSTALL_HOME/remove-nova.sh"
touch $INSTALL_HOME/remove-nova.sh
if [ "$linux_type" == "chkonfig" ]; then
    lastRpm=$(rpm -qa | grep nova)
    echo "rpm -e $lastRpm " > $INSTALL_HOME/remove-nova.sh
elif [ "$linux_type" == "update-rc.d" ]; then
    echo "apt-get remove nova" > $INSTALL_HOME/remove-nova.sh
fi
chmod +x $INSTALL_HOME/remove-nova.sh

}

chown -R $INSTALL_USER:$INSTALL_GROUP $INSTALL_HOME

chmod 755 $rpmLogDir/nova*

chown $INSTALL_USER:$INSTALL_GROUP $rpmLogDir/nova*

# Setup nova-service command
cp $INSTALL_HOME/nova-service /usr/bin/nova-service
chown root:root /usr/bin/nova-service
chmod 755 /usr/bin/nova-service

# Setup nova-tail command
mkdir -p /etc/nova/

echo "   INSTALL COMPLETE"
echo "   - The command nova-service can be used to control and check the Nova services as well as optional services. Use the command nova-service help to find out more information. "
echo "   - Please configure the application using the property files and scripts located under the '$INSTALL_HOME/nova-ui/conf' and '$INSTALL_HOME/nova-services/conf' folder.  See deployment guide for details."
echo "   - To remove nova run $INSTALL_HOME/remove-nova.sh "

