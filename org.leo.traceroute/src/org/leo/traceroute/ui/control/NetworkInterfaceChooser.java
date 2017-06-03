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
package org.leo.traceroute.ui.control;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.AbstractPanel;
import org.leo.traceroute.ui.control.ControlPanel.Mode;

/**
 * NetworkInterfaceChooser $Id: NetworkInterfaceChooser.java 241 2016-02-20 21:23:52Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author leo.lewis
 */
public class NetworkInterfaceChooser extends AbstractPanel {

	/**  */
	private static final long serialVersionUID = 7328287731274784321L;

	/** Choice combo box */
	private JComboBox _deviceSelectionCombo;

	private Mode _mode;

	/**
	 * Constructor
	 *
	 * @param route
	 */
	@SuppressWarnings("serial")
	public NetworkInterfaceChooser(final ServiceFactory services, final Mode mode) {
		super(services);
		_mode = mode;
		_deviceSelectionCombo = new JComboBox();
		_deviceSelectionCombo.setToolTipText(Resources.getLabel("device.select.label"));
		_deviceSelectionCombo.setPreferredSize(new Dimension(250, ControlPanel.HEIGHT));
		_deviceSelectionCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				label.setIcon(Resources.getImageIcon("network.png"));
				if (value != null) {
					final String text = value.toString();
					label.setText(text);
					label.setToolTipText(text);
				} else {
					label.setText("");
				}
				return label;
			}
		});
		add(_deviceSelectionCombo, BorderLayout.CENTER);
		final List<Pair<Integer, String>> devices = _services
				.getNetwork(mode == Mode.SNIFFER ? ServiceFactory.SNIFFER_NETWORK_LIB : ServiceFactory.TRACEROUTE_NETWORK_LIB)
				.getNetworkDevices();
		for (final Pair<Integer, String> net : devices) {
			_deviceSelectionCombo.addItem(net.getRight());
		}
		final int index = services
				.getNetwork(mode == Mode.SNIFFER ? ServiceFactory.SNIFFER_NETWORK_LIB : ServiceFactory.TRACEROUTE_NETWORK_LIB)
				.getCurrentNetworkInterfaceIndex();
		if (_deviceSelectionCombo.getItemCount() > index) {
			_deviceSelectionCombo.setSelectedIndex(index);
		} else if (_deviceSelectionCombo.getItemCount() > 0) {
			_deviceSelectionCombo.setSelectedIndex(0);
			if (mode == Mode.TRACE_ROUTE) {
				Env.INSTANCE.setTrInterfaceIndex(0);
			} else {
				Env.INSTANCE.setSnifferInterfaceIndex(0);
			}
		}
	}

	public void applySelection() {
		final int selection = _deviceSelectionCombo.getSelectedIndex();
		if (selection != -1) {
			_services
					.getNetwork(
							_mode == Mode.SNIFFER ? ServiceFactory.SNIFFER_NETWORK_LIB : ServiceFactory.TRACEROUTE_NETWORK_LIB)
					.setCurrentNetworkDevice(selection);
			if (_mode == Mode.TRACE_ROUTE) {
				Env.INSTANCE.setTrInterfaceIndex(selection);
			} else {
				Env.INSTANCE.setSnifferInterfaceIndex(selection);
			}
		}
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#newRoute(boolean)
	 */
	@Override
	public void newRoute(final boolean dnsLookup) {
		setEnabled(false);
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#routePointAdded(org.leo.traceroute.core.RoutePoint)
	 */
	@Override
	public void routePointAdded(final RoutePoint point) {

	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#done(long)
	 */
	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
		setEnabled(true);
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#error(java.io.IOException)
	 */
	@Override
	public void error(final Exception exception, final Object origin) {
		setEnabled(true);
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#cancelled()
	 */
	@Override
	public void routeCancelled() {
		setEnabled(true);
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#timeout()
	 */
	@Override
	public void routeTimeout() {
		setEnabled(true);
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#maxHops()
	 */
	@Override
	public void maxHops() {
		setEnabled(true);
	}

	/**
	 * @see javax.swing.JComponent#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		_deviceSelectionCombo.setEnabled(enabled);
	}

	/**
	 * @see org.leo.traceroute.core.IRouteListener#focusRoute(org.leo.traceroute.core.RoutePoint, boolean, boolean)
	 */
	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {

	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#startCapture(boolean)
	 */
	@Override
	public void startCapture() {
		setEnabled(false);
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#packetAdded(org.leo.traceroute.core.sniffer.PacketPoint)
	 */
	@Override
	public void packetAdded(final AbstractPacketPoint point) {

	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#done()
	 */
	@Override
	public void captureStopped() {
		setEnabled(true);
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#focusRoute(org.leo.traceroute.core.sniffer.PacketPoint, boolean, boolean)
	 */
	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#startWhoIs(org.leo.traceroute.core.geo.GeoPoint, java.lang.String)
	 */
	@Override
	public void startWhoIs(final String host) {
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#focusWhoIs(org.leo.traceroute.core.geo.GeoPoint)
	 */
	@Override
	public void focusWhoIs(final GeoPoint point) {
	}

	/**
	 * @see org.leo.traceroute.core.whois.IWhoIsListener#whoIsResult(java.lang.String)
	 */
	@Override
	public void whoIsResult(final String result) {
	}

}
