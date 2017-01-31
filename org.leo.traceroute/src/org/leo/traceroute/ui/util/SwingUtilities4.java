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
package org.leo.traceroute.ui.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.leo.traceroute.install.Env;

/**
 * SwingUtilities4 $Id: SwingUtilities4.java 277 2016-10-09 19:44:39Z leolewis $
 *
 * @author Leo Lewis
 */
public class SwingUtilities4 {

	/**
	 * Invoke now or later, depending if we are in the EDT
	 * @param r the runnable to invoke
	 */
	public static void invokeInEDT(final Runnable r) {
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	/**
	 * Set up a window, pack, center an apply current preference font
	 * @param win
	 */
	public static void setUp(final Window win) {
		// apply font to all components
		applyFont(win, Env.INSTANCE.getFont());
		packAndCenter(win);
	}

	/**
	 * Apply given font to all components of the given container
	 * @param root
	 * @param font
	 */
	public static void applyFont(final Container root, final Font font) {
		for (final Component c : root.getComponents()) {
			if (c instanceof JTextField || c instanceof JSpinner) {
				//continue;
			}
			c.setFont(font);
			if (c instanceof Container) {
				applyFont((Container) c, font);
			}
		}
	}

	public static void packAndCenter(final Window win) {
		// pack
		win.pack();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		win.setSize(Math.min(win.getWidth(), screenSize.width), Math.min(win.getHeight(), screenSize.height - 20));
		// Center on screen
		win.setLocationRelativeTo(null);
	}
}
