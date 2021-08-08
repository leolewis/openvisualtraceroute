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
//import java.io.IOException;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.nio.ByteOrder;
//
//import org.apache.commons.lang3.tuple.Pair;
//import org.jnetpcap.Pcap;
//import org.jnetpcap.PcapAddr;
//import org.jnetpcap.PcapBpfProgram;
//import org.jnetpcap.PcapHeader;
//import org.jnetpcap.PcapIf;
//import org.jnetpcap.nio.JBuffer;
//import org.jnetpcap.nio.JMemory;
//import org.jnetpcap.packet.JMemoryPacket;
//import org.jnetpcap.packet.JPacket;
//import org.jnetpcap.packet.JRegistry;
//import org.jnetpcap.packet.Payload;
//import org.jnetpcap.packet.PcapPacket;
//import org.jnetpcap.protocol.lan.Ethernet;
//import org.jnetpcap.protocol.network.Icmp;
//import org.jnetpcap.protocol.network.Icmp.IcmpType;
//import org.jnetpcap.protocol.network.Ip4;
//import org.jnetpcap.protocol.tcpip.Tcp;
//import org.leo.traceroute.core.ServiceFactory;
//import org.leo.traceroute.core.network.INetworkService;
//import org.leo.traceroute.ui.task.CancelMonitor;
//
///**
// * Trace Route service $Id: JNetCapTraceRoute.java 103 2014-05-04 10:57:42Z leolewis $
// *
// * <pre>
// * Trace route service. Instantiate the Route, initialize it, then add listener
// * and compute a route to the given destination.
// * </pre>
// *
// * @author Leo Lewis
// */
//public class JNetCapTraceRoute extends AbstractTraceRoute<PcapIf> {
//
//	/**
//	 * @see org.leo.traceroute.core.route.impl.AbstractTraceRoute#init(org.leo.traceroute.core.ServiceFactory)
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public void init(final ServiceFactory services) throws IOException {
//		super.init(services);
//		((INetworkService<PcapIf>) _services.getNetworkService()).addListener(this);
//	}
//
//	/**
//	 * Compute the route
//	 *
//	 * @param dest destination URL
//	 * @param monitor cancel monitor
//	 * @param resolveHostname if resolve host name (slower)
//	 * @throws IOException
//	 */
//	@Override
//	protected void computeRoute(final String dest, final CancelMonitor monitor, final boolean resolveHostname, final int maxHops) throws Exception {
//		// use the device that we found previously
//		final StringBuilder err = new StringBuilder();
//		final Pcap captor = Pcap.openLive(_device.getName(), 65535, Pcap.MODE_NON_PROMISCUOUS, 100, err);
//		if (captor == null) {
//			throw new IOException(err.toString());
//		}
//		try {
//			final InetAddress destIp = InetAddress.getByName(dest);
//
//			final InetAddress sourceIP = getIpOfDevice(_device);
//
//			final PcapBpfProgram filter = new PcapBpfProgram();
//			final int r = captor.compile(filter, "tcp and dst host " + sourceIP.getHostAddress(), 1, 0x0);
//			//			captor.setFilter(filter);
//
//			boolean over = false;
//			long previousTime = System.currentTimeMillis();
//			final JMemoryPacket sendPacket = creatPacket(destIp, sourceIP);
//
//			sendPacket(captor, sendPacket);
//
//			final int id = JRegistry.mapDLTToId(captor.datalink());
//			final PcapHeader header = new PcapHeader(JMemory.POINTER);
//			final JBuffer buff = new JBuffer(JMemory.POINTER);
//			// while route not over and monitor not canceled
//			while (!over && !monitor.isCanceled()) {
//				if (captor.nextEx(header, buff) != Pcap.NEXT_EX_OK) {
//					continue;
//				}
//				final PcapPacket packet = new PcapPacket(header, buff);
//				packet.scan(id);
//
//				final Ip4 receivedIP = packet.getHeader(new Ip4());
//				final Icmp receivedIcmp = packet.getHeader(new Icmp());
//				if (receivedIcmp == null || receivedIcmp.type() == IcmpType.TIME_EXCEEDED_ID || receivedIcmp.type() == IcmpType.REDIRECT_ID) {
//					// inc ttl
//					final Ip4 ip = sendPacket.getHeader(new Ip4());
//					ip.ttl(ip.ttl() + 1);
//				} else if (receivedIcmp.type() == IcmpType.ECHO_REPLY_ID) {
//					over = true;
//				} else {
//					continue;
//				}
//
//				final InetAddress address = InetAddress.getByAddress(receivedIP.destination());
//				String hostname = "";
//				final String ip = address.getHostAddress();
//				long dnslookup = 0;
//				final int latency = (int) (System.currentTimeMillis() - previousTime);
//				if (resolveHostname) {
//					final long now = System.currentTimeMillis();
//					hostname = _services.getDnsLookup().dnsLookup(ip);
//					dnslookup = System.currentTimeMillis() - now;
//				}
//				// add the new route point
//				addPoint(Pair.of(ip, hostname), latency, dnslookup);
//				previousTime = System.currentTimeMillis();
//				sendPacket(captor, sendPacket);
//			}
//		} finally {
//			// close
//			if (captor != null) {
//				captor.breakloop();
//				captor.close();
//			}
//		}
//	}
//
//	private JMemoryPacket creatPacket(final InetAddress destIp, final InetAddress sourceIP) {
//		final int packetSize = 500;
//		final JMemoryPacket packet = new JMemoryPacket(packetSize);
//		packet.order(ByteOrder.BIG_ENDIAN);
//		packet.setUShort(0 + 12, 0x800); // ethernet.type field
//		packet.scan(Ethernet.ID);
//
//		final Ethernet ethernet = packet.getHeader(new Ethernet());
//		ethernet.destination(destIp.getAddress());
//		ethernet.source(sourceIP.getAddress());
//
//		packet.setUByte(14, 0x04 | 0x05); // ip v4
//		packet.scan(Ethernet.ID);
//
//		//			final JPacket sendPacket = new JMemoryPacket(JProtocol.ETHERNET_ID, " 001801bf 6adc0025 4bb7afec 08004500 "
//		//					+ " 0041a983 40004006 d69ac0a8 00342f8c " + " ca30c3ef 008f2e80 11f52ea8 4b578018 "
//		//					+ " ffffa6ea 00000101 080a152e ef03002a " + " 2c943538 322e3430 204e4f4f 500d0a");
//
//		final Ip4 sendPacketIP = packet.getHeader(new Ip4());
//		//			sendPacketIP.destination(destIp.getAddress());
//		sendPacketIP.type(0x06); //TCP
//		sendPacketIP.length(packetSize - ethernet.size());
//		sendPacketIP.ttl(1);
//
//		packet.setUByte(46, 0x50); // TCP
//		packet.scan(Ethernet.ID);
//
//		final Tcp sendPacketTcp = packet.getHeader(new Tcp());
//		sendPacketTcp.destination(80);
//		sendPacketIP.checksum(sendPacketIP.calculateChecksum());
//		sendPacketTcp.checksum(sendPacketTcp.calculateChecksum());
//
//		final Payload payload = packet.getHeader(new Payload());
//		payload.setByteArray(1, "Neko".getBytes());
//		return packet;
//	}
//
//	private void sendPacket(final Pcap captor, final JPacket sendPacket) throws IOException {
//		if (captor.sendPacket(sendPacket) != Pcap.OK) {
//			throw new IOException(captor.getErr());
//		}
//	}
//
//	/**
//	 * Return the local IPV4 of the given device
//	 *
//	 * @param device the device
//	 * @return the IP
//	 */
//	public static InetAddress getIpOfDevice(final PcapIf device) {
//		for (final PcapAddr addr : device.getAddresses()) {
//			try {
//				final InetAddress net = InetAddress.getByAddress(addr.getAddr().getData());
//				if (net instanceof Inet4Address) {
//					return net;
//				}
//			} catch (final UnknownHostException e) {
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * @see org.leo.traceroute.core.route.impl.AbstractTraceRoute#dispose()
//	 */
//	@SuppressWarnings("unchecked")
//	@Override
//	public void dispose() {
//		if (_services != null) {
//			_services.getNetworkService().removeListener(this);
//		}
//		super.dispose();
//	}
//}
