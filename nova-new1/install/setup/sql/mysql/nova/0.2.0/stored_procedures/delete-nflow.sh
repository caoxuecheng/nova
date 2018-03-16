#!/usr/bin/env bash
mysql -h $1 -u $2 --password=$3  -e "CALL nova.delete_nflow('$3','$4');"
