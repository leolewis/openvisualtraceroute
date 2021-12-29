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
package org.leo.traceroute.ui.sniffer;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.ui.AbstractPanel;

/**
 * AbstractSnifferPanel $Id: AbstractSnifferPanel.java 232 2016-01-30 04:39:16Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public abstract class AbstractSnifferPanel extends AbstractPanel {

	/**  */
	private static final long serialVersionUID = 8684835442487875859L;

	/**
	 * Constructor
	 * @param services
	 */
	public AbstractSnifferPanel(final ServiceFactory services) {
		super(services);
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#newRoute(boolean)
	 */
	@Override
	public final void newRoute(final boolean dnsLookup) {
	}

	@Override
	public final void routePointAdded(final RoutePoint point) {
	}

	@Override
	public final void routeDone(final long tracerouteTime, final long lengthInKm) {
	}

	@Override
	public final void routeTimeout() {
	}

	@Override
	public void maxHops() {
	}

	@Override
	public final void error(final Exception exception, final Object origin) {
	}

	@Override
	public final void routeCancelled() {
	}

	@Override
	public final void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {
	}

	@Override
	public final void startWhoIs(final String host) {
	}

	@Override
	public void focusWhoIs(final GeoPoint point) {
	}

	@Override
	public final void whoIsResult(final String result) {
	}
}
