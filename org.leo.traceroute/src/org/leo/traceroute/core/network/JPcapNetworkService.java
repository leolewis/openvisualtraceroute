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
package org.leo.traceroute.core.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.AbstractObject;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.EthernetPacket;
import jpcap.packet.Packet;

/**
 * NetworkManager $Id: JPcapNetworkService.java 247 2016-02-23 06:19:59Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class JPcapNetworkService extends AbstractObject<INetworkInterfaceListener<NetworkInterface>> implements INetworkService<NetworkInterface> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceFactory.class);

	/** Ethernet */
	private final static String ETHERNET = "Ethernet";
	private final static String TEST_URL = "www.google.com";

	/** Network devices */
	private final List<NetworkInterface> _devices = new ArrayList<>();
	/** Selected Network device */
	private int _index;
	/** Default gateway mac adress */
	private final Map<NetworkInterface, byte[]> _gatewayMac = new HashMap<>();

	/**
	 * Constructor
	 */
	public JPcapNetworkService() {
		super();
	}

	/**
	 * Initialize services
	 *
	 * @throws IOException
	 * @throws RouteException
	 */
	@Override
	public void init(final ServiceFactory services) throws IOException {
		// first time we use the network device, need to find the one connected
		// to the Internet
		if (Env.INSTANCE.getOs() != OS.mac) {
			services.updateStartup("init.traceroute.network", true);
			final InetAddress pingAddr = InetAddress.getByName(TEST_URL);
			ExecutorService executor = null;
			try {
				final NetworkInterface[] netInterfaces = JpcapCaptor.getDeviceList();
				executor = Executors.newFixedThreadPool(1);
				final List<Pair<String, Future<Pair<NetworkInterface, byte[]>>>> futures = new ArrayList<>();
				for (int i = 0; i < netInterfaces.length; i++) {
					final int fi = i;
					futures.add(Pair.of(netInterfaces[fi].name, executor.submit(() -> {
						final NetworkInterface netInterface = netInterfaces[fi];
						if (!ETHERNET.equals(netInterface.datalink_description)) {
							return null;
						}
						LOGGER.info("Try using device " + netInterface.name + " " + netInterface.description);
						final JpcapCaptor captor = JpcapCaptor.openDevice(netInterface, 65535, false, 1000);
						// obtain MAC address of the default gateway
						captor.setFilter("tcp and dst host " + pingAddr.getHostAddress(), true);
						byte[] getwayMac = null;
						int retry = 0;
						while (getwayMac == null) {
							new URL("http://" + TEST_URL).openStream().close();
							final Packet ping = captor.getPacket();
							if (ping == null) {
								if (retry++ >= 3) {
									break;
								}
							} else if (!Arrays.equals(((EthernetPacket) ping.datalink).dst_mac, netInterface.mac_address)) {
								getwayMac = ((EthernetPacket) ping.datalink).dst_mac;
							}
						}
						return Pair.of(netInterface, getwayMac);
					})));
				}
				LOGGER.info("int " + netInterfaces.length);
				for (final Pair<String, Future<Pair<NetworkInterface, byte[]>>> f : futures) {
					try {
						final Pair<NetworkInterface, byte[]> res = f.getValue().get(2000, TimeUnit.MILLISECONDS);
						if (res == null) {
							continue;
						}
						final byte[] getwayMac = res.getRight();
						final NetworkInterface netInterface = res.getLeft();
						// a mac address for the default gateway
						if (getwayMac != null && !_devices.contains(netInterface)) {
							// interface is good to use
							_devices.add(netInterface);
							_gatewayMac.put(netInterface, getwayMac);
						}
					} catch (final TimeoutException e) {
						// device is not usable
						LOGGER.warn("Device timed out " + f.getKey());
					}
				}
			} catch (final Throwable ex) {
				LOGGER.warn("Cannot find a suitable network device for tracing.", ex);
			} finally {
				if (executor != null) {
					executor.shutdown();
				}
			}
		}

	}

	/**
	 * List of network devices
	 *
	 * @return the list
	 */
	@Override
	public List<Pair<Integer, String>> getNetworkDevices() {
		final List<Pair<Integer, String>> list = new ArrayList<>();
		for (int i = 0; i < _devices.size(); i++) {
			final NetworkInterface net = _devices.get(i);
			final String text = net.description == null || net.description.trim().length() == 0 ? net.name : (net.description + " (" + net.name + ")");
			list.add(Pair.of(i, text));
		}
		return list;
	}

	/**
	 * Set the current network device
	 *
	 * @param netDevice
	 */
	@Override
	public void setCurrentNetworkDevice(final int deviceIndex) {
		if (!_devices.isEmpty()) {
			_index = Math.min(Math.max(0, deviceIndex), _devices.size() - 1);
			notifyInterface();
		}
	}

	@Override
	public void notifyInterface() {
		if (!_devices.isEmpty()) {
			for (final INetworkInterfaceListener<NetworkInterface> listener : getListeners()) {
				final NetworkInterface net = _devices.get(_index);
				listener.notifyNewNetworkInterface(net, _gatewayMac.get(net));
			}
		}
	}

	/**
	 * @see org.leo.traceroute.core.network.INetworkService#getCurrentNetworkInterface()
	 */
	@Override
	public String getCurrentNetworkInterfaceName() {
		if (!_devices.isEmpty()) {
			return _devices.get(_index).name;
		}
		return null;
	}

	/**
	 * @see org.leo.traceroute.core.network.INetworkService#getCurrentNetworkInterfaceIndex()
	 */
	@Override
	public int getCurrentNetworkInterfaceIndex() {
		return _index;
	}
}
