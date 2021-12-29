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
package org.leo.traceroute.ui.route;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.ui.AbstractPanel;

/**
 * AbstractRoutePanel $Id: AbstractRoutePanel.java 208 2015-10-12 07:07:41Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public abstract class AbstractRoutePanel extends AbstractPanel {

	/**  */
	private static final long serialVersionUID = 8684835442487875859L;

	/**
	 * Constructor
	 */
	public AbstractRoutePanel(final ServiceFactory services) {
		super(services);
	}

	@Override
	public final void startCapture() {
	}

	@Override
	public final void packetAdded(final AbstractPacketPoint point) {
	}

	@Override
	public final void captureStopped() {
	}

	@Override
	public final void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {

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
