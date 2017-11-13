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
package org.leo.traceroute.core.sniffer.impl;

import org.leo.traceroute.core.sniffer.AbstractPacketPoint;

import jpcap.packet.DatalinkPacket;
import jpcap.packet.EthernetPacket;
import jpcap.packet.ICMPPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.TCPPacket;
import jpcap.packet.UDPPacket;

/**
 * JPcapPacket $Id$
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class JPcapPacketPoint extends AbstractPacketPoint {

	private static final int HEX_PER_LINE = 30;

	private IPPacket _packet;

	/**
	 * Return the value of the field packet
	 * @return the value of packet
	 */
	public IPPacket getPacket() {
		return _packet;
	}

	/**
	 * Set the value of the field packet
	 * @param packet the new packet to set
	 */
	public void setPacket(final IPPacket packet) {
		_packet = packet;
	}

	@Override
	public Integer getSourcePort() {
		if (_packet instanceof UDPPacket) {
			return ((UDPPacket) _packet).src_port;
		} else if (_packet instanceof TCPPacket) {
			return ((TCPPacket) _packet).src_port;
		}
		return null;
	}

	@Override
	public Integer getDestPort() {
		if (_packet instanceof UDPPacket) {
			return ((UDPPacket) _packet).dst_port;
		} else if (_packet instanceof TCPPacket) {
			return ((TCPPacket) _packet).dst_port;
		}
		return null;
	}

	@Override
	public int getDataLength() {
		return _packet.data.length;
	}

	@Override
	protected String buildPayload() {
		final IPPacket p = getPacket();
		final StringBuilder sb = new StringBuilder();
		final EthernetPacket ethernet = (EthernetPacket) p.datalink;
		sb.append("=====================================\n");
		sb.append("= " + getProtocol() + " Packet [Src IP=" + p.src_ip.getHostAddress() + " Dest IP=" + p.dst_ip.getHostAddress() + "]\n");
		sb.append("= Data length :" + getDataLength() + " bytes\n");
		//		sb.append("= Priority :" + (int) p.priority + "\n");
		sb.append("= Src mac :" + ethernet.getSourceAddress() + "\n");
		sb.append("= Dest mac :" + ethernet.getDestinationAddress() + "\n");
		//		sb.append("= Service :" + p.flow_label + "\n");
		if (p instanceof ICMPPacket) {
			final ICMPPacket packet = (ICMPPacket) p;
			sb.append("= Type :" + typeToString(packet.type) + "\n");
			sb.append(toHex(packet.data));
		} else if (p instanceof UDPPacket) {
			final UDPPacket packet = (UDPPacket) p;
			sb.append("= Src port :" + packet.src_port + " Dest port :" + packet.dst_port + "\n");
			sb.append(toHex(packet.data));
		} else if (p instanceof TCPPacket) {
			final TCPPacket packet = (TCPPacket) p;
			sb.append("= Src port :" + packet.src_port + " Dest port :" + packet.dst_port + "\n");
			String data;
			if (((TCPPacket) p).dst_port != 80) {
				data = toHex(packet.data);
			} else {
				data = new String(packet.data);
			}
			sb.append(data);
		}
		return sb.toString();
	}

	public String toHex(final byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			final byte b = bytes[i];
			final int j = b & 0xff;
			String subString = Integer.toHexString(j);
			if (subString.length() == 1) {
				subString = "0" + subString;
			}
			sb.append(subString + " ");
			if ((i + 1) % HEX_PER_LINE == 0) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	private String typeToString(final byte type) {
		switch (type) {
		case ICMPPacket.ICMP_ECHO:
			return "ECHO";
		case ICMPPacket.ICMP_ECHOREPLY:
			return "ECHOREPLY";
		case ICMPPacket.ICMP_IREQ:
			return "IREQ";
		case ICMPPacket.ICMP_IREQREPLY:
			return "IREQREPLY";
		case ICMPPacket.ICMP_MASKREPLY:
			return "MASKREPLY";
		case ICMPPacket.ICMP_MASKREQ:
			return "MASKREQ";
		case ICMPPacket.ICMP_PARAMPROB:
			return "PARAMPROB";
		case ICMPPacket.ICMP_REDIRECT:
			return "REDIRECT";
		case ICMPPacket.ICMP_TIMXCEED:
			return "TIMXCEED";
		case ICMPPacket.ICMP_TSTAMP:
			return "TSTAMP";
		case ICMPPacket.ICMP_UNREACH:
			return "UNREACH";
		}
		return "";
	}

	/**
	 * @param tpt
	 */
	private void print(final IPPacket tpt, final int c) {
		System.out.println("Packet " + c);
		final int ppp = tpt.protocol;
		System.out.println("dest ip :" + tpt.dst_ip);
		System.out.println("source ip :" + tpt.src_ip);
		System.out.println("hop limit :" + tpt.hop_limit);
		System.out.println("identification field  :" + tpt.ident);
		System.out.println("packet length :" + tpt.length);
		System.out.println("packet priority  :" + (int) tpt.priority);
		System.out.println("type of service field" + tpt.rsv_tos);

		if (tpt.r_flag) {
			System.out.println("reliable transmission");
		} else {
			System.out.println("not reliable");
		}
		System.out.println("protocol version : " + (int) tpt.version);
		System.out.println("flow label field" + tpt.flow_label);

		final DatalinkPacket dp = tpt.datalink;
		final EthernetPacket ept = (EthernetPacket) dp;
		System.out.println("Destination mac address :" + ept.getDestinationAddress());
		System.out.println("Source mac address" + ept.getSourceAddress());

		if (ppp == IPPacket.IPPROTO_TCP) {
			System.out.println("TCP packet");
			final TCPPacket tp = (TCPPacket) tpt;
			System.out.println("destination port of tcp :" + tp.dst_port);
			if (tp.ack) {
				System.out.println("acknowledgement");
			} else {
				System.out.println("not an acknowledgment packet");
			}

			if (tp.rst) {
				System.out.println("reset connection ");
			}
			System.out.println("protocol version is :" + tp.version);
			System.out.println("destination ip " + tp.dst_ip);
			System.out.println("source ip" + tp.src_ip);
			if (tp.fin) {
				System.out.println("sender does not have more data to transfer");
			}
			if (tp.syn) {
				System.out.println("request for connection");
			}

		} else if (ppp == IPPacket.IPPROTO_ICMP) {
			final ICMPPacket ipc = (ICMPPacket) tpt;
			System.out.println("ICMP packet");
			// java.net.InetAddress[] routers=ipc.router_ip;
			//for(int t=0;t
			//  System.out.println("\n"+routers[t]);
			// }
			System.out.println("alive time :" + ipc.alive_time);
			System.out.println("number of advertised address :" + (int) ipc.addr_num);
			System.out.println("mtu of the packet is :" + (int) ipc.mtu);
			System.out.println("subnet mask :" + ipc.subnetmask);
			System.out.println("source ip :" + ipc.src_ip);
			System.out.println("destination ip:" + ipc.dst_ip);
			System.out.println("check sum :" + ipc.checksum);
			System.out.println("icmp type :" + ipc.type);
		} else if (ppp == IPPacket.IPPROTO_UDP) {
			final UDPPacket pac = (UDPPacket) tpt;
			System.out.println("UDP packet");
			System.out.println("source port :" + pac.src_port);
			System.out.println("destination port :" + pac.dst_port);
		}
		System.out.println("******************************************************");
	}
}
