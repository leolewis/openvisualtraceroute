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
package org.leo.traceroute.install;

import java.awt.Container;
import java.awt.Font;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.geo.IMapConfigListener;
import org.leo.traceroute.ui.util.SwingUtilities4;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;

/**
 * Env $Id: Env.java 286 2016-10-30 06:04:59Z leolewis $
 * Singleton
 * @author Leo Lewis
 */
public enum Env {

	INSTANCE;

	private static final Logger LOGGER = LoggerFactory.getLogger(Env.class);

	public enum OS {
		win,
		linux,
		mac
	}

	public enum Arch {
		x86,
		x64
	}

	/**
	 * Language
	 */
	public enum Language {

		ENGLISH("english", true),
		FRENCH("french", true),
		JAPANESE("japanese", true),
		GERMAN("german", true);

		/** language name */
		private String _name;
		private boolean _enabled;

		/**
		 * Constructor
		 *
		 * @param name language name
		 */
		private Language(final String name, final boolean enabled) {
			_name = name;
			_enabled = enabled;
		}

		/**
		 * Return the value of the field name
		 *
		 * @return the value of name
		 */
		public String getLanguageName() {
			return Resources.getLabel("language." + _name);
		}

		/**
		 * Return the value of the field enabled
		 * @return the value of enabled
		 */
		public boolean isEnabled() {
			return _enabled;
		}
	}

	public interface IConfigProvider {

		String name();

		void load(Map<String, String> config);

		Map<String, String> save();
	}

	/** Application folder */
	public static final File APP_FOLDER = new File(Env.class.getProtectionDomain().getCodeSource().getLocation().getPath().replaceAll("%20", " ")).getParentFile();

	public static final File NATIVE_FOLDER = new File(APP_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "native");
	public static final File LIB_FOLDER = new File(APP_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "lib");
	public static final File RES_FOLDER = new File(APP_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "resources");
	public static final File SHAPE_DATA_FILE = new File(RES_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "world.shp");
	public static final File SHAPE_INDEX_DATA_FILE = new File(RES_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "world.ssx");

	public static final File OLD_OVTR_FOLDER = new File(System.getProperty("user.home"));
	public static final File OVTR_FOLDER = new File(System.getProperty("user.home") + Util.FILE_SEPARATOR + "ovtr");

	/** Put it into the tmp folder (to be sure the user has access to it) */
	public static final File GEO_DATA_FILE = new File(OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "GeoLite2-City.mmdb");
	public static final File GEO_DATA_FILE_OLD = new File(OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "GeoLite2-City.mmdb.old");
	public static final File HISTORY = new File(OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "ovtr.history");

	/** Config file */
	public static final File ORIG_CONFIG_FILE = new File(RES_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "conf.properties");
	public static File CONFIG_FILE = new File(OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "ovtr.properties");
	/** Log file */
	public static final File LOG_FILE = new File(OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + "ovtr.log");

	public static final String PASSWORD = "********";

	static {
		OVTR_FOLDER.mkdir();
		// migrate old files to new folder directory
		new File(OLD_OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + HISTORY.getName()).renameTo(HISTORY);
		new File(OLD_OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + CONFIG_FILE.getName()).renameTo(CONFIG_FILE);
		new File(OLD_OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + GEO_DATA_FILE.getName()).delete();
		new File(OLD_OVTR_FOLDER.getAbsolutePath() + Util.FILE_SEPARATOR + LOG_FILE.getName()).delete();
	}

	public static final String PROXY_HOST = "proxy_host";
	public static final String PROXY_PORT = "proxy_port";
	public static final String PROXY_USER = "proxy_user";
	public static final String PROXY_PASSWORD = "proxy_password";

	/** App H prop name */
	private static final String APP_HEIGHT = "apph";
	/** App W prop name */
	private static final String APP_WIDTH = "appw";
	/** App X prop name */
	private static final String APP_Y = "x";
	/** App Y prop name */
	private static final String APP_X = "y";
	/** Separator position prop name */
	private static final String SEPARATOR = "separator";
	/** Separator position prop name */
	private static final String RIGHT_SEPARATOR = "separator.right";
	/** 2D/3D prop name */
	private static final String IS_3D = "is.3D";
	/** Mode prop name */
	private static final String MODE = "mode";
	/** Mode prop name */
	private static final String FULL_SCREEN = "fullscreen";
	/** Mode prop name */
	private static final String HIDE_SPLASH_SCREEN = "hide.splashscreen";
	private static final String LANG = "lang";
	private static final String USE_OS_TR = "is.user.OS.tr";
	private static final String TR_INTERFACE = "tr.interface";
	private static final String SNIFFER_INTERFACE = "sniffer.interface";
	private static final String DISABLE_HISTORY = "disable.history";
	private static final String ANIMATION_SPEED = "animation.speed";
	private static final String MAP_LINE_THICKNESS = "map.line.thickness";
	private static final String REPLAY_SPEED = "replay.speed";
	private static final String TR_MAX_HOP = "tr.max.hop";
	private static final String MAP_SHOW_LABEL = "map.show.labels";
	private static final String FONT_NAME = "font.name";
	private static final String FONT_SIZE = "font.size";
	private static final String FONT_STYLE = "font.style";

	/** App config  */
	private final Properties _conf = new Properties();

	/** is full screen */
	private boolean _fullScreen;
	/** App H */
	private int _appHeight;
	/** App W */
	private int _appWidth;
	/** App X */
	private Integer _appX;
	/** App Y */
	private Integer _appY;
	/** Separator position */
	private int _separator;
	/** Separator position */
	private int _rightSeparator;
	/** 2d/3d map */
	private boolean _is3dMap;
	/** Trace route or sniffer mode */
	private Mode _mode;

	private OS _os;
	private Arch _arch;
	private volatile Boolean _openGlAvailable;
	private Language _appliLanguage;
	private boolean _useOSTraceroute;
	private int _trInterfaceIndex;
	private int _snifferInterfaceIndex;
	private String _proxyPort;
	private String _proxyHost;
	private String _proxyUser;
	private String _proxyPassword;
	private boolean _disableHistory;
	private boolean _hideSplashScreen;
	private int _animationSpeed;
	private int _mapLineThickness;
	private int _replaySpeed;
	private int _trMaxHop;
	private boolean _mapShowLabel;
	private Font _font;

	// dynamic conf
	private String[] _ipResolvers;
	private String _geoIpLocation;
	private String _donateUrl;
	private String _websitetUrl;
	private String _supportUrl;
	private String _versionUrl;
	private String _whatsnewUrl;
	private String _downloadUrl;
	private String _facebookUrl;

	private final Set<IMapConfigListener> _showLabelsListener = new HashSet<>();
	private final Set<IConfigProvider> _configProvider = new HashSet<>();

	/**
	 * Install the native lib folder into the path
	 *
	 * @throws EnvException
	 */
	public Pair<OS, Arch> initEnv() throws EnvException {
		// print some properties
		LOGGER.info("Java run-time version: " + System.getProperty("java.version"));
		LOGGER.info(gov.nasa.worldwind.Version.getVersion());
		LOGGER.info("Library Path " + System.getProperty("java.library.path"));
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");
		System.setProperty("java.net.useSystemProxies", "true");
//		System.setProperty("jogl.windows.useWGLVersionOf5WGLGDIFuncSet", "true");
		final Proxy proxy = getProxy();
		if (proxy != null) {
			final InetSocketAddress addr = (InetSocketAddress) proxy.address();
			if (addr != null) {
				final String host = addr.getHostName();
				final int port = addr.getPort();
				setProxyHost(host);
				setProxyPort(String.valueOf(port));
				LOGGER.info("Detected system proxy " + host + ":" + port);
			}

		}
		System.setErr(new PrintStream(new OutputStream() {
			@Override
			public void write(final int b) {
			}
		}));
		final Pair<OS, Arch> archOs = checkArchAndOs();
		_os = archOs.getLeft();
		_arch = archOs.getRight();
		if (_os == OS.win) {
			String path;
			if (_arch == Arch.x64) {
				path = "x64";
			} else {
				path = "Win32";
			}
			path = System.getenv("ProgramFiles(X86)") + "/Win10Pcap/" + path + "/";
			System.setProperty("org.pcap4j.core.pcapLibName", path + "wpcap.dll");
			System.setProperty("org.pcap4j.core.packetLibName", path + "Packet.dll");
		}
		try {
			loadConfig();
		} catch (final Exception e) {
			LOGGER.error("Error while loading application saved configuration", e);
			JOptionPane.showMessageDialog(null, Resources.getLabel("error.init", e.getMessage()), Resources.getLabel("fatal.error"), JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}
		return archOs;
	}

	private Pair<OS, Arch> checkArchAndOs() throws EnvException {
		final String osName = System.getProperty("os.name");
		final String archName = System.getProperty("os.arch");
		LOGGER.info("OS:" + osName + " / arch:" + archName);
		OS os = null;
		Arch arch = null;
		if (archName.toLowerCase().contains("86")) {
			arch = Arch.x86;
		} else if (archName.toLowerCase().contains("64")) {
			arch = Arch.x64;
		}
		if (osName.toLowerCase().contains("win")) {
			os = OS.win;
		} else if (osName.toLowerCase().contains("linux")) {
			os = OS.linux;
		} else if (osName.toLowerCase().contains("mac")) {
			os = OS.mac;
		}
		if (arch == null || os == null) {
			throw new EnvException("Unsupported os/architecture : " + osName + "/" + archName);
		}
		return Pair.of(os, arch);
	}

	/**
	 * Check current graphic card/drivers OpenGL capabilities to see if they match those required by WorldWind to work
	 * @return true if we are good, false otherwise
	 */
	public boolean isOpenGLAvailable() {
		_openGlAvailable = true;
		if (_openGlAvailable == null) {
			synchronized (this) {
				if (_openGlAvailable == null) {
					try {
						// create an offscreen context with the current graphic device
						final GLProfile glProfile = GLProfile.getDefault(GLProfile.getDefaultDevice());
						final GLCapabilities caps = new GLCapabilities(glProfile);
						caps.setOnscreen(false);
						caps.setPBuffer(false);
						final GLDrawable offscreenDrawable = GLDrawableFactory.getFactory(glProfile).createOffscreenDrawable(null, caps,
								new DefaultGLCapabilitiesChooser(), 1, 1);
						offscreenDrawable.setRealized(true);
						final GLContext context = offscreenDrawable.createContext(null);
						final int additionalCtxCreationFlags = 0;
						context.setContextCreationFlags(additionalCtxCreationFlags);
						context.makeCurrent();
						final GL gl = context.getGL();
						// WWJ will need those to render the globe
						_openGlAvailable = gl.isExtensionAvailable(GLExtensions.EXT_texture_compression_s3tc)
								|| gl.isExtensionAvailable(GLExtensions.NV_texture_compression_vtc);
						context.release();
					} catch (final Throwable e) {
						LOGGER.error("OpenGL", e);
						_openGlAvailable = false;
					}
				}
			}
		}
		return _openGlAvailable;
	}

	/**
	 * Init locale with preferences
	 */
	public void initLocale() {
		switch (_appliLanguage) {
		case FRENCH:
			Locale.setDefault(new Locale("fr", "FR"));
			break;
		case JAPANESE:
			Locale.setDefault(new Locale("ja", "JP"));
			break;
		case GERMAN:
			Locale.setDefault(new Locale("de", "DE"));
			break;
		default:
			Locale.setDefault(new Locale("en", "GB"));
			break;
		}
		System.out.println("Locale " + Locale.getDefault());
		Resources.initLabels();
	}

	public void saveConfig(final JSplitPane split, final JSplitPane rightSplit) {
		FileOutputStream os = null;
		try {
			LOGGER.info("Preferences saved.");
			final Window window = SwingUtilities.getWindowAncestor(split);
			_conf.put(APP_HEIGHT, String.valueOf(split.getHeight() + 42));
			_conf.put(APP_WIDTH, String.valueOf(split.getWidth()));
			_conf.put(APP_X, String.valueOf(window.getLocation().x));
			_conf.put(APP_Y, String.valueOf(window.getLocation().y));
			_conf.put(SEPARATOR, String.valueOf(split.getDividerLocation()));
			_conf.put(RIGHT_SEPARATOR, String.valueOf(rightSplit.getDividerLocation()));
			_conf.put(IS_3D, String.valueOf(_is3dMap));
			_conf.put(FULL_SCREEN, String.valueOf(_fullScreen));
			_conf.put(MODE, _mode.name());
			_conf.put(LANG, _appliLanguage.name());
			_conf.put(USE_OS_TR, String.valueOf(_useOSTraceroute));
			_conf.put(SNIFFER_INTERFACE, String.valueOf(_snifferInterfaceIndex));
			_conf.put(TR_INTERFACE, String.valueOf(_trInterfaceIndex));
			_conf.put(DISABLE_HISTORY, String.valueOf(_disableHistory));
			_conf.put(ANIMATION_SPEED, String.valueOf(_animationSpeed));
			_conf.put(MAP_LINE_THICKNESS, String.valueOf(_mapLineThickness));
			_conf.put(REPLAY_SPEED, String.valueOf(_replaySpeed));
			_conf.put(HIDE_SPLASH_SCREEN, String.valueOf(_hideSplashScreen));
			_conf.put(TR_MAX_HOP, String.valueOf(_trMaxHop));
			_conf.put(MAP_SHOW_LABEL, String.valueOf(_mapShowLabel));
			_conf.put(FONT_NAME, String.valueOf(_font.getFontName()));
			_conf.put(FONT_SIZE, String.valueOf(_font.getSize()));
			_conf.put(FONT_STYLE, String.valueOf(_font.getStyle()));
			for (final IConfigProvider c : _configProvider) {
				for (final Entry<String, String> entry : c.save().entrySet()) {
					_conf.put(c.name() + "." + entry.getKey(), entry.getValue());
				}
			}
			if (_proxyHost != null) {
				_conf.put(PROXY_HOST, _proxyHost);
			}
			if (_proxyPort != null) {
				_conf.put(PROXY_PORT, _proxyPort);
			}
			if (_proxyUser != null) {
				_conf.put(PROXY_USER, _proxyUser);
			}
			if (_proxyPassword != null) {
				_conf.put(PROXY_PASSWORD, _proxyPassword);
			}
			os = new FileOutputStream(Env.CONFIG_FILE);
			_conf.store(os, "");
		} catch (final Exception e) {
			LOGGER.error("Error while closing the main panel and saving preferences", e);
		} finally {
			IOUtils.closeQuietly(os);
		}
	}

	/**
	 * Load config
	 *
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void loadConfig() throws FileNotFoundException, IOException {
		FileInputStream is = null;
		try {
			if (!CONFIG_FILE.exists()) {
				if (!Util.copy(ORIG_CONFIG_FILE, CONFIG_FILE)) {
					LOGGER.warn("Failed to init user preferences file, will use the default settings");
					CONFIG_FILE = ORIG_CONFIG_FILE;
				}
			}
			is = new FileInputStream(CONFIG_FILE);
			_conf.load(is);
			final String lang = _conf.getProperty(LANG);
			_appliLanguage = lang == null ? Language.ENGLISH : Language.valueOf(lang);
			initLocale();
			final String proxyHost = _conf.getProperty(PROXY_HOST);
			final String proxyPort = _conf.getProperty(PROXY_PORT);
			if (proxyHost != null && proxyPort != null && !"".equals(proxyHost) && !"".equals(proxyPort)) {
				setProxyHost(proxyHost);
				setProxyPort(proxyPort);
			}
			final String proxyUser = _conf.getProperty(PROXY_USER);
			final String proxyPassword = _conf.getProperty(PROXY_PASSWORD);
			setProxyAuth(proxyUser, proxyPassword);
			_appHeight = Integer.parseInt(_conf.getProperty(APP_HEIGHT, "800"));
			_appWidth = Integer.parseInt(_conf.getProperty(APP_WIDTH, "1200"));
			_separator = Integer.parseInt(_conf.getProperty(SEPARATOR, "700"));
			_rightSeparator = Integer.parseInt(_conf.getProperty(RIGHT_SEPARATOR, "450"));
			_useOSTraceroute = Boolean.parseBoolean(_conf.getProperty(USE_OS_TR, "false"));
			_fullScreen = Boolean.parseBoolean(_conf.getProperty(FULL_SCREEN, "false"));
			_is3dMap = Env.INSTANCE.isOpenGLAvailable() ? Boolean.parseBoolean(_conf.getProperty(IS_3D, "true")) : false;
			_mode = Mode.TRACE_ROUTE;// Mode.valueOf(_conf.getProperty(MODE, Mode.TRACE_ROUTE.name()));
			_snifferInterfaceIndex = Integer.parseInt(_conf.getProperty(SNIFFER_INTERFACE, "-1"));
			_trInterfaceIndex = Integer.parseInt(_conf.getProperty(TR_INTERFACE, "-1"));
			//_tracerouteAnonymous = Boolean.parseBoolean(conf.getProperty(TRACEROUTE_ANONYMOUS, "false"));
			_disableHistory = Boolean.parseBoolean(_conf.getProperty(DISABLE_HISTORY, "false"));
			_animationSpeed = Integer.parseInt(_conf.getProperty(ANIMATION_SPEED, "1000"));
			_mapLineThickness = Integer.parseInt(_conf.getProperty(MAP_LINE_THICKNESS, "3"));
			_replaySpeed = Integer.parseInt(_conf.getProperty(REPLAY_SPEED, "2000"));
			_hideSplashScreen = Boolean.parseBoolean(_conf.getProperty(HIDE_SPLASH_SCREEN, "false"));
			_trMaxHop = Integer.parseInt(_conf.getProperty(TR_MAX_HOP, "50"));
			_mapShowLabel = Boolean.parseBoolean(_conf.getProperty(MAP_SHOW_LABEL, "true"));
			_appX = _conf.containsKey(APP_X) ? Integer.parseInt(_conf.getProperty(APP_X)) : null;
			_appY = _conf.containsKey(APP_Y) ? Integer.parseInt(_conf.getProperty(APP_Y)) : null;
			final String fontName = _conf.getProperty(FONT_NAME, "SansSerif");
			final int fontSize = Integer.parseInt(_conf.getProperty(FONT_SIZE, "9"));
			final int fontStyle = Integer.parseInt(_conf.getProperty(FONT_STYLE, String.valueOf(Font.PLAIN)));
			_font = new Font(fontName, fontStyle, fontSize);
			if (!Boolean.parseBoolean(_conf.getProperty("strictSSL", "false"))) {
				try {
					final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
						@Override
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}

						public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
						}

						public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
						}

						@Override
						public void checkClientTrusted(final java.security.cert.X509Certificate[] chain, final String authType) throws CertificateException {
						}

						@Override
						public void checkServerTrusted(final java.security.cert.X509Certificate[] chain, final String authType) throws CertificateException {
						}
					} };

					final SSLContext sc = SSLContext.getInstance("SSL");
					sc.init(null, trustAllCerts, new java.security.SecureRandom());
					HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

					final HostnameVerifier allHostsValid = new HostnameVerifier() {
						@Override
						public boolean verify(final String hostname, final SSLSession session) {
							return true;
						}
					};
					HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
				} catch (final Exception e) {
					LOGGER.error("Fail to set non strict SSL", e);
				}
			}
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	/**
	 * Return the value of the field appliLanguage
	 * @return the value of appliLanguage
	 */
	public Language getAppliLanguage() {
		return _appliLanguage;
	}

	/**
	 * Set the value of the field appliLanguage
	 * @param appliLanguage the new appliLanguage to set
	 */
	public void setAppliLanguage(final Language appliLanguage) {
		_appliLanguage = appliLanguage;
	}

	/**
	 * Return the value of the field os
	 * @return the value of os
	 */
	public OS getOs() {
		return _os;
	}

	/**
	 * Return the value of the field arch
	 * @return the value of arch
	 */
	public Arch getArch() {
		return _arch;
	}

	/**
	 * Return the value of the field is3dMap
	 * @return the value of is3dMap
	 */
	public boolean isIs3dMap() {
		return _is3dMap;
	}

	/**
	 * Set the value of the field is3dMap
	 * @param is3dMap the new is3dMap to set
	 */
	public void setIs3dMap(final boolean is3dMap) {
		_is3dMap = is3dMap;
	}

	/**
	 * Return the value of the field mode
	 * @return the value of mode
	 */
	public Mode getMode() {
		return _mode;
	}

	/**
	 * Set the value of the field mode
	 * @param mode the new mode to set
	 */
	public void setMode(final Mode mode) {
		_mode = mode;
	}

	/**
	 * Return the value of the field appHeight
	 * @return the value of appHeight
	 */
	public int getAppHeight() {
		return _appHeight;
	}

	/**
	 * Return the value of the field appWidth
	 * @return the value of appWidth
	 */
	public int getAppWidth() {
		return _appWidth;
	}

	/**
	 * Return the value of the field separator
	 * @return the value of separator
	 */
	public int getSeparator() {
		return _separator;
	}

	/**
	 * Return the value of the field rightSeparator
	 * @return the value of rightSeparator
	 */
	public int getRightSeparator() {
		return _rightSeparator;
	}

	/**
	 * Return the value of the field useOSTraceroute
	 * @return the value of useOSTraceroute
	 */
	public boolean isUseOSTraceroute() {
		return _useOSTraceroute;
	}

	/**
	 * Set the value of the field useOSTraceroute
	 * @param useOSTraceroute the new useOSTraceroute to set
	 */
	public void setUseOSTraceroute(final boolean useOSTraceroute) {
		_useOSTraceroute = useOSTraceroute;
	}

	/**
	 * Return the value of the field trInterfaceIndex
	 * @return the value of trInterfaceIndex
	 */
	public int getTrInterfaceIndex() {
		return _trInterfaceIndex;
	}

	/**
	 * Set the value of the field trInterfaceIndex
	 * @param trInterfaceIndex the new trInterfaceIndex to set
	 */
	public void setTrInterfaceIndex(final int trInterfaceIndex) {
		_trInterfaceIndex = trInterfaceIndex;
	}

	/**
	 * Return the value of the field snifferInterfaceIndex
	 * @return the value of snifferInterfaceIndex
	 */
	public int getSnifferInterfaceIndex() {
		return _snifferInterfaceIndex;
	}

	/**
	 * Set the value of the field snifferInterfaceIndex
	 * @param snifferInterfaceIndex the new snifferInterfaceIndex to set
	 */
	public void setSnifferInterfaceIndex(final int snifferInterfaceIndex) {
		_snifferInterfaceIndex = snifferInterfaceIndex;
	}

	/**
	 * Return the value of the field disableHistory
	 * @return the value of disableHistory
	 */
	public boolean isDisableHistory() {
		return _disableHistory;
	}

	/**
	 * Set the value of the field disableHistory
	 * @param disableHistory the new disableHistory to set
	 */
	public void setDisableHistory(final boolean disableHistory) {
		_disableHistory = disableHistory;
	}

	/**
	 * Return the value of the field proxyPort
	 * @return the value of proxyPort
	 */
	public String getProxyPort() {
		return _proxyPort;
	}

	/**
	 * Set the value of the field proxyPort
	 * @param proxyPort the new proxyPort to set
	 */
	public void setProxyPort(final String proxyPort) {
		_proxyPort = proxyPort;
		if (proxyPort == null || "".equals(proxyPort)) {
			Configuration.removeKey(AVKey.URL_PROXY_PORT);
			Configuration.removeKey(AVKey.URL_PROXY_TYPE);
			System.getProperties().remove("http.proxyPort");
		} else {
			Configuration.setValue(AVKey.URL_PROXY_PORT, proxyPort);
			Configuration.setValue(AVKey.URL_PROXY_TYPE, Proxy.Type.HTTP);
			System.setProperty("http.proxyPort", proxyPort);
		}
	}

	/**
	 * Return the value of the field proxyHost
	 * @return the value of proxyHost
	 */
	public String getProxyHost() {
		return _proxyHost;
	}

	/**
	 * Set the value of the field proxyHost
	 * @param proxyHost the new proxyHost to set
	 */
	public void setProxyHost(final String proxyHost) {
		_proxyHost = proxyHost;
		if (proxyHost == null || "".equals(proxyHost)) {
			Configuration.removeKey(AVKey.URL_PROXY_HOST);
			Configuration.removeKey(AVKey.URL_PROXY_TYPE);
			System.getProperties().remove("http.proxyHost");
		} else {
			Configuration.setValue(AVKey.URL_PROXY_HOST, proxyHost);
			Configuration.setValue(AVKey.URL_PROXY_TYPE, Proxy.Type.HTTP);
			System.setProperty("http.proxyHost", proxyHost);
		}
	}

	/**
	 * Set proxy auth
	 * @param user
	 * @param password
	 */
	public void setProxyAuth(final String user, final String password) {
		_proxyUser = user;
		if (user == null) {
			_proxyPassword = null;
			Authenticator.setDefault(new Authenticator() {
			});
			System.getProperties().remove("http.proxyUser");
			System.getProperties().remove("http.proxyPassword");
		} else {
			_proxyPassword = password.equals(PASSWORD) ? _proxyPassword : password;
			Authenticator.setDefault(new Authenticator() {
				@Override
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, password.toCharArray());
				}
			});
			System.setProperty("http.proxyUser", user);
			System.setProperty("http.proxyPassword", password);
		}
	}

	/**
	 * Return the value of the field proxyUser
	 * @return the value of proxyUser
	 */
	public String getProxyUser() {
		return _proxyUser;
	}

	/**
	 * Return the value of the field proxyPassword
	 * @return the value of proxyPassword
	 */
	public String getProxyPassword() {
		return _proxyPassword;
	}

	/**
	 * Return the value of the field animationSpeed
	 * @return the value of animationSpeed
	 */
	public int getAnimationSpeed() {
		return _animationSpeed;
	}

	/**
	 * Set the value of the field animationSpeed
	 * @param animationSpeed the new animationSpeed to set
	 */
	public void setAnimationSpeed(final int animationSpeed) {
		_animationSpeed = animationSpeed;
	}

	/**
	 * Return the value of the field replaySpeed
	 * @return the value of replaySpeed
	 */
	public int getReplaySpeed() {
		return _replaySpeed;
	}

	/**
	 * Set the value of the field replaySpeed
	 * @param replaySpeed the new replaySpeed to set
	 */
	public void setReplaySpeed(final int replaySpeed) {
		_replaySpeed = replaySpeed;
	}

	/**
	 * Return the value of the field fullScreen
	 * @return the value of fullScreen
	 */
	public boolean isFullScreen() {
		return _fullScreen;
	}

	/**
	 * Set the value of the field fullScreen
	 * @param fullScreen the new fullScreen to set
	 */
	public void setFullScreen(final boolean fullScreen) {
		_fullScreen = fullScreen;
	}

	/**
	 * Return the value of the field hideSplashScreen
	 * @return the value of hideSplashScreen
	 */
	public boolean isHideSplashScreen() {
		return _hideSplashScreen;
	}

	/**
	 * Set the value of the field hideSplashScreen
	 * @param hideSplashScreen the new hideSplashScreen to set
	 */
	public void setHideSplashScreen(final boolean hideSplashScreen) {
		_hideSplashScreen = hideSplashScreen;
	}

	/**
	 * Load dynamic config from server
	 */
	public void loadDynamicConf(final ServiceFactory factory) {
		InputStream dynConf = null;
		try {
			if (factory != null) {
				factory.updateStartup("loading.dynamic.conf", true);
			}
			dynConf = Util.followRedirectOpenConnection(Resources.getStatic("dynamic.conf.url"));
			final Properties prop = new Properties();
			prop.load(dynConf);
			final String ips = prop.getProperty("ip.resolver");
			if (ips != null) {
				_ipResolvers = ips.split(",");
			}
			_geoIpLocation = prop.getProperty("geo.ip.location2");
			_donateUrl = prop.getProperty("donate.url");
			_versionUrl = prop.getProperty("version.url");
			_whatsnewUrl = prop.getProperty("whats.new.url");
			_downloadUrl = prop.getProperty("download.url");
			_supportUrl = prop.getProperty("support.url");
			_websitetUrl = prop.getProperty("website.url");
			_facebookUrl = prop.getProperty("facebook.url");
		} catch (final Exception e) {
			LOGGER.error("Failed to download dynamic config. Fallback to default", e);
		} finally {
			IOUtils.closeQuietly(dynConf);
		}
		if (_geoIpLocation == null) {
			_geoIpLocation = Resources.getStatic("update.geoip.url");
		}
		if (_ipResolvers == null) {
			_ipResolvers = new String[] { "http://www.trackip.net/ip", "https://api.ipify.org/" };
		}
		if (_donateUrl == null) {
			_donateUrl = Resources.getStatic("donate.url");
		}
		if (_versionUrl == null) {
			_versionUrl = Resources.getStatic("version.url");
		}
		if (_whatsnewUrl == null) {
			_whatsnewUrl = Resources.getStatic("whats.new.url");
		}
		if (_downloadUrl == null) {
			_downloadUrl = Resources.getStatic("download.url");
		}
		if (_supportUrl == null) {
			_supportUrl = Resources.getStatic("support.url");
		}
		if (_websitetUrl == null) {
			_websitetUrl = Resources.getStatic("website.url");
		}
		if (_facebookUrl == null) {
			_facebookUrl = Resources.getStatic("facebook.url");
		}
	}

	public String[] getIpResolvers() {
		return _ipResolvers;
	}

	public String getGeoIpLocation() {
		return _geoIpLocation;
	}

	/**
	 * Return the value of the field donateUrl
	 * @return the value of donateUrl
	 */
	public String getDonateUrl() {
		return _donateUrl;
	}

	/**
	 * Return the value of the field trMaxHop
	 * @return the value of trMaxHop
	 */
	public int getTrMaxHop() {
		return _trMaxHop;
	}

	/**
	 * Set the value of the field trMaxHop
	 * @param trMaxHop the new trMaxHop to set
	 */
	public void setTrMaxHop(final int trMaxHop) {
		_trMaxHop = trMaxHop;
	}

	/**
	 * Return the value of the field mapShowLabel
	 * @return the value of mapShowLabel
	 */
	public boolean isMapShowLabel() {
		return _mapShowLabel;
	}

	/**
	 * Set the value of the field mapShowLabel
	 * @param mapShowLabel the new mapShowLabel to set
	 */
	public void setMapShowLabel(final boolean mapShowLabel) {
		if (mapShowLabel != _mapShowLabel) {
			_mapShowLabel = mapShowLabel;
			for (final IMapConfigListener listener : _showLabelsListener) {
				listener.setMapShowLabel(mapShowLabel);
			}
		}
	}

	/**
	 * Add a show label listener
	 */
	public void addShowLabelsListener(final IMapConfigListener listener) {
		_showLabelsListener.add(listener);
	}

	/**
	 * Return the value of the field supportUrl
	 * @return the value of supportUrl
	 */
	public String getSupportUrl() {
		return _supportUrl;
	}

	/**
	 * Return the value of the field versionUrl
	 * @return the value of versionUrl
	 */
	public String getVersionUrl() {
		return _versionUrl;
	}

	/**
	 * Return the value of the field whatsnewUrl
	 * @return the value of whatsnewUrl
	 */
	public String getWhatsnewUrl() {
		return _whatsnewUrl;
	}

	/**
	 * Return the value of the field downloadUrl
	 * @return the value of downloadUrl
	 */
	public String getDownloadUrl() {
		return _downloadUrl;
	}

	/**
	 * Return the value of the field facebookUrl
	 * @return the value of facebookUrl
	 */
	public String getFacebookUrl() {
		return _facebookUrl;
	}

	/**
	 * Return the value of the field websitetUrl
	 * @return the value of websitetUrl
	 */
	public String getWebsitetUrl() {
		return _websitetUrl;
	}

	/**
	 * Register a config provider
	 * @param provider
	 */
	public void registerConfigProvider(final IConfigProvider provider) {
		_configProvider.add(provider);
		final String name = provider.name();
		final Map<String, String> m = new HashMap<>();
		for (final Entry<Object, Object> entry : _conf.entrySet()) {
			if (entry.getKey().toString().startsWith(name)) {
				m.put(entry.getKey().toString().replace(name + ".", ""), entry.getValue().toString());
			}
		}
		provider.load(m);
	}

	/**
	 * Unregister config provider
	 * @param provider
	 */
	public void unregisterConfigProvider(final IConfigProvider provider) {
		_configProvider.remove(provider);
	}

	public void setUIFont(final Container root, final Font font) {
		_font = font;
		SwingUtilities4.applyFont(root, font);
	}

	/**
	 * Return the value of the field font
	 * @return the value of font
	 */
	public Font getFont() {
		return _font;
	}

	/**
	 * Return the value of the field appX
	 * @return the value of appX
	 */
	public Integer getAppX() {
		return _appX;
	}

	/**
	 * Return the value of the field appY
	 * @return the value of appY
	 */
	public Integer getAppY() {
		return _appY;
	}

	/**
	 * Return the value of the field mapLineWidth
	 * @return the value of mapLineWidth
	 */
	public int getMapLineThickness() {
		return _mapLineThickness;
	}

	/**
	 * Set the value of the field mapLineWidth
	 * @param mapLineThickness the new mapLineWidth to set
	 */
	public void setMapLineThickness(final int mapLineThickness) {
		_mapLineThickness = mapLineThickness;
		for (final IMapConfigListener listener : _showLabelsListener) {
			listener.setLineThickness(mapLineThickness);
		}
	}

	private static Proxy getProxy() {
		List<Proxy> l = null;
		try {
			final ProxySelector def = ProxySelector.getDefault();
			l = def.select(new URI("http://foo/bar"));
			ProxySelector.setDefault(null);
		} catch (final Exception e) {

		}
		if (l != null) {
			for (final Iterator<Proxy> iter = l.iterator(); iter.hasNext();) {
				final java.net.Proxy proxy = iter.next();
				return proxy;
			}
		}
		return null;
	}
}
