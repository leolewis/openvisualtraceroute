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
package org.leo.traceroute.ui.sniffer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.apache.commons.lang3.StringUtils;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.ui.util.SwingUtilities4;

/**
 * PacketDetailPanel $Id: PacketDetailPanel.java 240 2016-02-12 06:15:49Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class PacketDetailPanel extends AbstractSnifferPanel {

	/**  */
	private static final long serialVersionUID = -1677592480032643118L;

	private final JTextPane _details;

	/**
	 * Constructor
	 * @param services
	 */
	@SuppressWarnings("serial")
	public PacketDetailPanel(final ServiceFactory services) {
		super(services);
		setPreferredSize(new Dimension(getPreferredSize().width, 250));
		_details = new JTextPane() {
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return getUI().getPreferredSize(this).width <= getParent().getSize().width;
			}
		};
		final JScrollPane scroll = new JScrollPane(_details, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scroll, BorderLayout.CENTER);
	}

	@Override
	public void startCapture() {
		_details.setText("");
	}

	@Override
	public void packetAdded(final AbstractPacketPoint point) {
		//		showPacket(point);
	}

	@Override
	public void captureStopped() {

	}

	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {
		showPacket(point);
	}

	private void showPacket(final AbstractPacketPoint point) {
		SwingUtilities4.invokeInEDT(() -> {
			final String[] lines = point.getPayload().split("\n");
			int max = 0;
			for (final String line1 : lines) {
				max = Math.max(max, line1.length());
			}
			max += 5;
			for (final String line2 : lines) {
				final Color color = colorForLine(line2);
				final StyleContext sc = StyleContext.getDefaultStyleContext();
				AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);

				aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
				aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
				aset = sc.addAttribute(aset, StyleConstants.Bold, false);
				aset = sc.addAttribute(aset, StyleConstants.Background, color);
				final int len = _details.getDocument().getLength();
				_details.setCaretPosition(len);
				_details.setCharacterAttributes(aset, false);
				_details.replaceSelection(line2 + StringUtils.repeat(" ", max - line2.length()) + "\n");
			}
			_details.setCaretPosition(0);
		});
	}

	public static Color colorForLine(final String line) {
		if (line.startsWith("Frame")) {
			return new Color(250, 220, 220);
		}
		if (line.startsWith("Eth")) {
			return new Color(220, 250, 220);
		}
		if (line.startsWith("Ip")) {
			return new Color(220, 220, 250);
		}
		if (line.startsWith("Tcp") || line.startsWith("Udp") || line.startsWith("Icmp")) {
			return new Color(250, 250, 220);
		}
		if (line.startsWith("Http")) {
			return new Color(220, 230, 220);
		}
		return new Color(220, 220, 220);
	}
}
