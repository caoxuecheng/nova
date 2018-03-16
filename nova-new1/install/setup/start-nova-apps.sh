#!/bin/bash
CMD=${1:-start}
service nova-ui $CMD
service nova-services $CMD
service nova-spark-shell start
