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
package org.leo.traceroute.ui.control;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.leo.traceroute.core.autocomplete.AutoCompleteProvider;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.IRouteListener;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.whois.IWhoIsListener;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrenttrees.common.KeyValuePair;

/**
 * AutoCompleteComponent $Id: AutoCompleteComponent.java 133 2011-08-06
 * 12:53:54Z leolewis $
 *
 * <pre>
 * A component that display autocomplete values relative to the text
 * entered in the JTextField passed to the component.
 * Because of the bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6217905,
 * we need a strange but necessary behaviors on the JPopupMenu
 * </pre>
 *
 * @author Leo Lewis
 */
public class AutoCompleteComponent implements IRouteListener, IWhoIsListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCompleteComponent.class);

	/** JPopupMenu where are displayed the guest for the current search */
	private JPopupMenu _popup;

	/** JTextField which typed value will be autocompleted */
	private JTextField _autoCompleteTextField;

	/** Autocomplete in progress item */
	private JMenuItem _autocompleteInProgressItem;

	private ExecutorService _executor;

	private final AtomicReference<Future<?>> _future = new AtomicReference<>();

	private final AutoCompleteProvider _provider;

	/**
	 * Constructor
	 */
	public AutoCompleteComponent(final JTextField textfield, final AutoCompleteProvider provider) {
		super();
		_provider = provider;
		_autoCompleteTextField = textfield;
		init();
	}

	/**
	 * Init the component
	 */
	protected void init() {
		_executor = Executors.newFixedThreadPool(3, r -> {
			final Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("Autocomplete");
			t.setUncaughtExceptionHandler((t1, e) -> LOGGER.error("Unexpected error", e));
			return t;
		});
		_autocompleteInProgressItem = new JMenuItem();
		_autocompleteInProgressItem.setIcon(Resources.getImageIcon("in_progress.gif"));
		_popup = new JPopupMenu();
		_popup.setLightWeightPopupEnabled(false);
		// specific behavior for the Japanese Keyboard
		_autoCompleteTextField.addKeyListener(new KeyAdapter() {

			private boolean keyHasBeenPressed;

			@Override
			public void keyPressed(final KeyEvent e) {
				keyHasBeenPressed = true;
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				if (Env.INSTANCE.isDisableHistory()) {
					return;
				}
				final int key = e.getKeyCode();
				// if we are entering a word in kana mode, the keyPressed method
				// has not been called in this case, the enter key will not
				// search for the value of the TF but just valid the current
				// Kanji or Kana input
				if (keyHasBeenPressed) {
					keyHasBeenPressed = false;
					if (e.getKeyCode() == KeyEvent.VK_ENTER && !_popup.getSelectionModel().isSelected()) {
						// else, if it is Enter, but the popup is not selected,
						// we search the textfield value.

						// If the popup is selected, we'll search the selection
					} else if (key == KeyEvent.VK_KP_UP || key == KeyEvent.VK_UP) {
						// select the last element
						if (_popup.isVisible() && _popup.getComponentCount() > 1) {
							MenuElement menuElement;
							// careful, their might be a separator
							final Object obj = _popup.getComponent(_popup.getComponentCount() - 1);
							if (obj instanceof MenuElement) {
								menuElement = (MenuElement) obj;
							} else {
								menuElement = (MenuElement) _popup.getComponent(_popup.getComponentCount() - 2);
							}
							MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] { _popup, menuElement });
						}
					} else if (key == KeyEvent.VK_KP_DOWN || key == KeyEvent.VK_DOWN) {
						// select the first element
						if (_popup.isVisible() && _popup.getComponentCount() > 0) {
							MenuElement menuElement;
							// careful, their might be a separator
							final Object obj = _popup.getComponent(0);
							if (obj instanceof MenuElement) {
								menuElement = (MenuElement) obj;
							} else {
								menuElement = (MenuElement) _popup.getComponent(1);
							}
							MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] { _popup, menuElement });
						}
					} else if (key == KeyEvent.VK_ESCAPE && _popup.isVisible()) {
						// on escape char, hide the popup
						clearPopupAndStopCurrentAutoComplete();
					}
				}
			}

			@Override
			public void keyTyped(final KeyEvent e) {
				if (Env.INSTANCE.isDisableHistory()) {
					return;
				}
				final int key = e.getKeyCode();
				// if not Up, Down nor Enter or UNDEFINED (used when selecting a
				// kanji in the list of kanjis corresponding to the entered
				// characters)
				if (e.getKeyChar() != KeyEvent.VK_ENTER && e.getKeyChar() != KeyEvent.VK_ESCAPE && (key == KeyEvent.VK_UNDEFINED || (key != KeyEvent.VK_UP
						&& key != KeyEvent.VK_KP_UP && key != KeyEvent.VK_DOWN && key != KeyEvent.VK_KP_DOWN && key != KeyEvent.VK_ENTER && key != KeyEvent.VK_ESCAPE))) {
					String search = null;
					final String text = _autoCompleteTextField.getText();
					boolean remove = false;
					if (e.getKeyChar() == '\u0008') {
						remove = true;
						// backspace char, need to remove the text before searching
						if (text.length() > 1) {
							if (_autoCompleteTextField.getSelectedText() != null && !"".equals(_autoCompleteTextField.getSelectedText())) {
								search = text.substring(0, _autoCompleteTextField.getSelectionStart())
										+ text.substring(_autoCompleteTextField.getSelectionEnd(), text.length());
							} else if (_autoCompleteTextField.getCaretPosition() > 0) {
								search = text.substring(0, _autoCompleteTextField.getCaretPosition() - 1)
										+ text.substring(_autoCompleteTextField.getCaretPosition(), text.length());
							}
						}
					} else {
						// other char, we add it
						search = text + e.getKeyChar();
					}
					if (search != null) {
						if (remove) {
							_popup.removeAll();
							_popup.add(_autocompleteInProgressItem);
							showPopup();
							packPopup();
						}
						// display the autocomplete values
						getAutoCompleteValues(search);
					} else {
						clearPopupAndStopCurrentAutoComplete();
					}
				}
			}
		});

		// workaround of bug
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6217905
		_popup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuCanceled(final PopupMenuEvent e) {

			}

			@Override
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
			}

			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
				// By default, the first item of the JPopupMenu starts out
				// selected. We unselected it.
				final MenuSelectionManager manager = MenuSelectionManager.defaultManager();
				final MenuElement[] oldSelection = manager.getSelectedPath();
				final MenuElement[] newSelection = new MenuElement[oldSelection.length - 1];
				System.arraycopy(oldSelection, 0, newSelection, 0, oldSelection.length - 1);
				manager.setSelectedPath(newSelection);
			}
		});
		// add global mouse click listener.
		// the JPopupMenu hide on mouse click only if the target component take
		// the focus which is not the case of the OGLPanel for example
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			final MouseEvent mouseEvent = (MouseEvent) event;
			// click outside the popup, we hide the popup
			if ((mouseEvent.getComponent().getParent() == null || mouseEvent.getComponent().getParent() != _popup) && mouseEvent.getClickCount() > 0) {
				_popup.setVisible(false);
			}
		}, AWTEvent.MOUSE_EVENT_MASK);
	}

	private void getAutoCompleteValues(final String search) {
		final Future<?> f = _future.getAndSet(_executor.submit(() -> {
			try {
				final List<KeyValuePair<Integer>> values = _provider.getFromHistory(search);
				final Future<?> current = _future.getAndSet(null);
				if (current != null) {
					// if null, been cancelled
					asyncAutoComplete(values);
				}
			} catch (final Throwable e) {
				LOGGER.error("Fail to get autocomplete", e);
				JOptionPane.showMessageDialog(null, "failed " + Arrays.toString(ExceptionUtils.getStackFrames(e)));
			}
		}));
		if (f != null) {
			f.cancel(true);
		}
	}

	/**
	 * Dispose the component
	 */
	public void dispose() {
		final Future<?> f = _future.get();
		if (f != null) {
			f.cancel(true);
		}
		_executor.shutdownNow();
		_autoCompleteTextField = null;
		_popup = null;
	}

	public void asyncAutoComplete(final List<KeyValuePair<Integer>> autoCompleteFromIndex) {
		SwingUtilities.invokeLater(() -> {
			try {
				_popup.removeAll();
				// if null or empty, we hide the component
				if (autoCompleteFromIndex == null || autoCompleteFromIndex.isEmpty()) {
					_popup.setVisible(false);
				} else {
					for (int i = 0; i < autoCompleteFromIndex.size(); i++) {
						final String value = autoCompleteFromIndex.get(i).getKey().toString();
						final JMenuItem menuItem = new JMenuItem(value);

						menuItem.addActionListener(e -> searchWordsAndSetTextField(value));
						menuItem.addKeyListener(new KeyAdapter() {
							@Override
							public void keyPressed(final KeyEvent e) {
								if (e.getKeyCode() == KeyEvent.VK_ENTER) {
									searchWordsAndSetTextField(value);
								}
							}
						});
						// highlight/unhighlight manually the item on mouse
						// entered/exited
						// some JDK bug ?
						menuItem.addMouseListener(new MouseAdapter() {
							@Override
							public void mouseEntered(final MouseEvent evt) {
								menuItem.setBackground(UIManager.getColor("MenuItem.selectionBackground"));
								menuItem.setForeground(UIManager.getColor("MenuItem.selectionForeground"));
							}

							@Override
							public void mouseExited(final MouseEvent evt) {
								menuItem.setBackground(UIManager.getColor("MenuItem.background"));
								menuItem.setForeground(UIManager.getColor("MenuItem.foreground"));
							}
						});
						_popup.add(menuItem);
					}

					if (!_popup.isVisible()) {
						showPopup();
					} else {
						_popup.revalidate();
					}
					packPopup();

					// give back the focus to the textfield
					_autoCompleteTextField.requestFocus();
				}
			} catch (final Exception e) {
				LOGGER.warn("Not a big deal error", e);
			}
		});
	}

	private void showPopup() {
		_popup.show(_autoCompleteTextField, 0, _autoCompleteTextField.getHeight() + 2);
		if (Env.INSTANCE.getOs() == OS.mac) {
			// workaround for Mac, the textfield content got selected when showing the popup.
			// force unselect, otherwise typing will replace the text field content which is selected
			SwingUtilities.invokeLater(() -> {
				try {
					_autoCompleteTextField.setCaretPosition(_autoCompleteTextField.getCaretPosition());
				} catch (final Exception e) {
					LOGGER.warn("Not a big deal error", e);
				}
			});
		}
	}

	/**
	 * Pack the popumenu
	 */
	private void packPopup() {
		int height = 0;
		if (_popup.getComponentCount() > 1) {
			// careful their is the separator
			height = Math.max(_popup.getComponent(0).getPreferredSize().height, _popup.getComponent(1).getPreferredSize().height);
		} else {
			height = _popup.getComponent(0).getPreferredSize().height;
		}
		_popup.setPopupSize(_autoCompleteTextField.getWidth(), _popup.getComponentCount() * height + 10);

	}

	/**
	 * Search the given words, and set the value to the textfield
	 *
	 * @param words the words to search
	 */
	private void searchWordsAndSetTextField(final String words) {
		_autoCompleteTextField.setText(words);
		clearPopupAndStopCurrentAutoComplete();
	}

	/**
	 * Clear and hide the popup and stop the current autocomplete
	 */
	private void clearPopupAndStopCurrentAutoComplete() {
		_popup.removeAll();
		_popup.setVisible(false);
		stopCurrentAutoComplete();
	}

	/**
	 * Stop current auto complete
	 */
	private void stopCurrentAutoComplete() {
		final Future<?> currentAutocomplete = _future.getAndSet(null);
		if (currentAutocomplete != null) {
			currentAutocomplete.cancel(true);
		}
	}

	@Override
	public void newRoute(final boolean dnsLookup) {
		clearPopupAndStopCurrentAutoComplete();
	}

	@Override
	public void routePointAdded(final RoutePoint point) {

	}

	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {

	}

	@Override
	public void routeTimeout() {

	}

	@Override
	public void maxHops() {
	}

	@Override
	public void error(final Exception exception, final Object origin) {

	}

	@Override
	public void routeCancelled() {

	}

	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {

	}

	public void setEnabled(final boolean enable) {
		if (!enable) {
			clearPopupAndStopCurrentAutoComplete();
		}
	}

	@Override
	public void startWhoIs(final String host) {
		clearPopupAndStopCurrentAutoComplete();
	}

	@Override
	public void focusWhoIs(final GeoPoint point) {
	}

	@Override
	public void whoIsResult(final String result) {
	}

}
