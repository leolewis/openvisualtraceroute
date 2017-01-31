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
package org.leo.traceroute;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.route.IRouteListener;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RouteTest $Id: RouteTest.java 232 2016-01-30 04:39:16Z leolewis $
 *
 * @author Leo Lewis
 */
public class RouteTest extends TestCase {

	private static final Logger LOGGER = LoggerFactory.getLogger(RouteTest.class);

	private static final String DEST = "http://google.com/";

	@Override
	protected void setUp() throws Exception {
		Env.INSTANCE.initEnv();
		Env.INSTANCE.loadDynamicConf(null);
	}

	@Test
	public void testTraceRoute() {
		try {
			final long ts = System.currentTimeMillis();
			final List<String> ipList = IOUtils.readLines(new FileInputStream(new File("test/ip.txt")));
			final String[] ips = new String[ipList.size()];
			for (int i = 0; i < ips.length; i++) {
				ips[i] = ipList.get(i).trim();
			}
			traceRoute(ips);
			System.out.println("Test done in " + (System.currentTimeMillis() - ts) + "ms");
		} catch (final Exception e) {
			LOGGER.error("Test failed", e);
			fail(e.getMessage());
		}
	}

	/**
	 * trace route
	 */
	public void traceRoute(final String... dest) throws Exception {
		final ServiceFactory services = new ServiceFactory(null);
		final AtomicReference<CountDownLatch> cd = new AtomicReference<CountDownLatch>();
		final AtomicReference<String> ip = new AtomicReference<String>();
		services.init();
		services.getTraceroute().addListener(new IRouteListener() {
			int nb;

			@Override
			public void newRoute(final boolean dnsLookup) {
				System.out.println("New route");
				nb = 1;
			}

			@Override
			public void routePointAdded(final RoutePoint point) {
				assertNotNull(point);
				assertEquals(nb, point.getNumber());
				if (nb == 1) {
					assertEquals("192.168.0.1", point.getIp());
				}
				System.out.println(point.getNumber() + " " + point.getTown() + " " + point.getCountry() + " " + point.getIp()
						+ " " + point.getHostname() + " " + point.getLatency() + "ms " + point.getDnsLookUpTime() + "ms");
				nb++;
			}

			@Override
			public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {

			}

			@Override
			public void error(final Exception e, final Object origin) {
				System.out.println("Route error");
				LOGGER.error(e.getLocalizedMessage(), e);
				fail(e.getMessage());
				cd.get().countDown();
			}

			@Override
			public void routeDone(final long tracerouteTime, final long lengthInKm) {
				System.out.println("Route done in " + tracerouteTime + "ms");
				assertEquals(nb, services.getTraceroute().getRoute().size() + 1);
				if (!ip.get().equals(
						services.getTraceroute().getRoute().get(services.getTraceroute().getRoute().size() - 1).getIp())) {
					LOGGER.error("Wong final IP address {}, expected {}",
							services.getTraceroute().getRoute().get(services.getTraceroute().getRoute().size() - 1).getIp(),
							ip.get());
				}
				cd.get().countDown();
			}

			@Override
			public void routeCancelled() {
				System.out.println("Route cancelled");
				cd.get().countDown();
			}

			@Override
			public void routeTimeout() {
				System.out.println("Route timed out");
				cd.get().countDown();
			}

			@Override
			public void maxHops() {
				System.out.println("Route timed out");
				cd.get().countDown();
			}

		});
		final CancelMonitor monitor = new CancelMonitor();

		for (int i = 0; i < dest.length; i++) {
			final String d = dest[i];
			System.out.println("\n" + (i + 1) + "/" + dest.length + " : " + d);
			monitor.setCanceled(false);
			cd.set(new CountDownLatch(1));
			ip.set(InetAddress.getByName(d).getHostAddress());
			services.getTraceroute().compute(d, monitor, true, 0, false, true, 50);
			cd.get().await();
		}

	}
}
