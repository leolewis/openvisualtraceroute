#!/bin/sh
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")
xhost +SI:localuser:root
LIBPCAP08=`ldconfig -p | grep libpcap.so.0.8`
if [ -e $LIBPCAP08 ]; then
  LIBPCAP1=`ldconfig -p | grep libpcap.so.1 | head -1 | awk {'print $4'}`
  if [ `uname -m` == "x86_64" ]; then
    LINK="/usr/lib64/libpcap.so.0.8"
  else 
    LINK="/usr/lib/libpcap.so.0.8"
  fi
  echo "set symbolic link $LINK => $LIBPCAP1"
  ln -s "$LIBPCAP1" "$LINK"
fi
echo "starting OpenVisualTraceroute..."
export PATH="$PATH:/usr/sbin/"; java -Xmx512m -Djava.awt.headless=false -jar $DIR/org.leo.traceroute.jar
xhost -SI:localuser:root