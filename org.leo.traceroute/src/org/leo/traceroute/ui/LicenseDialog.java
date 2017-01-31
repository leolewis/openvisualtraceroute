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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.util.HyperlinkLabel;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.leo.traceroute.util.Util;

/**
 * LicenseDialog $Id: LicenseDialog.java 272 2016-09-22 05:38:31Z leolewis $
 *
 * @author Leo Lewis
 */
public class LicenseDialog extends JDialog {

	/**  */
	private static final long serialVersionUID = -172111964257906281L;

	/**
	 * Constructor
	 */
	public LicenseDialog(final Window parent) {
		super(parent, Resources.getLabel("license.button"), ModalityType.APPLICATION_MODAL);
		getContentPane().add(createHeaderPanel(false, null), BorderLayout.NORTH);
		final JTextArea license = new JTextArea(30, 50);
		license.setEditable(false);
		// read the license file and add its content to the JTextArea
		for (final String line : Util.readUTF8File(Resources.class.getResourceAsStream("/" + Resources.RESOURCE_PATH
				+ "/License.txt"))) {
			license.append("   " + line + "\n");
		}
		// scroll to the top of the JTextArea
		license.setCaretPosition(0);
		// the all thing in a ScrollPane
		final JScrollPane scroll = new JScrollPane(license);
		getContentPane().add(scroll, BorderLayout.CENTER);
		final JPanel donatePanel = new JPanel(new BorderLayout(5, 10));
		final JLabel donate = new JLabel(Resources.getLabel("donate"));
		donatePanel.add(donate, BorderLayout.NORTH);
		final JPanel center = new JPanel();
		center.setLayout(new FlowLayout());
		center.add(new JLabel(Resources.getImageIcon("donate.png")));
		center.add(new HyperlinkLabel(Resources.getLabel("donate.label"), Env.INSTANCE.getDonateUrl()));
		donatePanel.add(center, BorderLayout.CENTER);
		final JButton close = new JButton(Resources.getLabel("close.button"));
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				LicenseDialog.this.dispose();
			}
		});
		donatePanel.add(close, BorderLayout.SOUTH);
		getContentPane().add(donatePanel, BorderLayout.SOUTH);
		SwingUtilities4.setUp(this);
		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				dispose();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Create a title panel
	 *
	 * @param boutonLicense if true, show the license button
	 * @return the panel
	 */
	public static JComponent createHeaderPanel(final boolean boutonLicense, final Window parent) {
		if (boutonLicense) {
			final JButton license = new JButton(Resources.getLabel("license.label"), Resources.getImageIcon("route.png"));
			license.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final LicenseDialog dialog = new LicenseDialog(parent);
					dialog.setVisible(true);
				}
			});
			return license;
		} else {
			return new JLabel(Resources.getLabel("appli.title", Resources.getVersion()), Resources.getImageIcon("route.png"),
					JLabel.CENTER);
		}
	}

	public static void main(final String[] args) {
		Resources.initLabels();
		new LicenseDialog(null).setVisible(true);
	}
}
