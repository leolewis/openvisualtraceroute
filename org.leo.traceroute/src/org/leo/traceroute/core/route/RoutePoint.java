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
package org.leo.traceroute.core.route;

import java.awt.Color;

import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.ui.util.ColorUtil;

/**
 * Route Point $Id: RoutePoint.java 232 2016-01-30 04:39:16Z leolewis $ A route
 * point.
 *
 * @author Leo Lewis
 */
public class RoutePoint extends GeoPoint {

	/** Elevation  */
	private double _elevation;

	/** Color  */
	private Color _color = Color.RED.brighter();

	/** Latency */
	private int _latency;

	/** Distance to previous point in the route (in km) */
	private int _distanceToPrevious;

	/** Dns lookup time */
	private long _dnsLookUpTime;

	private boolean _isUnknown;

	/**
	 * Constructor
	 */
	public RoutePoint() {
	}

	/**
	 * Return the value of the field latency
	 *
	 * @return the value of latency
	 */
	public int getLatency() {
		return _latency;
	}

	/**
	 * Set the value of the field latency
	 *
	 * @param latency the new latency to set
	 */
	public void setLatency(final int latency) {
		this._latency = latency;
		// elevation proportional to the latency
		setElevation(latency);
		setColor(ColorUtil.INSTANCE.getColorForLatency(latency));
	}

	/**
	 * Return the value of the field distanceToPrevious
	 *
	 * @return the value of distanceToPrevious
	 */
	public int getDistanceToPrevious() {
		return _distanceToPrevious;
	}

	/**
	 * Set the value of the field distanceToPrevious
	 *
	 * @param distanceToPrevious the new distanceToPrevious to set
	 */
	public void setDistanceToPrevious(final int distanceToPrevious) {
		_distanceToPrevious = distanceToPrevious;
	}

	/**
	 * Return the value of the field dnsLookUpTime
	 *
	 * @return the value of dnsLookUpTime
	 */
	public long getDnsLookUpTime() {
		return _dnsLookUpTime;
	}

	/**
	 * Set the value of the field dnsLookUpTime
	 *
	 * @param dnsLookUpTime the new dnsLookUpTime to set
	 */
	public void setDnsLookUpTime(final long dnsLookUpTime) {
		this._dnsLookUpTime = dnsLookUpTime;
	}

	/**
	 * Return the value of the field elevation
	 * @return the value of elevation
	 */
	public double getElevation() {
		return _elevation;
	}

	/**
	 * Set the value of the field elevation
	 * @param elevation the new elevation to set
	 */
	public void setElevation(final double elevation) {
		_elevation = elevation;
	}

	/**
	 * Return the value of the field color
	 * @return the value of color
	 */
	public Color getColor() {
		return _color;
	}

	/**
	 * Set the value of the field color
	 * @param color the new color to set
	 */
	public void setColor(final Color color) {
		_color = color;
	}

	/**
	 * Return the value of the field isUnknown
	 * @return the value of isUnknown
	 */
	public boolean isUnknown() {
		return _isUnknown;
	}

	/**
	 * Set the value of the field isUnknown
	 * @param isUnknown the new isUnknown to set
	 */
	public void setUnknown(final boolean isUnknown) {
		_isUnknown = isUnknown;
	}

	public RoutePoint toUnkown() {
		final RoutePoint copy = new RoutePoint();
		copy.setCountry(UNKNOWN);
		copy.setCountryIso(UNKNOWN);
		copy.setTown(UNKNOWN);
		copy.setDnsLookUpTime(0);
		copy.setHostname(UNKNOWN);
		copy.setIp(UNKNOWN);
		copy.setNumber(getNumber() + 1);
		copy.setLat(getLat());
		copy.setLon(getLon());
		copy._isUnknown = true;
		return copy;
	}

}
