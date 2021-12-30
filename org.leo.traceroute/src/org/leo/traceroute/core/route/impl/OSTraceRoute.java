package org.leo.traceroute.core.route.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.network.INetworkInterfaceListener;
import org.leo.traceroute.core.route.IRouteListener;
import org.leo.traceroute.core.route.MaxHopsException;
import org.leo.traceroute.core.route.RouteException;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OSTraceRoute extends AbstractTraceRoute<INetworkInterfaceListener<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSTraceRoute.class);

    /**
     * Compute the route using OS command
     * @param formatedDest
     * @param monitor
     * @param resolveHostname
     */
    @Override
    public void computeRoute(final String formatedDest, final CancelMonitor monitor, final boolean resolveHostname, final boolean ipV4, final int maxHops)
            throws Exception {
        try {
            String cmd;
            if (Env.INSTANCE.getOs() == OS.win) {
                cmd = "tracert -d -w 1000";
                if (!ipV4) {
                    cmd += " -6";
                }
                cmd += " -h " + maxHops;
            } else {
                cmd = "traceroute";
                if (!ipV4) {
                    cmd += "6";
                }
                cmd += " -q 1 -n";
                cmd += " -m " + maxHops;
            }
            final Process process = Runtime.getRuntime().exec(cmd + " " + formatedDest);
            try {
                final InputStream input = process.getInputStream();
                final int ignoreLines = Env.INSTANCE.getOs() == OS.win ? 4 : (Env.INSTANCE.getOs() == OS.mac ? 0 : 1);
                // check if the host exists
                final String destIp = InetAddress.getByName(formatedDest).getHostAddress();
                int lineNum = 0;
                boolean completed = false;
                RoutePoint previous = null;
                while (!completed && !monitor.isCanceled()) {
                    char c;
                    final StringBuilder linebuffer = new StringBuilder();
                    do {
                        final int r = input.read();
                        if (r == -1) {
                            if (Env.INSTANCE.getOs() == OS.win) {
                                //on windows, we expect a Trace complete to terminate the execution
                                throw new RouteException("Failed to traceroute to host");
                            } else {
                                // but on other OS, that's just an end of stream
                                completed = true;
                                break;
                            }
                        }
                        c = Character.toChars(r)[0];
                        if (c != '\n') {
                            linebuffer.append(c);
                        }
                    } while (c != '\n');
                    //System.out.println(lineNum + ":" + linebuffer);
                    lineNum++;
                    if (lineNum <= ignoreLines) {
                        continue;
                    }
                    if (linebuffer.toString().startsWith("traceroute: Warning: " + formatedDest + " has multiple addresses")) {
                        continue;
                    }
                    if (linebuffer.toString().startsWith("over a maximum")) {
                        continue;
                    }
                    final String line = Util.replaceTs(linebuffer.toString().trim().replaceAll(" +", " "), Env.INSTANCE.getOs() == OS.win ? 3 : 1).replaceAll(" +", " ");
                    if (line.isEmpty()) {
                        continue;
                    }
                    if (line.contains("Trace complete")) {
                        break;
                    }
                    if (monitor.isCanceled()) {
                        break;
                    }
                    if (line.contains("*")) {
                        if (previous != null) {
                            previous = previous.toUnkown();
                            addPoint(previous);
                        }
                        continue;
                    }
                    final String[] routePoint = line.split(" ");
                    final String ip;
                    String host = "";
                    final int latency;
                    int dnslookupTime = DNSLookupService.UNDEF;

                    if (Env.INSTANCE.getOs() == OS.win) {
                        latency = (parseWindowsTime(routePoint[1]) + parseWindowsTime(routePoint[2]) + parseWindowsTime(routePoint[3])) / 3;
//                        if (resolveHostname) {
//                            if (routePoint.length > 5) {
//                                host = routePoint[4];
//                                ip = routePoint[5].replace("[", "").replace("]", "");
//                            } else {
//                                ip = routePoint[4];
//                            }
//                        } else {
                            ip = routePoint[4];
//                        }
                    } else {
//                        if (resolveHostname) {
//                            if (routePoint.length > 3) {
//                                host = routePoint[1];
//                                ip = routePoint[2].replace("(", "").replace(")", "");
//                                latency = (int) Float.parseFloat(routePoint[3]);
//                            } else {
//                                ip = routePoint[1].replace("(", "").replace(")", "");
//                                latency = (int) Float.parseFloat(routePoint[2]);
//                            }
//                        } else {
                            ip = routePoint[1];
                            latency = (int) Float.parseFloat(routePoint[2]);
//                        }
                    }
                    if (resolveHostname) {
                        final long now = System.currentTimeMillis();
                        host = _services.getDnsLookup().dnsLookup(ip);
                        dnslookupTime = (int) (System.currentTimeMillis() - now);
                    }
                    previous = addPoint(Pair.of(ip, host), latency, dnslookupTime);
                }
                if (monitor.isCanceled()) {
                    return;
                }
                final InputStream error = process.getErrorStream();

                final List<String> errors = Util.readUTF8File(error);
                if (!errors.isEmpty()) {
                    final StringBuilder m = new StringBuilder();
                    for (final String e : errors) {
                        // for some reason, this info message is dumped to the error stream, so just ignore it
                        if (!e.startsWith("traceroute to " + formatedDest) && !e.startsWith("traceroute: Warning: " + formatedDest + " has multiple addresses")) {
                            m.append(e).append("\n");
                        }
                    }
                    // notify error
                    if (!m.toString().isEmpty()) {
                        throw new IOException(m.toString());
                    }
                }
                // reached the max hops but not the target iup
                if (previous != null && previous.getNumber() == maxHops && !previous.getIp().equals(destIp)) {
                    throw new MaxHopsException();
                }
            } finally {
                try {
                    process.destroy();
                } catch (final Exception e) {
                    LOGGER.error("Failed to destroy os traceroute process", e);
                }
            }
        } catch (final MaxHopsException e) {
            throw e;
        } catch (final IOException e) {
            throw e;
        } catch (final Exception e) {
            LOGGER.error("error while performing trace route command", e);
        }
    }

    private static int parseWindowsTime(final String str) {
        if ("<1".equals(str)) {
            return 1;
        }
        return Integer.parseInt(str);
    }
}
