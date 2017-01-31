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
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.apache.commons.lang3.StringUtils;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * LogWindow $Id: LicenseDialog.java 180 2015-02-07 11:05:55Z leolewis $
 *
 * @author Leo Lewis
 */
public class LogWindow extends JDialog {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogWindow.class);

	public static class Appender extends AppenderBase<ILoggingEvent> {

		private PatternLayout patternLayout;
		private LogWindow display;

		@Override
		public void start() {
			patternLayout = new PatternLayout();
			patternLayout.setContext(getContext());
			patternLayout.setPattern("%date %level [%thread] %logger{10}:%line %msg%n");
			patternLayout.start();
			super.start();
		}

		/**
		 * @see ch.qos.logback.core.AppenderBase#append(java.lang.Object)
		 */
		@Override
		protected void append(final ILoggingEvent event) {
			if (display != null) {
				display.appendFormatted(patternLayout.doLayout(event));
			}
		}
	}

	/**  */
	private static final long serialVersionUID = -17211196425790681L;

	private Appender _appender;
	private final JTextPane _logs;
	private final Color _gray = new Color(245, 245, 245);

	/**
	 * Constructor
	 */
	public LogWindow(final Window parent) {
		super(parent, "Log", ModalityType.DOCUMENT_MODAL);
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		for (final ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
			for (final Iterator<ch.qos.logback.core.Appender<ILoggingEvent>> index = logger.iteratorForAppenders(); index
					.hasNext();) {
				final ch.qos.logback.core.Appender<ILoggingEvent> appender = index.next();
				if (appender instanceof Appender) {
					_appender = (Appender) appender;
					break;
				}
			}
		}
		_logs = new JTextPane() {
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return getUI().getPreferredSize(this).width <= getParent().getSize().width;
			}
		};
		if (_appender != null) {
			_appender.display = this;
		}
		try {
			for (final String line : Util.readUTF8File(new FileInputStream(Env.LOG_FILE))) {
				appendFormatted(line + "\n");
			}
		} catch (final FileNotFoundException e1) {
			appendFormatted("Failed to open file " + Env.LOG_FILE.getAbsolutePath());
		}
		final JScrollPane scroll = new JScrollPane(_logs, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setPreferredSize(new Dimension(800, 600));
		getContentPane().add(scroll, BorderLayout.CENTER);
		final JButton close = new JButton(Resources.getLabel("close.button"));
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				LogWindow.this.dispose();
			}
		});
		getContentPane().add(close, BorderLayout.SOUTH);
		SwingUtilities4.setUp(this);
		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				_appender.display = null;
				dispose();
				if (parent != null) {
					parent.toFront();
				}
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	int i = 0;

	private void appendFormatted(final String msg) {
		boolean bold = false;
		Color color = Color.BLACK;
		if (msg.contains("ERROR")) {
			color = Color.RED;
			bold = true;
		} else if (msg.contains("WARN")) {
			color = new Color(255, 107, 10);
			bold = true;
		} else if (msg.contains("INFO")) {
			color = Color.BLUE;
		}
		if (!msg.startsWith("\tat")) {
			i++;
		}
		final StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

		//aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
		aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
		aset = sc.addAttribute(aset, StyleConstants.Bold, bold);
		aset = sc.addAttribute(aset, StyleConstants.Background, ((i % 2) == 0) ? Color.WHITE : _gray);
		final int len = _logs.getDocument().getLength();
		_logs.setCaretPosition(len);
		_logs.setCharacterAttributes(aset, false);
		_logs.replaceSelection(StringUtils.repeat(" ", 4 - String.valueOf(i).length()) + i + " | " + msg);
	}

	public static void main(final String[] args) {
		Resources.initLabels();
		for (int i = 0; i < 50; i++) {
			LOGGER.info("Test");
		}
		new LogWindow(null).setVisible(true);
		while (true) {
			LOGGER.info("bla");
			try {
				Thread.sleep(10000);
			} catch (final InterruptedException e) {
			}
		}
	}
}
