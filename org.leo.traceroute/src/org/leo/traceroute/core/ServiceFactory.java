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
package org.leo.traceroute.core;

import org.leo.traceroute.core.autocomplete.AutoCompleteProvider;
import org.leo.traceroute.core.geo.GeoService;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.network.INetworkService;
import org.leo.traceroute.core.network.JNetCapNetworkService;
import org.leo.traceroute.core.network.JPcapNetworkService;
import org.leo.traceroute.core.route.ITraceRoute;
import org.leo.traceroute.core.route.impl.JNetCapTraceRoute;
import org.leo.traceroute.core.route.impl.JpcapTraceRoute;
import org.leo.traceroute.core.sniffer.IPacketsSniffer;
import org.leo.traceroute.core.sniffer.impl.JNetCapPacketSniffer;
import org.leo.traceroute.core.sniffer.impl.JPcapPacketSniffer;
import org.leo.traceroute.core.whois.WhoIs;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.ui.util.SplashScreen;

/**
 * ServiceFactory $Id: ServiceFactory.java 222 2016-01-09 19:19:33Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class ServiceFactory {

	public enum NetworkLibrary {
		JPCAP,
		JNETPCAP
	}

	public static final NetworkLibrary TRACEROUTE_NETWORK_LIB = NetworkLibrary.JPCAP;
	public static final NetworkLibrary SNIFFER_NETWORK_LIB = NetworkLibrary.JNETPCAP;

	private final ITraceRoute _traceroute;
	private final IPacketsSniffer _sniffer;

	private final INetworkService<?> _jpcapNetwork;
	private final INetworkService<?> _jnetcapNetwork;

	private final DNSLookupService _dnsLookup;
	private final WhoIs _whois;

	private final GeoService _geo;

	private final AutoCompleteProvider _autocomplete;

	private final SplashScreen _splash;

	public ServiceFactory(final ITraceRoute traceroute, final IPacketsSniffer sniffer, final INetworkService<?> jnetcapNetwork,
			final INetworkService<?> jpcapNetwork, final DNSLookupService dnsLookup, final GeoService geo,
			final AutoCompleteProvider autoComplete, final WhoIs whois) {
		super();
		_traceroute = traceroute;
		_sniffer = sniffer;
		_jpcapNetwork = jpcapNetwork;
		_jnetcapNetwork = jnetcapNetwork;
		_dnsLookup = dnsLookup;
		_geo = geo;
		_autocomplete = autoComplete;
		_whois = whois;
		_splash = null;
	}

	/**
	 * Constructor
	 */
	public ServiceFactory(final SplashScreen splash) {
		_splash = splash;

		_jpcapNetwork = new JPcapNetworkService();
		_jnetcapNetwork = new JNetCapNetworkService();

		if (TRACEROUTE_NETWORK_LIB == NetworkLibrary.JNETPCAP) {
			_traceroute = new JNetCapTraceRoute();
		} else {
			_traceroute = new JpcapTraceRoute();
		}
		if (SNIFFER_NETWORK_LIB == NetworkLibrary.JNETPCAP) {
			_sniffer = new JNetCapPacketSniffer();
		} else {
			_sniffer = new JPcapPacketSniffer();
		}
		_dnsLookup = new DNSLookupService();
		_geo = new GeoService();
		_autocomplete = new AutoCompleteProvider();
		_whois = new WhoIs();
	}

	public void init() throws Exception {
		_dnsLookup.init(this);
		_geo.init(this);
		_jpcapNetwork.init(this);
		_jnetcapNetwork.init(this);
		_traceroute.init(this);
		_sniffer.init(this);
		_autocomplete.init(this);
		_whois.init(this);

		_jpcapNetwork.notifyInterface();
		_jnetcapNetwork.notifyInterface();
		if (!isEmbeddedTRAvailable()) {
			Env.INSTANCE.setUseOSTraceroute(true);
		} else {
			getNetwork(ServiceFactory.TRACEROUTE_NETWORK_LIB).setCurrentNetworkDevice(Env.INSTANCE.getTrInterfaceIndex());
		}
		if (isSnifferAvailable()) {
			getNetwork(ServiceFactory.SNIFFER_NETWORK_LIB).setCurrentNetworkDevice(Env.INSTANCE.getSnifferInterfaceIndex());
		}
	}

	/**
	 * Dispose services
	 */
	public void dispose() {
		_jnetcapNetwork.dispose();
		_jpcapNetwork.dispose();
		_dnsLookup.dispose();
		_geo.dispose();
		_traceroute.dispose();
		_sniffer.dispose();
		_autocomplete.dispose();
		_whois.dispose();
	}

	/**
	 * Return the value of the field dnsLookup
	 * @return the value of dnsLookup
	 */
	public DNSLookupService getDnsLookup() {
		return _dnsLookup;
	}

	/**
	 * Return the value of the field geo
	 * @return the value of geo
	 */
	public GeoService getGeo() {
		return _geo;
	}

	/**
	 * Return the value of the field traceroute
	 * @return the value of traceroute
	 */
	public ITraceRoute getTraceroute() {
		return _traceroute;
	}

	/**
	 * Return the value of the field sniffer
	 * @return the value of sniffer
	 */
	public IPacketsSniffer getSniffer() {
		return _sniffer;
	}

	/**
	 * Return the value of the field jpcapNetwork
	 * @return the value of jpcapNetwork
	 */
	public INetworkService<?> getJpcapNetwork() {
		return _jpcapNetwork;
	}

	/**
	 * Return the value of the field jnetcapNetwork
	 * @return the value of jnetcapNetwork
	 */
	public INetworkService<?> getJnetcapNetwork() {
		return _jnetcapNetwork;
	}

	/**
	 * @param networkLibrary
	 * @return
	 */
	public INetworkService<?> getNetwork(final NetworkLibrary networkLibrary) {
		return networkLibrary == NetworkLibrary.JNETPCAP ? _jnetcapNetwork : _jpcapNetwork;
	}

	/**
	 * Tell if the sniffer mode is available
	 * @return
	 */
	public boolean isSnifferAvailable() {
		return !getNetwork(ServiceFactory.SNIFFER_NETWORK_LIB).getNetworkDevices().isEmpty();
	}

	/**
	 * Tell if the embedded TR mode is available
	 * @return
	 */
	public boolean isEmbeddedTRAvailable() {
		return !getNetwork(ServiceFactory.TRACEROUTE_NETWORK_LIB).getNetworkDevices().isEmpty();
	}

	/**
	 * Return the value of the field autocomplete
	 * @return the value of autocomplete
	 */
	public AutoCompleteProvider getAutocomplete() {
		return _autocomplete;
	}

	/**
	 * Return the value of the field whois
	 * @return the value of whois
	 */
	public WhoIs getWhois() {
		return _whois;
	}

	public SplashScreen getSplash() {
		return _splash;
	}

	/**
	 * @param labelKey
	 * @param incStep
	 */
	public void updateStartup(final String labelKey, final boolean incStep) {
		if (_splash != null) {
			_splash.updateStartup(labelKey, incStep);
		}

	}
}
