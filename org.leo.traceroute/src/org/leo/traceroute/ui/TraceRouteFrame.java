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

import java.awt.*;
import java.util.Map.Entry;

import javax.swing.*;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TraceRouteFrame $Id: TraceRouteFrame.java 273 2016-09-27 05:52:51Z leolewis $
 *
 * @author Leo Lewis
 */
public class TraceRouteFrame extends JFrame {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceRouteFrame.class);

	/**  */
	private static final long serialVersionUID = -886845144646546423L;

	/** Main panel */
	private MainPanel _mainPanel;

	/**
	 * Constructor
	 *
	 * @throws HeadlessException
	 */
	public TraceRouteFrame() throws HeadlessException {
		super(Resources.getLabel("appli.title.simple") + " " + Resources.getVersion());
		setIconImage(Resources.getImage("internet.png"));
	}

	/**
	 * Init the frame
	 */
	public void init(final ServiceFactory services) {
		_mainPanel = new MainPanel(services);
		getContentPane().add(_mainPanel, BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		services.updateStartup("init.completed", true);
		SwingUtilities4.applyFont(this, Env.INSTANCE.getFont());
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (Env.INSTANCE.getAppX() != null && Env.INSTANCE.getAppY() != null) {
			if (Env.INSTANCE.getAppX() + Env.INSTANCE.getAppWidth() > screenSize.width
					|| Env.INSTANCE.getAppY() + Env.INSTANCE.getAppHeight() > screenSize.height) {
				setLocationRelativeTo(null);
			} else {
				setLocation(Env.INSTANCE.getAppX(), Env.INSTANCE.getAppY());
			}
		} else {
			setLocationRelativeTo(null);
		}
		setVisible(true);
		toFront();
		getRootPane().setDefaultButton(_mainPanel.getControlPanel().getRootButton());

		if (Env.INSTANCE.isFullScreen()) {
			setExtendedState(Frame.MAXIMIZED_BOTH);
		}
		services.getSplash().dispose();
		_mainPanel.afterShow();
	}

	public void close() {
		Env.INSTANCE.setFullScreen(getExtendedState() == Frame.MAXIMIZED_BOTH);
		if (_mainPanel != null) {
			_mainPanel.dispose();
		}
		LOGGER.info("Application exited.");
	}
}
