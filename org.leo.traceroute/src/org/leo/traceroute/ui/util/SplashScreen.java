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
package org.leo.traceroute.ui.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Image;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

import org.leo.traceroute.resources.Resources;

/**
 * SplashScreen $Id: SplashScreen.java 185 2011-08-31 02:41:49Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author Leo Lewis
 */
public class SplashScreen extends JDialog {

	/**  */
	private static final long serialVersionUID = 685145934255448920L;

	private final JProgressBar _progress;
	private final AtomicInteger _step = new AtomicInteger(1);

	/**
	 * Constructor
	 *
	 * @throws HeadlessException
	 */
	public SplashScreen(final JFrame parent, final boolean showImage, final int startupStepsCount) throws HeadlessException {
		super(parent, ModalityType.APPLICATION_MODAL);
		final String version = Resources.getVersion();
		if (showImage) {
			final Image image = Resources.getImageIcon("splashscreen.jpg").getImage();
			getContentPane().add(new ImageComponent(image, new String[] { "Open Visual Traceroute", version + "                   Leo Lewis" }), BorderLayout.CENTER);
		}
		_progress = new JProgressBar(1, startupStepsCount);
		_progress.setIndeterminate(false);
		_progress.setStringPainted(true);
		_progress.setPreferredSize(new Dimension(500, _progress.getPreferredSize().height));
		getContentPane().add(_progress, BorderLayout.SOUTH);
		setFocusable(false);
		setUndecorated(true);
		pack();
		setLocationRelativeTo(null);
	}

	public void updateStartup(final String labelKey) {
		updateStartup(labelKey, true);
	}

	public void updateStartup(final String labelKey, final boolean incStep) {
		SwingUtilities4.invokeInEDT(() -> {
			if (incStep) {
				_progress.setValue(_step.incrementAndGet());
			}
			_progress.setString(Resources.getLabel(labelKey));
		});
	}

	public static void main(final String[] args) throws InterruptedException {
		Resources.initLabels();
		final SplashScreen s = new SplashScreen(null, true, 6);
		s.updateStartup("updating.geoip");
		Thread.sleep(1000);
		s.updateStartup("init.geoip");
		Thread.sleep(1000);
		s.updateStartup("init.traceroute.network");
		Thread.sleep(1000);
		s.updateStartup("init.sniffer.network");
		Thread.sleep(1000);
		s.updateStartup("init.ui");
		Thread.sleep(1000);
		s.updateStartup("init.completed");
		Thread.sleep(2000);
		s.dispose();
		System.exit(0);
	}
}
