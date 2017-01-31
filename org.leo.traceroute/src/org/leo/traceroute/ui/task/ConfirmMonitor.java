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
 * ConfirmMonitor $Id: ConfirmMonitor.java 146 2015-01-10 11:22:06Z leolewis $
 * 
 * @author Leo Lewis
 */
public class ConfirmMonitor {

	/** */
	private volatile boolean ok = false;

	/** */
	private volatile boolean set = false;

	/** */
	private String confirmLabel;
	/** */
	private String refuseLabel;

	/**
	 * Constructor
	 * 
	 * @param confirmLabel
	 * @param refuseLabel
	 */
	public ConfirmMonitor(final String confirmLabel, final String refuseLabel) {
		this.confirmLabel = confirmLabel;
		this.refuseLabel = refuseLabel;
	}

	/**
	 * Return the value of the filed ok
	 * 
	 * @return the value of ok
	 */
	public boolean isOk() {
		return ok;
	}

	/**
	 * Set the value of the field ok
	 * 
	 * @param ok the new ok to set
	 */
	public void setOk(final boolean ok) {
		this.ok = ok;
	}

	/**
	 * Return the value of the field set
	 * 
	 * @return the value of set
	 */
	public boolean isSet() {
		return set;
	}

	/**
	 * Set the value of the field set
	 * 
	 * @param set the new set to set
	 */
	public void setSet(final boolean set) {
		this.set = set;
	}

	/**
	 * Return the value of the field confirmLabel
	 * 
	 * @return the value of confirmLabel
	 */
	public String getConfirmLabel() {
		return confirmLabel;
	}

	/**
	 * Set the value of the field confirmLabel
	 * 
	 * @param confirmLabel the new confirmLabel to set
	 */
	public void setConfirmLabel(final String confirmLabel) {
		this.confirmLabel = confirmLabel;
	}

	/**
	 * Return the value of the field refuseLabel
	 * 
	 * @return the value of refuseLabel
	 */
	public String getRefuseLabel() {
		return refuseLabel;
	}

	/**
	 * Set the value of the field refuseLabel
	 * 
	 * @param refuseLabel the new refuseLabel to set
	 */
	public void setRefuseLabel(final String refuseLabel) {
		this.refuseLabel = refuseLabel;
	}

}
