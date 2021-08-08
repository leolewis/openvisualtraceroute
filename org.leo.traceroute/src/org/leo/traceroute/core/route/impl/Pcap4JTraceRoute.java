///**
// * Open Visual Trace Route
// *
// * This library is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 3 of the License, or (at your option) any later version.
//
// * This library is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
//
// * You should have received a copy of the GNU Lesser General Public
// * License along with this library; if not, write to the Free Software
// * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
// */
//package org.leo.traceroute.core.route.impl;
//
//import org.apache.commons.lang3.RandomUtils;
//import org.apache.commons.lang3.tuple.Pair;
//import org.leo.traceroute.core.ServiceFactory;
//import org.leo.traceroute.core.ServiceFactory.Mode;
//import org.leo.traceroute.core.network.DNSLookupService;
//import org.leo.traceroute.core.route.MaxHopsException;
//import org.leo.traceroute.core.route.RoutePoint;
//import org.leo.traceroute.ui.task.CancelMonitor;
//import org.leo.traceroute.util.Util;
//import org.pcap4j.core.*;
//import org.pcap4j.core.BpfProgram.BpfCompileMode;
//import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
//import org.pcap4j.packet.*;
//import org.pcap4j.packet.namednumber.IcmpV4Code;
//import org.pcap4j.packet.namednumber.IcmpV4Type;
//import org.pcap4j.packet.namednumber.IpNumber;
//import org.pcap4j.packet.namednumber.IpVersion;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//
///**
// * Trace Route service
// *
// * <pre>
// * Trace route service. Instantiate the Route, initialize it, then add listener
// * and compute a route to the given destination.
// * </pre>
// *
// * @author Leo Lewis
// */
//public class Pcap4JTraceRoute extends AbstractTraceRoute<PcapNetworkInterface> {
//
//	private static final Logger LOGGER = LoggerFactory.getLogger(Pcap4JTraceRoute.class);
//
//	private byte[] _gatewayMac;
//	private PcapHandle _captor;
//	private PcapHandle _sender;
//	private InetAddress _deviceIp;
//	private DNSLookupService _dnsService;
//
//	/**
//	 * @see AbstractTraceRoute#init(ServiceFactory)
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public void init(final ServiceFactory services) throws IOException {
//		super.init(services);
//		_dnsService = services.getDnsLookup();
//		services.getNetworkService().addListener(this);
//	}
//
//	/**
//	 * Compute the route
//	 *
//	 * @param dest destination URL
//	 * @param monitor cancel monitor
//	 * @param resolveHostname if resolve host name (slower)
//	 * @throws Exception
//	 */
//	@Override
//	protected void computeRoute(final String dest, final CancelMonitor monitor, final boolean resolveHostname, final int maxHops) throws Exception {
//		try {
//			double timeOut = 1000;
//			short hop = 0;
//			final InetAddress destIp = _dnsService.getIp(dest);
//
//			// open captor and create sender
//			openCaptor(timeOut, hop);
//
//			// create ICMP packet
//			Packet send = createPacket(destIp, hop);
//			_sender.sendPacket(send);
//			boolean over = false;
//			// while route not over and monitor not canceled
//			long previousTime = System.currentTimeMillis();
//			String previousIp = _deviceIp.getHostAddress();
//			RoutePoint previous = null;
//			int x = 1;
//			long ts = 0;
//
//			while (!over && !monitor.isCanceled()) {
//				final PcapPacket received = _captor.getNextPacket();
//				ts = System.currentTimeMillis();
//				if (!over) {
//					if (received == null) {
//						// capture timeout
//						timeOut = Math.pow(++x, 2);
//						final int threshold = calculateThreshold(hop);
//						if (timeOut > threshold) {
//							if (previous != null) {
//								previous = addPoint(previous.toUnkown());
//							}
//							hop++;
//							if (hop > maxHops) {
//								throw new MaxHopsException();
//							}
//							timeOut = 1;
//							x = 1;
//						}
//						//openCaptor(timeOut, hop);
//						send = createPacket(destIp, hop);
//						_sender.sendPacket(send);
//						previousTime = System.currentTimeMillis();
//						continue;
//					}
//					if (received.get(IcmpV4TimeExceededPacket.class) == null) {
//
//					} else if (received.get(IcmpV4EchoPacket.class) != null) {
//						over = true;
//					} else if (received.get(IcmpV4RedirectPacket.class) != null) {
//
//					} else {
//						LOGGER.info("T" + Util.deepToString(received));
//						continue;
//					}
//					LOGGER.info(Util.deepToString(received));
//				}
//				hop++;
//				if (hop > maxHops + 1) {
//					throw new MaxHopsException();
//				}
//				send = createPacket(destIp, hop);
//
//				final InetAddress address = received.getPacket().get(IpV4Packet.class).getHeader().getSrcAddr();
//				final boolean duplicate = address.getHostAddress().equals(previousIp);
//				previousIp = address.getHostAddress();
//				if (duplicate) {
//					previousTime = System.currentTimeMillis();
//					continue;
//				}
//				if (timeOut > 1) {
//					timeOut = 1;
//					x = 1;
//					openCaptor(timeOut, hop);
//				}
//				String hostname = "";
//				final String ip = address.getHostAddress();
//				long dnslookup = 0;
//				final int latency = (int) (ts - previousTime);
//				if (resolveHostname) {
//					final long now = System.currentTimeMillis();
//					hostname = _services.getDnsLookup().dnsLookup(ip);
//					dnslookup = System.currentTimeMillis() - now;
//				}
//				// add the new route point
//				previous = addPoint(Pair.of(ip, hostname), latency, dnslookup);
//				previousTime = System.currentTimeMillis();
//				_sender.sendPacket(send);
//			}
//		} finally {
//			// close
//			closeCaptor();
//		}
//	}
//
//	private void gotPacket(PcapPacket packet) {
//
//	}
//
//	/**
//	 * Threshold of timeout, the smaller the hop number, the shorter the timeout (close node should be reachable fast)
//	 * @param hop
//	 * @return
//	 */
//	private int calculateThreshold(final short hop) {
//		if (hop <= 2) {
//			return 100;
//		}
//		if (hop <= 5) {
//			return 500;
//		}
//		return 1000;
//	}
//
//	/**
//	 * @return
//	 */
//	private Packet createPacket(final InetAddress destIp, final short hop) {
//		IcmpV4EchoPacket.Builder echoBuilder = new IcmpV4EchoPacket.Builder();
//		echoBuilder
//				.identifier((short) RandomUtils.nextInt(0, 100))
//				.payloadBuilder(new UnknownPacket.Builder().rawData("ovtr".getBytes()));
//
//		IcmpV4CommonPacket.Builder icmpV4CommonBuilder = new IcmpV4CommonPacket.Builder();
//		icmpV4CommonBuilder
//				.type(IcmpV4Type.ECHO)
//				.code(IcmpV4Code.NO_CODE)
//				.payloadBuilder(echoBuilder)
//				.correctChecksumAtBuild(true);
//
//		IpV4Packet.Builder ipV4Builder = new IpV4Packet.Builder()
//				.version(IpVersion.IPV4)
//				.tos(IpV4Rfc791Tos.newInstance((byte) 0))
//				.ttl((byte) hop)
//				.protocol(IpNumber.ICMPV4)
//				.srcAddr((Inet4Address) _deviceIp)
//				.dstAddr((Inet4Address) destIp)
//				.payloadBuilder(icmpV4CommonBuilder)
//				.correctChecksumAtBuild(true)
//				.correctLengthAtBuild(true);
//
//		return ipV4Builder.build();
//	}
//
//	private void openCaptor(final double timeOut, final short hop) throws IOException {
//		closeCaptor();
//		try {
//			_sender = _device.openLive(65535, PromiscuousMode.PROMISCUOUS, (int) Math.round(timeOut));
//
//			_captor = _device.openLive(65535, PromiscuousMode.PROMISCUOUS, (int) Math.round(timeOut));
//			_captor.setFilter("icmp and dst host " + _deviceIp.getHostAddress(), BpfCompileMode.OPTIMIZE);
//
//		} catch (Exception e) {
//			throw new IOException("Failed to open interface " + _device.getName(), e);
//		}
//	}
//
//	private void closeCaptor() {
//		if (_sender != null) {
//			_sender.close();
//		}
//		if (_captor != null) {
//			_captor.close();
//		}
//		try {
//			Thread.sleep(100);
//		} catch (final InterruptedException e) {
//		}
//	}
//
//	@Override
//	public void notifyNewNetworkInterface(final PcapNetworkInterface device, Mode mode, final byte[] mac) {
//		if (mode != Mode.TRACE_ROUTE) {
//			return;
//		}
//		super.notifyNewNetworkInterface(device, mode, mac);
//		_gatewayMac = mac;
//		if (device != null) {
//			_deviceIp = getIpOfDevice(device);
//		}
//
//	}
//
//	/**
//	 * Return the local IPV4 of the given device
//	 *
//	 * @param device the device
//	 * @return the IP
//	 */
//	public static InetAddress getIpOfDevice(final PcapNetworkInterface device) {
//		for (final PcapAddress addr : device.getAddresses()) {
//			if (addr.getAddress() instanceof Inet4Address) {
//				return addr.getAddress();
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * @see AbstractTraceRoute#dispose()
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public void dispose() {
//		if (_services != null) {
//			_services.getNetworkService().removeListener(this);
//		}
//		super.dispose();
//	}
//
//}
