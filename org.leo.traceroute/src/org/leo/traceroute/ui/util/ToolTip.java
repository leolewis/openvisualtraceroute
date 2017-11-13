/**
 * Open Visual Trace Route
 * Copyright (C) 2010-2015 Leo Lewis.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.leo.traceroute.resources.Resources;

/**
 * ToolTip $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public enum ToolTip {

	INSTANCE;

	private List<JDialog> _dialogs;
	private List<JComponent> _components;
	private Component _component;
	private Point location;

	private void showToolTips(final Component component) {
		_component = component;
		location = _component.getLocationOnScreen();
		_components = new ArrayList<>();
		searchTooltip(_component);
		showToolTips();
	}

	public JButton buildHelpButton(final Component component, final int h) {
		final JButton helpButton = new JButton(Resources.getImageIcon("help.png"));
		helpButton.setToolTipText(Resources.getLabel("help.tooltip"));
		helpButton.addActionListener(e -> showToolTips(component));
		return helpButton;
	}

	private void searchTooltip(final Component component) {
		if (component instanceof JComponent && component.isVisible()) {
			final JComponent c = (JComponent) component;
			final String tooltip = c.getToolTipText();
			if (StringUtils.isNoneEmpty(tooltip)) {
				if (c.getWidth() >= 20 && (c instanceof JToggleButton || c.getClass() == JButton.class || c instanceof JTextField)) {
					_components.add(c);
				}
			}
		}
		if (component instanceof Container) {
			for (final Component c : ((Container) component).getComponents()) {
				if (c instanceof JComponent) {
					searchTooltip(c);
				}
			}
		}
	}

	private void showToolTips() {
		_dialogs = new ArrayList<>();
		int num = 0;
		final Window w = SwingUtilities.getWindowAncestor(_component);

		for (final JComponent c : _components) {
			num++;

			buildDialog(w, c, num);

		}
		final JDialog over = new JDialog(w, ModalityType.DOCUMENT_MODAL);
		final MouseAdapter dispose = new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				for (final JDialog d : _dialogs) {
					d.dispose();
				}
				_dialogs.clear();
				over.dispose();
			}
		};
		for (final JDialog d : _dialogs) {
			d.addMouseListener(dispose);
		}
		over.setUndecorated(true);
		over.setOpacity(0.2f);
		final JPanel panel = new JPanel();
		panel.setPreferredSize(w.getSize());
		over.getContentPane().add(panel);
		over.pack();
		over.setLocationRelativeTo(w);
		over.addMouseListener(dispose);
		over.setVisible(true);
	}

	private void buildDialog(final Window w, final JComponent component, final int num) {
		final Color color = color(num);
		final JDialog description = new JDialog(w, ModalityType.MODELESS);
		description.setUndecorated(true);
		final JLabel label = new AntialisingJLabel("<html><font size=5>&nbsp;" + string(num) + "&nbsp;" + component.getToolTipText() + "&nbsp;</font></html>");
		label.setBackground(color);
		description.getContentPane().setBackground(color);
		description.getContentPane().add(label);
		description.pack();

		description.setLocation(location.x + 25, location.y + 85 + 35 * num);
		description.setSize(description.getWidth(), description.getHeight() + 4);
		_dialogs.add(description);

		description.setVisible(true);
		final JDialog number = new JDialog(w, ModalityType.MODELESS);
		number.setUndecorated(true);
		final JLabel labelNumber = new AntialisingJLabel("<html><b><font size=5>&nbsp;" + string(num) + "&nbsp;</font></b></html>");
		labelNumber.setBackground(color);
		number.getContentPane().add(labelNumber);
		number.getContentPane().setBackground(color);
		number.pack();
		number.setLocationRelativeTo(component);
		number.setVisible(true);

		_dialogs.add(number);
	}

	private static String string(final int num) {
		switch (num) {
		case 1:
			return "\u2460";
		case 2:
			return "\u2461";
		case 3:
			return "\u2462";
		case 4:
			return "\u2463";
		case 5:
			return "\u2464";
		case 6:
			return "\u2465";
		case 7:
			return "\u2466";
		case 8:
			return "\u2467";
		case 9:
			return "\u2468";
		case 10:
			return "\u2469";
		case 11:
			return "\u246A";
		case 12:
			return "\u246B";
		case 13:
			return "\u246C";
		case 14:
			return "\u246D";
		case 15:
			return "\u246E";
		case 16:
			return "\u246F";
		case 17:
			return "\u2470";
		case 18:
			return "\u2471";
		case 19:
			return "\u2472";
		case 20:
			return "\u2473";
		}
		return String.valueOf(num);
	}

	private static Color color(final int num) {
		switch (num) {
		case 1:
			return Color.BLUE.brighter();
		case 2:
			return Color.GREEN;
		case 3:
			return Color.RED;
		case 4:
			return Color.ORANGE;
		case 5:
			return Color.MAGENTA;
		case 6:
			return Color.CYAN;
		case 7:
			return Color.YELLOW;
		case 8:
			return Color.WHITE;
		case 9:
			return Color.PINK;
		default:
			return color(1 + (num - 1) % 9);
		}
	}

	@SuppressWarnings("serial")
	public class AntialisingJLabel extends JLabel {

		public AntialisingJLabel(final String str) {
			super(str);
		}

		@Override
		public void paint(final Graphics g) {
			final Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			super.paint(g);
		}
	}
}
