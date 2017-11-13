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
package org.leo.traceroute.core.sniffer;

import java.util.List;
import java.util.Set;

import org.leo.traceroute.core.IComponent;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint.Protocol;

/**
 * IPacketsSniffer $Id$
 * <pre>
 * </pre>
 * @author leo
 */
public interface IPacketsSniffer extends IComponent {

	/**
	 * Start capture
	 * @param protocols
	 * @param port
	 * @param filterLenghtPackets
	 * @param length
	 * @param host
	 * @param captureTimeSeconds
	 */
	void startCapture(final Set<Protocol> protocols, final String port, final boolean filterLenghtPackets, final int length, String host, int captureTimeSeconds);

	/**
	 * End capture
	 */
	void endCapture();

	/**
	 * Focus the given captured point
	 * @param point
	 * @param animation
	 */
	void focus(final AbstractPacketPoint point, final boolean animation);

	void addListener(final IPacketListener listener);

	void removeListener(final IPacketListener listener);

	/**
	 * Clear the capture
	 */
	void clear();

	/**
	 * @return the capture
	 */
	List<AbstractPacketPoint> getCapture();

	/**
	 * Renotify packets
	 */
	void renotifyPackets();

	/**
	 * Convert capture to CSV
	 * @return
	 */
	String toCSV();

	/**
	 * Convert capture to text
	 */
	String toText();
}
