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
package org.leo.traceroute.resources;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;
import org.leo.traceroute.install.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resources $Id: Resources.java 279 2016-10-10 05:53:47Z leolewis $
 *
 * @author Leo Lewis
 */
public class Resources {

	private static final Logger LOGGER = LoggerFactory.getLogger(Resources.class);

	/** */
	protected static final String RESOURCE = Resources.class.getPackage().getName();
	/** */
	public static final String RESOURCE_PATH = RESOURCE.replace(".", "/");
	/** */
	private static final String RESOURCE_IMAGE = RESOURCE_PATH + "/images";
	/** */
	private static ResourceBundle LABEL_BUNDLE;
	/** */
	private static final ResourceBundle VERSION_BUNDLE = ResourceBundle.getBundle(RESOURCE + ".Version");

	private static final ResourceBundle STATIC_BUNDLE = ResourceBundle.getBundle(RESOURCE + ".static");

	public static void initLabels() {
		// custom bundle to be able to read the UTF-8 Japanese property file
		String name = "Labels";
		if (Env.INSTANCE.getAppliLanguage() != null) {
			switch (Env.INSTANCE.getAppliLanguage()) {
			case JAPANESE:
				name += "_ja_JP";
				break;
			case FRENCH:
				name += "_fr_FR";
				break;
			case GERMAN:
				name += "_de_DE";
				break;
			default:
				break;
			}
		}
		name += ".properties";
		final InputStream stream = Resources.class.getResourceAsStream(name);
		try {
			LABEL_BUNDLE = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
		} catch (final Exception e) {
			LOGGER.error("Failed to load resource bundle", e);
		}
	}

	/**
	 * Get resource label corresponding to given key
	 *
	 * @param key key
	 * @return String version
	 */
	public static String getLabel(final String key) {
		if (LABEL_BUNDLE.containsKey(key)) {
			return LABEL_BUNDLE.getString(key);
		}
		return key;
	}

	/**
	 * Get the installed version of the software
	 *
	 * @return String version
	 */
	public static String getWorkspaceVersion() {
		final File file = new File(Env.APP_FOLDER + "/version");
		if (file.exists()) {
			try {
				return IOUtils.toString(new FileInputStream(file));
			} catch (final Exception e) {
				LOGGER.error("Failed to load worspace version", e);
			}
		}
		try {
			IOUtils.write(getVersion(), new FileOutputStream(file));
		} catch (final Exception e) {
			LOGGER.error("Failed to save worspace version", e);
		}
		return null;
	}

	/**
	 * Get the version of the software
	 *
	 * @return String lable
	 */
	public static String getVersion() {
		return VERSION_BUNDLE.getString("version") + "." + VERSION_BUNDLE.getString("build") + " " + VERSION_BUNDLE.getString("type");
	}

	/**
	 * Get resource image corresponding to given key
	 *
	 * @param key key
	 * @return image
	 */
	public static Image getImage(final String key) {
		final URL url = Resources.class.getResource("/" + RESOURCE_IMAGE + "/" + key);
		if (url == null) {
			throw new IllegalArgumentException();
		}
		return Toolkit.getDefaultToolkit().getImage(url);
	}

	/**
	 * Get resource image icon corresponding to given key
	 *
	 * @param key key
	 * @return icon
	 */
	public static ImageIcon getImageIcon(final String key) {
		return new ImageIcon(getImage(key));
	}

	/**
	 * Get resource label corresponding to given key and formatted with the
	 * given parameters
	 *
	 * @param key key
	 * @param parameters parameters
	 * @return the formatted label
	 */
	public static String getLabel(final String key, final Object... parameters) {
		if (parameters.length > 0) {
			if (LABEL_BUNDLE.containsKey(key)) {
				return MessageFormat.format(getLabel(key), parameters);
			}
			return key + ": " + Arrays.toString(parameters);
		}
		return getLabel(key);
	}

	/**
	 * Get static bundle
	 * @param key
	 * @return
	 */
	public static String getStatic(final String key) {
		return STATIC_BUNDLE.getString(key);
	}
}
