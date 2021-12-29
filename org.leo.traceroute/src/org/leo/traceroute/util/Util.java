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
package org.leo.traceroute.util;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.eclipse.jetty.http.HttpTester.Request;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.IO;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.Earth;

/**
 * Util $Id: Util.java 281 2016-10-11 04:09:33Z leolewis $
 *
 * @author Leo Lewis
 */
public final class Util {

	private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

	/** File separator */
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	private Util() {

	}

	/**
	 * Copy a file
	 *
	 * @param src source file
	 * @param dst destination file
	 * @return flag if the copy succeeded
	 */
	public static boolean copy(final File src, final File dst) {
		FileInputStream in = null;
		boolean ok = true;
		try {
			in = new FileInputStream(src);
			ok = ok && copy(in, dst);
		} catch (final IOException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
			ok = false;
		} finally {
			// close
			try {
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
				ok = false;
			}
		}
		return ok;
	}

	/**
	 * Copy a file
	 *
	 * @param src source stream
	 * @param dst destination file
	 * @return flag if the copy succeeded
	 */
	public static boolean copy(final InputStream src, final File dst) {
		FileOutputStream out = null;
		boolean ok = true;
		try {
			out = new FileOutputStream(dst);
			final byte[] buffer = new byte[256];
			int read = src.read(buffer);
			while (read > 0) {
				out.write(buffer, 0, read);
				read = src.read(buffer);
			}
		} catch (final IOException e) {
			ok = false;
			LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			// close
			try {
				if (out != null) {
					out.close();
				}
			} catch (final IOException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
				ok = false;
			}
		}
		return ok;
	}

	/**
	 * Read a stream encoded in UTF-8 and return the content in a List of String
	 *
	 * @param stream stream
	 * @return list of String
	 */
	public static List<String> readUTF8File(final InputStream stream) {
		final List<String> list = new ArrayList<>();
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(stream, "UTF-8");
			br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
		} catch (final IOException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		} finally {
			IOUtils.closeQuietly(isr);
			IOUtils.closeQuietly(br);
		}
		return list;
	}

	/**
	 * Format a throwable
	 *
	 * @param t
	 * @return formatted
	 */
	public static String formatException(final Throwable t) {
		final StringBuilder builder = new StringBuilder();
		builder.append(t.getMessage());
		builder.append("\n");
		for (final StackTraceElement elt : t.getStackTrace()) {
			builder.append(elt.toString());
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * Converts a given Image into a BufferedImage
	 *
	 * @param img The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(final ImageIcon img) {
		final BufferedImage bimage = new BufferedImage(img.getIconWidth(), img.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

		final Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img.getImage(), 0, 0, null);
		bGr.dispose();

		return bimage;
	}

	/**
	 * Distance between two points on the globe (in km)
	 *
	 * @param point1
	 * @param point2
	 * @param globe
	 * @return
	 */
	public static int distance(final RoutePoint point1, final RoutePoint point2) {
		final LatLon ll1 = new LatLon(Angle.fromDegrees(point1.getLat()), Angle.fromDegrees(point1.getLon()));
		final LatLon ll2 = new LatLon(Angle.fromDegrees(point2.getLat()), Angle.fromDegrees(point2.getLon()));
		return (int) (Earth.WGS84_EQUATORIAL_RADIUS * LatLon.greatCircleDistance(ll1, ll2).getRadians() / 1000);
	}

	public static String uncompress(final Request request) throws IOException {
		if (request.getMethod().equals("GET")) {
			return request.toString();
		}
		ByteArrayInputStream bais = null;
		InputStream in = null;
		ByteArrayOutputStream out = null;
		final String compressionType = request.get("Accept-Encoding");
		try {
			bais = new ByteArrayInputStream(request.generate().array());

			if (compressionType.contains(GzipFilter.GZIP)) {
				try {
					in = new GZIPInputStream(bais);
				} catch (final Exception e) {
					in = null;
				}
			}
			if (in == null && compressionType.contains(GzipFilter.DEFLATE)) {
				in = new InflaterInputStream(bais, new Inflater(true));
			}
			out = new ByteArrayOutputStream();
			IO.copy(in, out);

			return out.toString();
		} finally {
			IO.close(out);
			IO.close(in);
			IO.close(bais);
		}
	}

	/**
	 * Public ip
	 * @return
	 */
	public static String getPublicIp() {
		int retry = 0;
		final int retryMax = 2;
		Exception ex = null;
		final String[] ipResolvers = Env.INSTANCE.getIpResolvers();
		if (ipResolvers != null) {
			final int rand = RandomUtils.nextInt(0, ipResolvers.length);
			while (retry < retryMax) {
				try {
					final String u = ipResolvers[((retry + rand) % retryMax)];
					final URL url = new URL(u);
					HttpURLConnection connection = null;
					try {
						connection = (HttpURLConnection) url.openConnection();
						connection.setConnectTimeout(10000);
						connection.setReadTimeout(10000);
						return IOUtils.toString(connection.getInputStream());
					} finally {
						if (connection != null) {
							connection.disconnect();
						}
					}
				} catch (final Exception e) {
					retry++;
					try {
						Thread.sleep(100);
					} catch (final InterruptedException e1) {
					}
					ex = e;
				}
			}
		}
		LOGGER.error("Error while getting local IP address", ex);
		return null;
	}

	/**
	 * A little of regex
	 * @param s
	 * @return
	 */
	public static String replaceTs(final String s, final int groupNum) {
		String pattern = ".*";
		for (int i = 0; i < groupNum; i++) {
			pattern += "(<?\\d+ ms)";
			if (i != groupNum - 1) {
				pattern += " ";
			}
		}
		pattern += ".*";
		final Pattern r = Pattern.compile(pattern);
		final Matcher m = r.matcher(s);
		final StringBuilder sb = new StringBuilder();
		if (m.find()) {
			sb.append(s.substring(0, m.start(1)));
			for (int i = 1; i <= m.groupCount(); i++) {
				final String time = m.group(i).replace("ms", "");
				sb.append(time);
			}
			sb.append(s.substring(m.end(m.groupCount()), s.length()));
		} else {
			return s;
		}
		return sb.toString();
	}

	/**
	 * Browse the given URL
	 *
	 * @param url url
	 * @param name title
	 */
	public static void browse(final URL url, final String name) {
		if (Desktop.isDesktopSupported()) {
			try {
				// need this strange code, because the URL.toURI() method have
				// some trouble dealing with UTF-8 encoding sometimes
				final URI uri = new URI(url.getProtocol(), url.getAuthority(), url.getPath(), url.getQuery(), url.getRef());
				Desktop.getDesktop().browse(uri);
			} catch (final Exception e) {
				LOGGER.error(e.getMessage(), e);
				JOptionPane.showMessageDialog(null, Resources.getLabel("error.open.url", name));
			}
		} else {
			JOptionPane.showMessageDialog(null, Resources.getLabel("error.open.url", name));
		}
	}

	/**
	 * Open the given file with the program associated with the file type by the
	 * OS
	 *
	 * @param file file
	 */
	public static boolean open(final File file) {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(file);
				return true;
			} catch (final Exception e) {
			}
		}
		return false;
	}

	public static String deepToString(final Object obj) {
		final StringBuilder sb = new StringBuilder();
		if (obj != null) {
			Class<?> c = obj.getClass();
			if (obj.getClass().isArray()) {
				if (obj.getClass() == byte[].class) {
					sb.append(Arrays.toString((byte[]) obj));
				} else {
					sb.append(Arrays.toString((Object[]) obj));
				}
			} else if (obj instanceof Number || obj instanceof Byte || obj instanceof Boolean || obj.getClass().isPrimitive() || obj instanceof String) {
				return obj.toString();
			} else if (obj instanceof InetAddress) {
				return ((InetAddress) obj).getHostAddress();
			} else {
				sb.append(obj.getClass().getSimpleName()).append("[ ");
				while (c != Object.class) {
					for (final Field f : c.getFields()) {
						if (!Modifier.isStatic(f.getModifiers())) {
							f.setAccessible(true);
							try {
								final Object o = f.get(obj);
								final String str = f.getType().isPrimitive() ? o.toString() : deepToString(o);
								sb.append(f.getName()).append("=").append(str).append(" ");
							} catch (final Exception e) {
								LOGGER.error("", e);
							}
						}
					}
					c = c.getSuperclass();
				}
				sb.append("]");
			}
		}
		return sb.toString();
	}

	public static void main(final String[] args) {
		System.out.println(getPublicIp());
	}

	public static InputStream followRedirectOpenConnection(final String url) throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setConnectTimeout((int) TimeUnit.MINUTES.toMillis(2));
		connection.setReadTimeout((int) TimeUnit.MINUTES.toMillis(2));
		connection.setInstanceFollowRedirects(true);
		InputStream inputStream = connection.getInputStream();
		int status = connection.getResponseCode();
		if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
			final String newUrl = connection.getHeaderField("Location");
			IOUtils.close(connection);
			connection = (HttpURLConnection) new URL(newUrl).openConnection();
			inputStream = connection.getInputStream();
			status = connection.getResponseCode();
		}
		if (status == HttpURLConnection.HTTP_OK) {
			return inputStream;
		} else {
			IOUtils.close(connection);
			throw new IOException("Failed to retrieve data from " + url + ", Server returned " + status);
		}
	}
}
