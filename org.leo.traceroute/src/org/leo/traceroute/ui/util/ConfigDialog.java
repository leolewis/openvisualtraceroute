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
package org.leo.traceroute.ui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

import org.apache.commons.lang3.StringUtils;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.Language;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.LicenseDialog;
import org.leo.traceroute.ui.control.ControlPanel;
import org.leo.traceroute.ui.control.NetworkInterfaceChooser;
import org.leo.traceroute.ui.geo.LocRecordsView;

import say.swing.JFontChooser;

/**
 * ConfigDialog $Id$
 *
 * <pre>
 * Dialog used to configure application properties
 * </pre>
 *
 * @author Leo Lewis
 */
public class ConfigDialog extends JDialog {

	/**  */
	private static final long serialVersionUID = -8746207460987280371L;

	private static final int HGAP = 10;
	private static final int VGAP = 5;
	private static final int NETWORK_COMBO_W = 300;

	private static final String[] FONT_SIZE = { "8", "9", "10", "11", "12", "14", "16", "18", "20" };

	/**
	 * Constructor
	 *
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ConfigDialog(final Window owner, final ServiceFactory services) {
		super(owner, Resources.getLabel("config.label"), ModalityType.DOCUMENT_MODAL);
		final JPanel main = new JPanel();
		final JScrollPane scrollPane = new JScrollPane(main);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

		// license
		final JPanel licensePanel = new JPanel();
		licensePanel.setLayout(new FlowLayout(FlowLayout.CENTER, HGAP, VGAP + 5));
		licensePanel.add(new JLabel(Resources.getLabel("appli.title", Resources.getVersion())));
		licensePanel.add(LicenseDialog.createHeaderPanel(true, owner));
		final JButton logs = new JButton(Resources.getLabel("show.log"), Resources.getImageIcon("log.png"));
		logs.addActionListener(e -> {
			final LogWindow window = new LogWindow(ConfigDialog.this);
			window.setVisible(true);
		});
		licensePanel.add(logs);

		main.add(licensePanel);

		// general panel
		final JPanel generalPanel = new JPanel();
		generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));

		final JPanel linkPanel = new JPanel();
		linkPanel.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		linkPanel.add(new HyperlinkLabel(Resources.getLabel("website"), Env.INSTANCE.getWebsitetUrl()));
		linkPanel.add(new HyperlinkLabel(Resources.getLabel("support"), Env.INSTANCE.getSupportUrl()));
		linkPanel.add(new HyperlinkLabel(Resources.getLabel("facebook"), Env.INSTANCE.getFacebookUrl()));
		linkPanel.add(new HyperlinkLabel(Resources.getLabel("donate.label"), Env.INSTANCE.getDonateUrl()));

		generalPanel.add(linkPanel);

		final JPanel languagePanel = new JPanel();
		languagePanel.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		languagePanel.add(new JLabel(Resources.getLabel("appli.language.label")));
		// add the combo of selection of the language of the application
		ButtonGroup languageGroup = new ButtonGroup();
		for (final Language language : Language.values()) {
			if (language.isEnabled()) {
				JRadioButton button = new JRadioButton(language.getLanguageName());
				button.setActionCommand(language.name());
				if (language == Env.INSTANCE.getAppliLanguage()) {
					button.setSelected(true);
				}
				languageGroup.add(button);
				languagePanel.add(button);
			}
		}
		generalPanel.add(languagePanel);

		final JPanel theme = new JPanel();
		theme.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		theme.add(new JLabel(Resources.getLabel("dark.theme")));
		final JToggleButton darkTheme = new JToggleButton(Resources.getImageIcon("theme.png"), Env.INSTANCE.isDarkTheme());
		darkTheme.setPreferredSize(new Dimension(100, ControlPanel.H));
		darkTheme.setToolTipText(Resources.getLabel("dark.theme"));
		darkTheme.setText((Resources.getLabel(darkTheme.isSelected() ? "yes" : "no")));
		darkTheme.addActionListener(e -> darkTheme.setText((Resources.getLabel(darkTheme.isSelected() ? "yes" : "no"))));
		theme.add(darkTheme);
		generalPanel.add(theme);

		final JPanel splash = new JPanel();
		splash.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		splash.add(new JLabel(Resources.getLabel("show.splashscreen")));
		final JToggleButton showSplash = new JToggleButton(Resources.getImageIcon("splash.png"), !Env.INSTANCE.isHideSplashScreen());
		showSplash.setPreferredSize(new Dimension(100, ControlPanel.H));
		showSplash.setToolTipText(Resources.getLabel("show.splashscreen"));
		showSplash.setText((Resources.getLabel(showSplash.isSelected() ? "yes" : "no")));
		showSplash.addActionListener(e -> showSplash.setText((Resources.getLabel(showSplash.isSelected() ? "yes" : "no"))));
		splash.add(showSplash);
//		generalPanel.add(splash);

		final JPanel font = new JPanel();
		font.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		font.add(new JLabel(Resources.getLabel("ui.font")));
		final AtomicReference<Font> currentFont = new AtomicReference<>(Env.INSTANCE.getFont());
		final JButton changeFont = new JButton(currentFont.get().getFamily() + " (" + currentFont.get().getSize() + ") "
				+ (currentFont.get().isBold() ? "BOLD" : currentFont.get().isItalic() ? "ITALIC" : "PLAIN"));
		changeFont.setToolTipText(Resources.getLabel("ui.changefont"));
		changeFont.addActionListener(e -> {
			@SuppressWarnings("serial")
			final JFontChooser fontChooser = new JFontChooser(FONT_SIZE) {
				@Override
				protected String[] getFontFamilies() {
					final String[] names = super.getFontFamilies();
					final List<String> filter = new ArrayList<>();
					for (final String n : names) {
						final Font f = new Font(n, Font.PLAIN, 10);
						final String japanese = Resources.getLabel("language.japanese");
						final String french = Resources.getLabel("language.french");
						if (f.canDisplayUpTo(japanese) == -1 && f.canDisplayUpTo(french) == -1) {
							filter.add(n);
						}
					}
					return filter.toArray(new String[filter.size()]);
				}
			};
			fontChooser.setSelectedFont(currentFont.get());
			final int result = fontChooser.showDialog(ConfigDialog.this);
			if (result == JFontChooser.OK_OPTION) {
				final Font newF = fontChooser.getSelectedFont();
				changeFont.setText(newF.getFamily() + " (" + Math.max(8, Math.min(20, newF.getSize())) + ") "
						+ (newF.isPlain() ? "PLAIN" : newF.isBold() ? "BOLD" : newF.isItalic() ? "ITALIC" : "ITALIC BOLD"));
				currentFont.set(newF);
				SwingUtilities4.applyFont(ConfigDialog.this, newF);
				SwingUtilities4.packAndCenter(ConfigDialog.this);
			}
		});
		font.add(changeFont);
		generalPanel.add(font);

		final JPanel geoip = new JPanel();
		geoip.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		geoip.add(new JLabel(Resources.getLabel("update.geoip")));
		final JButton updateGeoIp = new JButton(Resources.getImageIcon("update2.png"));
		updateGeoIp.setPreferredSize(new Dimension(60, ControlPanel.H));
		updateGeoIp.setToolTipText(Resources.getLabel("update.geoip"));
		updateGeoIp.addActionListener(e -> {
			services.getGeo().deleteGeoIpDbOnExit();
			JOptionPane.showMessageDialog(ConfigDialog.this, Resources.getLabel("update.geoip.completed"));
		});
		geoip.add(updateGeoIp);
		generalPanel.add(geoip);

		final JPanel dnsloc = new JPanel();
		dnsloc.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		dnsloc.add(new JLabel(Resources.getLabel("see.dnsloc")));
		final JButton openDns = new JButton(Resources.getImageIcon("map.png"));
		openDns.setPreferredSize(new Dimension(60, ControlPanel.H));
		openDns.setToolTipText(Resources.getLabel("see.dnsloc"));
		openDns.addActionListener(e -> new LocRecordsView(owner, services));
		dnsloc.add(openDns);
		generalPanel.add(dnsloc);

		generalPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), Resources.getLabel("settings.general")));
		main.add(generalPanel);

		// traceroute settings
		final JPanel trPanel = new JPanel();
		trPanel.setLayout(new BoxLayout(trPanel, BoxLayout.Y_AXIS));
		final NetworkInterfaceChooser trInterfaceChooser = new NetworkInterfaceChooser(services, Mode.TRACE_ROUTE);
		if (services.isEmbeddedTRAvailable()) {
			final JPanel trPanel1 = new JPanel();
			trPanel1.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
			trPanel1.add(new JLabel(Resources.getLabel("device.select.label")));
			trInterfaceChooser.setPreferredSize(new Dimension(NETWORK_COMBO_W, ControlPanel.H));
			trPanel1.add(trInterfaceChooser);
			trPanel.add(trPanel1);
		}
		final JPanel trPanel2 = new JPanel();
		trPanel2.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		trPanel2.add(new JLabel(Resources.getLabel("use.os.traceroute")));
		final JToggleButton useOSTraceroute = new JToggleButton(Resources.getImageIcon("terminal.png"), true);
		useOSTraceroute.setPreferredSize(new Dimension(100, ControlPanel.H));
		useOSTraceroute.setToolTipText(Resources.getLabel("use.os.traceroute.tooltip"));
		useOSTraceroute.setSelected(false);
		useOSTraceroute.addActionListener(e -> {
			trInterfaceChooser.setEnabled(!useOSTraceroute.isSelected());
			useOSTraceroute.setText((Resources.getLabel(useOSTraceroute.isSelected() ? "yes" : "no")));
		});
		if (!services.isEmbeddedTRAvailable()) {
			useOSTraceroute.setSelected(true);
			Env.INSTANCE.setUseOSTraceroute(true);
			useOSTraceroute.setEnabled(false);
		} else {
			useOSTraceroute.setSelected(Env.INSTANCE.isUseOSTraceroute());
			if (Env.INSTANCE.isUseOSTraceroute()) {
				trInterfaceChooser.setEnabled(false);
			}
		}
		useOSTraceroute.setText((Resources.getLabel(Env.INSTANCE.isUseOSTraceroute() ? "yes" : "no")));
		trPanel2.add(useOSTraceroute);
		trPanel.add(trPanel2);

		final JPanel trPanel3 = new JPanel();
		trPanel3.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		final JSpinner maxHops = new JSpinner();
		@SuppressWarnings("serial")
		final SpinnerModel model = new SpinnerNumberModel(Env.INSTANCE.getTrMaxHop(), 2, 1000, 1) {
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
		maxHops.setModel(model);
		final JLabel maxHopLabel = new JLabel(Resources.getLabel("tr.max.hop"));
		maxHopLabel.setToolTipText(Resources.getLabel("tr.max.hop"));
		maxHops.setPreferredSize(new Dimension(100, ControlPanel.H));
		trPanel3.add(maxHopLabel);
		trPanel3.add(maxHops);
		trPanel.add(trPanel3);

		final JPanel trPanel4 = new JPanel();
		trPanel4.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		trPanel4.add(new JLabel(Resources.getLabel("history.clear")));
		final JButton clearHistoryTraceroute = new JButton(Resources.getImageIcon("clear.png"));
		clearHistoryTraceroute.setPreferredSize(new Dimension(60, ControlPanel.H));
		clearHistoryTraceroute.setToolTipText(Resources.getLabel("history.clear"));
		clearHistoryTraceroute.addActionListener(e -> {
			services.getAutocomplete().clear();
			JOptionPane.showMessageDialog(ConfigDialog.this, Resources.getLabel("history.clear.confirmation"));
		});
		trPanel4.add(clearHistoryTraceroute);
		trPanel.add(trPanel4);

		final JPanel trPanel5 = new JPanel();
		trPanel5.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		trPanel5.add(new JLabel(Resources.getLabel("history")));
		final JToggleButton history = new JToggleButton(Resources.getImageIcon("history.png"), !Env.INSTANCE.isDisableHistory());
		history.setPreferredSize(new Dimension(130, ControlPanel.H));
		history.setToolTipText(Resources.getLabel("history.tooltip"));
		history.addActionListener(e -> history.setText((Resources.getLabel(history.isSelected() ? "enabled" : "disabled"))));
		history.setText((Resources.getLabel(!Env.INSTANCE.isDisableHistory() ? "enabled" : "disabled")));
		trPanel5.add(history);
		trPanel.add(trPanel5);

		final JPanel trPanel6 = new JPanel();
		trPanel6.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		trPanel6.add(new JLabel(Resources.getLabel("replay.time"), Resources.getImageIcon("play.png"), SwingConstants.CENTER));
		final JSpinner replayTime = new JSpinner();
		replayTime.setToolTipText(Resources.getLabel("replay.time.tooltip"));
		replayTime.setPreferredSize(new Dimension(50, ControlPanel.H));
		final double min = 1;
		replayTime.setModel(new SpinnerNumberModel(Math.max(min, Env.INSTANCE.getReplaySpeed() / 1000), min, 10, 1));
		trPanel6.add(replayTime);
		trPanel.add(trPanel6);

		trPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), Resources.getLabel("settings.tr")));
		main.add(trPanel);

		// sniffer settings
		final NetworkInterfaceChooser snifferInterfaceChooser = new NetworkInterfaceChooser(services, Mode.SNIFFER);
		if (services.isSnifferAvailable()) {
			final JPanel snifferPanel = new JPanel();
			snifferPanel.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
			snifferPanel.add(new JLabel(Resources.getLabel("device.select.label")));
			snifferInterfaceChooser.setPreferredSize(new Dimension(NETWORK_COMBO_W, ControlPanel.H));
			snifferPanel.add(snifferInterfaceChooser);

			snifferPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), Resources.getLabel("settings.sniffer")));
			main.add(snifferPanel);
		} else {
			if (Env.INSTANCE.getMode() == Mode.SNIFFER) {
				Env.INSTANCE.setMode(Mode.TRACE_ROUTE);
			}
		}

		// map setting
		final JPanel mapPanel = new JPanel();
		mapPanel.setLayout(new BoxLayout(mapPanel, BoxLayout.Y_AXIS));
		final JPanel mapPanel1 = new JPanel();

		mapPanel1.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		mapPanel1.add(new JLabel(Resources.getLabel("animation.time"), Resources.getImageIcon("pin.png"), SwingConstants.CENTER));
		final JSpinner animationTime = new JSpinner();
		animationTime.setToolTipText(Resources.getLabel("animation.time.tooltip"));
		animationTime.setPreferredSize(new Dimension(50, ControlPanel.H));
		animationTime.setModel(new SpinnerNumberModel(Math.max(min, Env.INSTANCE.getAnimationSpeed() / 1000), min, 10, 1));
		mapPanel1.add(animationTime);
		mapPanel.add(mapPanel1);

		final JPanel mapPanel2 = new JPanel();
		mapPanel2.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		mapPanel2.add(new JLabel(Resources.getLabel("map.show.label")));
		final JToggleButton mapShowLabel = new JToggleButton(Resources.getImageIcon("history.png"), Env.INSTANCE.isMapShowLabel());
		mapShowLabel.setPreferredSize(new Dimension(130, ControlPanel.H));
		mapShowLabel.setToolTipText(Resources.getLabel("map.show.label"));
		mapShowLabel.addActionListener(e -> mapShowLabel.setText((Resources.getLabel(mapShowLabel.isSelected() ? "yes" : "no"))));
		mapShowLabel.setText((Resources.getLabel(Env.INSTANCE.isMapShowLabel() ? "yes" : "no")));
		mapPanel2.add(mapShowLabel);
		mapPanel.add(mapPanel2);
		mapPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), Resources.getLabel("map.settings")));
		final JPanel mapPanel3 = new JPanel();
		mapPanel3.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		final JSpinner mapLineThickness = new JSpinner();
		@SuppressWarnings("serial")
		final SpinnerModel model2 = new SpinnerNumberModel(Env.INSTANCE.getMapLineThickness(), 1, 10, 1) {
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
		mapLineThickness.setModel(model2);
		final JLabel lineThicknessLabel = new JLabel(Resources.getLabel("map.line.thickness"));
		lineThicknessLabel.setToolTipText(Resources.getLabel("map.line.thickness"));
		mapLineThickness.setPreferredSize(new Dimension(100, ControlPanel.H));
		mapPanel3.add(lineThicknessLabel);
		mapPanel3.add(mapLineThickness);
		mapPanel.add(mapPanel3);

		main.add(mapPanel);

		// proxy settings
		final JPanel proxyPanel = new JPanel();
		proxyPanel.setLayout(new BoxLayout(proxyPanel, BoxLayout.Y_AXIS));
		final JPanel proxyPanel1 = new JPanel();
		proxyPanel1.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		final JTextField host = new JTextField();
		host.setColumns(20);
		proxyPanel1.add(new JLabel(Resources.getLabel("settings.proxy.host")));
		proxyPanel1.add(host);
		if (Env.INSTANCE.getProxyHost() != null) {
			host.setText(Env.INSTANCE.getProxyHost());
		}
		final JTextField port = new JTextField();
		port.setColumns(6);
		proxyPanel1.add(new JLabel(Resources.getLabel("settings.proxy.port")));
		proxyPanel1.add(port);
		if (Env.INSTANCE.getProxyPort() != null) {
			port.setText(Env.INSTANCE.getProxyPort());
		}
		final JPanel proxyPanel2 = new JPanel();
		proxyPanel2.setLayout(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
		final JTextField user = new JTextField();
		user.setColumns(15);
		proxyPanel2.add(new JLabel(Resources.getLabel("settings.proxy.user")));
		proxyPanel2.add(user);
		if (Env.INSTANCE.getProxyUser() != null) {
			user.setText(Env.INSTANCE.getProxyUser());
		}
		final JPasswordField password = new JPasswordField();
		password.setColumns(15);
		proxyPanel2.add(new JLabel(Resources.getLabel("settings.proxy.password")));
		proxyPanel2.add(password);
		if (Env.INSTANCE.getProxyPassword() != null) {
			password.setText(Env.PASSWORD);
		}

		proxyPanel.add(proxyPanel1);
		proxyPanel.add(proxyPanel2);
		proxyPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY, 2), Resources.getLabel("settings.proxy")));
		main.add(proxyPanel);

		setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

		// button panel
		final Runnable dispose = () -> {
			trInterfaceChooser.dispose();
			snifferInterfaceChooser.dispose();
			ConfigDialog.this.dispose();
		};
		final JButton ok = new JButton(Resources.getLabel("ok.button"));
		ok.addActionListener(e -> {
			boolean error = false;
			final StringBuffer message = new StringBuffer();
			boolean cancelled = false;
			try {
				final Language appliLanguage = Language.valueOf(languageGroup.getSelection().getActionCommand());
				if (Env.INSTANCE.getAppliLanguage() != appliLanguage) {
					JOptionPane.showConfirmDialog(ConfigDialog.this, Resources.getLabel("restart.appli.required"), "", JOptionPane.OK_OPTION);
					Env.INSTANCE.setAppliLanguage(appliLanguage);
				} else if (Env.INSTANCE.isDarkTheme() != darkTheme.isSelected()) {
					JOptionPane.showConfirmDialog(ConfigDialog.this, Resources.getLabel("restart.appli.required"), "", JOptionPane.OK_OPTION);
				}
			} catch (final Exception exp) {
				error = true;
				message.append(exp.getLocalizedMessage() + "\n");
				exp.printStackTrace();
			}
			if (!error) {
				if (!cancelled) {
					if (services.isEmbeddedTRAvailable()) {
						trInterfaceChooser.applySelection();
					}
					if (services.isSnifferAvailable()) {
						snifferInterfaceChooser.applySelection();
					}
					Env.INSTANCE.setProxyHost(host.getText());
					Env.INSTANCE.setProxyPort(port.getText());
					if (!StringUtils.isEmpty(user.getText())) {
						Env.INSTANCE.setProxyAuth(user.getText(), new String(password.getPassword()));
					} else {
						Env.INSTANCE.setProxyAuth(null, null);
					}
					Env.INSTANCE.setUseOSTraceroute(useOSTraceroute.isSelected());
					Env.INSTANCE.setTrMaxHop((Integer) maxHops.getValue());
					Env.INSTANCE.setDisableHistory(!history.isSelected());
					Env.INSTANCE.setDarkTheme(darkTheme.isSelected());
					Env.INSTANCE.setHideSplashScreen(!showSplash.isSelected());
					Env.INSTANCE.setMapShowLabel(mapShowLabel.isSelected());
					if (Env.INSTANCE.isDisableHistory()) {
						services.getAutocomplete().clear();
					}
					Env.INSTANCE.setAnimationSpeed((int) (Float.parseFloat(animationTime.getValue().toString()) * 1000));
					Env.INSTANCE.setReplaySpeed((int) (Float.parseFloat(replayTime.getValue().toString()) * 1000));
					Env.INSTANCE.setUIFont(SwingUtilities.getWindowAncestor(ConfigDialog.this), currentFont.get());
					Env.INSTANCE.setMapLineThickness((int) (Float.parseFloat(mapLineThickness.getValue().toString())));
					dispose.run();
				}
			} else {
				// some errors, display them
				JOptionPane.showMessageDialog(ConfigDialog.this, message.toString(), Resources.getLabel("error.label"), JOptionPane.ERROR_MESSAGE);
			}
		});

		final JButton cancel = new JButton(Resources.getLabel("cancel.button"));
		cancel.addActionListener(e -> dispose.run());

		final JPanel buttonPanel = new JPanel();
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		main.add(buttonPanel);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		SwingUtilities4.setUp(this);
		getRootPane().registerKeyboardAction(e -> dispose.run(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}
}
