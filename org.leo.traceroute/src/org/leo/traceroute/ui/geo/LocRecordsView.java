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
package org.leo.traceroute.ui.geo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.resources.Resources;

/**
 * LocRecordsDialog $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public class LocRecordsView extends JDialog {

	private static final long serialVersionUID = 340561605346486571L;

	public LocRecordsView(final Window parent, final ServiceFactory factory) {
		super(parent, Resources.getLabel("LocRecords"), ModalityType.APPLICATION_MODAL);
		final String rawLocRecords = factory.getGeo().getLocRecordsStr();

		final JTextPane text = new JTextPane();
		text.setEditable(true);
		text.setText(rawLocRecords);
		text.setPreferredSize(new Dimension(800, 400));
		getContentPane().add(text, BorderLayout.CENTER);
		final JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final Pair<String, Exception> error = factory.getGeo().parseAndLoadDNSRecords(text.getText());
				text.setText(factory.getGeo().getLocRecordsStr());
				if (StringUtils.isNotEmpty(error.getKey())) {
					JOptionPane.showMessageDialog(LocRecordsView.this, Resources.getLabel("dns.loc.warning", error.getKey()), Resources.getLabel("warning"),
							JOptionPane.WARNING_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(LocRecordsView.this, Resources.getLabel("dns.loc.updated"));
					LocRecordsView.this.dispose();
				}
			}
		});
		getContentPane().add(save, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
	}
}
