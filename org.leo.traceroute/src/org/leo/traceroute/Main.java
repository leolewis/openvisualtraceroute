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

import javax.swing.*;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.TraceRouteFrame;
import org.leo.traceroute.ui.util.SplashScreen;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Main $Id: Main.java 231 2016-01-27 08:24:31Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author Leo.lewis
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private static TraceRouteFrame _instance;

	/**
	 * Main
	 *
	 * @param args
	 */
	public static void main(final String[] args) {
		final long ts = System.currentTimeMillis();
		LOGGER.info("Open Visual Traceroute " + Resources.getVersion() + "");
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			final StackTraceElement elt = e.getStackTrace()[0];
			if (!elt.getClassName().startsWith("org.jfree.")) {
				LOGGER.error("Uncaught error", e);
			}
		});
		ToolTipManager.sharedInstance().setEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setInitialDelay(10);
		try {
			Env.INSTANCE.initEnv();
			try {
				if (Env.INSTANCE.isDarkTheme()) {
					UIManager.put("activeCaption", new javax.swing.plaf.ColorUIResource(new Color(128, 128, 128)));
					UIManager.put("activeCaptionText", new javax.swing.plaf.ColorUIResource(new Color( 230, 230, 230)));
					UIManager.put("control", new Color( 128, 128, 128) );
					UIManager.put("info", new Color(128,128,128) );
					UIManager.put("nimbusBase", new Color( 18, 30, 49) );
					UIManager.put("nimbusAlertYellow", new Color( 248, 187, 0) );
					UIManager.put("nimbusDisabledText", new Color( 128, 128, 128) );
					UIManager.put("nimbusFocus", new Color(115,164,209) );
					UIManager.put("nimbusGreen", new Color(176,179,50) );
					UIManager.put("nimbusInfoBlue", new Color( 66, 139, 221) );
					UIManager.put("nimbusLightBackground", new Color( 18, 30, 49) );
					UIManager.put("nimbusOrange", new Color(191,98,4) );
					UIManager.put("nimbusRed", new Color(169,46,34) );
					UIManager.put("nimbusSelectedText", new Color( 255, 255, 255) );
					UIManager.put("nimbusSelectionBackground", new Color( 104, 93, 156) );
					UIManager.put("text", new Color( 230, 230, 230) );
					UIManager.put("List.textForeground", new Color( 104, 93, 156));
					UIManager.put("Tree.textForeground", new Color( 104, 93, 156));
				}
				if (Env.INSTANCE.getOs() != OS.mac) {
					for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
						if ("Nimbus".equals(info.getName())) {
							UIManager.setLookAndFeel(info.getClassName());
							break;
						}
					}
				} else {
					try {
						gov.nasa.worldwindx.applications.sar.OSXAdapter.setQuitHandler(null, Main.class.getMethod("exit", (Class<?>[]) null));
					} catch (final Exception e) {
						LOGGER.warn("Failed to register CTRL+Q handler for MacOSX", e);
					}
				}
			} catch (Exception e) {
				LOGGER.warn("Failed to set dark theme", e);
			}
			_instance = new TraceRouteFrame();

			final SplashScreen splash = new SplashScreen(_instance, !Env.INSTANCE.isHideSplashScreen(), 6);
			SwingUtilities4.invokeInEDT(() -> {
				splash.updateStartup("application.startup");
				splash.setVisible(true);
				splash.toFront();
			});
			final Thread shutdown = new Thread(() -> _instance.close());
			shutdown.setName("Shutdown");
			shutdown.setDaemon(true);
			Runtime.getRuntime().addShutdownHook(shutdown);

			final ServiceFactory services = new ServiceFactory(splash, _instance);

			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					Env.INSTANCE.loadDynamicConf(services);
					services.init();
					return null;
				}

				@Override
				protected void done() {
					try {
						get();
						services.updateStartup("init.ui", true);
						_instance.init(services);
						LOGGER.info("Startup completed in {}ms", System.currentTimeMillis() - ts);
					} catch (final Throwable e) {
						LOGGER.error("Error while starting the application", e);
						JOptionPane.showMessageDialog(null, Resources.getLabel("error.init", e.getMessage()), Resources.getLabel("fatal.error"),
								JOptionPane.ERROR_MESSAGE);
						System.exit(-1);
					}
				}
			};
			worker.execute();
		} catch (final Exception e) {
			// fatal
			LOGGER.error("Error while starting the application", e);
			JOptionPane.showMessageDialog(null, Resources.getLabel("error.init", e.getMessage()), Resources.getLabel("fatal.error"), JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
	}

	public static void exit() {
		if (_instance != null) {
			_instance.dispose();
		}
		System.exit(0);
	}
}
