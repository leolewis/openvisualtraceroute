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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.Border;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.autocomplete.AutoCompleteProvider;
import org.leo.traceroute.core.geo.GeoService;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.network.EmptyNetworkService;
import org.leo.traceroute.core.route.IRouteListener;
import org.leo.traceroute.core.route.impl.AbstractTraceRoute;
import org.leo.traceroute.core.route.impl.OSTraceRoute;
import org.leo.traceroute.core.sniffer.impl.EmptyPacketsSniffer;
import org.leo.traceroute.core.whois.WhoIs;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.EnvException;
import org.leo.traceroute.ui.AbstractPanel;
import org.leo.traceroute.ui.geo.OpenMapPanel;
import org.leo.traceroute.ui.geo.WWJPanel;
import org.leo.traceroute.ui.route.GanttPanel;
import org.leo.traceroute.ui.route.RouteTablePanel;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.leo.traceroute.ui.util.SplashScreen;

/**
 * WWTest $Id: UITest.java 281 2016-10-11 04:09:33Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class UITest {

	private static boolean bool = true;

	/**
	 * @param args
	 */
	public static void main(final String[] args) throws HeadlessException, EnvException {
		ToolTipManager.sharedInstance().setEnabled(false);
		ToolTipManager.sharedInstance().setInitialDelay(10);
		final JFrame f = new JFrame();
		final SplashScreen splash = new SplashScreen(f, true, 6);
		Env.INSTANCE.initEnv();
		splash.dispose();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final JPanel main = new JPanel(new BorderLayout());
		final JPanel sub = new JPanel(new BorderLayout());

		main.setPreferredSize(new Dimension(800, 525));
		sub.setPreferredSize(new Dimension(800, 500));
		final JButton b = new JButton("Recreate");
		main.add(b, BorderLayout.NORTH);
		main.add(sub, BorderLayout.CENTER);
		b.setPreferredSize(new Dimension(50, 25));
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				display(f, sub);
			}
		});
		f.getContentPane().add(main, BorderLayout.CENTER);
		f.pack();
		f.setVisible(true);
		f.setLocationRelativeTo(null);
		display(f, sub);

	}

	private static void display(JFrame frame, final JPanel sub) {
		final ServiceFactory services = new ServiceFactory(new AbstractTraceRoute<Void>() {
			@Override
			protected void computeRoute(final String formatedDest, final CancelMonitor monitor, final boolean resolveHostname,
										final boolean ipV4, final int maxHops) throws IOException {
				for (int i = 0; i < 20; i++) {
					addPoint(Pair.of("118.236.194.140", "localhost"), 10, 10);
				}
				addPoint(Pair.of("66.249.64.0", "google.com"), 10, 10);
			}

		}, new EmptyPacketsSniffer() {
			@Override
			public void init(final ServiceFactory services) throws IOException {
			}
		}, new EmptyNetworkService() {
			@Override
			public void init(final ServiceFactory services) throws IOException {
			}
		}, new DNSLookupService(), new GeoService(), new AutoCompleteProvider(), new WhoIs() {
			@Override
			public void init(final ServiceFactory services) throws Exception {
			}
		}) {
			@Override
			public JFrame getMain() {
				return frame;
			}
		};
		try {
			services.init();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		final AbstractPanel map = bool ? new WWJPanel(services) : new OpenMapPanel(services);
		map.afterShow(Mode.TRACE_ROUTE);
		final GanttPanel gant = new GanttPanel(services);
		final RouteTablePanel table = new RouteTablePanel(services);
		bool = !bool;
		map.setPreferredSize(new Dimension(400, 600));
		gant.setPreferredSize(new Dimension(400, 300));
		table.setPreferredSize(new Dimension(400, 300));
		sub.removeAll();
		JPanel right = new JPanel(new BorderLayout());
		right.add(table, BorderLayout.CENTER);
		right.add(gant, BorderLayout.SOUTH);
		sub.add(map, BorderLayout.CENTER);
		sub.add(right, BorderLayout.EAST);
		sub.invalidate();
		sub.revalidate();
		services.getTraceroute().compute("test", new CancelMonitor(), false, 10000, true, 50);
		sub.repaint();
	}
}
