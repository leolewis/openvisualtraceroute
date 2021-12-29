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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.leo.traceroute.core.IComponent;
import org.leo.traceroute.core.ServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Address;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

/**
 * DNSLookupManager $Id: DNSLookupService.java 235 2016-01-31 09:10:45Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class DNSLookupService implements IComponent {

	private static final Logger LOGGER = LoggerFactory.getLogger(DNSLookupService.class);

	public static final int UNDEF = -1;
	/** Unknown hostname */
	public final static String UNKNOWN_HOST = "(None)";

	/** DNS resolver */
	protected Resolver _resolver;

	public DNSLookupService() {
	}

	@Override
	public void init(final ServiceFactory factory) throws UnknownHostException {
		_resolver = new ExtendedResolver();
	}

	/**
	 * Dns lookup more efficient than the INetAddress.getHostName(ip)
	 *
	 * @param hostIp
	 * @return
	 * @throws IOException
	 */
	public String dnsLookup(final String hostIp) {
		try {
			final Name name = ReverseMap.fromAddress(hostIp);
			final int type = Type.PTR;
			final int dclass = DClass.IN;
			final Record rec = Record.newRecord(name, type, dclass);
			final Message query = Message.newQuery(rec);

			final Message response = _resolver.send(query);

			final List<Record> answers = response.getSection(Section.ANSWER);
			if (!answers.isEmpty()) {
				String ret = answers.get(0).rdataToString();
				if (ret.endsWith(".")) {
					ret = ret.substring(0, ret.length() - 1);
				}
				return ret;
			}
		} catch (final IOException e) {
			LOGGER.warn("Failed to resolve hostname for " + hostIp, e);
		}
		return UNKNOWN_HOST;
	}

	public InetAddress getIp(final String name) throws UnknownHostException {
		return Address.getByName(name);
	}

	public static void main(final String[] args) throws UnknownHostException {
		long t = System.nanoTime();

		System.out.println(Address.getByName("www.google.fr").getHostAddress() + " " + (System.nanoTime() - t));
		t = System.nanoTime();
		System.out.println(InetAddress.getByName("www.google.fr").getHostAddress() + " " + (System.nanoTime() - t));
	}

	/**
	 * dispose
	 */
	@Override
	public void dispose() {
	}

}
