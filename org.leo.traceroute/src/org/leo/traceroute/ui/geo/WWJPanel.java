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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.geo.GeoPoint;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.CountryFlagManager.Resolution;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.control.ControlPanel.Mode;
import org.leo.traceroute.ui.util.ColorUtil;
import org.leo.traceroute.ui.util.GlassPane;
import org.leo.traceroute.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenAnnotation;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwind.view.orbit.OrbitViewInputHandler;
import gov.nasa.worldwindx.applications.worldwindow.core.AppPanel;
import gov.nasa.worldwindx.applications.worldwindow.core.Controller;
import gov.nasa.worldwindx.applications.worldwindow.core.ToolBar;
import gov.nasa.worldwindx.applications.worldwindow.core.WWPanel;
import gov.nasa.worldwindx.applications.worldwindow.core.layermanager.LayerPath;
import gov.nasa.worldwindx.examples.util.LabeledPath;

/**
 * WWJPanel $Id: WWJPanel.java 286 2016-10-30 06:04:59Z leolewis $
 *
 * @author Leo Lewis
 */
public class WWJPanel extends AbstractGeoPanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(WWJPanel.class);

	/**  */
	private static final long serialVersionUID = -2193398912295790389L;

	static {
		if (Configuration.isMacOS()) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", Resources.getLabel("appli.title.short"));
		} else if (Configuration.isWindowsOS()) {
			System.setProperty("sun.awt.noerasebackground", "true");
		}
	}

	private static final org.leo.traceroute.resources.CountryFlagManager.Resolution IMAGE_RESOLUTION = Resolution.R32;

	/** World Wind controller (static as we cannot reinit it) */
	private static Controller _controller;
	private static JPanel _container;

	/** RenderableLayer */
	private RenderableLayer _renderableLayer;

	/** Current route path */
	private final List<Path> _lines = new ArrayList<Path>();

	/** Map between point and label */
	private final Map<GeoPoint, LabeledPath> _pointToLabel = new HashMap<GeoPoint, LabeledPath>();
	/** Map between label and point */
	private final Map<ScreenAnnotation, List<GeoPoint>> _annotationToPoint = new HashMap<ScreenAnnotation, List<GeoPoint>>();

	/** To avoid duplicated labels */
	private final Map<String, Pair<LabeledPath, ScreenAnnotation>> _toAvoidDuplicatedLabels = new HashMap<String, Pair<LabeledPath, ScreenAnnotation>>();
	private final Map<String, Pair<Path, MutableInt>> _packetDestCoordToPath = new HashMap<String, Pair<Path, MutableInt>>();

	/**  */
	private Pair<LabeledPath, GeoPoint> _lastSelection;

	private Pair<LabeledPath, ScreenAnnotation> _sourcePoint;
	private Position _sourcePos;
	private Position _previousPos;

	private int _selectionIndex;

	/**
	 * Constructor
	 *
	 * @param route
	 */
	public WWJPanel(final ServiceFactory services) {
		super(services);
		try {
			_renderableLayer = new RenderableLayer();
			final Dimension dim = new Dimension(800, 600);

			if (_controller == null) {
				_container = new JPanel(new BorderLayout());
				_controller = new Controller() {
					@Override
					public Frame getFrame() {
						return (Frame) SwingUtilities.getRoot(WWJPanel.this);//.getParent();
					}

					@Override
					public AppPanel getAppPanel() {
						return new AppPanel() {
							@Override
							public boolean isInitialized() {
								return true;
							}

							@Override
							public void initialize(final Controller controller) {
							}

							@Override
							public JPanel getJPanel() {
								return _container;
							}
						};
					}
				};
				_controller.start("resources/AppConfiguration.xml", dim);
			} else {
				removeAll();
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					_controller.getWWd().getView().setPitch(Angle.fromDegrees(15));
					final WWPanel wwpanel = _controller.getWWPanel();
					add(_container, BorderLayout.NORTH);
					add(wwpanel.getJPanel(), BorderLayout.CENTER);
					final ToolBar toolBar = _controller.getToolBar();
					if (toolBar != null) {
						add(toolBar.getJToolBar(), BorderLayout.PAGE_START);
					}
					// StatusPanel statusPanel = controller.getStatusPanel();
					// if (statusPanel != null) {
					// add(statusPanel.getJPanel(), BorderLayout.PAGE_END);
					// }
					// add(new FlatWorldPanel(_controller.getWWd()),
					// BorderLayout.PAGE_END);
					final LayerPath path = new LayerPath("Trace Route");
					_controller.getLayerManager().addLayer(_renderableLayer, path);
					_controller.getLayerManager().getNode(path).setSelected(true);
					_controller.getLayerManager().selectLayer(_renderableLayer, true);
					_controller.getWWd().addSelectListener(new SelectListener() {
						@Override
						public void selected(final SelectEvent event) {
							if (event.getEventAction().equals(SelectEvent.LEFT_CLICK) && event.hasObjects() && event.getTopObject() instanceof ScreenAnnotation) {
								final ScreenAnnotation sa = (ScreenAnnotation) event.getTopObject();
								final List<GeoPoint> points = _annotationToPoint.get(sa);
								if (_lastSelection != null && _lastSelection.getLeft().getAnnotation() == sa) {
									// if same selection, rotate point
									_selectionIndex = (_selectionIndex + 1) % (points.size());
								} else {
									_selectionIndex = 0;
								}
								if (points == null) {
									focus(null);
								} else {
									focus(points.get(_selectionIndex));
								}
							}
						}
					});
					_controller.getWWd().getInputHandler().addMouseListener(new MouseAdapter() {
						@Override
						public void mouseClicked(final MouseEvent e) {
							final Position currentPosition = _controller.getWWd().getCurrentPosition();
							if (currentPosition != null) {
								onMousePosition(currentPosition.getLatitude().getDegrees(), currentPosition.getLongitude().getDegrees());
							}
						}

						@Override
						public void mouseDragged(final MouseEvent e) {
							final Position currentPosition = _controller.getWWd().getCurrentPosition();
							if (currentPosition != null && _lastSelection != null) {
								final LabeledPath path = _lastSelection.getKey();
								path.getAnnotation().setScreenPoint(e.getLocationOnScreen());
							}
						}

					});
					_controller.getWWPanel().getJPanel().invalidate();
					invalidate();
					revalidate();
				}
			});
		} catch (final Exception e) {
			LOGGER.error(e.getLocalizedMessage(), e);
			GlassPane.displayMessage(this, "Error while initializing the World Wind Component\n" + e.getMessage(), Resources.getImageIcon("error.png"));
		}
	}

	/**
	 * @see org.leo.traceroute.ui.AbstractRoutePanel#afterShow()
	 */
	@Override
	public void afterShow(final Mode mode) {
		super.afterShow(mode);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				_container.invalidate();
				_container.revalidate();
				final GeoPoint localGeo = _services.getGeo().getLocalIpGeoLocation();
				final Position p = new Position(Angle.fromDegrees(localGeo.getLat()), Angle.fromDegrees(localGeo.getLon()), 2000);
				((OrbitView) _controller.getWWd().getView()).setCenterPosition(p);
				if (mode == Mode.TRACE_ROUTE) {
					_route.renotifyRoute();
				} else if (mode == Mode.SNIFFER) {
					_sniffer.renotifyPackets();
				} else {
					_whois.renotifyWhoIs();
				}
			}
		});

	}

	private double normalizeElevation(final double arbitraryElevation, final double step) {
		double normalized;
		if (arbitraryElevation == 0) {
			normalized = 1e4;
		} else {
			normalized = Math.min(1e6, step * arbitraryElevation);
		}
		return normalized;
	}

	@Override
	protected void pointAdded(final GeoPoint point, final boolean addLine) {
		final String coordKey = point.getCoordKey();
		Pair<LabeledPath, ScreenAnnotation> labelAndAnnotation = _toAvoidDuplicatedLabels.get(coordKey);

		// lat/lon DD position to WW Position
		double elevation = 1e4;
		if (point instanceof RoutePoint) {
			elevation = normalizeElevation(((RoutePoint) point).getElevation(), 2e3);
		}
		final Position pos = new Position(Position.fromDegrees(point.getLat(), point.getLon()), elevation);
		if (labelAndAnnotation == null) {
			// label to display
			final ScreenAnnotation annotation = makeLabelAnnotation(getText(point), point.getCountryFlag(IMAGE_RESOLUTION));
			final LabeledPath label = new LabeledPath(Collections.singletonList(pos), annotation);
			if (_mapShowLabel) {
				_renderableLayer.addRenderable(label);
			}
			labelAndAnnotation = Pair.of(label, annotation);
			_toAvoidDuplicatedLabels.put(coordKey, labelAndAnnotation);
		}
		// in sniffer mode, don't add duplicated points
		if (_mode == Mode.SNIFFER) {
			final Pair<Path, MutableInt> pathAndNumberOfPackets = _packetDestCoordToPath.get(coordKey);
			if (pathAndNumberOfPackets == null) {
				final Path path = createPath(ColorUtil.INSTANCE.getColorForNumOfPoints(1));
				_renderableLayer.addRenderable(path);
				path.setPositions(Arrays.asList(new Position(_sourcePos, _sourcePos.getAltitude()), pos));
				_packetDestCoordToPath.put(coordKey, Pair.of(path, new MutableInt(1)));
			} else {
				// update elevation and color of the line depending on the number of packets received from the dest coordinates
				final MutableInt num = pathAndNumberOfPackets.getRight();
				num.increment();
				final Path path = pathAndNumberOfPackets.getLeft();
				final Color color = ColorUtil.INSTANCE.getColorForNumOfPoints(num.intValue());
				path.getAttributes().setOutlineMaterial(new Material(color));
				path.getAttributes().setInteriorMaterial(new Material(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50), 50));
				final List<Position> copy = new ArrayList<Position>();
				for (final Position p : path.getPositions()) {
					copy.add(new Position(p, normalizeElevation(num.intValue(), 5e3)));
				}
				path.setPositions(copy);
			}
		} else if (_mode == Mode.TRACE_ROUTE) {
			if (addLine && _previousPos != null) {
				final Path path = createPath(((RoutePoint) point).getColor());
				path.setPositions(Arrays.asList(new Position(_previousPos, pos.getElevation()), pos));
				_renderableLayer.addRenderable(path);
			}
			_previousPos = pos;
		}

		_pointToLabel.put(point, labelAndAnnotation.getLeft());
		List<GeoPoint> points = _annotationToPoint.get(labelAndAnnotation.getRight());
		if (points == null) {
			points = new ArrayList<GeoPoint>();
			_annotationToPoint.put(labelAndAnnotation.getRight(), points);
		}
		points.add(point);
		// redraw
		_controller.redraw();
	}

	/**
	 * Make screen annotation
	 *
	 * @param text text
	 * @param image image
	 * @return the annotation
	 */
	private ScreenAnnotation makeLabelAnnotation(final String text, final ImageIcon image) {
		final String normalizedText = text == null ? " " : text;
		final ScreenAnnotation screenAnnotation = new ScreenAnnotation(normalizedText, new Point());
		screenAnnotation.setAttributes(createAnnotationAttr(false, image, normalizedText));
		screenAnnotation.setPickEnabled(true);
		return screenAnnotation;
	}

	/**
	 * Create Annotation graphical Attributes
	 *
	 * @param selected if the annotation is selected
	 * @param image
	 * @param text
	 * @return the attributes
	 */
	private static AnnotationAttributes createAnnotationAttr(final boolean selected, final ImageIcon image, final String text) {
		final String normalizedText = text == null || text.length() == 0 ? " " : text;
		final AnnotationAttributes attrs = new AnnotationAttributes();
		attrs.setAdjustWidthToText(AVKey.SIZE_FIT_TEXT);
		attrs.setFrameShape(AVKey.SHAPE_RECTANGLE);
		attrs.setDrawOffset(new Point(0, 10));
		attrs.setLeaderGapWidth(5);
		final Color color = getColor(selected);
		attrs.setTextColor(color);
		attrs.setBackgroundColor(new Color(1f, 1f, 1f, selected ? 1f : 0.5f));
		attrs.setBorderColor(color);
		attrs.setTextAlign(AVKey.CENTER);
		attrs.setInsets(new Insets(5, 15, 28, 15));
		attrs.setHighlighted(selected);
		attrs.setImageOpacity(1);
		int w = normalizedText.length() * 3;
		if (normalizedText.equals("*")) {
			w = 28;
		}
		attrs.setImageOffset(new Point(w, 17));
		// attrs.setImageScale(0.5);
		attrs.setImageSource(Util.toBufferedImage(image));
		attrs.setImageRepeat(AVKey.REPEAT_NONE);
		return attrs;
	}

	/**
	 * @see org.leo.traceroute.ui.AbstractRoutePanel#dispose()
	 */
	public void dispose(final boolean destroy) {
		super.dispose();
		if (destroy) {
			//_controller.getWWd().shutdown();
		}
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#startCapture(boolean)
	 */
	@Override
	public void startCapture() {
		_mode = Mode.SNIFFER;
		reinit();
		_packetDestCoordToPath.clear();
		final GeoPoint localGeo = _services.getGeo().getLocalIpGeoLocation();
		_sourcePos = new Position(Position.fromDegrees(localGeo.getLat(), localGeo.getLon()), 1e4);
		localGeo.setTown(youAreHere);
		localGeo.setCountry(youAreHere);
		pointAdded(localGeo, false);
		_sourcePoint = _toAvoidDuplicatedLabels.get(localGeo.getCoordKey());
		final AnnotationAttributes attrs = _sourcePoint.getRight().getAttributes();
		final Color color = SOURCE_COLOR;
		attrs.setTextColor(color);
		attrs.setBackgroundColor(new Color(1f, 1f, 1f, 1f));
		attrs.setBorderColor(color);
	}

	@Override
	protected void reinit() {
		_annotationToPoint.clear();
		_pointToLabel.clear();
		_toAvoidDuplicatedLabels.clear();
		_lines.clear();
		_previousPos = null;
		_renderableLayer.removeAllRenderables();
		_controller.redraw();
	}

	private Path createPath(final Color color) {
		final ShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setEnableLighting(true);
		attrs.setOutlineMaterial(new Material(color));
		attrs.setInteriorMaterial(new Material(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50), 50));
		attrs.setInteriorOpacity(0.3);
		attrs.setOutlineWidth(_mapLineThickness);
		attrs.setOutlineOpacity(0.7);
		final Path path = new Path();
		path.setExtrude(true);
		path.setAttributes(attrs);
		path.setVisible(true);
		path.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
		path.setPathType(AVKey.GREAT_CIRCLE);
		path.setFollowTerrain(true);
		return path;
	}

	@Override
	protected void focusPoint(final GeoPoint point, final boolean isRunning, final boolean animation) {
		final long elevation = 5000 * 1000;
		if (_controller == null || _controller.getWWd() == null) {
			return;
		}
		// center the map on the given point
		final LabeledPath label = point == null ? null : _pointToLabel.get(point);
		if (label != null) {
			highlightAnnotation(label, point);
			final View view = _controller.getWWd().getView();
			final OrbitViewInputHandler ovih = (OrbitViewInputHandler) view.getViewInputHandler();
			if (animation && Env.INSTANCE.getAnimationSpeed() > 0) {
				final Position pos = new Position(label.getLocations().iterator().next(), 10);
				ovih.addPanToAnimator(pos, view.getHeading(), view.getPitch(), elevation, Env.INSTANCE.getAnimationSpeed(), true);
				//				if (_mode == Mode.TRACE_ROUTE && isRunning) {
				//					// if tracing, move at the speed of the timeout
				//					final Position pos = new Position(label.getLocations().iterator().next(), 10);
				//					ovih.addPanToAnimator(pos, view.getHeading(), view.getPitch(), elevation,
				//							Env.INSTANCE.getAnimationSpeed(), true);
				//				} else if (_mode == Mode.TRACE_ROUTE || !isRunning) {
				//					_controller.getWWd().getView()
				//							.goTo(new Position(label.getLocations().iterator().next(), 10), elevation);
				//				}
			} else {
				final Position p = new Position(Angle.fromDegrees(point.getLat()), Angle.fromDegrees(point.getLon()), 2000);
				((OrbitView) view).setCenterPosition(p);
			}
		}
	}

	/**
	 * Highlight the given annotation
	 *
	 * @param annotation
	 * @param point
	 */
	private void highlightAnnotation(final LabeledPath label, final GeoPoint point) {
		final ScreenAnnotation annotation = label.getAnnotation();
		if (_lastSelection != null) {
			final LabeledPath lastSelectedLabel = _lastSelection.getLeft();
			final ScreenAnnotation lastSelectedAnnotation = lastSelectedLabel.getAnnotation();
			final GeoPoint lastSelectedPoint = _lastSelection.getRight();
			if (_mapShowLabel) {
				lastSelectedAnnotation.setAttributes(createAnnotationAttr(false, lastSelectedPoint.getCountryFlag(IMAGE_RESOLUTION), getText(lastSelectedPoint)));
				lastSelectedAnnotation.setAlwaysOnTop(false);
			} else {
				_renderableLayer.removeRenderable(lastSelectedLabel);
			}
		}
		final ImageIcon image = point.getCountryFlag(IMAGE_RESOLUTION);
		final String text = getText(point);
		annotation.setAttributes(createAnnotationAttr(true, image, text));
		annotation.setAlwaysOnTop(true);
		if (!_mapShowLabel) {
			_renderableLayer.addRenderable(label);
			_controller.redraw();
		}
		_lastSelection = Pair.of(label, point);
	}

	/**
	 * Enable or disable the childs of the given component
	 *
	 * @param enable true or false !
	 * @param comp the component
	 */
	public static void recursiveSetEnable(final boolean enable, final JComponent comp) {
		for (final Component c : comp.getComponents()) {
			c.setEnabled(enable);
			if (c instanceof JComponent) {
				recursiveSetEnable(enable, (JComponent) c);
			}
		}
	}

	/**
	 * Set the value of the field mapShowLabel
	 * @param mapShowLabel the new mapShowLabel to set
	 */
	@Override
	public void changeMapShowLabel(final boolean mapShowLabel) {
		if (mapShowLabel) {
			for (final Pair<LabeledPath, ScreenAnnotation> pair : _toAvoidDuplicatedLabels.values()) {
				if (_lastSelection == null || pair.getLeft() != _lastSelection.getLeft()) {
					_renderableLayer.addRenderable(pair.getLeft());
				}
			}
		} else {
			for (final Pair<LabeledPath, ScreenAnnotation> pair : _toAvoidDuplicatedLabels.values()) {
				if (_lastSelection == null || pair.getLeft() != _lastSelection.getLeft()) {
					_renderableLayer.removeRenderable(pair.getLeft());
				}
			}
		}
		_controller.redraw();
	}

	/**
	 * @see org.leo.traceroute.ui.geo.AbstractGeoPanel#changeLineThickness(int)
	 */
	@Override
	protected void changeLineThickness(final int thickness) {
		final Iterator<Renderable> iterator = _renderableLayer.getRenderables().iterator();
		while (iterator.hasNext()) {
			final Renderable r = iterator.next();
			if (r instanceof Path) {
				final Path p = (Path) r;
				p.getAttributes().setOutlineWidth(thickness);
			}
		}
		_controller.redraw();
	}

}
