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

import javax.swing.ImageIcon;

import org.leo.traceroute.resources.CountryFlagManager;
import org.leo.traceroute.resources.CountryFlagManager.Resolution;

/**
 * GeoPoint $Id: GeoPoint.java 236 2016-02-01 06:03:57Z leolewis $
 *
 * @author Leo Lewis
 */
public class GeoPoint {

	public static final String UNKNOWN = "*";

	/** IP */
	private String _ip;
	/** Host */
	private String _hostname;

	/** Town */
	private String _town;
	/** Country */
	private String _country;
	/** Country iso */
	private String _countryIso;

	/** Latitude (DD) */
	private float _lat;
	/** Longitude (DD) */
	private float _lon;

	/** Number in the path */
	private int _number;

	private boolean _unknownGeo;

	/**
	 * Constructor
	 */
	public GeoPoint() {
	}

	/**
	 * Return the value of the field _ip
	 *
	 * @return the _ip
	 */
	public String getIp() {
		return _ip;
	}

	/**
	 * Set the value of the field _ip
	 *
	 * @param ip the new _ip to set
	 */
	public void setIp(final String ip) {
		_ip = ip;
	}

	/**
	 * Return the value of the field _country
	 *
	 * @return the _country
	 */
	public String getCountry() {
		return _country;
	}

	/**
	 * Set the value of the field _country
	 *
	 * @param country the new _country to set
	 */
	public void setCountry(final String country) {
		_country = country;
	}

	/**
	 * Return the value of the field _lat
	 *
	 * @return the _lat
	 */
	public float getLat() {
		return _lat;
	}

	/**
	 * Set the value of the field _lat
	 *
	 * @param lat the new _lat to set
	 */
	public void setLat(final float lat) {
		_lat = lat;
	}

	/**
	 * Return the value of the field _lon
	 *
	 * @return the _lon
	 */
	public float getLon() {
		return _lon;
	}

	/**
	 * Set the value of the field _lon
	 *
	 * @param lon the new _lon to set
	 */
	public void setLon(final float lon) {
		_lon = lon;
	}

	/**
	 * Return the value of the field _town
	 *
	 * @return the _town
	 */
	public String getTown() {
		return _town;
	}

	/**
	 * Set the value of the field _town
	 *
	 * @param town the new _town to set
	 */
	public void setTown(final String town) {
		_town = town;
	}

	/**
	/**
	 * Return the value of the field hostname
	 *
	 * @return the value of hostname
	 */
	public String getHostname() {
		return _hostname;
	}

	/**
	 * Set the value of the field hostname
	 *
	 * @param hostname the new hostname to set
	 */
	public void setHostname(final String hostname) {
		_hostname = hostname;
	}

	/**
	 * Return the value of the field countryIso
	 *
	 * @return the value of countryIso
	 */
	public String getCountryIso() {
		return _countryIso;
	}

	/**
	 * Set the value of the field countryIso
	 *
	 * @param countryIso the new countryIso to set
	 */
	public void setCountryIso(final String countryIso) {
		_countryIso = countryIso;
	}

	/**
	 * Return the value of the field _number
	 *
	 * @return the _number
	 */
	public int getNumber() {
		return _number;
	}

	/**
	 * Set the value of the field _number
	 *
	 * @param number the new _number to set
	 */
	public void setNumber(final int number) {
		_number = number;
	}

	/**
	 * Get country flag image
	 *
	 * @param resolution
	 * @return image
	 */
	public ImageIcon getCountryFlag(final Resolution resolution) {
		return CountryFlagManager.getImageFor(this, resolution);
	}

	/**
	 * @return
	 */
	public String getCoordKey() {
		return getLat() + "_" + getLon();
	}

	/**
	 * Return the value of the field unknownGeo
	 * @return the value of unknownGeo
	 */
	public boolean isUnknownGeo() {
		return _unknownGeo;
	}

	/**
	 * Set the value of the field unknownGeo
	 * @param unknownGeo the new unknownGeo to set
	 */
	public void setUnknownGeo(final boolean unknownGeo) {
		_unknownGeo = unknownGeo;
	}

}
