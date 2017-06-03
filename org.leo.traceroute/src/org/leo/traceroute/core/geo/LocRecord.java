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
package org.leo.traceroute.core.geo;

import com.maxmind.geoip.Location;

import gov.nasa.worldwind.geom.Angle;

/**
 * LocRecord $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public class LocRecord {

	public static final String LOC = "LOC";
	private static final String SPACE = " ";

	private final String raw;
	private Location location;
	private boolean valid;
	private String owner;

	public LocRecord(final String raw, final String comment) {
		this.raw = (comment != null ? comment : "") + raw + "\n";
		if (raw.startsWith(";")) {
			return;
		}
		boolean north;
		boolean east;
		int d1 = 0;
		int m1 = 0;
		float s1 = 0;
		int d2 = 0;
		int m2 = 0;
		float s2 = 0;
		// LOC record format is
		// owner TTL class LOC ( d1 [m1 [s1]] {"N"|"S"} d2 [m2 [s2]] {"E"|"W"} alt["m"] [siz["m"] [hp["m"] [vp["m"]]]] )
		final String[] fields = raw.split("\\h+");
		int index = 0;
		owner = fields[index++];
		if (owner.endsWith(".")) {
			owner = owner.substring(0, owner.length() - 1);
		}
		if ("IN".equals(fields[index])) {
			index++;
		}
		if (!"LOC".equals(fields[index])) {
			try {
				Integer.parseInt(fields[index]);
			} catch (final NumberFormatException e) {
				throw new IllegalArgumentException("invalid TTL field " + fields[index]);
			}
			index++;
		}
		if (!"LOC".equals(fields[index])) {
			index++;
		}
		if (!"LOC".equals(fields[index])) {
			throw new IllegalArgumentException("invalid LOC field " + fields[index]);
		}
		index++;
		// lat
		try {
			d1 = Integer.parseInt(fields[index]);
			index++;
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("invalid d1 field " + fields[index]);
		}
		if (!"N".equals(fields[index]) && !"S".equals(fields[index])) {
			try {
				m1 = Integer.parseInt(fields[index]);
				index++;
			} catch (final NumberFormatException e) {
				throw new IllegalArgumentException("invalid m1 field " + fields[index]);
			}
			if (!"N".equals(fields[index]) && !"S".equals(fields[index])) {
				try {
					s1 = Float.parseFloat(fields[index]);
					index++;
				} catch (final NumberFormatException e) {
					throw new IllegalArgumentException("invalid s1 field " + fields[index]);
				}
			}
		}
		if (!"N".equals(fields[index]) && !"S".equals(fields[index])) {
			throw new IllegalArgumentException("invalid {S|N} field " + fields[index]);
		}
		north = "N".equals(fields[index]);
		index++;

		// lon
		try {
			d2 = Integer.parseInt(fields[index]);
			index++;
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("invalid d2 field " + fields[index]);
		}
		if (!"W".equals(fields[index]) && !"E".equals(fields[index])) {
			try {
				m2 = Integer.parseInt(fields[index]);
				index++;
			} catch (final NumberFormatException e) {
				throw new IllegalArgumentException("invalid m2 field " + fields[index]);
			}
			if (!"W".equals(fields[index]) && !"E".equals(fields[index])) {
				try {
					s2 = Float.parseFloat(fields[index]);
					index++;
				} catch (final NumberFormatException e) {
					throw new IllegalArgumentException("invalid s2 field " + fields[index]);
				}
			}
		}
		if (!"W".equals(fields[index]) && !"E".equals(fields[index])) {
			throw new IllegalArgumentException("invalid {E|W} field " + fields[index]);
		}
		east = "E".equals(fields[index]);
		index++;
		// caclulate location
		final float lat = (north ? 1 : -1) * d1 + m1 / 60f + s1 / 3600f;
		final float lon = (east ? 1 : -1) * d2 + m2 / 60f + s2 / 3600f;
		location = new Location();
		location.countryCode = LOC;
		location.countryName = "Loc record";
		location.latitude = lat;
		location.longitude = lon;
		valid = true;
	}

	public LocRecord(final String owner, final double lat, final double lon) {
		this.owner = owner;
		final StringBuilder sb = new StringBuilder();
		//owner TTL class LOC ( d1 [m1 [s1]] {"N"|"S"} d2 [m2 [s2]] {"E"|"W"} alt["m"] [siz["m"] [hp["m"] [vp["m"]]]] )
		final double[] latDms = Angle.fromDegreesLatitude(lat).toDMS();
		final double[] lonDms = Angle.fromDegreesLatitude(lon).toDMS();
		sb.append(owner).append(".").append(SPACE).append(0).append(SPACE).append("IN").append(SPACE).append("LOC").append(SPACE);
		sb.append(Math.abs((int) latDms[0])).append(SPACE).append((int) latDms[1]).append(SPACE).append((float) latDms[2]).append(SPACE).append(lat >= 0 ? "N" : "S")
				.append(SPACE);
		sb.append(Math.abs((int) lonDms[0])).append(SPACE).append((int) lonDms[1]).append(SPACE).append((float) lonDms[2]).append(SPACE).append(lon >= 0 ? "E" : "W")
				.append(SPACE);
		sb.append("0m").append(SPACE).append("0m").append(SPACE).append("0m").append(SPACE).append("0m");
		raw = sb.toString();
		location = new Location();
		location.countryCode = LOC;
		location.countryName = "Loc record";
		location.latitude = (float) lat;
		location.longitude = (float) lon;
		valid = true;
	}

	/**
	 * Return the value of the field owner
	 * @return the value of owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Return the value of the field location
	 * @return the value of location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Return the value of the field raw
	 * @return the value of raw
	 */
	public String getRaw() {
		return raw;
	}

	/**
	 * Return the value of the field valid
	 * @return the value of valid
	 */
	public boolean isValid() {
		return valid;
	}
}
