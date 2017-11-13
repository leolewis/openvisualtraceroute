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
package org.leo.traceroute.core.autocomplete;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.leo.traceroute.core.AbstractObject;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.install.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;

/**
 * AutoCompleteProvider $Id$
 * <pre>
 * </pre>
 * @author Leo
 */
public class AutoCompleteProvider extends AbstractObject<Void> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCompleteProvider.class);

	private static final int MAX = 20;

	/** Prefix tries */
	private RadixTree<Integer> _tree = new ConcurrentRadixTree<>(new DefaultByteArrayNodeFactory());
	/** For serialization */
	private Map<String, Integer> _map;

	private final Comparator<KeyValuePair<Integer>> _comp = (o1, o2) -> (o2.getValue() == null ? 0 : o2.getValue()) - (o1.getValue() == null ? 0 : o1.getValue());

	/**
	 * @see org.leo.traceroute.core.AbstractObject#init(org.leo.traceroute.core.ServiceFactory)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void init(final ServiceFactory factory) throws IOException, ClassNotFoundException {
		if (Env.HISTORY.exists()) {
			FileInputStream is = null;
			ObjectInput input = null;
			try {
				is = new FileInputStream(Env.HISTORY);
				input = new ObjectInputStream(is);
				_map = (HashMap<String, Integer>) input.readObject();
			} catch (final Exception e) {
				Env.HISTORY.delete();
			} finally {
				IOUtils.closeQuietly(is);
				if (input != null) {
					input.close();
				}
			}
		}
		if (_map == null) {
			_map = new HashMap<>();
		} else {
			for (final Entry<String, Integer> entry : _map.entrySet()) {
				_tree.put(entry.getKey(), entry.getValue());
			}
		}

	}

	/**
	 * Get from history
	 * @param prefix
	 * @return
	 */
	public List<KeyValuePair<Integer>> getFromHistory(final String prefix) {
		final List<KeyValuePair<Integer>> res = new ArrayList<>();
		final Iterable<KeyValuePair<Integer>> entries = _tree.getKeyValuePairsForKeysStartingWith(prefix);
		for (final KeyValuePair<Integer> entry : entries) {
			res.add(entry);
		}
		Collections.sort(res, _comp);
		return res.subList(0, Math.min(res.size(), MAX));
	}

	/**
	 * Add an entry into the history
	 * @param value
	 */
	public void addToHistory(final String value) {
		Integer occurence = _tree.getValueForExactKey(value);
		if (occurence == null) {
			occurence = 1;
		} else {
			occurence++;
		}
		_tree.put(value, occurence);
		_map.put(value, occurence);
	}

	/**
	 * @see org.leo.traceroute.core.AbstractObject#dispose()
	 */
	@Override
	public void dispose() {
		if (_map != null) {
			FileOutputStream os = null;
			ObjectOutput output = null;
			try {
				os = new FileOutputStream(Env.HISTORY);
				output = new ObjectOutputStream(os);
				output.writeObject(_map);
				output.flush();
			} catch (final Exception e) {
				LOGGER.error("Failed to persist history", e);
				Env.HISTORY.delete();
			} finally {
				IOUtils.closeQuietly(os);
				if (output != null) {
					try {
						output.close();
					} catch (final IOException e) {

					}
				}
			}
		}
	}

	/**
	 * Clear history
	 */
	public void clear() {
		_map.clear();
		_tree = new ConcurrentRadixTree<>(new DefaultByteArrayNodeFactory());
	}
}
