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
package org.leo.traceroute;

import org.leo.traceroute.util.Util;

/**
 * Regex $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public class Regex {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		System.out.println(Util.replaceTs(" dsds 23 ms 13 ms 3 ms dsdsds", 3));
		System.out.println(Util.replaceTs("23 ms 3 ms", 2));
		System.out.println(Util.replaceTs("23dsds", 3));
	}
}
