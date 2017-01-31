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

import org.leo.traceroute.core.AbstractObject;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.network.INetworkInterfaceListener;
import org.leo.traceroute.core.route.IRouteListener;
import org.leo.traceroute.core.route.ITraceRoute;
import org.leo.traceroute.core.route.MaxHopsException;
import org.leo.traceroute.core.route.RouteException;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.ui.route.RouteTablePanel.Column;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.leo.traceroute.util.Pair;
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
public abstract class AbstractTraceRoute<T> extends AbstractObject<IRouteListener>
		implements ITraceRoute, INetworkInterfaceListener<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTraceRoute.class);

	/** Route */
	protected List<RoutePoint> _route = new ArrayList<RoutePoint>();

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

	private final BlockingQueue<RoutePoint> _notifyQueue = new LinkedBlockingQueue<RoutePoint>();
	private final AtomicInteger _notified = new AtomicInteger();
	private final Thread _notifyThread = new Thread("Route async notify thread") {
		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					final RoutePoint point = _notifyQueue.poll(10, TimeUnit.MILLISECONDS);
					// notify in the EDT
					if (point != null) {
						SwingUtilities.invokeAndWait(new Runnable() {
							@Override
							public void run() {
								// notify route point added
								for (final IRouteListener listener : getListeners()) {
									listener.routePointAdded(point);
								}
								// focus on the point
								for (final IRouteListener listener : getListeners()) {
									listener.focusRoute(point, true, true);
								}
								_notified.incrementAndGet();
							}
						});
					}
				} catch (final InterruptedException e) {

				} catch (final InvocationTargetException e) {
				}

			}
		}
	};
	{
		_notifyThread.setDaemon(true);
	}

	/**
	 * Constructor
	 */
	protected AbstractTraceRoute() {
		super();
	}

	/**
	 * @see org.leo.traceroute.core.ITraceRoute#init()
	 */
	@Override
	public void init(final ServiceFactory services) throws IOException {
		_services = services;
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
	public void compute(final String dest, final CancelMonitor monitor, final boolean resolveHostname, final long timeOutMs,
			final boolean useOsTraceroute, final boolean ipV4, final int maxHops) {
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
			_threadPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						// notify new route
						for (final IRouteListener listener : getListeners()) {
							listener.newRoute(resolveHostname);
						}
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
						LOGGER.info("Starting {} traceroute to {} with maxhops={} and timeout={}ms",
								useOsTraceroute ? "OS" : "embedded", fdest, maxHops, timeOutMs);
						if (useOsTraceroute || !ipV4) {
							computeOSRoute(fdest, monitor, resolveHostname, ipV4, maxHops);
						} else {
							computeRoute(fdest, monitor, resolveHostname, maxHops);
						}
						if (timer != null) {
							timer.cancel();
						}
						// if timed out, nobody to notify (already done)
						if (!timedOut.get()) {
							// if monitor canceled, notify canceled
							if (monitor.isCanceled()) {
								for (final IRouteListener listener : getListeners()) {
									listener.routeCancelled();
								}
							} else {
								// notify done
								_tracerouteTime = System.currentTimeMillis() - time;
								// wait for pending notifications
								while (_notified.get() < _route.size()) {
									Thread.sleep(100);
								}
								for (final IRouteListener listener : getListeners()) {
									listener.routeDone(_tracerouteTime, _lengthInKm.get());
								}
							}
						} else {
							// notify listeners
							for (final IRouteListener listener : getListeners()) {
								listener.routeTimeout();
							}
						}
						// if the traceroute didn't failed, add it to the history
						_services.getAutocomplete().addToHistory(dest);
						LOGGER.info("Traceroute to {} completed.", fdest);
					} catch (final Exception e) {
						if (!monitor.isCanceled() && !timedOut.get()) {
							if (e instanceof MaxHopsException) {
								// notify max hops
								LOGGER.warn("Traceroute to {} stopped because reached the max hops", fdest);
								for (final IRouteListener listener : getListeners()) {
									listener.maxHops();
								}
							} else {
								// notify error
								LOGGER.error("Traceroute to {} failed", fdest, e);
								for (final IRouteListener listener : getListeners()) {
									listener.error(e, AbstractTraceRoute.this);
								}
							}
						}
					} finally {
						_semaphore.release();
					}
				}
			});
		} catch (final Exception e) {
			if (!monitor.isCanceled()) {
				// notify error
				LOGGER.error("Traceroute failed", e);
				for (final IRouteListener listener : getListeners()) {
					listener.error(e, AbstractTraceRoute.this);
				}
			}
			_semaphore.release();
		}
	}

	/**
	 * Compute the route using the network library specified
	 * @param formatedDest
	 * @param monitor
	 * @param resolveHostname
	 * @param maxHops
	 */
	protected abstract void computeRoute(final String formatedDest, final CancelMonitor monitor, final boolean resolveHostname,
			final int maxHops) throws Exception;

	/**
	 * Compute the route using OS command
	 * @param formatedDest
	 * @param monitor
	 * @param resolveHostname
	 */
	private void computeOSRoute(final String formatedDest, final CancelMonitor monitor, final boolean resolveHostname,
			final boolean ipV4, final int maxHops) throws Exception {
		try {
			String cmd;
			if (Env.INSTANCE.getOs() == OS.win) {
				cmd = "tracert";
				if (!resolveHostname) {
					cmd += " -d";
				}
				if (!ipV4) {
					cmd += " -6";
				}
				cmd += " -h " + maxHops;
			} else {
				cmd = "traceroute";
				if (!ipV4) {
					cmd += "6";
				}
				cmd += " -q 1";
				if (!resolveHostname) {
					cmd += " -n";
				}
				cmd += " -m " + maxHops;
			}
			final Process process = Runtime.getRuntime().exec(cmd + " " + formatedDest);
			try {
				final InputStream input = process.getInputStream();
				final int ignoreLines = Env.INSTANCE.getOs() == OS.win ? 4 : (Env.INSTANCE.getOs() == OS.mac ? 0 : 1);
				// check if the host exists
				final String destIp = InetAddress.getByName(formatedDest).getHostAddress();
				int lineNum = 0;
				boolean completed = false;
				RoutePoint previous = null;
				while (!completed && !monitor.isCanceled()) {
					char c;
					final StringBuilder linebuffer = new StringBuilder();
					do {
						final int r = input.read();
						if (r == -1) {
							if (Env.INSTANCE.getOs() == OS.win) {
								//on windows, we expect a Trace complete to terminate the execution
								throw new RouteException("Failed to traceroute to host");
							} else {
								// but on other OS, that's just an end of stream
								completed = true;
								break;
							}
						}
						c = Character.toChars(r)[0];
						if (c != '\n') {
							linebuffer.append(c);
						}
					} while (c != '\n');
					lineNum++;
					if (lineNum <= ignoreLines) {
						continue;
					}
					if (linebuffer.toString().startsWith("traceroute: Warning: " + formatedDest + " has multiple addresses")) {
						continue;
					}
					if (linebuffer.toString().startsWith("over a maximum")) {
						continue;
					}
					final String line = Util
							.replaceTs(linebuffer.toString().trim().replaceAll(" +", " "), Env.INSTANCE.getOs() == OS.win ? 3 : 1)
							.replaceAll(" +", " ");
					if (line.isEmpty()) {
						continue;
					}
					if (line.contains("Trace complete")) {
						break;
					}
					if (monitor.isCanceled()) {
						break;
					}
					if (line.contains("*")) {
						if (previous != null) {
							addPoint(previous.toUnkown());
						}
						continue;
					}
					final String[] routePoint = line.split(" ");
					final String ip;
					String host = "";
					final int latency;
					final int dnslookupTime = DNSLookupService.UNDEF;

					if (Env.INSTANCE.getOs() == OS.win) {
						latency = (parseWindowsTime(routePoint[1]) + parseWindowsTime(routePoint[2])
								+ parseWindowsTime(routePoint[3])) / 3;
						if (resolveHostname) {
							if (routePoint.length > 5) {
								host = routePoint[4];
								ip = routePoint[5].replace("[", "").replace("]", "");
							} else {
								ip = routePoint[4];
							}
						} else {
							ip = routePoint[4];
						}
					} else {
						if (resolveHostname) {
							if (routePoint.length > 3) {
								host = routePoint[1];
								ip = routePoint[2].replace("(", "").replace(")", "");
								latency = (int) Float.parseFloat(routePoint[3]);
							} else {
								ip = routePoint[1].replace("(", "").replace(")", "");
								latency = (int) Float.parseFloat(routePoint[2]);
							}
						} else {
							ip = routePoint[1];
							latency = (int) Float.parseFloat(routePoint[2]);
						}
					}
					previous = addPoint(Pair.create(ip, host), latency, dnslookupTime);
				}
				if (monitor.isCanceled()) {
					return;
				}
				final InputStream error = process.getErrorStream();

				final List<String> errors = Util.readUTF8File(error);
				if (!errors.isEmpty()) {
					final StringBuilder m = new StringBuilder();
					for (final String e : errors) {
						// for some reason, this info message is dumped to the error stream, so just ignore it
						if (!e.startsWith("traceroute to " + formatedDest)
								&& !e.startsWith("traceroute: Warning: " + formatedDest + " has multiple addresses")) {
							m.append(e).append("\n");
						}
					}
					// notify error
					if (!m.toString().isEmpty()) {
						throw new IOException(m.toString());
					}
				}
				// reached the max hops but not the target iup
				if (previous != null && previous.getNumber() == maxHops && !previous.getIp().equals(destIp)) {
					throw new MaxHopsException();
				}
			} finally {
				try {
					process.destroy();
				} catch (final Exception e) {
					LOGGER.error("Failed to destroy os traceroute process", e);
				}
			}
		} catch (final MaxHopsException e) {
			throw e;
		} catch (final IOException e) {
			throw e;
		} catch (final Exception e) {
			LOGGER.error("error while performing trace route command", e);
		}
	}

	private static int parseWindowsTime(final String str) {
		if ("<1".equals(str)) {
			return 1;
		}
		return Integer.parseInt(str);
	}

	/**
	 * Add a point corresponding to the given IP
	 *
	 * @param ipAndHost the IP and host
	 * @param latency latency
	 * @param dnslookupTime
	 */
	protected RoutePoint addPoint(final Pair<String, String> ipAndHost, final int latency, final long dnslookupTime) {
		final String ip = ipAndHost.getLeft();
		final String hostname = ipAndHost.getRight();
		RoutePoint point;
		if (ip.startsWith("192.168.") || ip.equals("127.0.0.1")) {
			// private Ips, calculate location with public IP
			point = _services.getGeo().populateGeoDataForIP(new RoutePoint(), _services.getGeo().getPublicIp().getLeft());
			point.setIp(ip);
		} else {
			point = _services.getGeo().populateGeoDataForIP(new RoutePoint(), ip);
		}
		// unknown location
		if (point.isUnknownGeo()) {
			if (_route.isEmpty()) {
				// set to local ip
				point = _services.getGeo().populateGeoDataForIP(new RoutePoint(), _services.getGeo().getPublicIp().getLeft());
			} else {
				// set to previous point
				final RoutePoint previous = _route.get(_route.size() - 1);
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
		point.setHostname(hostname);
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

	/**
	 * @see org.leo.traceroute.core.ITraceRoute#getRoute()
	 */
	@Override
	public List<RoutePoint> getRoute() {
		return _route;
	}

	/**
	 * @see org.leo.traceroute.core.ITraceRoute#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		_route.clear();
		_threadPool.shutdown();
		_notifyThread.interrupt();
	}

	/**
	 * @see org.leo.traceroute.core.ITraceRoute#size()
	 */
	@Override
	public int size() {
		return _route.size();
	}

	/**
	 * @see org.leo.traceroute.core.ITraceRoute#focus(org.leo.traceroute.core.RoutePoint)
	 */
	@Override
	public void focus(final RoutePoint point, final boolean animation) {
		_focusedPoint = point;
		for (final IRouteListener listener : getListeners()) {
			listener.focusRoute(point, false, animation);
		}
	}

	/**
	 * @see org.leo.traceroute.core.route.ITraceRoute#toCSV()
	 */
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

	/**
	 * @see org.leo.traceroute.core.ITraceRoute#renotifyRoute()
	 */
	@Override
	public void renotifyRoute() {
		if (!_route.isEmpty()) {
			for (final IRouteListener listener : getListeners()) {
				listener.newRoute(_resolveHostname);
				for (final RoutePoint point : _route) {
					listener.routePointAdded(point);
				}
				listener.routeDone(_tracerouteTime, _lengthInKm.get());
				if (_focusedPoint != null) {
					focus(_focusedPoint, false);
				}
			}
		}
	}

	/**
	 * @see org.leo.traceroute.core.network.ISnifferNetworkInterfaceListener#notifyNewNetworkInterface(org.jnetpcap.PcapIf)
	 */
	@Override
	public void notifyNewNetworkInterface(final T device, final byte[] mac) {
		_device = device;
	}

	/**
	 * @see org.leo.traceroute.core.route.ITraceRoute#clear()
	 */
	@Override
	public void clear() {
		_route.clear();
	}

}
