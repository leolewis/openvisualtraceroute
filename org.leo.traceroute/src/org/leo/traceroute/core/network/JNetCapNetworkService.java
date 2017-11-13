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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;
import org.leo.traceroute.core.AbstractObject;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JNetCapNetworkService $Id$
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class JNetCapNetworkService extends AbstractObject<INetworkInterfaceListener<PcapIf>> implements INetworkService<PcapIf> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JNetCapNetworkService.class);

	private final List<PcapIf> _devices = new ArrayList<>();
	private final Map<PcapIf, byte[]> _gatewayMac = new HashMap<>();

	private int _index;

	/**
	 * Constructor
	 */
	public JNetCapNetworkService() {
		super();
	}

	/**
	 * Initialize service
	 *
	 * @throws IOException
	 * @throws RouteException
	 */
	@Override
	public void init(final ServiceFactory services) throws Exception {
		super.init(services);
		if (Env.INSTANCE.getOs() != OS.mac) {
			services.updateStartup("init.sniffer.network", true);
			try {
				final StringBuilder sb = new StringBuilder();
				final int r = Pcap.findAllDevs(_devices, sb);
				if (r == Pcap.NOT_OK || _devices.isEmpty()) {
					throw new IOException(sb.toString());
				}
				for (final PcapIf net : new ArrayList<>(_devices)) {
					if (net.getAddresses().isEmpty()) {
						_devices.remove(net);
					} else {
						_gatewayMac.put(net, net.getHardwareAddress());
					}
				}
			} catch (final Throwable e) {
				LOGGER.warn("Cannot find a suitable network device for tracing.", e);
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
		int i = 0;
		for (final PcapIf net : _devices) {
			final String text = net.getDescription() == null || net.getDescription().trim().length() == 0 ? net.getName()
					: (net.getDescription() + " (" + net.getName() + ")");
			list.add(Pair.of(i++, text));
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
			if (deviceIndex >= 0) {
				_index = Math.min(deviceIndex, _devices.size() - 1);
			} else {
				final INetworkService<?> net = _factory.getNetwork(ServiceFactory.TRACEROUTE_NETWORK_LIB);
				final String name = net.getCurrentNetworkInterfaceName();
				if (name == null) {
					_index = 0;
				} else {
					for (int i = 0; i < _devices.size(); i++) {
						if (_devices.get(i).getName().equals(name)) {
							_index = i;
							break;
						}
					}
				}
			}
			notifyInterface();
		}
	}

	@Override
	public void notifyInterface() {
		if (!_devices.isEmpty()) {
			for (final INetworkInterfaceListener<PcapIf> listener : getListeners()) {
				final PcapIf net = _devices.get(_index);
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
			return _devices.get(_index).getName();
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
