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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.leo.traceroute.core.IComponent;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.network.INetworkInterfaceListener;
import org.leo.traceroute.core.network.INetworkService;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint.Protocol;
import org.leo.traceroute.core.sniffer.IPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

/**
 * Packet Sniffer $Id: JPcapPacketSniffer.java 102 2014-05-04 07:16:40Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
@Deprecated
public class JPcapPacketSniffer extends AbstractSniffer implements PacketReceiver, IComponent, INetworkInterfaceListener<NetworkInterface> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JPcapPacketSniffer.class);
	private static final int RESET = 500;

	/** Current device */
	private NetworkInterface _device;

	/** JPcap */
	private volatile JpcapCaptor _captor;
	private volatile AtomicBoolean _reinitCapture = new AtomicBoolean();
	private int _packetLenghtFilter;
	private ExecutorService _executor;

	/**
	 * Constructor
	 */
	public JPcapPacketSniffer() {

	}

	/**
	 * @see org.leo.traceroute.core.IComponent#init(org.leo.traceroute.core.ServiceFactory)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void init(final ServiceFactory services) throws Exception {
		super.init(services);
		if (_services != null) {
			((INetworkService<NetworkInterface>) _services.getJpcapNetwork()).addListener(this);
		}
		final Thread t = new Thread(() -> {
			while (true) {
				if (_capturing && _reinitCapture.getAndSet(false)) {
					LOGGER.info("Reinit capture");
					_threadPool.execute(() -> doStartCapture());
				} else {
					try {
						Thread.sleep(100);
					} catch (final InterruptedException e) {
					}
				}
			}
		});
		t.setName("Capture reinit");
		t.setDaemon(true);
		t.start();
	}

	/**
	 * @see jpcap.PacketReceiver#receivePacket(jpcap.packet.Packet)
	 */
	@Override
	public void receivePacket(final Packet packet) {
		if (!_capturing) {
			return;
		}
		try {
			if (packet instanceof IPPacket) {
				final IPPacket tpt = (IPPacket) packet;
				if (_filterLenghtPackets && tpt.data.length < _packetLenghtFilter) {
					return;
				}
				final int ppp = tpt.protocol;
				Protocol protocol = null;
				if (ppp == IPPacket.IPPROTO_TCP) {
					protocol = Protocol.TCP;
				} else if (ppp == IPPacket.IPPROTO_ICMP) {
					protocol = Protocol.ICMP;
				} else if (ppp == IPPacket.IPPROTO_UDP) {
					protocol = Protocol.UDP;
				}
				// to workaround jpcap bug
				if (!_captureProtocols.contains(protocol)) {
					return;
				}
				final InetAddress dest = tpt.dst_ip;
				JPcapPacketPoint point;
				// packets with dest to local device
				if (_localAddresses.contains(dest)) {
					point = _services.getGeo().populateGeoDataForLocalIp(new JPcapPacketPoint(), dest.getHostAddress());
				} else {
					point = _services.getGeo().populateGeoDataForIP(new JPcapPacketPoint(), dest.getHostAddress(), dest.getHostName());
				}
				if (point != null) {
					point.setProtocol(protocol);
					final int c = _count.incrementAndGet();
					point.setNumber(c);
					point.setHostname(_services.getDnsLookup().dnsLookup(point.getIp()));
					point.setPacket(tpt);
					point.setTs(System.currentTimeMillis());

					_capture.add(point);
					//					print(tpt, c);
					for (final IPacketListener listener : getListeners()) {
						listener.packetAdded(point);
					}
				}
			} else {
				System.out.println(packet);
			}
		} catch (final Exception e) {
			for (final IPacketListener listener : getListeners()) {
				listener.error(e, this);
			}
		}
		// reinit the captor to avoid crash on JPacap side
		if (_count.get() % 10 == 0) {
			LOGGER.info("Captured packets : " + _count.get());
		}
		if ((_count.get() + 1) % RESET == 0) {
			//_reinitCapture.set(true);
			System.gc();
			return;
		}
	}

	/**
	 * Start the capture
	 * @param types
	 * @param port
	 */
	@Override
	public void startCapture(final Set<Protocol> protocols, final String port, final boolean filterLenghtPackets, final int length, final String host,
			final int captureTimeSeconds) {
		_focusedPoint = null;
		_count.set(0);
		_capture.clear();
		_packetLenghtFilter = length;
		_captureProtocols = protocols;
		_filterLenghtPackets = filterLenghtPackets;
		_threadPool.execute(() -> {
			for (final IPacketListener listener : getListeners()) {
				listener.startCapture();
			}
			_capturing = true;
			String filter = "";
			boolean first = true;
			for (final Protocol prot : _captureProtocols) {
				if (!first) {
					filter += " or ";
				} else {
					first = false;
				}
				if (prot == Protocol.ICMP) {
					filter += prot.name().toLowerCase() + " ";
				} else {
					filter += prot.name().toLowerCase() + (port != null ? " dst port " + port : "");
				}
			}
			// bug on jpcap side for UDP filter
			if (_captureProtocols.contains(Protocol.UDP) && !_captureProtocols.contains(Protocol.TCP)) {
				filter += " or " + Protocol.TCP.name().toLowerCase() + (port != null ? " dst port " + port : "");
			}
			if (StringUtils.isNotEmpty(host)) {
				filter += " " + host;
			}
			LOGGER.info("Capture filter : " + filter);
			_filter = filter;
			doStartCapture();
		});
	}

	private void doStartCapture() {
		try {
			if (_captor == null) {
				_captor = JpcapCaptor.openDevice(_device, 65535, true, 100);
				_captor.setFilter(_filter, true);
				_captor.setPacketReadTimeout(5000);
				System.gc();
			} else {
				_captor.breakLoop();
				_captor.close();
			}
			_captor.loopPacket(-1, this);
		} catch (final IOException e) {
			// notify error
			for (final IPacketListener listener : getListeners()) {
				listener.error(e, this);
			}
		} catch (final Exception e) {
			LOGGER.error("Error during sniffer", e);
		}
	}

	/**
	 * Stop capture
	 */
	@Override
	public void endCapture() {
		if (_capturing) {
			_capturing = false;
			for (final IPacketListener listener : getListeners()) {
				listener.captureStopped();
			}
			_captor.breakLoop();
			_captor.close();
			_captor = null;
		}
	}

	/**
	 * @see org.leo.traceroute.core.network.INetworkInterfaceListener#notifyNewNetworkInterface(jpcap.NetworkInterface, byte[])
	 */
	@Override
	public void notifyNewNetworkInterface(final NetworkInterface device, final byte[] gatewayMac) {
		_device = device;
		_localAddresses.clear();
		if (_device != null) {
			for (final NetworkInterfaceAddress add : _device.addresses) {
				_localAddresses.add(add.address.getHostAddress());
			}
		}
	}

	/**
	 * @see org.leo.traceroute.core.IComponent#dispose()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void dispose() {
		super.dispose();
		endCapture();
		if (_services != null) {
			((INetworkService<NetworkInterface>) _services.getJpcapNetwork()).removeListener(this);
		}
	}
}
