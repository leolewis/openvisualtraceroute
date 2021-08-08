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
//package org.leo.traceroute.core.sniffer.impl;
//
//import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
//import org.pcap4j.core.PcapPacket;
//import org.pcap4j.packet.*;
//import org.pcap4j.packet.EthernetPacket.EthernetHeader;
//import org.pcap4j.packet.IcmpV4CommonPacket.IcmpV4CommonHeader;
//import org.pcap4j.packet.IpV4Packet.IpV4Header;
//import org.pcap4j.packet.IpV6Packet.IpV6Header;
//import org.pcap4j.packet.TcpPacket.TcpHeader;
//import org.pcap4j.packet.UdpPacket.UdpHeader;
//
///**
// * JNetCapPacketPoint $Id$
// * <pre>
// * </pre>
// * @author Leo Lewis
// */
//public class Pcap4JPacketPoint extends AbstractPacketPoint {
//
//	private PcapPacket _packet;
//
//	private EthernetHeader eth;
//	private IpV4Header ip;
//	private IpV6Header ip6;
//	private TcpHeader tcp;
//	private UdpHeader udp;
//	private IcmpV4CommonHeader icmp;
//
//	/**
//	 * @see AbstractPacketPoint#getDestPort()
//	 */
//	@Override
//	public Integer getDestPort() {
//		if (tcp != null) {
//			return tcp.getDstPort().valueAsInt();
//		} else if (udp != null) {
//			return udp.getDstPort().valueAsInt();
//		}
//		return null;
//	}
//
//	/**
//	 * @see AbstractPacketPoint#getSourcePort()
//	 */
//	@Override
//	public Integer getSourcePort() {
//		if (tcp != null) {
//			return tcp.getSrcPort().valueAsInt();
//		} else if (udp != null) {
//			return udp.getSrcPort().valueAsInt();
//		}
//		return null;
//	}
//
//	/**
//	 * @see AbstractPacketPoint#getDataLength()
//	 */
//	@Override
//	public int getDataLength() {
//		return _packet.getOriginalLength();
//	}
//
//	/**
//	 * @see AbstractPacketPoint#buildPayload()
//	 */
//	@Override
//	protected String buildPayload() {
//		try {
//			final String str = _packet.toString();
//			return str.substring(1, str.length());
//		} catch (final Exception e) {
//			try {
//				return _packet.toHexString();
//			} catch (final Exception e2) {
//				return "";
//			}
//		}
//	}
//
//	/**
//	 * Set the value of the field packet
//	 * @param packet the new packet to set
//	 */
//	public boolean setPacket(final PcapPacket packet) {
//		boolean unkown = false;
//		_packet = packet;
//		eth = packet.get(EthernetPacket.class).getHeader();
//		ip = packet.get(IpV4Packet.class).getHeader();
//		tcp = packet.get(TcpPacket.class).getHeader();
//		if (tcp == null) {
//			udp = packet.get(UdpPacket.class).getHeader();
//			if (udp == null) {
//				icmp = packet.get(IcmpV4CommonPacket.class).getHeader();
//				if (icmp == null) {
//					unkown = true;
//					setProtocol(Protocol.OTHER);
//				} else {
//					setProtocol(Protocol.ICMP);
//				}
//			} else {
//				setProtocol(Protocol.UDP);
//			}
//		} else {
//			setProtocol(Protocol.TCP);
//		}
//		return !unkown;
//	}
//
//}
