#!/bin/sh
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")
echo "starting OpenVisualTraceroute..."
export PATH="$PATH:/usr/sbin/"; java -Xmx512m -Djava.awt.headless=false -jar $DIR/org.leo.traceroute.jar --illegal-access=warn
