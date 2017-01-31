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
package org.leo.traceroute.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;

import org.apache.commons.io.IOUtils;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.control.ControlPanel;
import org.leo.traceroute.ui.control.ControlPanel.Mode;
import org.leo.traceroute.ui.geo.OpenMapPanel;
import org.leo.traceroute.ui.geo.WWJPanel;
import org.leo.traceroute.ui.route.GanttPanel;
import org.leo.traceroute.ui.route.ReplayPanel;
import org.leo.traceroute.ui.route.RouteTablePanel;
import org.leo.traceroute.ui.sniffer.PacketDetailPanel;
import org.leo.traceroute.ui.sniffer.PacketTablePanel;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.leo.traceroute.ui.whois.WhoIsPanel;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MainPanel $Id: MainPanel.java 272 2016-09-22 05:38:31Z leolewis $
 *
 * @author Leo Lewis
 */
public class MainPanel extends JPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(MainPanel.class);

	/**  */
	private static final long serialVersionUID = 9197337032949585106L;

	/** Service factory */
	private final ServiceFactory _services;

	/** 3D Panel */
	private WWJPanel _3dPanel;

	/** 2D Panel */
	private OpenMapPanel _2dPanel;

	/** Route Table Panel */
	private RouteTablePanel _routeTablePanel;
	/** Gantt Panel */
	private GanttPanel _ganttPanel;

	/** Sniffer Table Panel */
	private PacketTablePanel _packetTablePanel;
	/** Packet details Table Panel */
	private PacketDetailPanel _packetDetailsPanel;

	/** Who is panel */
	private WhoIsPanel _whoIsPanel;

	/** Status Panel */
	private StatusPanel _statusPanel;

	/** Replay Panel */
	private ReplayPanel _replayPanel;

	/** Split */
	private JSplitPane _split;
	/** Split */
	private JSplitPane _rightSplit;
	private JPanel _rightPanel;

	/** Control panel */
	private ControlPanel _controlPanel;

	/**
	 * Constructor
	 */
	public MainPanel(final ServiceFactory services) {
		super(new BorderLayout());
		_services = services;
		init();
	}

	/**
	 * Init the component
	 */
	private void init() {
		if (!Env.INSTANCE.isOpenGLAvailable()) {
			LOGGER.warn("No graphic card that supports required OpenGL features has been detected. The 3D map will be not be available");
		}
		ToolTipManager.sharedInstance().setInitialDelay(0);
		// init panels
		_statusPanel = new StatusPanel(_services);
		_replayPanel = new ReplayPanel(_services, _statusPanel);
		_controlPanel = new ControlPanel(_services, this, _replayPanel, Env.INSTANCE.isIs3dMap(), Env.INSTANCE.getMode());

		_split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		_rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		_rightPanel = new JPanel(new BorderLayout());
		_rightPanel.add(_rightSplit, BorderLayout.CENTER);
		_rightPanel.add(_statusPanel, BorderLayout.SOUTH);
		_split.setRightComponent(_rightPanel);
		createRightView();
		createMap(false);
		add(_split, BorderLayout.CENTER);

		// add header
		add(_controlPanel, BorderLayout.NORTH);
		resizeSplit();
	}

	private void resizeSplit() {
		_split.setSize(new Dimension(Env.INSTANCE.getAppWidth(), Env.INSTANCE.getAppHeight()));
		setPreferredSize(new Dimension(Env.INSTANCE.getAppWidth(), Env.INSTANCE.getAppHeight()));
		_split.setDividerLocation(Env.INSTANCE.getSeparator());
		_rightSplit.setDividerLocation(Env.INSTANCE.getRightSeparator());
	}

	/**
	 * Create map component based on the _is3dMap variable
	 */
	private void createRightView() {
		final int loc = _split.getDividerLocation();
		final int rightLoc = _rightSplit.getDividerLocation();
		if (Env.INSTANCE.getMode() == Mode.TRACE_ROUTE) {
			_services.getSniffer().clear();
			if (_packetTablePanel != null) {
				_packetTablePanel.dispose();
			}
			if (_packetDetailsPanel != null) {
				_packetDetailsPanel.dispose();
			}
			if (_whoIsPanel != null) {
				_rightPanel.remove(_whoIsPanel);
				_whoIsPanel.dispose();
				_rightPanel.add(_rightSplit, BorderLayout.CENTER);
			}
			_ganttPanel = new GanttPanel(_services);
			_routeTablePanel = new RouteTablePanel(_services);
			_rightSplit.setTopComponent(_routeTablePanel);
			_rightSplit.setBottomComponent(_ganttPanel);
		} else if (Env.INSTANCE.getMode() == Mode.SNIFFER) {
			_services.getTraceroute().clear();
			if (_ganttPanel != null) {
				_ganttPanel.dispose();
			}
			if (_routeTablePanel != null) {
				_routeTablePanel.dispose();
			}
			if (_whoIsPanel != null) {
				_rightPanel.remove(_whoIsPanel);
				_whoIsPanel.dispose();
				_rightPanel.add(_rightSplit, BorderLayout.CENTER);
			}
			_packetTablePanel = new PacketTablePanel(_services);
			_packetDetailsPanel = new PacketDetailPanel(_services);
			_rightSplit.setTopComponent(_packetTablePanel);
			_rightSplit.setBottomComponent(_packetDetailsPanel);
		} else {
			if (_packetTablePanel != null) {
				_packetTablePanel.dispose();
			}
			if (_packetDetailsPanel != null) {
				_packetDetailsPanel.dispose();
			}
			if (_ganttPanel != null) {
				_ganttPanel.dispose();
			}
			if (_routeTablePanel != null) {
				_routeTablePanel.dispose();
			}
			_whoIsPanel = new WhoIsPanel(_services);
			_rightPanel.remove(_rightSplit);
			_rightPanel.add(_whoIsPanel, BorderLayout.CENTER);
		}
		_statusPanel.reinit();
		_rightSplit.setDividerLocation(rightLoc);
		_split.setDividerLocation(loc);
	}

	/**
	 * Create map component based on the _is3dMap variable
	 */
	private void createMap(final boolean callAfterShow) {
		final int loc = _split.getDividerLocation();
		if (Env.INSTANCE.isIs3dMap()) {
			_3dPanel = new WWJPanel(_services);
			_split.setLeftComponent(_3dPanel);
			if (_2dPanel != null) {
				_2dPanel.dispose();
				_2dPanel = null;
			}
			if (callAfterShow) {
				_3dPanel.afterShow(_controlPanel.getCurrentMode());
			}
		} else {
			_2dPanel = new OpenMapPanel(_services);
			_split.setLeftComponent(_2dPanel);
			if (_3dPanel != null) {
				_3dPanel.dispose();
				_3dPanel = null;
			}
			if (callAfterShow) {
				_2dPanel.afterShow(_controlPanel.getCurrentMode());
			}
		}
		_split.setDividerLocation(loc);
	}

	/**
	 * Set 2d/3d map mode
	 * @param is3d
	 */
	public void set3DMap(final boolean is3d) {
		if (Env.INSTANCE.isIs3dMap() != is3d) {
			Env.INSTANCE.setIs3dMap(is3d);
			createMap(true);
		}
	}

	/**
	 * Set traceroute/sniffer mode
	 * @param mode
	 */
	public void setMode(final Mode mode) {
		if (Env.INSTANCE.getMode() != mode) {
			Env.INSTANCE.setMode(mode);
			createRightView();
			if (Env.INSTANCE.isIs3dMap()) {
				_3dPanel.newRoute(false);
			} else {
				_2dPanel.newRoute(false);
			}
		}
	}

	/**
	 * To call after the ParentFrame is shown
	 */
	public void afterShow() {

		final SwingWorker<Void, Void> initWorker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				// init services
				_controlPanel.updateButtons();
				try {
					_services.updateStartup("init.check.update", true);
					final String[] latestVersion = IOUtils
							.toString(Util.followRedirectOpenConnection(Env.INSTANCE.getVersionUrl())).replace(" ", "")
							.split("\\.");
					final String[] currentVersion = Resources.getVersion().replace(" ", "").split("\\.");
					final int[] latestDigits = new int[latestVersion.length];
					final int[] currentDigits = new int[latestVersion.length];
					for (int i = 0; i < latestVersion.length; i++) {
						latestDigits[i] = Integer.parseInt(latestVersion[i]);
						currentDigits[i] = Integer.parseInt(currentVersion[i]);
					}
					final boolean newVersionAvailable = (latestDigits[0] > currentDigits[0]
							|| (latestDigits[0] == currentDigits[0] && latestDigits[1] > currentDigits[1]) || (latestDigits[0] == currentDigits[0]
									&& latestDigits[1] == currentDigits[1] && latestDigits[2] > currentDigits[2]));
					if (newVersionAvailable) {
						final String content = IOUtils.toString(Util.followRedirectOpenConnection(Env.INSTANCE.getWhatsnewUrl()));
						_controlPanel.setNewVersionAvailable(content);
					}
				} catch (final Exception e) {
					LOGGER.info("Cannot check latest version");
				}
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
				} catch (final Exception e) {
					LOGGER.error("Error while initializing the application", e);
					_services.getSplash().dispose();
					JOptionPane.showMessageDialog(null, Resources.getLabel("error.init", e.getMessage()),
							Resources.getLabel("fatal.error"), JOptionPane.ERROR_MESSAGE);
					System.exit(-1);
				}
				_services.updateStartup("init.completed", true);
				_controlPanel.setEnabled(true);
				if (Env.INSTANCE.isIs3dMap()) {
					_3dPanel.afterShow(_controlPanel.getCurrentMode());
				} else {
					_2dPanel.afterShow(_controlPanel.getCurrentMode());
				}
				_services.getSplash().dispose();
				final Window windowAncestor = SwingUtilities.getWindowAncestor(MainPanel.this);
				windowAncestor.toFront();
				SwingUtilities4.applyFont(windowAncestor, Env.INSTANCE.getFont());
				windowAncestor.toFront();
				getRootPane().setDefaultButton(_controlPanel.getRootButton());
			}

		};
		// wait for the route tracer to be initialized
		initWorker.execute();
	}

	/**
	 * Dispose the components
	 */
	public void dispose() {
		// dispose everybody
		if (_3dPanel != null) {
			_3dPanel.dispose(true);
		}
		if (_2dPanel != null) {
			_2dPanel.dispose();
		}
		if (_routeTablePanel != null) {
			_routeTablePanel.dispose();
		}
		if (_ganttPanel != null) {
			_ganttPanel.dispose();
		}
		if (_packetTablePanel != null) {
			_packetTablePanel.dispose();
		}
		if (_packetDetailsPanel != null) {
			_packetDetailsPanel.dispose();
		}
		if (_whoIsPanel != null) {
			_whoIsPanel.dispose();
		}
		if (_services != null) {
			_services.dispose();
		}
		Env.INSTANCE.saveConfig(_split, _rightSplit);
	}

	public JSplitPane getSplit() {
		return _split;
	}

}
