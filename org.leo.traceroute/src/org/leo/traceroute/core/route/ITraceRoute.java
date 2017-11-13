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

import java.util.List;

import org.leo.traceroute.core.IComponent;
import org.leo.traceroute.ui.task.CancelMonitor;

/**
 * ITraceRoute $Id: ITraceRoute.java 231 2016-01-27 08:24:31Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public interface ITraceRoute extends IComponent {

	/**
	 * Compute the route to the given destination
	 *
	 * @param dest destination
	 * @param monitor cancelMonitor that will be used to see if the calling
	 *            component wants to interrupt the process
	 * @param resolveHostname if resolve host name
	 * @param timeOutMs time out (ms)
	 * @param useOsTraceroute if use the OS traceroute, or the embedded network library
	 * @param useIpV4 true to use ipv4, false for ipv6
	 * @param maxHops max number of hops
	 */
	void compute(final String dest, final CancelMonitor monitor, final boolean resolveHostname, final long timeOutMs, final boolean useOsTraceroute,
			final boolean useIpV4, final int maxHops);

	/**
	 * The computed route
	 *
	 * @return the route
	 */
	List<RoutePoint> getRoute();

	/**
	 * Add a listener
	 *
	 * @param listener
	 */
	void addListener(final IRouteListener listener);

	/**
	 * Remove a listener
	 *
	 * @param listener
	 */
	void removeListener(final IRouteListener listener);

	/**
	 * Notify the listeners of a focus on the given point
	 *
	 * @param point the point to focus
	 * @param animation if execute an animation when focusing
	 */
	void focus(final RoutePoint point, final boolean animation);

	/**
	 * Size of the route
	 *
	 * @return size
	 */
	int size();

	/**
	 * Convert the route to CSV
	 */
	String toCSV();

	/**
	 * Convert the route to text
	 */
	String toText();

	/**
	 * Resend notification of points added to the route to the listeners
	 */
	void renotifyRoute();

	/**
	 * Clear the route
	 */
	void clear();
}
