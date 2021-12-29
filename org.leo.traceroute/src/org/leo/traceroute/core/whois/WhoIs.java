/**
 * Open Visual Trace Route
 * Copyright (C) 2010-2015 Leo Lewis.
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
package org.leo.traceroute.core.whois;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.leo.traceroute.core.AbstractObject;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.geo.GeoService;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WhoIs $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public class WhoIs extends AbstractObject<IWhoIsListener> {

	private static final Logger LOGGER = LoggerFactory.getLogger(WhoIs.class);

	private ExecutorService _executor;
	private GeoService _geo;
	private DNSLookupService _dns;

	/** Last result */
	private volatile String _whois;
	private volatile GeoPoint _point;
	private volatile boolean _cancel;

	/**
	 * @see org.leo.traceroute.core.IComponent#init(org.leo.traceroute.core.ServiceFactory)
	 */
	@Override
	public void init(final ServiceFactory services) throws Exception {
		_executor = Executors.newFixedThreadPool(3);
		_geo = services.getGeo();
		_dns = services.getDnsLookup();
	}

	/**
	 * @see org.leo.traceroute.core.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		if (_executor != null) {
			_executor.shutdown();
		}
	}

	public void whoIs(final String ipOrHost) {
		try {
			_cancel = false;
			_executor.execute(() -> doRun(ipOrHost, "whois"));
		} catch (final Exception e) {
			_whois = null;
			_point = null;
			notifyListeners(listener -> listener.error(new IOException("Failed to get the whois info", e), WhoIs.this));
		}
	}

	private void doRun(final String ipOrHost, String cmd) {
		try {
			notifyListeners(listener -> listener.startWhoIs(ipOrHost));
			final InetAddress address = InetAddress.getByName(ipOrHost);
			_point = _geo.populateGeoDataForIP(new GeoPoint(), address.getHostAddress(), address.getHostName());

			_point.setHostname(_dns.dnsLookup(_point.getIp()));
			notifyListeners(listener -> listener.focusWhoIs(_point));

			if (Env.INSTANCE.getOs() == OS.win) {
				cmd = Env.NATIVE_FOLDER + Util.FILE_SEPARATOR + OS.win.name() + Util.FILE_SEPARATOR + cmd + ".exe";
			}
			final Process process = Runtime.getRuntime().exec(cmd + " " + address.getHostAddress());
			final InputStream input = process.getInputStream();
			final int exit = process.waitFor();
			final StringBuilder res = new StringBuilder();
			final List<String> lines = IOUtils.readLines(input);
			boolean empty = false;
			for (int i = 0; i < lines.size(); i++) {
				final String line = lines.get(i);
				if ((Env.INSTANCE.getOs() == OS.win && i <= 4) || line.startsWith("#") || line.startsWith("%%")) {
					continue;
				}
				if (line.length() == 0) {
					if (empty) {
						continue;
					} else {
						empty = true;
					}
				} else {
					empty = false;
				}
				res.append(line);
				if (Env.INSTANCE.getOs() == OS.win) {
					res.append(System.lineSeparator());
				} else {
					res.append("\n");
				}
			}
			final InputStream error = process.getErrorStream();

			final List<String> errors = Util.readUTF8File(error);
			if (!errors.isEmpty()) {
				final StringBuilder m = new StringBuilder();
				for (final String e : errors) {
					m.append(e).append("\n");
				}
				// notify error
				if (!m.toString().isEmpty()) {
					throw new IOException(m.toString());
				}
			}
			if (exit != 0) {
				throw new IOException("Exit with code " + exit + " " + res);
			}
			_whois = res.toString().replace("&gt;", "").replace("\n\n", "");
			if (!_cancel) {
				notifyListeners(listener -> listener.whoIsResult(_whois));
			}
		} catch (final Exception e) {
			if (Env.INSTANCE.getOs() == OS.linux && e.getMessage().contains("Cannot run program \"" + cmd + "\": error=2, No such file or directory")) {
				doRun(ipOrHost, "jwhois");
				return;
			}
			LOGGER.error("WhoIs failed", e);
			_whois = null;
			_point = null;
			notifyListeners(listener -> listener.error(new IOException("Failed to get the whois info", e), WhoIs.this));
		}
	}

	/**
	 * Last who is result
	 * @return
	 */
	public String toText() {
		return _whois;
	}

	/**
	 * Renotify whois last result
	 */
	public void renotifyWhoIs() {
		if (_point != null) {
			notifyListeners(listener -> {
				listener.startWhoIs(_point.getHostname());
				listener.focusWhoIs(_point);
				listener.whoIsResult(_whois);
			});
		}
	}

	public void cancel() {
		_cancel = true;
	}

	public void clear() {
		_whois = null;
	}
}
