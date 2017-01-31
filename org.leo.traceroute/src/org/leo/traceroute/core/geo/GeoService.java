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
package org.leo.traceroute.core.geo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.leo.traceroute.core.IComponent;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.util.Pair;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

/**
 * GeoService $Id: GeoService.java 272 2016-09-22 05:38:31Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class GeoService implements IComponent {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeoService.class);

	/** Unknown location */
	private final static String UNKNOWN_LOCATION = "(Unknown)";

	/** City lookup service */
	protected LookupService _lookupService;

	/** Public IP */
	protected Pair<String, InetAddress> _publicIp;
	/** Local Ip geo location */
	protected GeoPoint _localIpGeoLocation;

	private boolean _deleteDbOnClose;

	/**
	 * @see org.leo.traceroute.core.IComponent#init(org.leo.traceroute.core.ServiceFactory)
	 */
	@Override
	public void init(final ServiceFactory services) throws IOException {
		doInit(services, 0);
	}

	private void doInit(final ServiceFactory services, int retry) throws IOException {
		GZIPInputStream gzis = null;
		FileOutputStream out = null;
		try {
			if (Env.GEO_DATA_FILE.exists()
					&& (Env.GEO_DATA_FILE.lastModified() + TimeUnit.DAYS.toMillis(30)) < System.currentTimeMillis()) {
				// geoip db expires after once month
				Env.GEO_DATA_FILE.renameTo(Env.GEO_DATA_FILE_OLD);
				LOGGER.info("GeoIP database expired, force redownloading a new one");
			} else {
				LOGGER.info("Use geoip db {} which is {} day(s) old", Env.GEO_DATA_FILE.getAbsolutePath(),
						TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Env.GEO_DATA_FILE.lastModified()));
			}
			services.updateStartup("updating.geoip", retry == 0);

			if (!Env.GEO_DATA_FILE.exists()) {
				final String url = Env.INSTANCE.getGeoIpLocation();
				LOGGER.info("Downloading GeoIP database to " + Env.GEO_DATA_FILE.getAbsolutePath() + "...");
				final byte[] buffer = new byte[1024];
				gzis = new GZIPInputStream(Util.followRedirectOpenConnection(url));
				out = new FileOutputStream(Env.GEO_DATA_FILE);
				int len;
				while ((len = gzis.read(buffer)) > 0) {
					out.write(buffer, 0, len);
				}
				out.flush();
			}
		} catch (final Exception e) {
			// try restore one old DB, better than nothing
			if (!Env.GEO_DATA_FILE_OLD.exists() && !Env.GEO_DATA_FILE_OLD.renameTo(Env.GEO_DATA_FILE)) {
				throw new IOException(Resources.getLabel("geoip.init.failed"), e);
			}
		} finally {
			IOUtils.closeQuietly(gzis);
			IOUtils.closeQuietly(out);
		}
		// init lookup service
		services.updateStartup("init.geoip", retry == 0);
		_lookupService = new LookupService(Env.GEO_DATA_FILE, LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);

		// public IP
		services.updateStartup("init.public.ip", retry == 0);
		final String ip = Util.getPublicIp();
		try {
			_publicIp = Pair.create(ip, Inet4Address.getByName(ip));
			_localIpGeoLocation = populateGeoDataForIP(new GeoPoint(), _publicIp.getLeft());
		} catch (final UnknownHostException e) {
			_publicIp = Pair.create(ip, null);
		} catch (final ArrayIndexOutOfBoundsException e) {
			LOGGER.info("Corrupted GeoIP database, force redownloading a new one");
			if (retry++ > 2) {
				Env.GEO_DATA_FILE.delete();
				doInit(services, retry);
			}
		}

	}

	public void deleteGeoIpDbOnExit() {
		_deleteDbOnClose = true;
	}

	/**
	 * Populate the point with geo data corresponding to the given IP address
	 *
	 * @param point the point
	 * @param ip the IP
	 * @return the updated point
	 */
	public <P extends GeoPoint> P populateGeoDataForIP(final P point, final String ip) {
		Location response = null;
		try {
			point.setIp(ip);
			response = _lookupService.getLocation(ip);
			if (response != null) {
				final String city = response.city;
				final String country = response.countryName;
				if (city == null || "".equals(city)) {
					point.setTown(UNKNOWN_LOCATION);
				} else {
					point.setTown(city);
				}
				if (country == null || "".equals(country)) {
					point.setCountry(UNKNOWN_LOCATION);
				} else {
					point.setCountry(country);
				}
				if (response.latitude == 0f && response.longitude == 0f) {
					point.setUnknownGeo(true);
				} else {
					point.setLat(response.latitude);
					point.setLon(response.longitude);
				}
				point.setCountryIso(response.countryCode);
				if (ip.equals("239.255.255.250")) {
					point.setCountry("SSDP");
					point.setTown("SSDP");
				}
			}
		} catch (final Exception e) {
			LOGGER.warn("Failed to lookup geoip data for ip " + ip, e);
		}
		if (response == null) {
			point.setUnknownGeo(true);
			point.setTown(UNKNOWN_LOCATION);
			point.setCountry(UNKNOWN_LOCATION);
		}
		return point;
	}

	public <P extends GeoPoint> P populateGeoDataForLocalIp(final P point, final String ip) {
		point.setIp(ip);
		point.setCountry(_localIpGeoLocation.getCountry());
		point.setTown(_localIpGeoLocation.getTown());
		point.setLat(_localIpGeoLocation.getLat());
		point.setLon(_localIpGeoLocation.getLon());
		point.setCountryIso(_localIpGeoLocation.getCountryIso());
		return point;
	}

	public GeoPoint getLocalIpGeoLocation() {
		return populateGeoDataForIP(new GeoPoint(), _publicIp.getLeft());
	}

	/**
	 * Return the value of the field publicIp <Ip, Hostname>
	 * @return the value of publicIp
	 */
	public Pair<String, InetAddress> getPublicIp() {
		return _publicIp;
	}

	/**
	 * dispose
	 */
	@Override
	public void dispose() {
		if (_lookupService != null) {
			_lookupService.close();
		}
		if (_deleteDbOnClose) {
			LOGGER.info("Delete " + Env.GEO_DATA_FILE.getAbsolutePath() + " to force redownloading the file on next startup");
			Env.GEO_DATA_FILE_OLD.delete();
			if (!Env.GEO_DATA_FILE.renameTo(Env.GEO_DATA_FILE_OLD)) {
				LOGGER.error("Failed to delete geoip db {} {}", Env.GEO_DATA_FILE.getAbsolutePath());
			}
		}
	}
}
