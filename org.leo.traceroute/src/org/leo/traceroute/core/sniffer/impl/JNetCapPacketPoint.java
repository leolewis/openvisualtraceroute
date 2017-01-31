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

import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Icmp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.network.Ip6;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;

/**
 * JNetCapPacketPoint $Id$
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class JNetCapPacketPoint extends AbstractPacketPoint {

	private PcapPacket _packet;

	private Ethernet eth;
	private Ip4 ip;
	private Ip6 ip6;
	private Tcp tcp;
	private Udp udp;
	private Icmp icmp;

	/**
	 * @see org.leo.traceroute.core.sniffer.AbstractPacketPoint#getDestPort()
	 */
	@Override
	public Integer getDestPort() {
		if (tcp != null) {
			return tcp.destination();
		} else if (udp != null) {
			return udp.destination();
		}
		return null;
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.AbstractPacketPoint#getSourcePort()
	 */
	@Override
	public Integer getSourcePort() {
		if (tcp != null) {
			return tcp.source();
		} else if (udp != null) {
			return udp.source();
		}
		return null;
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.AbstractPacketPoint#getDataLength()
	 */
	@Override
	public int getDataLength() {
		return _packet.getPacketWirelen();
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.AbstractPacketPoint#buildPayload()
	 */
	@Override
	protected String buildPayload() {
		try {
			final String str = _packet.toString();
			return str.substring(1, str.length());
		} catch (final Exception e) {
			try {
				return _packet.toHexdump();
			} catch (final Exception e2) {
				return "";
			}
		}
	}

	/**
	 * Set the value of the field packet
	 * @param packet the new packet to set
	 */
	public boolean setPacket(final PcapPacket packet) {
		boolean unkown = false;
		_packet = packet;
		eth = packet.getHeader(new Ethernet());
		ip = packet.getHeader(new Ip4());
		tcp = packet.getHeader(new Tcp());
		if (tcp == null) {
			udp = packet.getHeader(new Udp());
			if (udp == null) {
				icmp = packet.getHeader(new Icmp());
				if (icmp == null) {
					unkown = true;
					setProtocol(Protocol.OTHER);
				} else {
					setProtocol(Protocol.ICMP);
				}
			} else {
				setProtocol(Protocol.UDP);
			}
		} else {
			setProtocol(Protocol.TCP);
		}
		return !unkown;
	}

}
