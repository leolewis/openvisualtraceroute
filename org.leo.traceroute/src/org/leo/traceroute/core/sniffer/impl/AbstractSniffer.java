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
package org.leo.traceroute.core.sniffer.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.leo.traceroute.core.AbstractObject;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint.Protocol;
import org.leo.traceroute.core.sniffer.IPacketListener;
import org.leo.traceroute.core.sniffer.IPacketsSniffer;
import org.leo.traceroute.ui.sniffer.PacketTablePanel.Column;

/**
 * AbstractSniffer $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public abstract class AbstractSniffer extends AbstractObject<IPacketListener> implements IPacketsSniffer {

	/** Counter of packets */
	protected final AtomicInteger _count = new AtomicInteger();

	/** Capture running */
	protected volatile boolean _capturing;
	protected volatile boolean _filterLenghtPackets;
	protected volatile int _length;
	/** Captured packets */
	protected final List<AbstractPacketPoint> _capture = new ArrayList<AbstractPacketPoint>();
	protected AbstractPacketPoint _focusedPoint;
	protected Set<Protocol> _captureProtocols;
	protected final Set<String> _localAddresses = new HashSet<String>();
	protected String _filter;
	/** Services */
	protected ServiceFactory _services;

	/**
	 * @see org.leo.traceroute.core.IComponent#init(org.leo.traceroute.core.ServiceFactory)
	 */
	@Override
	public void init(final ServiceFactory services) throws Exception {
		_services = services;
	}

	/**
	 * @return
	 */
	@Override
	public List<AbstractPacketPoint> getCapture() {
		return _capture;
	}

	/**
	 * Renotify packets
	 */
	@Override
	public void renotifyPackets() {
		if (!_capture.isEmpty()) {
			for (final IPacketListener listener : getListeners()) {
				listener.startCapture();
				for (final AbstractPacketPoint point : _capture) {
					listener.packetAdded(point);
				}
				listener.captureStopped();
				if (_focusedPoint != null) {
					focus(_focusedPoint, false);
				}
			}
		}
	}

	/**
	 * Clear current packets capture
	 */
	@Override
	public void clear() {
		_capture.clear();
	}

	/**
	 * Focus on the given packet
	 * @param point
	 * @param animation
	 */
	@Override
	public void focus(final AbstractPacketPoint point, final boolean animation) {
		_focusedPoint = point;
		for (final IPacketListener listener : getListeners()) {
			listener.focusPacket(point, false, animation);
		}
	}

	@Override
	public String toText() {
		return toString("\t");
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketsSniffer#toCSV()
	 */
	@Override
	public String toCSV() {
		return toString(", ");
	}

	public String toString(final String separator) {
		final StringBuilder builder = new StringBuilder();
		final int colSize = Column.values().length;
		for (int i = 0; i < colSize; i++) {
			final Column col = Column.values()[i];
			if (col.isExport()) {
				builder.append(col.getLabel());
				if (i != colSize - 1) {
					builder.append(separator);
				}
			}
		}
		builder.append("\n");
		for (int row = 0; row < _capture.size(); row++) {
			for (int col = 0; col < colSize; col++) {
				final Column column = Column.values()[col];
				if (column.isExport()) {
					builder.append(column.getValue(_capture.get(row)));
					if (col != colSize - 1) {
						builder.append(separator);
					}
				}
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	protected String convertPortToFilter(final String protocol, final String port) {
		if (port == null) {
			return "";
		}
		final String[] split = port.split(",");
		final StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (final String s : split) {
			if (first) {
				first = false;
			} else {
				sb.append(" or ");
			}
			sb.append("(").append(protocol).append(" dst ");
			if (s.contains("-")) {
				sb.append("portrange ");
			} else {
				sb.append("port ");
			}
			sb.append(s);
			sb.append(")");
		}
		return sb.toString();
	}
}
