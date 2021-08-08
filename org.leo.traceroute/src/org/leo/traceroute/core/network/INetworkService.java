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
package org.leo.traceroute.core.network;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.IComponent;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;

/**
 * INetworkService $Id$
 * <pre>
 * </pre>
 * @author leo
 */
public interface INetworkService<T> extends IComponent {

	String OS_DEFAULT = "OS Default";

	List<Pair<Integer, String>> getNetworkDevices(Mode mode);

	void setCurrentNetworkDevice(final Mode mode, final int deviceIndex);

	void notifyInterface(Mode mode);

	void addListener(INetworkInterfaceListener<?> listener);

	void removeListener(INetworkInterfaceListener<?> listener);

	String getCurrentNetworkInterfaceName(Mode mode);
	
	int getCurrentNetworkInterfaceIndex(Mode mode);
}
