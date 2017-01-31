#!/bin/sh
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")
gksudo -u root "$DIR/ovtr_run_as_root.sh"