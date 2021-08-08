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
package org.leo.traceroute.ui;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.IRouteListener;
import org.leo.traceroute.core.route.ITraceRoute;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.core.sniffer.IPacketListener;
import org.leo.traceroute.core.sniffer.IPacketsSniffer;
import org.leo.traceroute.core.whois.IWhoIsListener;
import org.leo.traceroute.core.whois.WhoIs;
import org.leo.traceroute.resources.Resources;

/**
 * AbstractPanel $Id: AbstractPanel.java 204 2015-08-30 22:31:06Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public abstract class AbstractPanel extends JPanel implements IRouteListener, IPacketListener, IWhoIsListener {

	/**  */
	private static final long serialVersionUID = -1488129292849418998L;
	protected static final Color SELECTED_COLOR = new Color(250, 20, 0, 255);
	protected static final Color UNSELECTED_COLOR = new Color(0, 50, 150, 255);
	protected static final Color SOURCE_COLOR = new Color(0, 150, 50, 255);

	/** Services */
	protected ServiceFactory _services;
	/** Route */
	protected ITraceRoute _route;
	/** Sniffer */
	protected IPacketsSniffer _sniffer;
	/** Whois */
	protected WhoIs _whois;

	protected String youAreHere = Resources.getLabel("you.are.here");

	/**
	 * Constructor
	 * @param services route
	 */
	public AbstractPanel(final ServiceFactory services) {
		super(new BorderLayout());
		_services = services;
		if (_services != null) {
			_route = services.getTraceroute();
			if (_route != null) {
				_route.addListener(this);
			}
			_sniffer = services.getSniffer();
			if (_sniffer != null) {
				_sniffer.addListener(this);
			}
			_whois = services.getWhois();
			if (_whois != null) {
				_whois.addListener(this);
			}
		}
	}

	/**
	 * Dispose the panel
	 */
	public void dispose() {
		if (_route != null) {
			_route.removeListener(this);
		}
		if (_sniffer != null) {
			_sniffer.removeListener(this);
		}
	}

	public void afterShow(final Mode mode) {
	}

	/**
	 * Text for a point
	 *
	 * @param point
	 * @return text
	 */
	public static String getText(final GeoPoint point) {
		return point.getTown() == null ? point.getCountry() : point.getTown();
	}

	public static Color getColor(final boolean selected) {
		return selected ? SELECTED_COLOR : UNSELECTED_COLOR;
	}

	protected void focus(final GeoPoint point) {
		if (point instanceof RoutePoint && _route != null) {
			_route.focus((RoutePoint) point, true);
		} else if (point instanceof AbstractPacketPoint && _sniffer != null) {
			_sniffer.focus((AbstractPacketPoint) point, true);
		}
	}

	/**
	 * Return the value of the field services
	 * @return the value of services
	 */
	public ServiceFactory getServices() {
		return _services;
	}

}
