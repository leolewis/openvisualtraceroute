#!/bin/sh
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")
sudo -E $DIR/ovtr_run_as_root.sh 