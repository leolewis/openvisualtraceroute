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
//import org.leo.traceroute.core.IComponent;
//import org.leo.traceroute.core.ServiceFactory;
//import org.leo.traceroute.core.ServiceFactory.Mode;
//import org.leo.traceroute.core.network.INetworkInterfaceListener;
//import org.leo.traceroute.core.sniffer.AbstractPacketPoint.Protocol;
//import org.leo.traceroute.core.sniffer.IPacketListener;
//import org.leo.traceroute.install.Env;
//import org.leo.traceroute.install.Env.OS;
//import org.pcap4j.core.*;
//import org.pcap4j.core.BpfProgram.BpfCompileMode;
//import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
//import org.pcap4j.packet.IpV4Packet;
//import org.pcap4j.packet.IpV4Packet.IpV4Header;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.util.Set;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
//
///**
// * Packet Sniffer $Id: JNetCapPacketSniffer.java 287 2016-10-30 20:35:16Z leolewis $
// * <pre>
// * </pre>
// * @author Leo Lewis
// */
//public class Pcap4JPacketSniffer extends AbstractSniffer implements PacketListener, IComponent, INetworkInterfaceListener<PcapNetworkInterface> {
//
//	private static final Logger LOGGER = LoggerFactory.getLogger(Pcap4JPacketSniffer.class);
//
//	/** Current device */
//	private PcapNetworkInterface _device;
//
//	/** Captor */
//	private volatile PcapHandle _captor;
//
//	private ScheduledExecutorService _schedule;
//	private ScheduledFuture<?> _scheduleStop;
//	private volatile String _host;
//
//	/**
//	 * Constructor
//	 */
//	public Pcap4JPacketSniffer() {
//
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public void init(final ServiceFactory services) throws Exception {
//		super.init(services);
//		_schedule = Executors.newScheduledThreadPool(2);
//		if (_services != null) {
//			_services.getNetworkService().addListener(this);
//		}
//	}
//
//	@Override
//	public void gotPacket(final PcapPacket packet) {
//		if (!_capturing) {
//			return;
//		}
//		try {
//			final IpV4Header ip = packet.get(IpV4Packet.class).getHeader();
//			if (ip != null) {
//				final InetAddress dest = ip.getDstAddr();
//				Pcap4JPacketPoint point;
//				// packets with dest to local device
//				point = _services.getGeo().populateGeoDataForIP(new Pcap4JPacketPoint(), dest.getHostAddress(), dest.getHostName());
//				if (point == null || _localAddresses.contains(dest.getHostAddress()) || point.isUnknownGeo()) {
//					point = _services.getGeo().populateGeoDataForLocalIp(new Pcap4JPacketPoint(), dest.getHostAddress());
//				}
//				if (point != null) {
//					if (point.setPacket(packet) && _captureProtocols.contains(point.getProtocol())) {
//						point.setHostname(_services.getDnsLookup().dnsLookup(point.getIp()));
//						if (_host == null || _host.isEmpty() || point.getHostname().contains(_host)) {
//							final int c = _count.incrementAndGet();
//							point.setNumber(c);
//							point.setTs(System.currentTimeMillis());
//							if (!_capturing) {
//								return;
//							}
//							_capture.add(point);
//							for (final IPacketListener listener : getListeners()) {
//								listener.packetAdded(point);
//							}
//						}
//					}
//				}
//			}
//		} catch (final Exception e) {
//			for (final IPacketListener listener : getListeners()) {
//				listener.error(e, this);
//			}
//		}
//	}
//
//	/**
//	 * Start the capture
//	 */
//	@Override
//	public void startCapture(final Set<Protocol> protocols, final String port, final boolean filterLenghtPackets, final int length, final String host,
//			final int captureTimeSeconds) {
//		_focusedPoint = null;
//		_count.set(0);
//		_capture.clear();
//		_captureProtocols = protocols;
//		_host = host;
//		_filterLenghtPackets = filterLenghtPackets;
//		_length = length;
//		if (captureTimeSeconds > 0) {
//			_scheduleStop = _schedule.schedule(() -> endCapture(), captureTimeSeconds, TimeUnit.SECONDS);
//		}
//		_threadPool.execute(() -> {
//			for (final IPacketListener listener : getListeners()) {
//				listener.startCapture();
//			}
//			_capturing = true;
//			String filter = "";
//			String previous = "";
//			for (final Protocol prot : _captureProtocols) {
//				String s = "";
//				if (prot == Protocol.ICMP) {
//					s += prot.name().toLowerCase();
//				} else {
//					s += convertPortToFilter(prot.name().toLowerCase(), port);
//				}
//				if (!previous.isEmpty() && !s.isEmpty()) {
//					s = " or " + s;
//				}
//				if (filter.isEmpty() && !s.isEmpty()) {
//					filter = "(";
//				}
//				filter += s;
//				previous = s;
//			}
//			if (!filter.isEmpty()) {
//				filter += ")";
//			}
//			if (filterLenghtPackets) {
//				if (!filter.isEmpty()) {
//					filter += " and";
//				}
//				filter += " greater " + length;
//			}
//			LOGGER.info("Capture filter : " + filter);
//			_filter = filter;
//			doStartCapture();
//		});
//	}
//
//	private void doStartCapture() {
//		try {
//			closeCaptor();
//			final StringBuilder err = new StringBuilder();
//			_captor = _device.openLive(65535, PromiscuousMode.NONPROMISCUOUS, 10 * 1000);
//			if (_captor == null) {
//				throw new IOException(err.toString());
//			}
//			final BpfProgram r = _captor.compileFilter(_filter, BpfCompileMode.OPTIMIZE, PcapHandle.PCAP_NETMASK_UNKNOWN);
//			_captor.setFilter(r);
//			_captor.loop(-1, this, null);
//		} catch (final IOException e) {
//			// notify error
//			for (final IPacketListener listener : getListeners()) {
//				listener.error(e, this);
//			}
//		} catch (final Exception e) {
//			LOGGER.error("Error during sniffer", e);
//		}
//	}
//
//	/**
//	 * Stop capture
//	 */
//	@Override
//	public void endCapture() {
//		if (_scheduleStop != null) {
//			_scheduleStop.cancel(true);
//			_scheduleStop = null;
//		}
//		if (_capturing) {
//			_capturing = false;
//			closeCaptor();
//			for (final IPacketListener listener : getListeners()) {
//				listener.captureStopped();
//			}
//		}
//	}
//
//	private void closeCaptor() {
//		if (_captor != null) {
//			try {
//				_captor.breakLoop();
//				if (Env.INSTANCE.getOs() != OS.win) {
//					try {
//						// give it some time, otherwise it crashed the libpcap driver
//						Thread.sleep(1000);
//					} catch (final InterruptedException e) {
//					}
//				}
//				_captor.close();
//				_captor = null;
//			} catch (Exception e) {
//				LOGGER.error("Error closing caption", e);
//			}
//		}
//	}
//
//	@Override
//	public void notifyNewNetworkInterface(final PcapNetworkInterface device, Mode mode, final byte[] mac) {
//		if (mode != Mode.SNIFFER) {
//			return;
//		}
//		_device = device;
//		_localAddresses.clear();
//		if (_device != null) {
//			for (final PcapAddress add : _device.getAddresses()) {
//				_localAddresses.add(add.getAddress().getHostAddress());
//			}
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public void dispose() {
//		super.dispose();
//		endCapture();
//		if (_services != null) {
//			 _services.getNetworkService().removeListener(this);
//		}
//		_schedule.shutdown();
//	}
//}
