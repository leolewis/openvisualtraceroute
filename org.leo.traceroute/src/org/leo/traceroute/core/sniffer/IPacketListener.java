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

import org.leo.traceroute.core.IListener;

/**
 * IPacketListener $Id: IPacketListener.java 208 2015-10-12 07:07:41Z leolewis $
 * Object that want to be notify of packet capture
 *
 * @author Leo Lewis
 */
public interface IPacketListener extends IListener {

	/**
	 * New capture started
	 *
	 */
	void startCapture();

	/**
	 * Packet captured
	 *
	 * @param point point captured
	 */
	void packetAdded(AbstractPacketPoint point);

	/**
	 * Capture stopped
	 */
	void captureStopped();

	/**
	 * Focus the given point
	 *
	 * @param point the point
	 * @param isTracing if the focus is done when the route is tracing
	 * @param animation if we do an animation (might not be supported by the view)
	 */
	void focusPacket(AbstractPacketPoint point, boolean isCapturing, boolean animation);
}
