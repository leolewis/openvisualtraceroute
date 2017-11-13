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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.OS;

/**
 * ImagePanel $Id: ImageComponent.java 181 2011-08-29 07:14:02Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author Leo Lewis
 */
public class ImageComponent extends JComponent {

	/**  */
	private static final long serialVersionUID = -567863652049704870L;

	/** Image to paint on the component */
	private final Image _img;

	/** Text to display on the image */
	private final String[] _text;

	/**
	 * Constructor
	 *
	 * @param img image
	 * @param text text to display on the image
	 */
	public ImageComponent(final Image img, final String[] text) {
		super();
		_img = img;
		_text = text;
		setPreferredSize(new Dimension(_img.getWidth(null), _img.getHeight(null)));
		setSize(getPreferredSize());
	}

	/**
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public synchronized void paintComponent(final Graphics g) {
		super.paintComponent(g);
		final Graphics2D g2D = (Graphics2D) g;
		g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		g2D.drawImage(_img, 0, 0, Color.black, null);
		g2D.setColor(Color.WHITE);
		int size = 13;
		if (_text != null && _text.length > 0) {
			g2D.setFont(new Font("Arial", Font.BOLD, size));
			String title = "";
			for (int i = 0; i < _text[0].length(); i++) {
				title += _text[0].charAt(i) + "   ";
			}
			final float factor = Env.INSTANCE.getOs() == OS.win ? 3f : 2.2f;
			g2D.drawString(title, new Double(0.5 * (_img.getWidth(null) - title.length() * size / factor)).intValue(), _img.getHeight(null) - 70 - (_text.length) * size);
			for (int i = 1; i < _text.length; i++) {
				size -= 2;
				g2D.setFont(new Font("Arial", Font.PLAIN, size));
				g2D.drawString(_text[i], new Double(0.5 * (_img.getWidth(null) - _text[i].length() * size / factor)).intValue(),
						_img.getHeight(null) - 20 - (_text.length - i) * size);
			}
		}
	}
}
