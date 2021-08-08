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

import javax.swing.JProgressBar;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;

/**
 * RouteStatusPanel $Id: StatusPanel.java 290 2016-12-31 19:36:34Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author Leo Lewis
 */
public class StatusPanel extends AbstractPanel {

	/**  */
	private static final long serialVersionUID = 2670760035825862852L;

	/** Progress */
	private final JProgressBar _progress;

	/**
	 * Constructor
	 */
	public StatusPanel(final ServiceFactory services) {
		super(services);
		// progress bar
		_progress = new JProgressBar();
		add(_progress, BorderLayout.SOUTH);
		reinit();
	}

	public void reinit() {
		_progress.setValue(0);
		_progress.setStringPainted(true);
		_progress.setString(Resources.getLabel("enter.route.value"));
		_progress.setToolTipText(Resources.getLabel("enter.route.value"));
	}

	@Override
	public void newRoute(final boolean dnsLookup) {
		showMessage(Resources.getLabel("route.tracing"), true);
	}

	@Override
	public void routePointAdded(final RoutePoint point) {

	}

	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
		taskEnded(true, Resources.getLabel("route.tracing.done", tracerouteTime / 1000f, lengthInKm));
	}
	@Override
	public void error(final Exception exception, final Object origin) {
		if (origin == _route) {
			taskEnded(false, Resources.getLabel("route.tracing.error") + exception.getMessage());
		} else if (origin == _sniffer) {
			taskEnded(false, Resources.getLabel("error.sniffer") + exception.getMessage());
		} else if (Env.INSTANCE.getMode() == Mode.WHOIS) {
			taskEnded(false, Resources.getLabel("no.whois.data"));
		}

	}

	@Override
	public void routeCancelled() {
		taskEnded(false, Resources.getLabel("route.tracing.cancelled"));
	}

	@Override
	public void maxHops() {
		taskEnded(false, Resources.getLabel("route.tracing.maxhops"));
	}

	public void routeTimeout() {
		taskEnded(false, Resources.getLabel("route.timeout"));
	}

	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {

	}

	/**
	 * Task ended
	 *
	 * @param complete complete or cancel/error
	 * @param label label to show in the progress bar
	 */
	private void taskEnded(final boolean complete, final String label) {
		_progress.setValue(complete ? 100 : 0);
		_progress.setIndeterminate(false);
		_progress.setString(label);
		_progress.setToolTipText(label);
	}

	/**
	 * Show given message
	 *
	 * @param message
	 * @param indeterminate
	 */
	public void showMessage(final String message, final boolean indeterminate) {
		_progress.setIndeterminate(indeterminate);
		_progress.setString(message);
		_progress.setToolTipText(message);
	}

	@Override
	public void startCapture() {
		showMessage(Resources.getLabel("capture.running"), true);
	}

	@Override
	public void packetAdded(final AbstractPacketPoint point) {
		showMessage(Resources.getLabel("capture.running") + ": " + Resources.getLabel("capture.packet.count", _sniffer.getCapture().size()), true);
	}

	@Override
	public void captureStopped() {
		showMessage(Resources.getLabel("capture.stopped"), false);
	}

	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {

	}

	@Override
	public void startWhoIs(final String host) {
		if (Env.INSTANCE.getMode() == Mode.WHOIS) {
			showMessage(Resources.getLabel("whois.running"), true);
		}
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#focusWhoIs(org.leo.traceroute.core.geo.GeoPoint)
	 */
	@Override
	public void focusWhoIs(final GeoPoint point) {

	}

	@Override
	public void whoIsResult(final String result) {
		if (Env.INSTANCE.getMode() == Mode.WHOIS) {
			showMessage(Resources.getLabel("whois.finished"), false);
		}
	}
}
