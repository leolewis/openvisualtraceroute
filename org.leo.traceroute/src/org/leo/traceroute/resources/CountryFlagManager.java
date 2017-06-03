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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CountryFlagManager $Id: CountryFlagManager.java 231 2016-01-27 08:24:31Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author leo.lewis
 */
public class CountryFlagManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(CountryFlagManager.class);

	/**
	 * Resolution $Id: CountryFlagManager.java 231 2016-01-27 08:24:31Z leolewis $
	 */
	public enum Resolution {

		R16(16),
		R32(32);

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

	/** cache */
	private static final Map<String, ImageIcon> CACHE = new HashMap<String, ImageIcon>();

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
		final String key = resolution.name() + "_" + point.getCountryIso();

		ImageIcon icon = CACHE.get(key);
		if (icon == null) {
			try {
				if (!GeoPoint.UNKNOWN.equals(point.getCountryIso())) {
					icon = Resources.getImageIcon("flag/" + resolution._res + "/" + point.getCountryIso() + ".png");
				}
			} catch (final Exception e) {
				LOGGER.warn("Failed to load icon {}", key, e);
			}
			if (icon == null) {
				switch (resolution) {
				case R16:
					icon = Resources.getImageIcon("info.gif");
					break;
				default:
					icon = Resources.getImageIcon("info2.png");
					break;
				}
			}
			CACHE.put(key, icon);
		}
		return icon;
	}
}
