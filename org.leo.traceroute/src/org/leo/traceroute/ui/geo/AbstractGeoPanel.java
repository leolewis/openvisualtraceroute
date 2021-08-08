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
package org.leo.traceroute.ui.geo;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.ui.AbstractPanel;

/**
 * AbstractGeoPanel $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
@SuppressWarnings("serial")
public abstract class AbstractGeoPanel extends AbstractPanel implements IMapConfigListener {

	protected boolean _mapShowLabel = Env.INSTANCE.isMapShowLabel();
	protected int _mapLineThickness = Env.INSTANCE.getMapLineThickness();
	protected Mode _mode;
	private final LocPanel _loc;

	/**
	 * Constructor
	 * @param services
	 */
	public AbstractGeoPanel(final ServiceFactory services) {
		super(services);
		Env.INSTANCE.addShowLabelsListener(this);
		_loc = new LocPanel(services);
	}

	@Override
	public void afterShow(final Mode mode) {
		super.afterShow(mode);
		//add(_loc, BorderLayout.SOUTH);
	}

	protected void reinit() {

	}

	@Override
	public void routePointAdded(final RoutePoint point) {
		pointAdded(point, true);
	}

	@Override
	public void packetAdded(final AbstractPacketPoint point) {
		pointAdded(point, false);
	}

	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {
		focusPoint(point, isTracing, animation);
	}

	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {
		focusPoint(point, isCapturing, animation);
	}

	@Override
	public void newRoute(final boolean dnsLookup) {
		_mode = Mode.TRACE_ROUTE;
		reinit();
	}

	@Override
	public void startWhoIs(final String host) {
		if (Env.INSTANCE.getMode() == Mode.WHOIS) {
			_mode = Mode.WHOIS;
			reinit();
		}
	}

	@Override
	public void focusWhoIs(final GeoPoint geo) {
		if (Env.INSTANCE.getMode() == Mode.WHOIS) {
			pointAdded(geo, false);
		}
		focusPoint(geo, true, true);
	}

	@Override
	public void captureStopped() {

	}

	@Override
	public void error(final Exception exception, final Object origin) {
	}

	@Override
	public void routeCancelled() {
	}

	@Override
	public void routeTimeout() {
	}

	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
	}

	@Override
	public void maxHops() {
	}

	@Override
	public void whoIsResult(final String result) {

	}

	protected abstract void pointAdded(final GeoPoint point, final boolean addLine);

	protected abstract void focusPoint(final GeoPoint point, final boolean isRunning, final boolean animation);

	/**
	 * Set the value of the field mapShowLabel
	 * @param mapShowLabel the new mapShowLabel to set
	 */
	@Override
	public void setMapShowLabel(final boolean mapShowLabel) {
		if (mapShowLabel != _mapShowLabel) {
			_mapShowLabel = mapShowLabel;
			changeMapShowLabel(_mapShowLabel);
		}
	}

	@Override
	public void setLineThickness(final int thickness) {

		if (_mapLineThickness != thickness) {
			_mapLineThickness = thickness;
			changeLineThickness(_mapLineThickness);
		}
	}

	protected abstract void changeMapShowLabel(boolean show);

	protected abstract void changeLineThickness(int thickness);

	protected void onMousePosition(final double lat, final double lon) {
		_loc.setPoint(lat, lon);
	}
}
