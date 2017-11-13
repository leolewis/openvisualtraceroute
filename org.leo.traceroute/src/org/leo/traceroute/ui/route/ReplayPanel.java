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
package org.leo.traceroute.ui.route;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.route.ITraceRoute;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.StatusPanel;

/**
 * ReplayPanel $Id: ReplayPanel.java 272 2016-09-22 05:38:31Z leolewis $
 *
 * <pre>
 * Panel to control the replay of a route
 * </pre>
 *
 * @author Leo Lewis
 */
public class ReplayPanel extends AbstractRoutePanel {

	/**  */
	private static final long serialVersionUID = -2811733377173483401L;

	/** Unset */
	private static final int UNSET = -1;

	/** Replay button */
	private JButton _replayButton;

	/** if is replaying */
	private boolean _isReplaying;

	/** Selected index */
	private volatile int _routeSelectedIndex;

	/** Timer for replay */
	private Timer _timer;

	/** Status panel */
	private StatusPanel _statusPanel;

	/**
	 * Constructor
	 *
	 * @param route
	 * @param statusPanel
	 */
	public ReplayPanel(final ServiceFactory services, final StatusPanel statusPanel) {
		super(services);
		final ITraceRoute traceRoute = services.getTraceroute();
		_statusPanel = statusPanel;
		_replayButton = new JButton(Resources.getImageIcon("play.png"));
		_replayButton.setToolTipText(Resources.getLabel("replay.tooltip"));
		_replayButton.setEnabled(false);
		_replayButton.addActionListener(e -> {
			if (traceRoute.size() <= 1) {
				return;
			}
			if (_isReplaying) {
				stopReplay(false, true);
			} else {
				_isReplaying = true;
				if (_routeSelectedIndex >= traceRoute.size() - 1) {
					_routeSelectedIndex = 0;
				}
				_timer = new Timer();
				_timer.schedule(new TimerTask() {
					@Override
					public void run() {
						if (_routeSelectedIndex == UNSET) {
							return;
						}
						if (traceRoute.getRoute().size() > _routeSelectedIndex) {
							traceRoute.focus(traceRoute.getRoute().get(_routeSelectedIndex), true);
							_routeSelectedIndex++;
						}
						if (_routeSelectedIndex >= traceRoute.size()) {
							stopReplay(false, true);
							_routeSelectedIndex = 0;
						}
					}
				}, 0, Env.INSTANCE.getReplaySpeed());
				_replayButton.setIcon(Resources.getImageIcon("pause.png"));
				_statusPanel.showMessage(Resources.getLabel("replay.in.pogress"), true);
			}
		});
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#newRoute(boolean)
	 */
	@Override
	public void newRoute(final boolean dnsLookup) {
		stopReplay(true, false);
		_replayButton.setEnabled(false);
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#routePointAdded(org.leo.traceroute.core.RoutePoint)
	 */
	@Override
	public void routePointAdded(final RoutePoint point) {

	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#done(long)
	 */
	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#error(java.io.IOException)
	 */
	@Override
	public void error(final Exception exception, final Object origin) {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#cancelled()
	 */
	@Override
	public void routeCancelled() {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#maxHops()
	 */
	@Override
	public void maxHops() {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#timeout()
	 */
	@Override
	public void routeTimeout() {
		traceRouteEnded();
	}

	private void traceRouteEnded() {
		_replayButton.setEnabled(true);
	}

	/**
	 * @see org.leo.traceroute.core.IRouteListener#focusRoute(org.leo.traceroute.core.RoutePoint, boolean, boolean)
	 */
	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {
		_routeSelectedIndex = _route.getRoute().indexOf(point);
	}

	/**
	 * @see org.leo.traceroute.ui.AbstractRoutePanel#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		stopReplay(true, true);
	}

	/**
	 * Stop the replay
	 *
	 * @param resetRouteIndex reset route
	 * @param clearMessage if clear the message on the status
	 */
	private void stopReplay(final boolean resetRouteIndex, final boolean clearMessage) {
		_isReplaying = false;
		if (resetRouteIndex) {
			_routeSelectedIndex = UNSET;
		}
		if (_timer != null) {
			_timer.cancel();
			_timer = null;
		}
		if (clearMessage) {
			_statusPanel.showMessage("", false);
		}
		_replayButton.setIcon(Resources.getImageIcon("play.png"));
	}

	public JButton getReplayButton() {
		return _replayButton;
	}
}
