/**
 * Open Visual Trace Route
 * Copyright (c) 2010-2015 Leo Lewis.
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
package org.leo.traceroute.core.route;

import java.io.IOException;

/**
 * RouteException $Id: RouteException.java 146 2015-01-10 11:22:06Z leolewis $
 *
 * @author Leo Lewis
 */
public class RouteException extends IOException {

	/**  */
	private static final long serialVersionUID = 8591084195721388655L;

	/**
	 * Constructor
	 *
	 * @param mes message
	 */
	public RouteException(final String mes) {
		super(mes);
	}

	/**
	 * Constructor
	 *
	 * @param mes message
	 * @param e exception
	 */
	public RouteException(final String mes, final Throwable e) {
		super(mes, e);
	}
}
