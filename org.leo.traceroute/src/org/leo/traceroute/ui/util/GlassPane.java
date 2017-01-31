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

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.leo.traceroute.ui.task.ConfirmMonitor;

/**
 * GlassPane $Id: GlassPane.java 237 2016-02-06 18:49:50Z leolewis $
 *
 * @author Leo Lewis
 */
public class GlassPane extends JPanel {

	/** */
	private static final long serialVersionUID = -306946459371762497L;

	/**
	 * Constructor
	 *
	 * @param rootPane rootpane on which display the glasspane
	 * @param label label to display
	 * @param image image to display
	 * @param dim dimension
	 * @param cancelMonitor cancel monitor (will be activated on click on the
	 *            cancel button).
	 * @param confirmMonitor ok/cancel monitor (will be activated on click on
	 *            the ok/cancel button). If <code>null</code>, no OK button
	 *            will be shown
	 */
	private GlassPane(final JRootPane rootPane, final String label, final ImageIcon image, final Dimension dim,
			final CancelMonitor cancelMonitor, final ConfirmMonitor confirmMonitor) {
		super(new GridBagLayout());

		// setOpaque(false);
		final FontMetrics metrics = rootPane.getGraphics().getFontMetrics(rootPane.getFont());
		final int w = metrics.stringWidth(label) + 25;
		final JPanel main = new JPanel(new BorderLayout());
		main.setPreferredSize(new Dimension(w, 220));
		main.setBackground(Color.WHITE);
		main.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.LIGHT_GRAY));
		main.setOpaque(true);
		final JLabel jlabel = new JLabel(label);
		final JLabel labelImg = new JLabel(image);
		labelImg.setOpaque(false);
		main.add(labelImg, BorderLayout.NORTH);
		main.add(jlabel, BorderLayout.CENTER);

		jlabel.setOpaque(false);
		// if okmonitor null, no OK button
		if (confirmMonitor == null) {
			final JButton cancel = new JButton(cancelMonitor == null ? Resources.getLabel("ok.button")
					: Resources.getLabel("cancel.button"));
			cancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					if (cancelMonitor != null) {
						cancelMonitor.setCanceled(true);
					}
					rootPane.setEnabled(true);
					rootPane.getGlassPane().setVisible(false);
				}
			});
			main.add(cancel, BorderLayout.SOUTH);
		} else {
			// both OK and cancel button
			final JButton ok = new JButton(confirmMonitor.getConfirmLabel());
			ok.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					confirmMonitor.setOk(true);
					confirmMonitor.setSet(true);
					rootPane.setEnabled(true);
					rootPane.getGlassPane().setVisible(false);
				}
			});
			final JButton cancel = new JButton(confirmMonitor.getRefuseLabel());
			cancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					confirmMonitor.setOk(false);
					confirmMonitor.setSet(true);
					rootPane.setEnabled(true);
					rootPane.getGlassPane().setVisible(false);
				}
			});
			final JPanel panel = new JPanel(new BorderLayout(10, 10));
			panel.add(ok, BorderLayout.WEST);
			panel.add(cancel, BorderLayout.EAST);
			main.add(panel, BorderLayout.SOUTH);
		}
		add(main, new GridBagConstraints(0, 0, dim.width, dim.height, 0.0, 0.0, GridBagConstraints.CENTER,
				GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}

	/**
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(final Graphics g) {
		// transparent panel
		if (g instanceof Graphics2D) {
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.7f));
		}
		super.paint(g);
	}

	/**
	 * Show a glasspane on the Root panel of the given panel
	 *
	 * @param panel the given panel
	 * @param label label to show
	 * @param image image to show
	 * @param cancelMonitor cancel monitor (will be activated on click on the
	 *            cancel button)
	 */
	public static void showGlassPane(final JPanel panel, final String label, final ImageIcon image,
			final CancelMonitor cancelMonitor) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final JRootPane rootPane = SwingUtilities.getRootPane(panel);
				final Component newGlassPane = new GlassPane(rootPane, label, image, rootPane.getPreferredSize(), cancelMonitor,
						null);
				rootPane.setGlassPane(newGlassPane);
				newGlassPane.setVisible(true);
				rootPane.setEnabled(false);

				rootPane.invalidate();
				rootPane.revalidate();
			}
		});
	}

	/**
	 * Hide the glass pane
	 *
	 * @param panel the panel on which the glasspane was shown on the first
	 *            place
	 */
	public static void hideGlassPane(final JPanel panel) {
		SwingUtilities4.invokeInEDT(new Runnable() {
			@Override
			public void run() {
				final JRootPane rootPane = SwingUtilities.getRootPane(panel);
				rootPane.setEnabled(true);
				rootPane.getGlassPane().setVisible(false);
				rootPane.invalidate();
				rootPane.revalidate();
			}
		});
	}

	/**
	 * Diplay a message on a glasspane
	 *
	 * @param panel panel on which rootpane the glasspane will be displayed
	 * @param label label to show
	 * @param image image to show
	 */
	public static void displayMessage(final JPanel panel, final String label, final ImageIcon image) {
		JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(panel), label, label, JOptionPane.INFORMATION_MESSAGE);
		//		SwingUtilities4.invokeInEDT(new Runnable() {
		//			@Override
		//			public void run() {
		//				final JRootPane rootPane = SwingUtilities.getRootPane(panel);
		//				final Component newGlassPane = new GlassPane(rootPane, label, image, rootPane.getPreferredSize(), null,
		//						null);
		//				rootPane.setGlassPane(newGlassPane);
		//				newGlassPane.setVisible(true);
		//				rootPane.setEnabled(false);
		//				rootPane.invalidate();
		//				rootPane.revalidate();
		//			}
		//		});
	}

	/**
	 * Display a Ok/Cancel message on the glasspane of the given panel
	 *
	 * @param panel panel on which rootpane the glasspane will be displayed
	 * @param label label to display
	 * @param image image to display
	 * @param okMonitor Ok/cancell monitor
	 */
	public static void displayQuestion(final JPanel panel, final String label, final ImageIcon image,
			final ConfirmMonitor okMonitor) {
		SwingUtilities4.invokeInEDT(new Runnable() {
			@Override
			public void run() {
				final JRootPane rootPane = SwingUtilities.getRootPane(panel);
				final Component newGlassPane = new GlassPane(rootPane, label, image, rootPane.getPreferredSize(), null, okMonitor);
				rootPane.setGlassPane(newGlassPane);
				newGlassPane.setVisible(true);
				rootPane.setEnabled(false);
				rootPane.invalidate();
				rootPane.revalidate();
			}
		});
	}
}
