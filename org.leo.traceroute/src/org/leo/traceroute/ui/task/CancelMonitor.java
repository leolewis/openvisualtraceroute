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
package org.leo.traceroute.ui.task;

/**
 * CancelMonitor $Id: CancelMonitor.java 146 2015-01-10 11:22:06Z leolewis $
 * Object that will be created at some point in the code, passed to other parts of the code (thread, UI).
 * Typical use is the value will be changed by the GUI (user cancel the process) and the Thread will
 * run until the monitor is not canceled.
 * @author Leo Lewis
 */
public class CancelMonitor {

	/** Canceled value */
	private volatile boolean canceled = false;

	/**
	 * Return the value of the field canceled
	 * 
	 * @return the value of canceled
	 */
	public boolean isCanceled() {
		return canceled;
	}

	/**
	 * Set the value of the field canceled
	 * 
	 * @param canceled the new canceled to set
	 */
	public void setCanceled(final boolean canceled) {
		this.canceled = canceled;
	}
}
