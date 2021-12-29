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
package org.leo.traceroute.core.route.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.AbstractObject;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.network.INetworkInterfaceListener;
import org.leo.traceroute.core.route.IRouteListener;
import org.leo.traceroute.core.route.ITraceRoute;
import org.leo.traceroute.core.route.MaxHopsException;
import org.leo.traceroute.core.route.RouteException;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.ui.AbstractPanel;
import org.leo.traceroute.ui.route.RouteTablePanel.Column;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AbstractTraceRoute $Id: AbstractTraceRoute.java 290 2016-12-31 19:36:34Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author Leo Lewis
 */
public abstract class AbstractTraceRoute<T> extends AbstractObject<IRouteListener> implements ITraceRoute, INetworkInterfaceListener<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTraceRoute.class);

	/** Route */
	protected List<RoutePoint> _route = new ArrayList<>();

	/** Route length in KM */
	protected AtomicLong _lengthInKm;
	/** Traceroute time */
	protected volatile long _tracerouteTime;

	/** DNS lookup ? */
	protected volatile boolean _resolveHostname;

	/** Focused point */
	protected RoutePoint _focusedPoint;

	/** Selected Network device */
	protected T _device;

	/** Services factory */
	protected ServiceFactory _services;

	private final Semaphore _semaphore = new Semaphore(1);

	private final BlockingQueue<RoutePoint> _notifyQueue = new LinkedBlockingQueue<>();
	private final AtomicInteger _notified = new AtomicInteger();
	private final Thread _notifyThread = new Thread("Route async notify thread") {
		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					final RoutePoint point = _notifyQueue.poll(10, TimeUnit.MILLISECONDS);
					if (point != null) {
						// notify route point added
						notifyListeners((listener) -> listener.routePointAdded(point));
						// focus on the point
						notifyListeners((listener) -> listener.focusRoute(point, true, true));
						_notified.incrementAndGet();
					}
				} catch (final InterruptedException e) {

				}
			}
		}
	};

	/**
	 * Constructor
	 */
	protected AbstractTraceRoute() {
		super();
	}

	@Override
	public void init(final ServiceFactory services) throws IOException {
		_services = services;
		_notifyThread.setDaemon(true);
		_notifyThread.start();
	}

	/**
	 * Compute the route to the given destination
	 *
	 * @param dest destination
	 * @param monitor cancelMonitor that will be used to see if the calling
	 *            component wants to interrupt the process
	 * @param resolveHostname if resolve host name
	 */
	@Override
	public void compute(final String dest, final CancelMonitor monitor, final boolean resolveHostname, final long timeOutMs, final boolean ipV4, final int maxHops) {
		try {
			_semaphore.acquire();
			_route.clear();
			_notifyQueue.clear();
			_notified.set(0);
			_resolveHostname = resolveHostname;
			_lengthInKm = new AtomicLong();
			_tracerouteTime = 0L;
			_focusedPoint = null;
			// delete useless characters sequence to keep only the hostname or ip
			String formatedDest = dest;
			if (formatedDest.toLowerCase().startsWith("http://")) {
				formatedDest = dest.substring(7);
			} else if (formatedDest.toLowerCase().startsWith("https://")) {
				formatedDest = formatedDest.substring(8);
			} else if (formatedDest.toLowerCase().startsWith("ftp://")) {
				formatedDest = formatedDest.substring(6);
			} else if (formatedDest.toLowerCase().startsWith("ftps://")) {
				formatedDest = formatedDest.substring(7);
			}
			if (formatedDest.contains("/")) {
				formatedDest = formatedDest.substring(0, formatedDest.indexOf("/"));
			}
			formatedDest = formatedDest.trim();
			final String fdest = formatedDest;
			// launch the tracing in a thread
			final AtomicBoolean timedOut = new AtomicBoolean();
			_threadPool.execute(() -> {
				try {
					// notify new route
					notifyListeners((listener) -> listener.newRoute(resolveHostname));
					// compute route
					final long time = System.currentTimeMillis();
					// check for time out if required
					Timer timer = null;
					if (timeOutMs > 0) {
						final long startTime = System.currentTimeMillis();
						timer = new Timer(true);
						timer.scheduleAtFixedRate(new TimerTask() {
							@Override
							public void run() {
								// check if timed out
								if (System.currentTimeMillis() - startTime > timeOutMs) {
									timedOut.set(true);
									monitor.setCanceled(true);
									cancel();
								}
							}
						}, 0, 100);
					}
					LOGGER.info("Starting traceroute to {} with maxhops={} and timeout={}ms", fdest, maxHops, timeOutMs);
					computeRoute(fdest, monitor, resolveHostname, ipV4, maxHops);
					if (timer != null) {
						timer.cancel();
					}
					// if timed out, nobody to notify (already done)
					if (!timedOut.get()) {
						// if monitor canceled, notify canceled
						if (monitor.isCanceled()) {
							notifyListeners(listener -> listener.routeCancelled());
						} else {
							// notify done
							_tracerouteTime = System.currentTimeMillis() - time;
							// wait for pending notifications
							while (_notified.get() < _route.size()) {
								Thread.sleep(100);
							}
							notifyListeners(listener -> listener.routeDone(_tracerouteTime, _lengthInKm.get()));
						}
					} else {
						// notify listeners
						notifyListeners(listener -> listener.routeTimeout());
					}
					// if the traceroute didn't failed, add it to the history
					_services.getAutocomplete().addToHistory(dest);
					LOGGER.info("Traceroute to {} completed.", fdest);
				} catch (final Exception e) {
					if (!monitor.isCanceled() && !timedOut.get()) {
						if (e instanceof MaxHopsException) {
							// notify max hops
							LOGGER.warn("Traceroute to {} stopped because reached the max hops", fdest);
							notifyListeners(listener -> listener.maxHops());
						} else {
							// notify error
							LOGGER.error("Traceroute to {} failed", fdest, e);
							notifyListeners(listener -> listener.error(e, AbstractTraceRoute.this));
						}
					}
				} finally {
					_semaphore.release();
				}
			});
		} catch (final Exception e) {
			if (!monitor.isCanceled()) {
				// notify error
				LOGGER.error("Traceroute failed", e);
				notifyListeners(listener -> listener.error(e, AbstractTraceRoute.this));
			}
			_semaphore.release();
		}
	}

	/**
	 * Compute the route using the network library specified
	 * @param formatedDest
	 * @param monitor
	 * @param resolveHostname
	 * @param ipV4
	 * @param maxHops
	 */
	protected abstract void computeRoute(final String formatedDest, final CancelMonitor monitor, final boolean resolveHostname, final boolean ipV4, final int maxHops) throws Exception;

	/**
	 * Add a point corresponding to the given IP
	 *
	 * @param ipAndHost the IP and host
	 * @param latency latency
	 * @param dnslookupTime
	 */
	protected RoutePoint addPoint(final Pair<String, String> ipAndHost, final int latency, final long dnslookupTime) {
		final String ip = ipAndHost.getLeft();
		final String dns = ipAndHost.getRight();
		RoutePoint point;
		// set to previous point
		RoutePoint previous = null;
		if (!_route.isEmpty()) {
			previous = _route.get(_route.size() - 1);
		}
		if (ip.startsWith("192.168.") || ip.equals("127.0.0.1") || ip.startsWith("fc00::/7")) {
			// private Ips, calculate location with public IP
			point = _services.getGeo().populateGeoDataForIP(new RoutePoint(), _services.getGeo().getPublicIp().getLeft(), null);
			point.setIp(ip);
		} else {
			point = _services.getGeo().populateGeoDataForIP(new RoutePoint(), ip, dns, previous);
		}
		// unknown location
		if (point.isUnknownGeo()) {
			if (_route.isEmpty()) {
				// set to local ip
				point = _services.getGeo().populateGeoDataForIP(new RoutePoint(), _services.getGeo().getPublicIp().getLeft(), null);
			} else {
				point.setCountry(previous.getCountry());
				point.setCountryIso(previous.getCountryIso());
				point.setTown(previous.getTown());
				point.setLat(previous.getLat());
				point.setLon(previous.getLon());
			}
		}
		point.setNumber(_route.size() + 1);
		point.setLatency(latency);
		point.setDnsLookUpTime(dnslookupTime);
		point.setHostname(dns);
		return addPoint(point);
	}

	protected RoutePoint addPoint(final RoutePoint point) {
		if (!_route.isEmpty()) {
			final RoutePoint previousPoint = _route.get(_route.size() - 1);
			final int distance = Util.distance(point, previousPoint);
			point.setDistanceToPrevious(distance);
			_lengthInKm.addAndGet(distance);
		}
		_route.add(point);
		_notifyQueue.offer(point);
		return point;
	}

	@Override
	public List<RoutePoint> getRoute() {
		return _route;
	}

	@Override
	public void dispose() {
		super.dispose();
		_route.clear();
		_threadPool.shutdown();
		_notifyThread.interrupt();
	}

	@Override
	public int size() {
		return _route.size();
	}

	@Override
	public void focus(final RoutePoint point, final boolean animation) {
		_focusedPoint = point;
		notifyListeners(listener -> listener.focusRoute(point, false, animation));
	}

	@Override
	public String toCSV() {
		return toString(", ");
	}

	@Override
	public String toText() {
		return toString("\t");
	}

	public String toString(final String separator) {
		final StringBuilder builder = new StringBuilder();
		final int colSize = Column.values().length;
		for (int i = 0; i < colSize; i++) {
			final Column col = Column.values()[i];
			if (col.isExport()) {
				builder.append(col.getLabel());
				if (i != colSize - 1) {
					builder.append(separator);
				}
			}
		}
		builder.append("\n");
		for (int row = 0; row < size(); row++) {
			for (int col = 0; col < colSize; col++) {
				final Column column = Column.values()[col];
				if (column.isExport()) {
					builder.append(column.getValue(_route.get(row)));
					if (col != colSize - 1) {
						builder.append(separator);
					}
				}
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	@Override
	public void renotifyRoute() {
		if (!_route.isEmpty()) {
			notifyListeners(listener -> {
				listener.newRoute(_resolveHostname);
				for (final RoutePoint point : _route) {
					listener.routePointAdded(point);
				}
				listener.routeDone(_tracerouteTime, _lengthInKm.get());
				if (_focusedPoint != null) {
					focus(_focusedPoint, false);
				}
			});
		}
	}

	@Override
	public void notifyNewNetworkInterface(final T device, Mode mode, final byte[] mac) {
		if (mode != Mode.TRACE_ROUTE) {
			return;
		}
		_device = device;
	}

	@Override
	public void clear() {
		_route.clear();
	}

}
