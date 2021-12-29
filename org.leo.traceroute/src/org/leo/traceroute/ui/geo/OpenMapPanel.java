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
package org.leo.traceroute.ui.geo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.CountryFlagManager.Resolution;
import org.leo.traceroute.ui.util.ColorUtil;
import org.leo.traceroute.ui.util.SwingUtilities4;

import com.bbn.openmap.LayerHandler;
import com.bbn.openmap.MapBean;
import com.bbn.openmap.MapHandler;
import com.bbn.openmap.MouseDelegator;
import com.bbn.openmap.event.CenterEvent;
import com.bbn.openmap.event.OMMouseMode;
import com.bbn.openmap.event.SelectMouseMode;
import com.bbn.openmap.gui.EmbeddedNavPanel;
import com.bbn.openmap.gui.EmbeddedScaleDisplayPanel;
import com.bbn.openmap.gui.OverlayMapPanel;
import com.bbn.openmap.gui.ToolPanel;
import com.bbn.openmap.layer.GraticuleLayer;
import com.bbn.openmap.layer.OMGraphicHandlerLayer;
import com.bbn.openmap.layer.policy.BufferedImageRenderPolicy;
import com.bbn.openmap.layer.policy.StandardPCPolicy;
import com.bbn.openmap.layer.shape.ShapeLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMGraphicConstants;
import com.bbn.openmap.omGraphics.OMGraphicList;
import com.bbn.openmap.omGraphics.OMLine;
import com.bbn.openmap.omGraphics.OMPoint.Image;
import com.bbn.openmap.omGraphics.OMText;
import com.bbn.openmap.omGraphics.OMTextLabeler;
import com.bbn.openmap.omGraphics.event.StandardMapMouseInterpreter;
import com.bbn.openmap.proj.Orthographic;
import com.bbn.openmap.proj.coords.LatLonPoint;

/**
 * OpenMapPanel $Id: OpenMapPanel.java 286 2016-10-30 06:04:59Z leolewis $
 * <pre>
 * </pre>
 * @author Leo Lewis
 */
public class OpenMapPanel extends AbstractGeoPanel {

	enum Theme {

		NORMAL(new Color(121, 193, 192), "72CE9F", "EDF2AD"),
		DARK(new Color(10, 10, 35), "0000FF", "000000");

		private Color bg;
		private String line;
		private String fill;

		/**
		 * Constructor
		 * @param bg
		 * @param line
		 * @param fill
		 */
		private Theme(final Color bg, final String line, final String fill) {
			this.bg = bg;
			this.line = line;
			this.fill = fill;
		}
	}

	public class TraceRouteLayer extends OMGraphicHandlerLayer {

		/**  */
		private static final long serialVersionUID = -7019129484563304077L;

		private final OMGraphicList _omGraphicList;

		public TraceRouteLayer() {
			super();
			_omGraphicList = new OMGraphicList();
			setName("Trace route layer");
			final StandardMapMouseInterpreter mouseInterpreter = new StandardMapMouseInterpreter(this);
			setMouseEventInterpreter(mouseInterpreter);
			setProjectionChangePolicy(new StandardPCPolicy(this, true));
			setRenderPolicy(new BufferedImageRenderPolicy());
		}

		@Override
		public synchronized OMGraphicList prepare() {
			_omGraphicList.generate(getProjection());
			return _omGraphicList;
		}

		public void clear() {
			_omGraphicList.clear();
			doPrepare();
		}

		public void addShape(final OMGraphic shape, final boolean repaint, final boolean top) {
			if (top) {
				_omGraphicList.add(0, shape);
			} else {
				_omGraphicList.add(shape);
			}
			if (repaint) {
				SwingUtilities4.invokeInEDT(() -> TraceRouteLayer.this.doPrepare());
			}
		}

		public void removeShape(final OMGraphic shape, final boolean repaint) {
			while (_omGraphicList.remove(shape)) {
				;
			}
			if (repaint) {
				SwingUtilities4.invokeInEDT(() -> TraceRouteLayer.this.doPrepare());
			}
		}

		/**
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		@Override
		protected void paintComponent(final Graphics g) {
			if (g instanceof Graphics2D) {
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
			super.paintComponent(g);
		}

	}

	/**  */
	private static final long serialVersionUID = 4913575380062889700L;

	private static final org.leo.traceroute.resources.CountryFlagManager.Resolution IMAGE_RESOLUTION = Resolution.R32;

	private final TraceRouteLayer _layer;

	private Pair<OMText, Image> _previousPoint;
	private Pair<OMText, Image> _selectedPoint;
	private final Map<OMText, List<GeoPoint>> _omLabelToPoint = new HashMap<>();
	private final Map<Image, List<GeoPoint>> _omImageToPoint = new HashMap<>();
	private final Map<GeoPoint, Pair<OMText, Image>> _pointToOMPoint = new HashMap<>();
	private final Map<String, Pair<OMText, Image>> _toAvoidDuplicatedLabels = new HashMap<>();
	private Pair<OMText, Image> _sourcePoint;
	private final MapBean _mapBean;
	private final Theme _theme = Theme.NORMAL;
	private final Map<String, Pair<OMLine, MutableInt>> _packetDestCoordToPath = new HashMap<>();
	private int _selectionIndex;

	/**
	 * Constructor
	 */
	public OpenMapPanel(final ServiceFactory serviceFactory) {
		super(serviceFactory);
		final OverlayMapPanel mapPanel = new OverlayMapPanel();

		final MapHandler mapHandler = mapPanel.getMapHandler();

		_mapBean = mapPanel.getMapBean();

		mapHandler.add(new LayerHandler());
		// Add navigation tools over the map
		mapHandler.add(new EmbeddedNavPanel());
		// Add scale display widget over the map
		mapHandler.add(new EmbeddedScaleDisplayPanel());
		// Add MouseDelegator, which handles mouse modes (managing mouse events)
		final MouseDelegator mouseDelegator = new MouseDelegator();
		mouseDelegator.setActive(new SelectMouseMode());
		mapHandler.add(mouseDelegator);
		// Add OMMouseMode, which handles how the map reacts to mouse movements
		final OMMouseMode omMouseMode = new OMMouseMode();
		omMouseMode.setActive(true);
		mapHandler.add(omMouseMode);
		// Add a ToolPanel for widgets on the north side of the map.
		mapHandler.add(new ToolPanel());
		// world
		final ShapeLayer shapeLayer = new ShapeLayer();
		final Properties shapeLayerProps = new Properties();

		shapeLayerProps.put("prettyName", "World");
		shapeLayerProps.put("lineColor", _theme.line);
		shapeLayerProps.put("fillColor", _theme.fill);
		shapeLayerProps.put("lineWidth", "1.0");
		shapeLayerProps.put("selectColor", "ff000000");
		shapeLayerProps.put("shapeFile", Env.SHAPE_DATA_FILE.getAbsolutePath());
		shapeLayerProps.put("spatialIndex", Env.SHAPE_INDEX_DATA_FILE.getAbsolutePath());
		shapeLayer.setProperties(shapeLayerProps);
		_mapBean.setBackground(_theme.bg);

		final GraticuleLayer graticule = new GraticuleLayer();
		graticule.setEnabled(true);
		_layer = new TraceRouteLayer();
		mapHandler.add(graticule);
		mapHandler.add(shapeLayer);
		mapHandler.add(_layer);
		_mapBean.setProjection(new Orthographic(new LatLonPoint.Double(MapBean.DEFAULT_CENTER_LAT, MapBean.DEFAULT_CENTER_LON), 50000000f, MapBean.DEFAULT_WIDTH,
				MapBean.DEFAULT_HEIGHT));

		_mapBean.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				final OMGraphic omGraphic = ((StandardMapMouseInterpreter) _layer.getMouseEventInterpreter()).getGeometryUnder(e);
				List<GeoPoint> points = null;
				OMText text = null;
				Image img = null;
				if (omGraphic instanceof OMText) {
					text = (OMText) omGraphic;
					points = _omLabelToPoint.get(omGraphic);
				} else if (omGraphic instanceof Image) {
					img = (Image) omGraphic;
					points = _omImageToPoint.get(omGraphic);
				}
				if (text != null || img != null) {
					if (_selectedPoint != null && ((text != null && _selectedPoint.getLeft() == text) || (img != null && _selectedPoint.getRight() == img))) {
						_selectionIndex = (_selectionIndex + 1) % (points.size());
					} else {
						_selectionIndex = 0;
					}
					focus(points.get(_selectionIndex));
				} else {
					focus(null);
				}
				final Point2D forward = _mapBean.getProjection().forward(e.getX(), e.getY());
				onMousePosition(forward.getY(), forward.getX());
			}
		});
		add(BorderLayout.CENTER, mapPanel);
	}

	@Override
	public void afterShow(final Mode mode) {
		super.afterShow(mode);
		SwingUtilities.invokeLater(() -> {
			final GeoPoint localGeo = _services.getGeo().getLocalIpGeoLocation();
			_mapBean.setCenter(new LatLonPoint.Double(localGeo.getLat(), localGeo.getLon()));
			if (mode == Mode.TRACE_ROUTE) {
				_route.renotifyRoute();
			} else if (mode == Mode.SNIFFER) {
				_sniffer.renotifyPackets();
			} else {
				_whois.renotifyWhoIs();
			}
		});
	}

	@Override
	public void startCapture() {
		_mode = Mode.SNIFFER;

		reinit();
		final GeoPoint localGeo = _services.getGeo().getLocalIpGeoLocation();
		localGeo.setTown(youAreHere);
		localGeo.setCountry(youAreHere);
		pointAdded(localGeo, false);
		_sourcePoint = _toAvoidDuplicatedLabels.get(localGeo.getCoordKey());
		_sourcePoint.getLeft().setLinePaint(SOURCE_COLOR);
		_sourcePoint.getLeft().setMattingPaint(SOURCE_COLOR);
		SwingUtilities4.invokeInEDT(() -> _mapBean.repaint());
	}

	@Override
	protected void reinit() {
		_previousPoint = null;
		_selectedPoint = null;
		_pointToOMPoint.clear();
		_omLabelToPoint.clear();
		_omImageToPoint.clear();
		_toAvoidDuplicatedLabels.clear();
		_layer.clear();
		_packetDestCoordToPath.clear();
	}

	@Override
	protected void pointAdded(final GeoPoint point, final boolean addLine) {
		final String coordKey = point.getCoordKey();
		Pair<OMText, Image> omPoint = _toAvoidDuplicatedLabels.get(coordKey);
		if (omPoint == null) {
			final Image flagImage = new Image(point.getLat(), point.getLon());
			flagImage.setImage(point.getCountryFlag(IMAGE_RESOLUTION).getImage());
			final OMText text = new OMText(point.getLat(), point.getLon(), " " + getText(point) + " ", OMText.JUSTIFY_CENTER);
			text.setY(text.getY() - 15);
			text.setFillPaint(Color.WHITE);
			text.setMatted(true);
			text.setLinePaint(UNSELECTED_COLOR);
			text.setMattingPaint(UNSELECTED_COLOR);
			text.putAttribute(OMGraphicConstants.LABEL, new OMTextLabeler(getText(point)));
			omPoint = Pair.of(text, flagImage);
			_toAvoidDuplicatedLabels.put(coordKey, omPoint);
			// in sniffer mode, don't add duplicated points
			if (_mapShowLabel && _mode != Mode.SNIFFER) {
				_layer.addShape(omPoint.getRight(), false, true);
				_layer.addShape(omPoint.getLeft(), true, true);
			}
		}

		final Pair<OMText, Image> previous = addLine ? _previousPoint : _sourcePoint;
		if (previous != null) {
			final Pair<OMLine, MutableInt> pair = _packetDestCoordToPath.get(coordKey);
			if (pair == null) {
				final OMLine line = new OMLine(previous.getRight().getLat(), previous.getRight().getLon(), point.getLat(), point.getLon(),
						OMGraphicConstants.LINETYPE_GREATCIRCLE);
				if (point instanceof RoutePoint) {
					line.setLinePaint(((RoutePoint) point).getColor());
				} else {
					line.setLinePaint(ColorUtil.INSTANCE.getColorForNumOfPoints(1));
				}
				line.setStroke(new BasicStroke(_mapLineThickness));
				//			line.putAttribute(OMGraphicConstants.LABEL, new OMTextLabeler(point.getLatency() + "ms"));
				_layer.addShape(line, false, false);
				if (_mode == Mode.TRACE_ROUTE) {
					line.addArrowHead(true);
					_packetDestCoordToPath.put(coordKey, Pair.of(line, new MutableInt(1)));
				}
			} else if (_mode == Mode.SNIFFER) {
				final OMLine line = pair.getLeft();
				final MutableInt num = pair.getRight();
				num.increment();
				line.setLinePaint(ColorUtil.INSTANCE.getColorForNumOfPoints(num.intValue()));
				_layer.repaint();
			}
		}
		List<GeoPoint> l = _omLabelToPoint.get(omPoint.getLeft());
		if (l == null) {
			l = new ArrayList<>();
			_omLabelToPoint.put(omPoint.getLeft(), l);
			_omImageToPoint.put(omPoint.getRight(), l);
		}
		l.add(point);
		_pointToOMPoint.put(point, omPoint);
		if (_mapShowLabel && _mode != Mode.SNIFFER) {
			_layer.addShape(omPoint.getRight(), false, true);
			_layer.addShape(omPoint.getLeft(), true, true);
		}
		_previousPoint = omPoint;
		_layer.doPrepare();
	}

	@Override
	protected void focusPoint(final GeoPoint point, final boolean isRunning, final boolean animation) {
		if (_selectedPoint != null) {
			if (_mapShowLabel) {
				_selectedPoint.getLeft().setLinePaint(UNSELECTED_COLOR);
				_selectedPoint.getLeft().setMattingPaint(UNSELECTED_COLOR);
			} else {
				_layer.removeShape(_selectedPoint.getLeft(), false);
				_layer.removeShape(_selectedPoint.getRight(), false);
			}
		}
		if (point != null) {
			final Pair<OMText, Image> omPoint = _pointToOMPoint.get(point);
			if (omPoint != null) {
				omPoint.getLeft().setLinePaint(SELECTED_COLOR);
				omPoint.getLeft().setMattingPaint(SELECTED_COLOR);
				if (!_mapShowLabel) {
					_layer.addShape(omPoint.getLeft(), false, true);
					_layer.addShape(omPoint.getRight(), false, true);
				}
				_mapBean.center(new CenterEvent(this, omPoint.getLeft().getLat(), omPoint.getLeft().getLon()));
			}
			_selectedPoint = omPoint;
		} else {
			_selectedPoint = null;
		}
		if (_mapShowLabel) {
			_layer.repaint();
		} else {
			_layer.doPrepare();
		}

	}

	/**
	 * Set the value of the field mapShowLabel
	 * @param mapShowLabel the new mapShowLabel to set
	 */
	@Override
	public void changeMapShowLabel(final boolean mapShowLabel) {
		if (_mapShowLabel) {
			for (final Pair<OMText, Image> pair : _toAvoidDuplicatedLabels.values()) {
				if (_selectedPoint == null || pair.getLeft() != _selectedPoint.getLeft()) {
					_layer.addShape(pair.getLeft(), false, true);
					_layer.addShape(pair.getRight(), false, true);
				}
			}
		} else {
			for (final Pair<OMText, Image> pair : _toAvoidDuplicatedLabels.values()) {
				if (_selectedPoint == null || pair.getLeft() != _selectedPoint.getLeft()) {
					_layer.removeShape(pair.getLeft(), false);
					_layer.removeShape(pair.getRight(), false);
				}
			}
		}
		SwingUtilities4.invokeInEDT(() -> _layer.doPrepare());
	}

	/**
	 * @see org.leo.traceroute.ui.geo.AbstractGeoPanel#changeLineThickness(int)
	 */
	@Override
	protected void changeLineThickness(final int thickness) {
		final OMGraphicList list = _layer.getList();
		for (final OMGraphic g : list) {
			if (g instanceof OMLine) {
				final OMLine line = (OMLine) g;
				line.setStroke(new BasicStroke(thickness));
			}
		}
		SwingUtilities4.invokeInEDT(() -> _layer.doPrepare());
	}
}
