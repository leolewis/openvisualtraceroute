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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.leo.traceroute.core.geo.GeoPoint;

/**
 * PacketPoint $Id: AbstractPacketPoint.java 106 2014-05-10 13:28:12Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public abstract class AbstractPacketPoint extends GeoPoint {

	private static final String ENCODING = "Accept-Encoding: ";

	public enum Protocol {
		TCP, UDP, ICMP, OTHER
	}

	enum Encoding {
		PLAIN, GZIP;
	}

	private static ThreadLocal<SimpleDateFormat> THREAD_LOCAL = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS");
		}
	};

	private String _string;

	private Protocol _protocol;

	private long _ts;

	private String _date;

	/**
	 * Return the value of the field protocol
	 * @return the value of protocol
	 */
	public Protocol getProtocol() {
		return _protocol;
	}

	/**
	 * Set the value of the field protocol
	 * @param protocol the new protocol to set
	 */
	public void setProtocol(final Protocol protocol) {
		_protocol = protocol;
	}

	/**
	 * Return the value of the field ts
	 * @return the value of ts
	 */
	public long getTs() {
		return _ts;
	}

	/**
	 * Set the value of the field ts
	 * @param ts the new ts to set
	 */
	public void setTs(final long ts) {
		_ts = ts;
		_date = THREAD_LOCAL.get().format(new Date(_ts));
	}

	public abstract Integer getSourcePort();

	public abstract Integer getDestPort();

	public abstract int getDataLength();

	/**
	 * Return the value of the field date
	 * @return the value of date
	 */
	public String getDate() {
		return _date;
	}

	protected abstract String buildPayload();

	public String getPayload() {
		if (_string == null) {
			_string = buildPayload();
		}
		return _string;
	}
}
