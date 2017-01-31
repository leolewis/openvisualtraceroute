/**
 * Open Visual Trace Route
 * Copyright (c) 2010-2015 Leo Lewis.
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
package org.leo.traceroute.resources;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.leo.traceroute.core.geo.GeoPoint;

/**
 * CountryFlagManager $Id: CountryFlagManager.java 231 2016-01-27 08:24:31Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author leo.lewis
 */
public class CountryFlagManager {

	/**
	 * Resolution $Id: CountryFlagManager.java 231 2016-01-27 08:24:31Z leolewis $
	 */
	public enum Resolution {

		R16(16),
		R32(32),
		R64(64);

		/** res */
		int _res;

		/**
		 * Constructor
		 *
		 * @param res
		 */
		private Resolution(final int res) {
			_res = res;
		}
	}

	/** Unknown */
	private static final Map<Resolution, ImageIcon> UNKNOWN = new HashMap<Resolution, ImageIcon>() {
		{
			put(Resolution.R16, Resources.getImageIcon("info.gif"));
			put(Resolution.R32, Resources.getImageIcon("info2.png"));
			put(Resolution.R64, Resources.getImageIcon("info3.png"));
		}
	};

	/**
	 * Get the image for the given route point
	 *
	 * @param point
	 * @return image
	 */
	public static ImageIcon getImageFor(final GeoPoint point) {
		return getImageFor(point, Resolution.R16);
	}

	/**
	 * Get the image for the given route point and the given resolution
	 *
	 * @param point
	 * @param resolution
	 * @return image
	 */
	public synchronized static ImageIcon getImageFor(final GeoPoint point, final Resolution resolution) {
		try {
			if (point.getCountryIso() == GeoPoint.UNKNOWN) {
				return UNKNOWN.get(resolution);
			}
			return Resources.getImageIcon("flag/" + resolution._res + "/" + point.getCountryIso() + ".png");
		} catch (final Exception e) {
			return UNKNOWN.get(resolution);
		}
	}
}
