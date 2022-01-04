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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import com.maxmind.db.InvalidDatabaseException;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.IComponent;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.maxmind.db.Reader.FileMode;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;

/**
 * GeoService $Id: GeoService.java 272 2016-09-22 05:38:31Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class GeoService implements IComponent {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeoService.class);

	/** DNS Loc records file */
	private final static File LOC_RECORDS = new File(Env.OVTR_FOLDER.getAbsolutePath() + "/dns.loc");

	/** Unknown location */
	private final static String UNKNOWN_LOCATION = "(Unknown)";

	/** City lookup service */
	protected DatabaseReader _lookupService;

	/** Public IP */
	protected Pair<String, InetAddress> _publicIp;
	/** Local Ip geo location */
	protected GeoPoint _localIpGeoLocation;

	private boolean _deleteDbOnClose;

	/** DNS loc records */
	private final Map<String, CityResponse> _locRecords = new HashMap<>();
	/** DNS loc records (raw)*/
	private final List<LocRecord> _rawLocRecords = new ArrayList<>();
	private final Map<String, LocRecord> _rawLocRecordsMap = new HashMap<>();

	@Override
	public void init(final ServiceFactory services) throws IOException {
		doInit(services, 0);
	}

	private void doInit(final ServiceFactory services, int retry) throws IOException {
		GZIPInputStream gzis = null;
		TarArchiveInputStream tis = null;
		FileOutputStream out = null;
		try {
			if (Env.GEO_DATA_FILE.exists() && (Env.GEO_DATA_FILE.lastModified() + TimeUnit.DAYS.toMillis(30)) < System.currentTimeMillis()) {
				// geoip db expires after once month
				Env.GEO_DATA_FILE_OLD.delete();
				if (Env.GEO_DATA_FILE.renameTo(Env.GEO_DATA_FILE_OLD)) {
					LOGGER.info("GeoIP database expired, force redownloading a new one");
				}
			} else {
				LOGGER.info("Use geoip db {} which is {} day(s) old", Env.GEO_DATA_FILE.getAbsolutePath(),
						TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Env.GEO_DATA_FILE.lastModified()));
			}
			services.updateStartup("updating.geoip", retry == 0);

			if (!Env.GEO_DATA_FILE.exists()) {
				final String[] urls = Env.INSTANCE.getGeoIpLocation();
				String url = null;
				for (String u : urls) {
					try {
						InetAddress.getByName(u.replace("https://", "").replace("http://", "").split("/")[0]);
						url = u;
					} catch (Exception e) {
						LOGGER.info("Can't resolve " + u + ", skip and try next one");
					}
				}
				if (url == null) {
					throw new IOException(Resources.getLabel("geoip.init.failed"));
				}
				LOGGER.info("Downloading GeoIP database to " + Env.GEO_DATA_FILE.getAbsolutePath() + "...");
				final byte[] buffer = new byte[1024];
				gzis = new GZIPInputStream(Util.followRedirectOpenConnection(url));
				tis = new TarArchiveInputStream(gzis);
				TarArchiveEntry tarEntry = null;

				// tarIn is a TarArchiveInputStream
				while ((tarEntry = tis.getNextTarEntry()) != null) {
					if (tarEntry.getName().endsWith(Env.GEO_DATA_FILE.getName())) {
						break;
					}
				}
				out = new FileOutputStream(Env.GEO_DATA_FILE);
				int len;
				while ((len = tis.read(buffer)) > 0) {
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
			IOUtils.closeQuietly(tis);
			IOUtils.closeQuietly(out);
		}
		// public IP
		final String ip = Util.getPublicIp();
		_publicIp = Pair.of(ip, InetAddress.getByName(ip));
		services.updateStartup("init.public.ip", retry == 0);
		// init lookup service
		services.updateStartup("init.geoip", retry == 0);
		try {
			_lookupService = new DatabaseReader.Builder(Env.GEO_DATA_FILE).fileMode(FileMode.MEMORY).build();
			computePublicIpGeoLocation();
		} catch (final ArrayIndexOutOfBoundsException | InvalidDatabaseException e) {
			LOGGER.info("Corrupted GeoIP database, force redownloading a new one");
			if (retry++ <= 2) {
				Env.GEO_DATA_FILE.delete();
				doInit(services, retry);
				return;
			}
			throw new IOException(Resources.getLabel("geoip.init.failed"), e);
		} catch (Exception e) {
			_publicIp = Pair.of(ip, null);
		}
		if (LOC_RECORDS.exists()) {
			final Pair<String, Exception> error = parseAndLoadDNSRecords(IOUtils.toString(new FileInputStream(LOC_RECORDS)));
			if (StringUtils.isNoneEmpty(error.getKey())) {
				LOGGER.error(error.getKey(), error.getValue());
			} else {
				LOGGER.info("DNS LOC records file {} loaded", LOC_RECORDS.getAbsolutePath());
			}
		}
	}

	private void computePublicIpGeoLocation() {
		_localIpGeoLocation = populateGeoDataForIP(new GeoPoint(), _publicIp.getLeft(), null);
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
	public <P extends GeoPoint> P populateGeoDataForIP(final P point, final String ip, final String dns) {
		return populateGeoDataForIP(point, ip, dns, null);
	}

	public <P extends GeoPoint> P populateGeoDataForIP(final P point, final String ip, final String dns, final P pointIfUnknown) {
		CityResponse location = null;
		try {
			point.setIp(ip);
			// check loc records
			if (DNSLookupService.UNKNOWN_HOST.equals(dns)) {
				location = _locRecords.get(ip);
			} else {
				if (dns == null) {
					location = _locRecords.get("localhost");
				} else {
					location = _locRecords.get(dns);
				}
			}
			// nothing in the loc records, check with the geoip db
			if (location == null) {
				try {
					location = _lookupService.city(InetAddress.getByName(ip));
				} catch (Exception e) {

				}
			}
			if (location != null) {
				final City city = location.getCity();
				final Country country = location.getCountry();
				float lat = location.getLocation().getLatitude().floatValue();
				float lon = location.getLocation().getLongitude().floatValue();
				if (city == null || city.getName() == null || "".equals(city.getName())) {
					point.setTown(UNKNOWN_LOCATION);
					if (pointIfUnknown != null && pointIfUnknown.getCountry().equals(country)) {
						lat = pointIfUnknown.getLat();
						lon = pointIfUnknown.getLon();
					}
				} else {
					point.setTown(city.getName());
				}
				if (country == null || country.getName() == null || "".equals(country.getName())) {
					point.setCountry(UNKNOWN_LOCATION);
				} else {
					point.setCountry(country.getName());
				}
				if (lat == 0f && lon == 0f) {
					point.setUnknownGeo(true);
				} else {
					point.setLat(lat);
					point.setLon(lon);
				}
				point.setCountryIso(location.getCountry().getIsoCode());
				if (ip.equals("239.255.255.250")) {
					point.setCountry("SSDP");
					point.setTown("SSDP");
				}
			}
		} catch (final Exception e) {
			LOGGER.warn("Failed to lookup geoip data for ip " + ip, e);
		}
		if (location == null) {
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
		return populateGeoDataForIP(new GeoPoint(), _publicIp.getLeft(), null);
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
			try {
				_lookupService.close();
			} catch (final IOException e) {
				LOGGER.warn("Failed to closed GeoIP db", e);
			}
		}
		if (_deleteDbOnClose) {
			LOGGER.info("Delete " + Env.GEO_DATA_FILE.getAbsolutePath() + " to force redownloading the file on next startup");
			Env.GEO_DATA_FILE_OLD.delete();
			if (!Env.GEO_DATA_FILE.renameTo(Env.GEO_DATA_FILE_OLD)) {
				LOGGER.error("Failed to delete geoip db {}", Env.GEO_DATA_FILE.getAbsolutePath());
			}
		}
		try {
			IOUtils.write(getLocRecordsStr(), new FileOutputStream(LOC_RECORDS));
			LOGGER.info("DNS LOC records saved to {}", LOC_RECORDS.getAbsolutePath());
		} catch (final Exception e) {
			LOGGER.error("Failed to save DNS LOC records {}", LOC_RECORDS, e);
		}
	}

	public Pair<String, Exception> parseAndLoadDNSRecords(final String raw) {
		clear();
		final StringBuilder error = new StringBuilder();
		Exception ex = null;
		final String[] lines = raw.split("\\r?\\n");
		try {
			StringBuilder comment = null;
			for (final String line : lines) {
				if (line.trim().isEmpty() || line.startsWith(";")) {
					if (comment == null) {
						comment = new StringBuilder();
					}
					comment.append(line).append("\n");
					continue;
				}
				LocRecord record = null;
				try {
					record = new LocRecord(line, comment == null ? "" : comment.toString());
				} catch (final Exception e) {
					error.append("Invalid record " + line + ": " + e.getMessage());
					ex = e;
					// invalid record, comment it out
					final String r = ";Invalid record :" + e.getMessage() + "\n;" + line;
					record = new LocRecord(r, comment == null ? "" : comment.toString());
				}
				addLocRecord(record);
				comment = null;
			}
		} catch (final Exception e) {
			error.append("Failed to load loc records " + LOC_RECORDS.getAbsolutePath() + e.getMessage());
			ex = e;
		}
		return Pair.of(error.toString(), ex);
	}

	private void addLocRecord(final LocRecord record) {
		if (record.isValid()) {
			final CityResponse old = _locRecords.put(record.getOwner(), record.getLocation());
			if (old != null) {
				final LocRecord o = _rawLocRecordsMap.remove(record.getOwner());
				_rawLocRecords.remove(o);
			}
		}
		_rawLocRecords.add(record);
		_rawLocRecordsMap.put(record.getOwner(), record);
	}

	/**
	 * Return the value of the field locRecordsStr
	 * @return the value of locRecordsStr
	 */
	public String getLocRecordsStr() {
		if (_rawLocRecords.isEmpty()) {
			return "; LOC record format is https://en.wikipedia.org/wiki/LOC_record\n"
					+ ";owner TTL class LOC ( d1 [m1 [s1]] {\"N\"|\"S\"} d2 [m2 [s2]] {\"E\"|\"W\"} alt[\"m\"] [siz[\"m\"] [hp[\"m\"] [vp[\"m\"]]]] )\n" + "; example\n"
					+ "; statdns.net. IN LOC 52 22 23.000 N 4 53 32.000 E -2.00m 0.00m 10000m 10m\n" + "; example to override local addresses\n"
					+ "; localhost. IN LOC 49 14 46.6512 N 123 6 58.4136 W";
		}
		final StringBuilder sb = new StringBuilder();
		for (final LocRecord record : _rawLocRecords) {
			sb.append(record.getRaw());
		}
		return sb.toString();
	}

	/**
	 * @param host
	 * @param lat
	 * @param lon
	 */
	public void addLocRecord(final String host, final double lat, final double lon) {
		addLocRecord(new LocRecord(host, lat, lon));
		computePublicIpGeoLocation();
	}

	private void clear() {
		_rawLocRecords.clear();
		_locRecords.clear();
	}
}
