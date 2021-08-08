#!/bin/sh
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")
#setcap cap_net_raw,cap_net_admin=eip /path/to/java
sudo -E $DIR/ovtr_run_as_root.sh