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
package org.leo.traceroute.ui.whois;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.resources.CountryFlagManager.Resolution;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.AbstractPanel;
import org.leo.traceroute.ui.route.RouteTablePanel.Column;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.leo.traceroute.ui.util.WrapLayout;

/**
 * WhoIsDialog $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public class WhoIsPanel extends AbstractPanel {

	/**  */
	private static final long serialVersionUID = -4249083499712317930L;

	private final JTextArea _textArea;
	private final JLabel _label;

	public WhoIsPanel(final ServiceFactory factory) {
		super(factory);
		final JPanel top = new JPanel();
		top.setLayout(new WrapLayout(FlowLayout.LEFT, 2, 0));
		_label = new JLabel("", JLabel.LEFT);
		top.add(_label);
		add(top, BorderLayout.NORTH);
		_textArea = new JTextArea("", 30, 70);
		_textArea.setEditable(false);
		final JScrollPane scroll = new JScrollPane(_textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scroll, BorderLayout.CENTER);
		_whois.addListener(this);
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#startWhoIs(org.leo.traceroute.core.geo.GeoPoint, java.lang.String)
	 */
	@Override
	public void startWhoIs(final String host) {
		_textArea.setText(Resources.getLabel("wait"));
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#focusWhoIs(org.leo.traceroute.core.geo.GeoPoint)
	 */
	@Override
	public void focusWhoIs(final GeoPoint point) {
		_label.setText("<html>Who is <b>" + Column.IP.getLabel() + ":</b> " + point.getIp() + "   " + "<b>"
				+ Column.HOSTNAME.getLabel() + ":</b> " + point.getHostname() + "</html>");
		_label.setIcon(point.getCountryFlag(Resolution.R32));
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#whoIsResult(java.lang.String)
	 */
	@Override
	public void whoIsResult(final String result) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				_textArea.setText(result);
				_textArea.setCaretPosition(0);
			}
		});
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#error(java.lang.Exception)
	 */
	@Override
	public void error(final Exception error, final Object origin) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				_textArea.setText(Resources.getLabel("no.whois.data"));
				_textArea.setCaretPosition(0);
			}
		});
	}

	@Override
	public void dispose() {
		_whois.removeListener(this);
		super.dispose();
	}

	/**
	 * Show a dialog with whois data for the given point
	 * @param parent
	 * @param whois
	 * @param point
	 */
	public static void showWhoIsDialog(final JComponent parent, final ServiceFactory services, final GeoPoint point) {
		final WhoIsPanel panel = new WhoIsPanel(services);
		final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Who is " + point.getIp(),
				ModalityType.APPLICATION_MODAL) {

			private static final long serialVersionUID = 1258611715478157956L;

			@Override
			public void dispose() {
				panel.dispose();
				super.dispose();
			}

		};
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		final JPanel bottom = new JPanel();
		final JButton close = new JButton(Resources.getLabel("close.button"));
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				dialog.dispose();
			}
		});
		bottom.add(close);
		dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
		services.getWhois().whoIs(point.getIp());
		SwingUtilities4.setUp(dialog);
		dialog.setVisible(true);

	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#newRoute(boolean)
	 */
	@Override
	public void newRoute(final boolean dnsLookup) {

	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#routePointAdded(org.leo.traceroute.core.route.RoutePoint)
	 */
	@Override
	public void routePointAdded(final RoutePoint point) {

	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#routeDone(long, long)
	 */
	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {

	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#routeTimeout()
	 */
	@Override
	public void routeTimeout() {

	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#routeCancelled()
	 */
	@Override
	public void routeCancelled() {
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#maxHops()
	 */
	@Override
	public void maxHops() {

	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#focusRoute(org.leo.traceroute.core.route.RoutePoint, boolean, boolean)
	 */
	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {

	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#startCapture()
	 */
	@Override
	public void startCapture() {

	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#packetAdded(org.leo.traceroute.core.sniffer.AbstractPacketPoint)
	 */
	@Override
	public void packetAdded(final AbstractPacketPoint point) {

	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#captureStopped()
	 */
	@Override
	public void captureStopped() {

	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#focusPacket(org.leo.traceroute.core.sniffer.AbstractPacketPoint, boolean, boolean)
	 */
	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {

	}
}
