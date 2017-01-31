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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.leo.traceroute.util.Util;

/**
 * HyperlinkLabel $Id: HyperlinkLabel.java 143 2011-08-16 08:02:47Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class HyperlinkLabel extends JLabel {

	/**  */
	private static final long serialVersionUID = 1334125848537782354L;

	/** URL */
	private URL _url;

	/**
	 * Constructor
	 */
	public HyperlinkLabel(final String text, final String url) {
		this();
		setText(text);
		try {
			setUrl(new URL(url));
		} catch (final MalformedURLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor
	 */
	public HyperlinkLabel() {
		super();
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent evt) {
				if (_url != null) {
					Util.browse(_url, getText());
				}
			}
		});
		setForeground(Color.BLUE);
	}

	/**
	 * Return the value of the field url
	 * @return the value of url
	 */
	public URL getUrl() {
		return _url;
	}

	/**
	 * Set the value of the field url
	 * @param url the new url to set
	 */
	public void setUrl(final URL url) {
		_url = url;
	}
}