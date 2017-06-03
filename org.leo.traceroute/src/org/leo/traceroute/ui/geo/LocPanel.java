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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.AbstractPanel;

/**
 * AddLocRecordsDialog $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public class LocPanel extends AbstractPanel {

	private static final long serialVersionUID = 34056165346486571L;

	private double _lat;
	private double _lon;
	private final JLabel _geo;
	private final JTextField _host;
	private final JButton _add;

	public LocPanel(final ServiceFactory factory) {
		super(factory);
		setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
		add(new JLabel(Resources.getLabel("host")));
		_host = new JTextField(20);
		add(_host);
		_geo = new JLabel("                     ");
		add(_geo);
		_add = new JButton(Resources.getLabel("add"));
		_add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				factory.getGeo().addLocRecord(_host.getText(), _lat, _lon);
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(LocPanel.this), Resources.getLabel("dns.loc.updated"));
			}
		});
		add(_add);
	}

	public void setPoint(final double lat, final double lon) {
		_lat = lat;
		_lon = lon;
		_geo.setText(lat + "Lat " + lon + "Lon");
	}

	public void setHost(final GeoPoint point) {
		_host.setText(point != null
				? (StringUtils.isEmpty(point.getHostname()) || point.getHostname().equals(DNSLookupService.UNKNOWN_HOST)) ? point.getIp() : point.getHostname() : null);
		if (point != null) {
			setPoint(point.getLat(), point.getLon());
		}
		_add.setEnabled(point != null);
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#focusRoute(org.leo.traceroute.core.route.RoutePoint, boolean, boolean)
	 */
	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {
		setHost(point);
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#focusWhoIs(org.leo.traceroute.core.geo.GeoPoint)
	 */
	@Override
	public void focusWhoIs(final GeoPoint point) {
		setHost(point);
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#focusPacket(org.leo.traceroute.core.sniffer.AbstractPacketPoint, boolean, boolean)
	 */
	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {
		setHost(point);
	}

	@Override
	public void newRoute(final boolean dnsLookup) {
		setHost(null);
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
	 * @see org.leo.traceroute.core.route.IRouteListener#maxHops()
	 */
	@Override
	public void maxHops() {
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#routeCancelled()
	 */
	@Override
	public void routeCancelled() {
	}

	/**
	 * @see org.leo.traceroute.core.IListener#error(java.lang.Exception, java.lang.Object)
	 */
	@Override
	public void error(final Exception ex, final Object origin) {
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
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#startWhoIs(java.lang.String)
	 */
	@Override
	public void startWhoIs(final String host) {
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#whoIsResult(java.lang.String)
	 */
	@Override
	public void whoIsResult(final String result) {
	}
}
