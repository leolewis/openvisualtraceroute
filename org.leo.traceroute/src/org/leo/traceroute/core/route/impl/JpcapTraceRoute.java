/**
 * Open Visual Trace Route
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.leo.traceroute.core.route.impl;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.network.INetworkService;
import org.leo.traceroute.core.route.MaxHopsException;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;

/**
 * Trace Route service $Id: JpcapTraceRoute.java 242 2016-02-21 20:53:16Z leolewis $
 *
 * <pre>
 * Trace route service. Instantiate the Route, initialize it, then add listener
 * and compute a route to the given destination.
 * </pre>
 *
 * @author Leo Lewis
 */
public class JpcapTraceRoute extends AbstractTraceRoute<NetworkInterface> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JpcapTraceRoute.class);

	private byte[] _gatewayMac;
	private volatile JpcapCaptor _captor;
	private InetAddress _deviceIp;
	private volatile JpcapSender _sender;
	private DNSLookupService _dnsService;

	/**
	 * @see org.leo.traceroute.core.route.impl.AbstractTraceRoute#init(org.leo.traceroute.core.ServiceFactory)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void init(final ServiceFactory services) throws IOException {
		super.init(services);
		_dnsService = services.getDnsLookup();
		((INetworkService<NetworkInterface>) services.getJpcapNetwork()).addListener(this);
	}

	/**
	 * Compute the route
	 *
	 * @param dest destination URL
	 * @param monitor cancel monitor
	 * @param resolveHostname if resolve host name (slower)
	 * @param timeout timeout
	 * @param listeners route listeners
	 * @throws IOException
	 * @throws TimeoutException
	 */
	@Override
	protected void computeRoute(final String dest, final CancelMonitor monitor, final boolean resolveHostname, final int maxHops) throws Exception {
		try {
			double timeOut = 1;
			short hop = 0;
			final InetAddress destIp = _dnsService.getIp(dest);

			// open captor and create sender
			openCaptor(timeOut, hop);

			// create ICMP packet
			ICMPPacket packet = createPacket(destIp, hop);
			_sender.sendPacket(packet);
			boolean over = false;
			// while route not over and monitor not canceled
			long previousTime = System.currentTimeMillis();
			String previousIp = _deviceIp.getHostAddress();
			RoutePoint previous = null;
			int x = 1;
			long ts = 0;
			while (!over && !monitor.isCanceled()) {
				final IPPacket p = (IPPacket) _captor.getPacket();
				ts = System.currentTimeMillis();
				if (!over) {
					final ICMPPacket icmpPacket = p instanceof ICMPPacket ? (ICMPPacket) p : null;
					if (icmpPacket == null) {
						// capture timeout
						timeOut = Math.pow(++x, 2);
						final int threshold = calculateThreshold(hop);
						if (timeOut > threshold) {
							if (previous != null) {
								previous = addPoint(previous.toUnkown());
							}
							hop++;
							if (hop > maxHops) {
								throw new MaxHopsException();
							}
							timeOut = 1;
							x = 1;
						}
						openCaptor(timeOut, hop);
						packet = createPacket(destIp, hop);
						_sender.sendPacket(packet);
						previousTime = System.currentTimeMillis();
						continue;
					}
					if (icmpPacket.type == ICMPPacket.ICMP_TIMXCEED) {

					} else if (icmpPacket.type == ICMPPacket.ICMP_ECHOREPLY) {
						over = true;
					} else if (icmpPacket.type == ICMPPacket.ICMP_REDIRECT_TOSHOST) {

					} else {
						continue;
					}
					LOGGER.info(Util.deepToString(icmpPacket));
				}
				hop++;
				if (hop > maxHops + 1) {
					throw new MaxHopsException();
				}
				packet.hop_limit = hop;

				final InetAddress address = p.src_ip;
				final boolean duplicate = address.getHostAddress().equals(previousIp);
				previousIp = address.getHostAddress();
				if (duplicate) {
					previousTime = System.currentTimeMillis();
					continue;
				}
				if (timeOut > 1) {
					timeOut = 1;
					x = 1;
					openCaptor(timeOut, hop);
				}
				String hostname = "";
				final String ip = address.getHostAddress();
				long dnslookup = 0;
				final int latency = (int) (ts - previousTime);
				if (resolveHostname) {
					final long now = System.currentTimeMillis();
					hostname = _services.getDnsLookup().dnsLookup(ip);
					dnslookup = System.currentTimeMillis() - now;
				}
				// add the new route point
				previous = addPoint(Pair.of(ip, hostname), latency, dnslookup);
				previousTime = System.currentTimeMillis();
				_sender.sendPacket(packet);
			}
		} finally {
			// close
			closeCaptor();
		}
	}

	/**
	 * Threshold of timeout, the smaller the hop number, the shorter the timeout (close node should be reachable fast)
	 * @param hop
	 * @return
	 */
	private int calculateThreshold(final short hop) {
		if (hop <= 2) {
			return 100;
		}
		if (hop <= 5) {
			return 500;
		}
		return 1000;
	}

	/**
	 * @return
	 */
	private ICMPPacket createPacket(final InetAddress destIp, final short hop) {
		final ICMPPacket packet = new ICMPPacket();
		packet.type = ICMPPacket.ICMP_ECHO;
		packet.seq = 100;
		packet.id = (short) RandomUtils.nextInt(0, 100);
		packet.setIPv4Parameter(0, false, false, false, 0, false, false, false, 0, 0, 0, IPPacket.IPPROTO_ICMP, _deviceIp, destIp);
		final String data = "ovtr";
		packet.data = data.getBytes();
		final EthernetPacket ether = new EthernetPacket();
		ether.frametype = EthernetPacket.ETHERTYPE_IP;
		ether.src_mac = _device.mac_address;
		ether.dst_mac = _gatewayMac;
		packet.datalink = ether;
		packet.hop_limit = hop;
		return packet;
	}

	private void openCaptor(final double timeOut, final short hop) throws IOException {
		closeCaptor();
		//System.out.println("hop=" + hop + " timeout=" + timeOut);
		_captor = JpcapCaptor.openDevice(_device, 65535, false, (int) Math.round(timeOut));
		_captor.setFilter("icmp and dst host " + _deviceIp.getHostAddress(), true);
		_sender = _captor.getJpcapSenderInstance();
	}

	private void closeCaptor() {
		if (_sender != null) {
			_sender.close();
		}
		if (_captor != null) {
			_captor.close();
		}
		try {
			Thread.sleep(100);
		} catch (final InterruptedException e) {
		}
	}

	/**
	 * @see org.leo.traceroute.core.network.ISnifferNetworkInterfaceListener#notifyNewNetworkInterface(org.jnetpcap.PcapIf)
	 */
	@Override
	public void notifyNewNetworkInterface(final NetworkInterface device, final byte[] mac) {
		super.notifyNewNetworkInterface(device, mac);
		_gatewayMac = mac;
		if (device != null) {
			_deviceIp = getIpOfDevice(device);
		}

	}

	/**
	 * Return the local IPV4 of the given device
	 *
	 * @param device the device
	 * @return the IP
	 */
	public static InetAddress getIpOfDevice(final NetworkInterface device) {
		for (final NetworkInterfaceAddress addr : device.addresses) {
			if (addr.address instanceof Inet4Address) {
				return addr.address;
			}
		}
		return null;
	}

	/**
	 * @see org.leo.traceroute.core.route.impl.AbstractTraceRoute#dispose()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void dispose() {
		if (_services != null) {
			((INetworkService<NetworkInterface>) _services.getJpcapNetwork()).removeListener(this);
		}
		super.dispose();
	}

}
