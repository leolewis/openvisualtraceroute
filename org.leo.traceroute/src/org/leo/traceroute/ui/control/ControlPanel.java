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

import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import org.apache.commons.io.IOUtils;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint.Protocol;
import org.leo.traceroute.core.sniffer.IPacketListener;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.AbstractPanel;
import org.leo.traceroute.ui.MainPanel;
import org.leo.traceroute.ui.route.ReplayPanel;
import org.leo.traceroute.ui.task.CancelMonitor;
import org.leo.traceroute.ui.util.ConfigDialog;
import org.leo.traceroute.ui.util.GlassPane;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.leo.traceroute.ui.util.ToolTip;
import org.leo.traceroute.ui.util.WrapLayout;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ControlPanel $Id: ControlPanel.java 287 2016-10-30 20:35:16Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author Leo Lewis
 */
public class ControlPanel extends AbstractPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlPanel.class);

	/**  */
	private static final long serialVersionUID = 925469690303028997L;

	/** H */
	public static final int H = 32;

	public static final ImageIcon GO_IMG = Resources.getImageIcon("go.png");

	private final MainPanel _mainPanel;
	private Mode _mode;
	private final JPanel _customControls;

	private final JToggleButton _tracerouteButton;
	private final JToggleButton _snifferButton;
	private final JToggleButton _whoisButton;
	private final TraceRouteControl _tracerouteControls;
	private final SnifferControl _snifferControls;
	private final WhoIsControl _whoisControls;
	private final JButton _openConfigDialogButton;
	/** Copy button */
	private final JButton _copyToClipboard;
	/** Export button */
	private final JButton _exportToFile;

	/** 2d/3d */
	private final JToggleButton _switch2D3D;

	/** Search is pending */
	private volatile boolean _running = false;

	private File _saveDirectory;

	/**
	 * Constructor
	 */
	public ControlPanel(final ServiceFactory services, final MainPanel mainPanel, final ReplayPanel replayPanel, final boolean is3d, final Mode mode) {
		super(services);
		_mainPanel = mainPanel;
		setLayout(new WrapLayout(FlowLayout.LEFT, 5, 2));

		_tracerouteButton = new JToggleButton(Resources.getLabel("mode.traceroute"), Resources.getImageIcon("route.png"));
		_tracerouteButton.setToolTipText(Resources.getLabel("mode.traceroute.description"));
		_tracerouteButton.setSelected(mode == Mode.TRACE_ROUTE);
		_snifferButton = new JToggleButton(Resources.getLabel("mode.sniffer"), Resources.getImageIcon("network.png"));
		_snifferButton.setToolTipText(Resources.getLabel("mode.sniffer.description"));
		_snifferButton.setSelected(mode == Mode.SNIFFER);
		_whoisButton = new JToggleButton(Resources.getLabel("mode.whois"), Resources.getImageIcon("identity.png"));
		_whoisButton.setToolTipText(Resources.getLabel("mode.whois.description"));
		_whoisButton.setSelected(mode == Mode.WHOIS);
		_tracerouteButton.addActionListener(e -> setMode(Mode.TRACE_ROUTE, true));
		_snifferButton.addActionListener(e -> setMode(Mode.SNIFFER, true));
		_whoisButton.addActionListener(e -> setMode(Mode.WHOIS, true));
		final ButtonGroup group = new ButtonGroup();
		group.add(_tracerouteButton);
		group.add(_snifferButton);
		group.add(_whoisButton);

		_tracerouteControls = new TraceRouteControl(replayPanel);
		_snifferControls = new SnifferControl();
		_whoisControls = new WhoIsControl();

		_switch2D3D = new JToggleButton(Resources.getLabel("2d.3d"), Resources.getImageIcon("cube.png"), is3d);
		_switch2D3D.setToolTipText(Resources.getLabel("switch.2d.tooltip"));
		_switch2D3D.addActionListener(e -> _mainPanel.set3DMap(_switch2D3D.isSelected()));
		_customControls = new JPanel();
		_customControls.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
		add(_tracerouteButton);
		add(_snifferButton);
		add(_whoisButton);

		// if no opengl available, don't allow to change the map
		if (Env.INSTANCE.isOpenGLAvailable()) {
			add(_switch2D3D);
		}

		final JPanel otherControls = new JPanel();
		otherControls.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		final JButton screenshot = new JButton(Resources.getImageIcon("png.png"));
		screenshot.setToolTipText(Resources.getLabel("screenshot"));
		screenshot.addActionListener(e -> {
			final JFileChooser chooser = new JFileChooser();
			if (_saveDirectory != null) {
				chooser.setCurrentDirectory(_saveDirectory);
			}
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "PNG file";
				}

				@Override
				public boolean accept(final File f) {
					if (f.isDirectory()) {
						return true;
					}
					return f.getName().toLowerCase().endsWith(".png");
				}
			});
			final Window parent = SwingUtilities.getWindowAncestor(ControlPanel.this);
			try {
				final SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
					@Override
					protected BufferedImage doInBackground() throws Exception {
						return new Robot().createScreenCapture(new Rectangle(parent.getBounds()));
					}

					@Override
					protected void done() {
						try {
							final BufferedImage image = get();
							final int ret = chooser.showSaveDialog(parent);
							if (ret == JFileChooser.APPROVE_OPTION) {
								File file = chooser.getSelectedFile();
								if (!file.getName().toLowerCase().endsWith(".png")) {
									file = new File(file.getAbsolutePath() + ".png");
								}
								_saveDirectory = file.getParentFile();
								ImageIO.write(image, "png", file);
								final File f = file;
								SwingUtilities.invokeLater(() -> {
									if (!Util.open(f)) {
										JOptionPane.showMessageDialog(parent, Resources.getLabel("screenshot.success", f.getAbsolutePath()), "",
												JOptionPane.INFORMATION_MESSAGE);
									}
								});
							}
						} catch (final Exception ex) {
							JOptionPane.showMessageDialog(parent, Resources.getLabel("screenshot.failed"), Resources.getLabel("error"), JOptionPane.ERROR_MESSAGE);
							LOGGER.error(ex.getMessage(), e);
						}
					}
				};
				worker.execute();

			} catch (final Exception ex) {
				JOptionPane.showMessageDialog(parent, Resources.getLabel("screenshot.failed"), Resources.getLabel("error"), JOptionPane.ERROR_MESSAGE);
				LOGGER.error(ex.getMessage(), e);
			}
		});
		otherControls.add(screenshot);
		_copyToClipboard = new JButton(Resources.getImageIcon("clipboard.png"));
		_copyToClipboard.setToolTipText(Resources.getLabel("copy.clipboard.button"));
		_copyToClipboard.addActionListener(e -> {
			final String csv;
			if (_mode == Mode.TRACE_ROUTE) {
				csv = _route.toText();
			} else if (_mode == Mode.SNIFFER) {
				csv = _sniffer.toText();
			} else {
				csv = _whois.toText();
			}
			final StringSelection data = new StringSelection(csv);
			final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(data, data);
			JOptionPane.showMessageDialog(null, Resources.getLabel("copied.clipboard.message"), Resources.getLabel("copied.clipboard.message"),
					JOptionPane.INFORMATION_MESSAGE);
		});
		otherControls.add(_copyToClipboard);
		_exportToFile = new JButton(Resources.getImageIcon("export.png"));
		_exportToFile.setToolTipText(Resources.getLabel("export.button"));
		_exportToFile.addActionListener(e -> {
			final String ext = _mode != Mode.WHOIS ? ".csv" : ".txt";
			final JFileChooser chooser = new JFileChooser();
			if (_saveDirectory != null) {
				chooser.setCurrentDirectory(_saveDirectory);
			}
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "CSV file";
				}

				@Override
				public boolean accept(final File f) {
					if (f.isDirectory()) {
						return true;
					}
					return f.getName().toLowerCase().endsWith(ext);
				}
			});
			final Window parent = SwingUtilities.getWindowAncestor(ControlPanel.this);
			try {
				final int ret = chooser.showSaveDialog(parent);
				if (ret == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					if (!file.getName().toLowerCase().endsWith(ext)) {
						file = new File(file.getAbsolutePath() + ext);
					}
					final String csv;
					if (_mode == Mode.TRACE_ROUTE) {
						csv = _route.toCSV();
					} else if (_mode == Mode.SNIFFER) {
						csv = _sniffer.toCSV();
					} else {
						csv = _whois.toText();
					}
					final File f = file;
					FileOutputStream stream = null;
					try {
						stream = new FileOutputStream(f);
						IOUtils.write(csv, stream);
					} finally {
						if (stream != null) {
							stream.flush();
							IOUtils.closeQuietly(stream);
						}
					}
					_saveDirectory = f.getParentFile();
					SwingUtilities.invokeLater(() -> {
						if (!Util.open(f)) {
							JOptionPane.showMessageDialog(parent, Resources.getLabel("export.success", f.getAbsolutePath()), "", JOptionPane.INFORMATION_MESSAGE);
						}
					});
				}
			} catch (final Exception ex) {
				JOptionPane.showMessageDialog(parent, Resources.getLabel("export.failed", ex.getMessage()), Resources.getLabel("error"), JOptionPane.ERROR_MESSAGE);
				LOGGER.error(ex.getMessage(), e);
			}
		});
		otherControls.add(_exportToFile);
		_openConfigDialogButton = new JButton(Resources.getImageIcon("settings.png"));
		_openConfigDialogButton.setToolTipText(Resources.getLabel("settings.tooltip"));
		_openConfigDialogButton.addActionListener(e -> new ConfigDialog(SwingUtilities.getWindowAncestor(ControlPanel.this), services).setVisible(true));
		otherControls.add(_openConfigDialogButton);

		otherControls.add(ToolTip.INSTANCE.buildHelpButton(this));

		add(otherControls);
		add(_customControls);

		setMode(mode, false);
		setEnabled(false);

	}

	/**
	 * Update buttons (depending on what mode is available)
	 */
	public void updateButtons() {
		if (!getServices().isSnifferAvailable()) {
			_snifferButton.setVisible(false);
		}
		if (!Env.INSTANCE.isOpenGLAvailable()) {
			_switch2D3D.setVisible(false);
		}
	}

	/**
	 * Notify the panel than a new version of the app is available
	 * @param content
	 */
	public void setNewVersionAvailable(final String content) {
		final JButton showUpdateDialog = new JButton(Resources.getImageIcon("update.png"));
		showUpdateDialog.setToolTipText(Resources.getLabel("new.version.title"));
		showUpdateDialog.addActionListener(e -> showUpdateDialog(content));
		add(showUpdateDialog);
		invalidate();
		revalidate();
	}

	private void showUpdateDialog(final String content) {
		final int res = JOptionPane.showConfirmDialog(_mainPanel, Resources.getLabel("new.version") + "\n\n" + content, Resources.getLabel("new.version.title"),
				JOptionPane.YES_NO_OPTION);
		if (res == JOptionPane.OK_OPTION) {
			boolean redirectOK = false;
			final String url = Env.INSTANCE.getDownloadUrl();
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URL(url).toURI());
					redirectOK = true;
				} catch (final Exception e) {

				}
			}
			// if redirect failed for some reason, just show the url in a dialog
			if (!redirectOK) {
				JOptionPane.showMessageDialog(_mainPanel, Resources.getLabel("go.to.url") + url);
			}
		}
	}

	private void setMode(final Mode mode, final boolean notifyMainPanel) {
		if (_mode == mode) {
			return;
		}
		_mode = mode;
		if (mode == Mode.TRACE_ROUTE) {
			if (_snifferControls != null) {
				_customControls.remove(_snifferControls);
				_sniffer.endCapture();
			}
			if (_whoisControls != null) {
				_customControls.remove(_whoisControls);
			}
			_customControls.add(_tracerouteControls);
		} else if (mode == Mode.SNIFFER) {
			if (_tracerouteControls != null) {
				_customControls.remove(_tracerouteControls);
			}
			if (_whoisControls != null) {
				_customControls.remove(_whoisControls);
			}
			_customControls.add(_snifferControls);
		} else {
			if (_tracerouteControls != null) {
				_customControls.remove(_tracerouteControls);
			}
			if (_snifferControls != null) {
				_customControls.remove(_snifferControls);
			}
			_customControls.add(_whoisControls);
		}

		if (notifyMainPanel) {
			_mainPanel.setMode(mode);
		}
		SwingUtilities4.applyFont(_mainPanel, Env.INSTANCE.getFont());
		_customControls.invalidate();
		_customControls.revalidate();
	}

	@Override
	public void newRoute(final boolean dnsLookup) {
		setEnabled(false);
		_running = true;
	}

	@Override
	public void routePointAdded(final RoutePoint point) {

	}

	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
		taskEnded();
	}

	@Override
	public void error(final Exception exception, final Object origin) {
		String message;
		taskEnded();
		if (origin == _whois) {
			return;
		}
		if (getCurrentMode() == Mode.TRACE_ROUTE) {
			message = Resources.getLabel("error.traceroute");
		} else if (getCurrentMode() == Mode.SNIFFER) {
			message = Resources.getLabel("error.sniffer");
		} else {
			return;
		}
		// show the error on the glass pane
		if (exception instanceof UnknownHostException) {
			message += Resources.getLabel("unknown.host");
		} else {
			message += Util.formatException(exception);
		}
		LOGGER.error("Error", exception);
		GlassPane.displayMessage(this, message, Resources.getImageIcon("error.png"));
	}

	@Override
	public void routeCancelled() {
		taskEnded();
	}

	@Override
	public void routeTimeout() {
		taskEnded();
	}

	@Override
	public void maxHops() {
		taskEnded();
	}

	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {

	}

	/**
	 * Return the value of the field searchRoute
	 *
	 * @return the value of searchRoute
	 */
	public JButton getRootButton() {
		if (_mode == Mode.TRACE_ROUTE) {
			return _tracerouteControls._traceRouteButton;
		} else if (_mode == Mode.WHOIS) {
			return _whoisControls._whoIsButton;
		}
		return null;
	}

	@Override
	public void captureStopped() {
		taskEnded();
	}

	/**
	 * Task ended
	 */
	private void taskEnded() {
		setEnabled(true);
		_running = false;
	}

	public Mode getCurrentMode() {
		return _tracerouteButton.isSelected() ? Mode.TRACE_ROUTE : (_snifferButton.isSelected() ? Mode.SNIFFER : Mode.WHOIS);
	}

	@Override
	public void startCapture() {
		setEnabled(false);
		_running = true;
	}

	@Override
	public void setEnabled(final boolean enable) {
		_snifferButton.setEnabled(enable);
		_tracerouteButton.setEnabled(enable);
		_whoisButton.setEnabled(enable);
		_switch2D3D.setEnabled(enable);
		_copyToClipboard.setEnabled(enable);
		_openConfigDialogButton.setEnabled(enable);
		_exportToFile.setEnabled(enable);
		if (_snifferControls != null) {
			_snifferControls.setEnabled(enable);
		}
		if (_tracerouteControls != null) {
			_tracerouteControls.setEnabled(enable);
		}
		if (_whoisControls != null) {
			_whoisControls.setEnabled(enable);
		}
	}

	@Override
	public void packetAdded(final AbstractPacketPoint point) {

	}

	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {

	}

	@Override
	public void startWhoIs(final String host) {
		if (getCurrentMode() == Mode.WHOIS) {
			setEnabled(false);
			_running = true;
		}
	}

	@Override
	public void focusWhoIs(final GeoPoint point) {
	}

	@Override
	public void whoIsResult(final String result) {
		if (getCurrentMode() == Mode.WHOIS) {
			taskEnded();
		}
	}

	public class TraceRouteControl extends JPanel {

		/**  */
		private static final long serialVersionUID = -7032963987378895780L;

		/** Search button */
		private final JButton _traceRouteButton;

		/** Search textfield */
		private final JTextField _hostIpTextField;

		/** Resolve HostName */
		private final JToggleButton _resolveHostname;
		/** Ip V4 o V6 */
		private final JToggleButton _ipV4;

		/** Timeout */
		private final JSpinner _timeOut;

		private final CancelMonitor _monitor = new CancelMonitor();;

		private AutoCompleteComponent _autocomplete;

		private long _ts;

		@SuppressWarnings("serial")
		public TraceRouteControl(final ReplayPanel replayPanel) {
			super();
			setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
			// top panel, start/cancel button and JTextField
			_traceRouteButton = new JButton(GO_IMG);
			_traceRouteButton.setToolTipText(Resources.getLabel("search.button"));
			_hostIpTextField = new JTextField(22);
			_hostIpTextField.setText(Resources.getLabel("url.tooltip"));
			_hostIpTextField.setCaretPosition(0);

			final FirstInputListener listener = new FirstInputListener(_hostIpTextField);
			_hostIpTextField.addMouseListener(listener);
			_hostIpTextField.addKeyListener(listener);
			_hostIpTextField.setToolTipText(Resources.getLabel("url.tooltip"));
			_resolveHostname = new JToggleButton(Resources.getImageIcon("host.png"), true);
			_resolveHostname.setToolTipText(Resources.getLabel("resolve.hostname.tooltip"));

			_ipV4 = new JToggleButton("ip.v4", Resources.getImageIcon("host.png"), true);
			_ipV4.setToolTipText(Resources.getLabel("resolve.hostname.tooltip"));
			_ipV4.addActionListener(e -> _ipV4.setText(_ipV4.isSelected() ? "ip.v4" : "ip.v6"));
			_ipV4.setSelected(true);
			_timeOut = new JSpinner();
			_timeOut.setToolTipText(Resources.getLabel("timeout.tooltip"));
			final SpinnerModel model = new SpinnerNumberModel(0, 0, 120, 1) {
				@Override
				public Object getPreviousValue() {
					final Number n = (Number) getValue();
					return n.intValue() - getStepSize().intValue();
				}

				@Override
				public Object getNextValue() {
					final Number n = (Number) getValue();
					return n.intValue() + getStepSize().intValue();
				}
			};
			_timeOut.setModel(model);

			final JLabel timeOutLabel = new JLabel(Resources.getLabel("timeout.label"));
			timeOutLabel.setToolTipText(Resources.getLabel("timeout.tooltip"));
			setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			add(_hostIpTextField);
			add(_resolveHostname);
			add(timeOutLabel);
			add(_timeOut);
			add(replayPanel.getReplayButton());
			add(_traceRouteButton);
			// search button enable if text is not blank
			_hostIpTextField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent e) {
					_traceRouteButton.setEnabled(!_hostIpTextField.getText().equals(""));
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						_traceRouteButton.getActionListeners()[0].actionPerformed(new ActionEvent(this, 0, "traceRoute"));
					}
				}
			});
			// action of search/cancel trace route
			_traceRouteButton.addActionListener(arg0 -> traceroute());

			_traceRouteButton.setEnabled(false);
			_autocomplete = new AutoCompleteComponent(_hostIpTextField, _services.getAutocomplete());
			final JFormattedTextField editor = ((JSpinner.DefaultEditor) _timeOut.getEditor()).getTextField();
			editor.setFormatterFactory(new NumberFormatterFactory());
		}

		/**
		 * Execute traceroute
		 */
		public void traceroute() {
			final long ts = System.currentTimeMillis();
			if (ts < _ts + 500) {
				return;
			}
			_ts = ts;
			if (!_running) {
				try {
					_timeOut.commitEdit();
				} catch (final ParseException e) {

				}
				_monitor.setCanceled(false);
				_running = true;
				_route.compute(_hostIpTextField.getText(), _monitor, _resolveHostname.isSelected(), 1000 * Integer.parseInt(_timeOut.getValue().toString()), _ipV4.isSelected(), Env.INSTANCE.getTrMaxHop());
			} else {
				_monitor.setCanceled(true);
				_traceRouteButton.setEnabled(false);
				_running = false;
			}
		}

		@Override
		public void setEnabled(final boolean enable) {
			_hostIpTextField.setEnabled(enable);
			_resolveHostname.setEnabled(enable);
			_autocomplete.setEnabled(enable);
			_timeOut.setEnabled(enable);
			if (enable) {
				_traceRouteButton.setToolTipText(Resources.getLabel("search.button"));
				_traceRouteButton.setIcon(GO_IMG);
			} else {
				_traceRouteButton.setToolTipText(Resources.getLabel("cancel.button"));
				_traceRouteButton.setIcon(Resources.getImageIcon("over.png"));
			}
			if (enable) {
				_traceRouteButton.setEnabled(true);
			}
		}
	}

	public class SnifferControl extends JPanel {

		/**  */
		private static final long serialVersionUID = -703263987378895780L;

		/** Search button */
		private final JButton _captureButton;

		private final JTextField _portTF;
		private final JCheckBox _allPortCheck;
		private final JTextField _filterLengthTF;
		private final JTextField _capturePeriod;
		private final JCheckBox _filterPacketLengthCheck;
		/** Search textfield */
		private final JTextField _hostIpTextField;

		private final Map<Protocol, JCheckBox> _packets = new HashMap<>();

		private long _ts;

		public SnifferControl() {
			super();
			setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
			_hostIpTextField = new JTextField(17);
			_hostIpTextField.setText(Resources.getLabel("sniffer.host.tooltip"));
			final FirstInputListener listener = new FirstInputListener(_hostIpTextField);
			_hostIpTextField.addMouseListener(listener);
			_hostIpTextField.addKeyListener(listener);
			_hostIpTextField.setToolTipText(Resources.getLabel("sniffer.host.tooltip"));
			add(_hostIpTextField);

			final JLabel protocolLabel = new JLabel(Resources.getLabel("protocol.label"));
			protocolLabel.setToolTipText(Resources.getLabel("protocol.desc"));
			add(protocolLabel);
			for (final Protocol type : Protocol.values()) {
				if (type == Protocol.OTHER) {
					continue;
				}
				final JCheckBox check = new JCheckBox(type.name(), type == Protocol.TCP);
				_packets.put(type, check);
				add(check);
			}
			final JLabel portLabel = new JLabel(Resources.getLabel("port.label"));
			portLabel.setToolTipText(Resources.getLabel("port.desc"));
			_allPortCheck = new JCheckBox(Resources.getLabel("all.port.label"));
			_allPortCheck.setToolTipText(Resources.getLabel("all.port.desc"));
			_allPortCheck.setSelected(false);
			add(_allPortCheck);

			_portTF = new JFormattedTextField();
			_portTF.setText("80,443");
			_portTF.setColumns(15);
			//			_portTF.setMaximumSize(new Dimension(30, _portTF.getPreferredSize().height));
			add(portLabel);
			add(_portTF);
			_portTF.setEnabled(true);

			_allPortCheck.addChangeListener(e -> _portTF.setEnabled(!_allPortCheck.isSelected()));

			_filterPacketLengthCheck = new JCheckBox(Resources.getLabel("filter.length"));
			_filterPacketLengthCheck.setToolTipText(Resources.getLabel("filter.length.desc"));
			_filterPacketLengthCheck.setSelected(false);
			add(_filterPacketLengthCheck);

			_filterLengthTF = new JFormattedTextField(new NumberFormatterFactory());
			_filterLengthTF.setText("128");
			_filterLengthTF.setColumns(5);
			add(_filterLengthTF);

			_filterPacketLengthCheck.addChangeListener(e -> _filterLengthTF.setEnabled(_filterPacketLengthCheck.isEnabled() && _filterPacketLengthCheck.isSelected()));
			_capturePeriod = new JFormattedTextField(new NumberFormatterFactory());
			_capturePeriod.setText("0");
			_capturePeriod.setColumns(5);
			add(new JLabel(Resources.getLabel("capture.period")));
			add(_capturePeriod);

			_captureButton = new JButton(GO_IMG);
			_captureButton.setToolTipText(Resources.getLabel("capture.packet.start"));
			add(_captureButton);
			_captureButton.addActionListener(arg0 -> start());
		}

		private void start() {
			final long ts = System.currentTimeMillis();
			if (ts < _ts + 500) {
				return;
			}
			_ts = ts;
			if (!_running) {
				final Set<Protocol> pt = new HashSet<>();
				for (final Entry<Protocol, JCheckBox> entry : _packets.entrySet()) {
					if (entry.getValue().isSelected()) {
						pt.add(entry.getKey());
					}
				}
				String port = null;
				if (!_allPortCheck.isSelected()) {
					port = _portTF.getText();
				}
				if (pt.isEmpty()) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(ControlPanel.this), Resources.getLabel("protocol.ports.empty"),
							Resources.getLabel("protocol.ports.empty"), JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				_running = true;
				String host = _hostIpTextField.getText();
				if (host.contains(Resources.getLabel("sniffer.host.tooltip"))) {
					host = null;
				}
				_sniffer.startCapture(pt, port, _filterPacketLengthCheck.isSelected(), Integer.parseInt(_filterLengthTF.getText().replace(",", "")), host,
						Integer.parseInt(_capturePeriod.getText()));
			} else {
				_sniffer.endCapture();
				_running = false;
			}
		}

		@Override
		public void setEnabled(final boolean enable) {
			for (final JCheckBox check : _packets.values()) {
				check.setEnabled(enable);
			}
			_hostIpTextField.setEnabled(enable);
			_allPortCheck.setEnabled(enable);
			_capturePeriod.setEnabled(enable);
			_filterLengthTF.setEnabled(enable && _filterPacketLengthCheck.isSelected());
			_filterPacketLengthCheck.setEnabled(enable);
			_portTF.setEnabled(enable && !_allPortCheck.isSelected());
			if (enable) {
				_captureButton.setToolTipText(Resources.getLabel("capture.packet.start"));
				_captureButton.setIcon(GO_IMG);
			} else {
				_captureButton.setToolTipText(Resources.getLabel("cancel.button"));
				_captureButton.setIcon(Resources.getImageIcon("over.png"));
			}
		}
	}

	public class WhoIsControl extends JPanel {

		/**  */
		private static final long serialVersionUID = -703296398737889570L;

		/** Search button */
		private final JButton _whoIsButton;

		/** Search textfield */
		private final JTextField _hostIpTextField;

		private final AutoCompleteComponent _autocomplete;

		public WhoIsControl() {
			super();
			setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
			// top panel, start/cancel button and JTextField
			_whoIsButton = new JButton(GO_IMG);
			_whoIsButton.setToolTipText(Resources.getLabel("whois.button"));
			_hostIpTextField = new JTextField(17);
			_hostIpTextField.setText(Resources.getLabel("enter.whois"));

			final FirstInputListener listener = new FirstInputListener(_hostIpTextField);
			_hostIpTextField.addMouseListener(listener);
			_hostIpTextField.addKeyListener(listener);
			_hostIpTextField.setToolTipText(Resources.getLabel("enter.whois"));

			add(_hostIpTextField);
			add(_whoIsButton);
			// search button enable if text is not blank
			_hostIpTextField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent e) {
					_whoIsButton.setEnabled(!_hostIpTextField.getText().equals(""));
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						_whoIsButton.getActionListeners()[0].actionPerformed(new ActionEvent(this, 0, "whois"));
					}
				}
			});
			// action of search/cancel trace route
			_whoIsButton.addActionListener(arg0 -> whois());

			_whoIsButton.setEnabled(false);
			_autocomplete = new AutoCompleteComponent(_hostIpTextField, _services.getAutocomplete());
		}

		/**
		 * Execute whois
		 */
		public void whois() {
			if (!_running) {
				_running = true;
				_whois.whoIs(_hostIpTextField.getText());
			} else {
				_running = false;
			}
		}

		@Override
		public void setEnabled(final boolean enable) {
			_hostIpTextField.setEnabled(enable);
			_autocomplete.setEnabled(enable);
			if (enable) {
				_whoIsButton.setToolTipText(Resources.getLabel("whois.button"));
				_whoIsButton.setIcon(GO_IMG);
			} else {
				_whoIsButton.setToolTipText(Resources.getLabel("cancel.button"));
				_whoIsButton.setIcon(Resources.getImageIcon("over.png"));
			}
		}

		/**
		 * Return the value of the field hostIpTextField
		 * @return the value of hostIpTextField
		 */
		public JTextField getHostIpTextField() {
			return _hostIpTextField;
		}
	}

	private static class FirstInputListener extends KeyAdapter implements MouseListener {

		private boolean _first = true;
		private final JTextField _textField;

		public FirstInputListener(final JTextField textField) {
			_textField = textField;
		}

		@Override
		public void keyPressed(final KeyEvent e) {
			clear();
		}

		@Override
		public void mouseClicked(final MouseEvent e) {
			clear();
		}

		@Override
		public void mousePressed(final MouseEvent e) {
			clear();
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
		}

		@Override
		public void mouseEntered(final MouseEvent e) {
		}

		@Override
		public void mouseExited(final MouseEvent e) {
		}

		private void clear() {
			if (_first) {
				_textField.setText("");
				_first = false;
			}
		}
	}

	@SuppressWarnings("serial")
	public static class NumberFormatterFactory extends DefaultFormatterFactory {

		public NumberFormatterFactory() {
			super();
			final NumberFormat format = NumberFormat.getIntegerInstance();
			format.setGroupingUsed(false);
			final NumberFormatter nf = new NumberFormatter(format);
			setDefaultFormatter(nf);
			setDisplayFormatter(nf);
			setEditFormatter(nf);
		}
	}
}
