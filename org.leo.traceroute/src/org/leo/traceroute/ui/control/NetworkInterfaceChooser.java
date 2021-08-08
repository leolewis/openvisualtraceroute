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
import java.awt.image.ImageObserver;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.AbstractPanel;

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
	private JComboBox<String> _deviceSelectionCombo;

	private Mode _mode;

	/**
	 * Constructor
	 */
	@SuppressWarnings("serial")
	public NetworkInterfaceChooser(final ServiceFactory services, final Mode mode) {
		super(services);
		_mode = mode;
		_deviceSelectionCombo = new JComboBox<>();
		_deviceSelectionCombo.setToolTipText(Resources.getLabel("device.select.label"));
		_deviceSelectionCombo.setPreferredSize(new Dimension(250, ImageObserver.HEIGHT));
		_deviceSelectionCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
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
		final List<Pair<Integer, String>> devices = _services.getNetworkService().getNetworkDevices(mode);
		for (final Pair<Integer, String> net : devices) {
			_deviceSelectionCombo.addItem(net.getRight());
		}
		final int index = services.getNetworkService().getCurrentNetworkInterfaceIndex(mode);
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
			_services.getNetworkService().setCurrentNetworkDevice(_mode, selection);
			if (_mode == Mode.TRACE_ROUTE) {
				Env.INSTANCE.setTrInterfaceIndex(selection);
			} else {
				Env.INSTANCE.setSnifferInterfaceIndex(selection);
			}
		}
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#newRoute(boolean)
	 */
	@Override
	public void newRoute(final boolean dnsLookup) {
		setEnabled(false);
	}

	@Override
	public void routePointAdded(final RoutePoint point) {

	}

	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
		setEnabled(true);
	}

	@Override
	public void error(final Exception exception, final Object origin) {
		setEnabled(true);
	}

	@Override
	public void routeCancelled() {
		setEnabled(true);
	}

	@Override
	public void routeTimeout() {
		setEnabled(true);
	}

	@Override
	public void maxHops() {
		setEnabled(true);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		_deviceSelectionCombo.setEnabled(enabled);
	}

	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {

	}

	@Override
	public void startCapture() {
		setEnabled(false);
	}

	@Override
	public void packetAdded(final AbstractPacketPoint point) {

	}

	@Override
	public void captureStopped() {
		setEnabled(true);
	}

	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {
	}

	@Override
	public void startWhoIs(final String host) {
	}

	@Override
	public void focusWhoIs(final GeoPoint point) {
	}

	@Override
	public void whoIsResult(final String result) {
	}

}
