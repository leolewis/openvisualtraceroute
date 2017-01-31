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
package org.leo.traceroute;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import jpcap.JpcapCaptor;
import jpcap.JpcapSender;
import jpcap.NetworkInterface;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;
import jpcap.packet.TCPPacket;
import jpcap.packet.UDPPacket;

import org.leo.traceroute.core.route.impl.JpcapTraceRoute;
import org.leo.traceroute.util.Util;

/**
 * UDP $Id: UDP.java 127 2015-01-03 08:33:47Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class UDP {

	private final static String TEST_URL = "www.google.com";

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			tcp();
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void udp() throws Exception {
		int sourcePort = 42000;
		final int destinationPort = 33434;

		final InetAddress add = InetAddress.getByName(Util.getPublicIp());
		final NetworkInterface device = JpcapCaptor.getDeviceList()[1];
		final JpcapCaptor captor = JpcapCaptor.openDevice(device, 65535, false, 100);
		final InetAddress pingAddr = InetAddress.getByName(TEST_URL);

		final JpcapSender sender = captor.getJpcapSenderInstance();
		final InetAddress thisIP = JpcapTraceRoute.getIpOfDevice(device);
		final InetAddress dest = InetAddress.getByName(TEST_URL);
		short ttl = 1;
		for (int i = 0; i < 10; i++) {
			captor.setFilter("icmp and dst host " + thisIP.getHostAddress(), true);
			//			captor.setFilter("port " + destinationPort, true);
			//			captor.setFilter("icmp", true);
			final UDPPacket p = new UDPPacket(sourcePort++, destinationPort);
			//			p.setIPv4Parameter(0, false, false, false, 0, false, false, false, 0, 0, 0, IPPacket.IPPROTO_ICMP,
			//					thisIP, InetAddress.getByName("www.google.com"));
			p.setIPv4Parameter(0, false, false, false, 0, false, false, false, 0, 0, ttl++, IPPacket.IPPROTO_UDP,
					thisIP, dest);

			p.data = "ping".getBytes();

			final EthernetPacket ether = new EthernetPacket();
			ether.frametype = EthernetPacket.ETHERTYPE_IP;
			ether.src_mac = new byte[] { (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };
			ether.dst_mac = new byte[] { (byte) 0, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10 };
			p.datalink = ether;
			System.out.println("Send " + p);

			Packet ret = null;
			while (ret == null) {
				sender.sendPacket(p);
				ret = captor.getPacket();
				System.out.println("Got " + ret);
			}
		}
	}

	private static void tcp() throws Exception {
		int sourcePort = 42000;
		final int destinationPort = 33434;

		final InetAddress add = InetAddress.getByName(Util.getPublicIp());
		final NetworkInterface device = JpcapCaptor.getDeviceList()[1];
		final JpcapCaptor captor = JpcapCaptor.openDevice(device, 65535, false, 100);
		final InetAddress pingAddr = InetAddress.getByName(TEST_URL);

		final JpcapSender sender = captor.getJpcapSenderInstance();
		final InetAddress thisIP = JpcapTraceRoute.getIpOfDevice(device);
		final InetAddress dest = InetAddress.getByName(TEST_URL);
		short ttl = 1;
		for (int i = 0; i < 10; i++) {
			captor.setFilter("tcp dst port " + destinationPort + " and src port " + sourcePort, true);
			//			captor.setFilter("port " + destinationPort, true);
			//			captor.setFilter("icmp", true);
			final TCPPacket p = new TCPPacket(sourcePort++, destinationPort, 123456789, 1, false, false, false, false,
					true, true, true, true, 10, 10);

			//			p.setIPv4Parameter(0, false, false, false, 0, false, false, false, 0, 0, 0, IPPacket.IPPROTO_ICMP,
			//					thisIP, InetAddress.getByName("www.google.com"));
			p.setIPv4Parameter(0, false, false, false, 0, false, false, false, 0, 0, ttl++, IPPacket.IPPROTO_TCP,
					thisIP, dest);

			p.data = "ping".getBytes();

			final EthernetPacket ether = new EthernetPacket();
			ether.frametype = EthernetPacket.ETHERTYPE_IP;
			ether.src_mac = new byte[] { (byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };
			ether.dst_mac = new byte[] { (byte) 0, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10 };
			p.datalink = ether;
			System.out.println("Send " + p);

			Packet ret = null;
			while (ret == null) {
				sender.sendPacket(p);
				ret = captor.getPacket();
				if (ret instanceof TCPPacket) {
					final TCPPacket tcp = (TCPPacket) ret;
				}
				System.out.println("Got " + ret);
			}
		}
	}

	private static void test() throws Exception {
		final String hostname = "google.com";
		final String localhost = "localhost";
		final MulticastSocket datagramSocket = new MulticastSocket();
		datagramSocket.setSoTimeout(10000);
		short ttl = 1;
		final InetAddress receiverAddress = InetAddress.getByName(hostname);
		while (ttl < 100) {
			try {
				byte[] buffer = "0123456789".getBytes();
				datagramSocket.setTimeToLive(ttl++);
				final DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, receiverAddress, 80);

				datagramSocket.send(sendPacket);

				buffer = new byte[10];
				final DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

				datagramSocket.receive(receivePacket);
				System.out.println("ttl=" + ttl + " address=" + receivePacket.getAddress().getHostAddress() + " data="
						+ new String(receivePacket.getData()));
				Thread.sleep(1000);
			} catch (final SocketTimeoutException e) {
				System.out.println("timeout ttl=" + ttl);
			}
		}
	}

}
