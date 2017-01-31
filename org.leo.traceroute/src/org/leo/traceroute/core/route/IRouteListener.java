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

import org.leo.traceroute.core.IListener;

/**
 * RouteListener $Id: IRouteListener.java 232 2016-01-30 04:39:16Z leolewis $
 * Object that want to be notify of trace route status must implement this
 * interface and add to the Route Listeners
 *
 * @author Leo Lewis
 */
public interface IRouteListener extends IListener {

	/**
	 * New route created
	 *
	 * @param dnsLookup if dns lookup is done for this route
	 */
	void newRoute(boolean dnsLookup);

	/**
	 * Route point added notification
	 *
	 * @param point point added
	 */
	void routePointAdded(RoutePoint point);

	/**
	 * Route computation done
	 *
	 * @param tracerouteTime traceroute time
	 * @param length route length
	 */
	void routeDone(long tracerouteTime, long lengthInKm);

	/**
	 * Trace route timed out
	 */
	void routeTimeout();

	/**
	 * Reached max hops
	 */
	void maxHops();

	/**
	 * Tracing cancelled
	 */
	void routeCancelled();

	/**
	 * Focus the given route point
	 *
	 * @param point the point
	 * @param isTracing if the focus is done when the route is tracing
	 * @param animation if we do an animation (might not be supported by the view)
	 */
	void focusRoute(RoutePoint point, boolean isTracing, boolean animation);
}
