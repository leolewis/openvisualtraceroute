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
package org.leo.traceroute.ui.util;

import java.awt.Color;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

/**
 * ColorUtil $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public enum ColorUtil {

	INSTANCE;

	private final RangeMap<Integer, Color> _latencyToColor;
	private final RangeMap<Integer, Color> _numOfPointsToColor;

	private ColorUtil() {
		_latencyToColor = ImmutableRangeMap.<Integer, Color> builder().put(Range.lessThan(5), new Color(0, 51, 255)).put(Range.closedOpen(5, 10), new Color(0, 126, 246))
				.put(Range.closedOpen(10, 20), new Color(0, 231, 244)).put(Range.closedOpen(20, 50), new Color(0, 249, 126))
				.put(Range.closedOpen(50, 100), new Color(255, 255, 102)).put(Range.closedOpen(100, 200), new Color(255, 233, 55))
				.put(Range.closedOpen(200, 500), new Color(255, 172, 39)).put(Range.closedOpen(500, 1000), new Color(255, 23, 21))
				.put(Range.atLeast(1000), new Color(219, 0, 0)).build();
		_numOfPointsToColor = ImmutableRangeMap.<Integer, Color> builder().put(Range.lessThan(2), new Color(0, 51, 255))
				.put(Range.closedOpen(2, 5), new Color(0, 126, 246)).put(Range.closedOpen(5, 10), new Color(0, 231, 244))
				.put(Range.closedOpen(10, 20), new Color(0, 249, 126)).put(Range.closedOpen(20, 40), new Color(255, 255, 102))
				.put(Range.closedOpen(40, 80), new Color(255, 233, 55)).put(Range.closedOpen(80, 100), new Color(255, 172, 39))
				.put(Range.closedOpen(100, 150), new Color(255, 23, 21)).put(Range.atLeast(150), new Color(219, 0, 0)).build();
	}

	public Color getColorForLatency(final int latency) {
		return _latencyToColor.get(latency);
	}

	public Color getColorForNumOfPoints(final int num) {
		return _numOfPointsToColor.get(num);
	}

	public Color brighter(final Color c, final float f) {
		int r, g, b;
		if (c.getRed() > c.getGreen()) {
			if (c.getRed() > c.getBlue()) {
				r = Math.min(255, (int) (c.getRed() * f));
				g = c.getGreen();
				b = c.getBlue();
			} else if (c.getRed() < c.getBlue()) {
				r = c.getRed();
				g = c.getBlue();
				b = Math.min(255, (int) (c.getBlue() * f));
			} else {
				r = Math.min(255, (int) (c.getRed() * f));
				g = c.getBlue();
				b = Math.min(255, (int) (c.getBlue() * f));
			}
		} else if (c.getRed() < c.getGreen()) {
			if (c.getGreen() > c.getBlue()) {
				r = c.getRed();
				g = Math.min(255, (int) (c.getGreen() * f));
				b = c.getBlue();
			} else if (c.getGreen() < c.getBlue()) {
				r = c.getRed();
				g = c.getBlue();
				b = Math.min(255, (int) (c.getBlue() * f));
			} else {
				r = c.getRed();
				g = Math.min(255, (int) (c.getGreen() * f));
				b = Math.min(255, (int) (c.getBlue() * f));
			}
		} else {
			if (c.getGreen() > c.getBlue()) {
				r = Math.min(255, (int) (c.getRed() * f));
				g = c.getBlue();
				b = Math.min(255, (int) (c.getBlue() * f));
			} else if (c.getGreen() < c.getBlue()) {
				r = c.getRed();
				g = c.getBlue();
				b = Math.min(255, (int) (c.getBlue() * f));
			} else {
				r = Math.min(255, (int) (c.getRed() * f));
				g = Math.min(255, (int) (c.getGreen() * f));
				b = Math.min(255, (int) (c.getBlue() * f));
			}
		}
		return new Color(r, g, b);
	}
}
