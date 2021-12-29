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
package org.leo.traceroute.core;

import org.leo.traceroute.ui.AbstractPanel;
import org.leo.traceroute.ui.util.SwingUtilities4;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AbstractListener $Id: AbstractObject.java 241 2016-02-20 21:23:52Z leolewis $
 * <pre>
 * Generic listeners provider class
 * </pre>
 * @author Leo Lewis
 */
public abstract class AbstractObject<T> implements IDisposable {

	/**
	 * Threads pool
	 */
	protected ExecutorService _threadPool = Executors.newFixedThreadPool(5, r -> {
		final Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	});

	protected ServiceFactory _factory;

	/**
	 * Service Init
	 *
	 * @param factory
	 */
	public void init(final ServiceFactory factory) throws Exception {
		_factory = factory;
	}

	/**
	 * Set of listeners
	 */
	private final Set<T> _listeners = new HashSet<>();

	/**
	 * Add listener
	 *
	 * @param listener
	 */
	public void addListener(final T listener) {
		_listeners.add(listener);
	}

	/**
	 * Remove listener
	 *
	 * @param listener
	 */
	public void removeListener(final T listener) {
		_listeners.remove(listener);
	}

	/**
	 * @see org.leo.traceroute.core.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		_listeners.clear();
		_threadPool.shutdown();
	}

	public void notifyListeners(INotifyRunnable<T> notify) {
		for (T listener : _listeners) {
			if (listener instanceof AbstractPanel) {
				if (SwingUtilities.isEventDispatchThread()) {
					notify.run(listener);
				} else {
					try {
						SwingUtilities.invokeAndWait(() -> notify.run(listener));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				notify.run(listener);
			}
		}
	}
	@FunctionalInterface
	public interface INotifyRunnable<T> {
		void run(T listener);
	}
}
