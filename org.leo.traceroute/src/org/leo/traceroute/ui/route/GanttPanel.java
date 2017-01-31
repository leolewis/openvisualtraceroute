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
package org.leo.traceroute.ui.route;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.route.ITraceRoute;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.resources.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GanttPanel $Id: GanttPanel.java 232 2016-01-30 04:39:16Z leolewis $
 *
 * <pre>
 * </pre>
 *
 * @author Leo Lewis
 */
public class GanttPanel extends AbstractRoutePanel {

	private static final Logger LOGGER = LoggerFactory.getLogger(GanttPanel.class);

	/**  */
	private static final long serialVersionUID = -6919944810189375750L;

	enum DisplayMode {

		TIME("Time"),
		DISTANCE("Distance");

		private String _mode;

		private DisplayMode(final String mode) {
			_mode = mode;
		}

		/**
		 * Return the value of the field mode
		 *
		 * @return the value of mode
		 */
		public String getMode() {
			return _mode;
		}
	}

	/**
	 * Legend
	 */
	enum Legend {

		LATENCY(Resources.getLabel("latency"), new Color(78, 205, 196), new Color(255, 107, 10)),
		DNS(Resources.getLabel("dns.lookup"), new Color(199, 244, 100), new Color(196, 77, 88)),
		DISTANCE(Resources.getLabel("distance"), new Color(0, 160, 176), new Color(204, 51, 63));

		private String label;
		private Color color;
		private Color selectedColor;

		/**
		 * Constructor
		 *
		 * @param label
		 * @param color
		 * @param selectedColor
		 */
		private Legend(final String label, final Color color, final Color selectedColor) {
			this.label = label;
			this.color = color;
			this.selectedColor = selectedColor;
		}

	}

	/** Chart */
	private JFreeChart _chart;

	/** points series */
	private TaskSeries _pointsSeries;

	/** Length (distance or time) */
	private final AtomicLong _length = new AtomicLong();

	/** If the dns lookup function is enable for the current route */
	private volatile boolean _dnsLookup = true;

	/** Current display mode */
	private DisplayMode _currentMode;

	private JComboBox _modeCombo;

	/** Focus adjusting */
	private volatile boolean _focusAdjusting = false;

	/** Selected point index */
	private volatile int _selectedPoint = -1;
	private ChartPanel _chartPanel;
	private JScrollPane _scrollPane;
	private final ITraceRoute _route;

	/**
	 * Constructor
	 *
	 * @param route
	 */
	public GanttPanel(final ServiceFactory services) {
		super(services);
		_route = services.getTraceroute();
		init();
	}

	/**
	 * Init
	 */
	@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
	private void init() {
		_currentMode = DisplayMode.TIME;
		_modeCombo = new JComboBox(DisplayMode.values());
		_modeCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				_currentMode = (DisplayMode) _modeCombo.getSelectedItem();
			}
		});
		_modeCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				final DisplayMode mode = (DisplayMode) value;
				if (mode != null) {
					label.setText(mode.getMode());
					label.setToolTipText(mode.getMode());
					if (mode == DisplayMode.TIME) {
						label.setIcon(Resources.getImageIcon("network.png"));
					} else {
						label.setIcon(Resources.getImageIcon("map.png"));
					}
				} else {
					label.setText("");
				}
				return label;
			}
		});
		_modeCombo.setMaximumSize(new Dimension(50, 25));
		_pointsSeries = new TaskSeries("Route");

		final TaskSeriesCollection collection = new TaskSeriesCollection();
		collection.add(_pointsSeries);

		_chart = ChartFactory.createGanttChart("", "", "", collection, true, true, false);
		_chart.setAntiAlias(true);
		_chart.setBackgroundPaint(new Color(240, 240, 240, 255));
		_chart.setBorderVisible(false);
		_chart.setTextAntiAlias(true);

		updateLegend();

		final CategoryPlot plot = _chart.getCategoryPlot();
		final Color grey = new Color(200, 200, 200, 255);
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setTickLabelsVisible(false);
		plot.getRangeAxis().setTickMarksVisible(false);
		plot.getRangeAxis().setVisible(false);
		plot.getRangeAxis().setAutoRange(true);
		plot.getRangeAxis().setUpperMargin(0D);
		plot.getRangeAxis().setLowerMargin(0D);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(grey);

		plot.getDomainAxis().setTickLabelFont(plot.getRangeAxis().getTickLabelFont().deriveFont(8));
		plot.getDomainAxis().setLabelFont(plot.getDomainAxis().getLabelFont().deriveFont(8));
		plot.getDomainAxis().setTickMarksVisible(false);
		plot.setDomainCrosshairPaint(Color.RED);
		plot.setDomainGridlinePaint(grey);
		plot.setOutlineVisible(false);

		final GanttRenderer renderer = new GanttRenderer() {

			int previousCol = -1;
			int index;

			@Override
			public Paint getItemPaint(final int row, final int col) {
				Color color;
				if (col != previousCol) {
					index = 0;
				}
				if (index % 3 == 0) {
					if (_selectedPoint != col) {
						color = Legend.LATENCY.color;
					} else {
						color = Legend.LATENCY.selectedColor;
					}
				} else if (index % 1 == 0) {
					if (_selectedPoint != col) {
						color = Legend.DNS.color;
					} else {
						color = Legend.DNS.selectedColor;
					}
				} else {
					if (_selectedPoint != col) {
						color = Color.LIGHT_GRAY;
					} else {
						color = Color.RED;
					}
				}
				index++;
				previousCol = col;
				return color;
			}
		};
		renderer.setShadowVisible(false);
		plot.setRenderer(renderer);
		renderer.setBaseToolTipGenerator(new CategoryToolTipGenerator() {
			@Override
			public String generateToolTip(final CategoryDataset dataset, final int row, final int col) {
				final RoutePoint point = _route.getRoute().get(col);
				final StringBuilder sb = new StringBuilder();
				sb.append(point.getIp());
				if (point.getHostname() != null && point.getHostname().length() > 0) {
					sb.append(" (" + point.getHostname() + ")");
				}
				sb.append(" [ " + point.getTown() + ", " + point.getCountry() + "]");
				sb.append(": " + point.getLatency() + "ms");
				return sb.toString();
			}
		});
		_chartPanel = new ChartPanel(_chart);
		// chartPanel.addMouseWheelListener(new MouseWheelZoom(chartPanel));
		_chartPanel.addChartMouseListener(new ChartMouseListener() {
			@Override
			public void chartMouseMoved(final ChartMouseEvent event) {
			}

			@Override
			public void chartMouseClicked(final ChartMouseEvent event) {
				final ChartEntity entity = event.getEntity();
				if (entity != null && entity instanceof CategoryItemEntity) {
					final CategoryItemEntity ent = (CategoryItemEntity) entity;
					@SuppressWarnings("deprecation")
					final int index = ent.getCategoryIndex();
					_focusAdjusting = true;
					try {
						final RoutePoint point = _route.getRoute().get(index);
						_route.focus(point, true);
						selectPoint(point);
					} catch (final Exception e) {
						LOGGER.error("Error while focusing a point from the Gantt view", e);
					} finally {
						_focusAdjusting = false;
					}
				}
			}
		});
		_chartPanel.setMinimumDrawHeight(100);
		_chartPanel.setMaximumDrawHeight(5000);

		_chartPanel.setMinimumDrawWidth(100);
		_chartPanel.setMaximumDrawWidth(5000);
		// add(_modeCombo, BorderLayout.NORTH);
		_scrollPane = new JScrollPane(_chartPanel);
		_scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(_scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Update legend
	 */
	private void updateLegend() {
		_chart.removeLegend();
		final LegendTitle legend = new LegendTitle(new LegendItemSource() {
			@Override
			public LegendItemCollection getLegendItems() {
				final LegendItemCollection collection = new LegendItemCollection();
				if (_currentMode == DisplayMode.TIME) {
					collection.add(new LegendItem(Legend.LATENCY.label, null, null, null, new Rectangle(30, 20),
							Legend.LATENCY.color));
					if (_dnsLookup && _services.isEmbeddedTRAvailable() && !Env.INSTANCE.isUseOSTraceroute()) {
						collection
								.add(new LegendItem(Legend.DNS.label, null, null, null, new Rectangle(30, 20), Legend.DNS.color));
					}
				} else {
					collection.add(new LegendItem(Legend.DISTANCE.label, Legend.DISTANCE.color));
				}
				return collection;
			}
		});
		legend.setPosition(RectangleEdge.BOTTOM);
		_chart.addLegend(legend);
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#newRoute()
	 */
	@Override
	public void newRoute(final boolean dnsLookup) {
		_modeCombo.setEnabled(false);
		_selectedPoint = -1;
		_modeCombo.setEditable(false);
		_dnsLookup = dnsLookup;
		_length.set(0);
		_pointsSeries.removeAll();
		updateLegend();
		_chart.fireChartChanged();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#routePointAdded(org.leo.traceroute.core.RoutePoint,
	 *      boolean)
	 */
	@Override
	public void routePointAdded(final RoutePoint point) {
		final long f = 1000;
		if (_currentMode == DisplayMode.TIME) {
			final String name = String.valueOf(point.getNumber()) + ".";
			//					+ ((point.getHostname().equals("") || point.getHostname().equals(DNSLookupService.UNKNOWN_HOST)) ? point
			//							.getIp() : point.getHostname());

			long latency = point.getLatency();
			if (latency <= 0) {
				latency = 1;
			}
			long lookup = point.getDnsLookUpTime();
			if (_dnsLookup && lookup != DNSLookupService.UNDEF) {
				if (lookup == 0) {
					lookup = 1;
				}
				long l = _length.getAndAdd((latency + lookup) * f + 2);
				final Date beginDate = new Date(l);
				final Task latencyTask = new Task("Latency", new Date(l), new Date(l += (latency * f)));
				l++;
				final Task dns = new Task("DNS", new Date(l), new Date(l += (lookup * f)));
				final Task join = new Task(name, beginDate, new Date(l));
				join.addSubtask(latencyTask);
				join.addSubtask(dns);

				_pointsSeries.add(join);
			} else {
				final long l = _length.getAndAdd(latency * f + 1);
				final Task latencyTask = new Task(name, new Date(l), new Date(l + (latency * f)));
				_pointsSeries.add(latencyTask);
			}
		} else {
			final long l = _length.getAndAdd(point.getDistanceToPrevious() + 1);
			final Task location = new Task(point.getNumber() + ". " + point.getCountry() + "("
					+ (point.getTown() != null ? point.getTown() : "?") + ")", new Date(l), new Date(l
							+ point.getDistanceToPrevious()));
			_pointsSeries.add(location);
		}
		_chartPanel.setPreferredSize(new Dimension(_chartPanel.getPreferredSize().width, _route.size() * 20));
		_chartPanel.setMaximumSize(new Dimension(_chartPanel.getPreferredSize().width, _route.size() * 20));
		_chartPanel.invalidate();
		_scrollPane.revalidate();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#done(long)
	 */
	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#error(java.io.IOException)
	 */
	@Override
	public void error(final Exception exception, final Object origin) {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#cancelled()
	 */
	@Override
	public void routeCancelled() {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.RouteListener#timeout()
	 */
	@Override
	public void routeTimeout() {
		traceRouteEnded();
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#maxHops()
	 */
	@Override
	public void maxHops() {
		traceRouteEnded();
	}

	private void traceRouteEnded() {
		_modeCombo.setEnabled(true);
		_chart.fireChartChanged();
	}

	/**
	 * @see org.leo.traceroute.core.IRouteListener#focusRoute(org.leo.traceroute.core.RoutePoint, boolean, boolean)
	 */
	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {
		if (!_focusAdjusting) {
			_focusAdjusting = true;
			try {
				selectPoint(point);
			} finally {
				_focusAdjusting = false;
			}
		}
	}

	/**
	 * Select the given point
	 *
	 * @param point
	 */
	private void selectPoint(final RoutePoint point) {
		if (point != null) {
			_selectedPoint = point.getNumber() - 1;
		} else {
			_selectedPoint = -1;
		}
		_chart.fireChartChanged();
	}

	/**
	 * MouseWheelZoom $Id: GanttPanel.java 232 2016-01-30 04:39:16Z leolewis $
	 */
	private class MouseWheelZoom implements MouseWheelListener {

		private final ChartPanel _chartPanel;

		/**
		 * Constructor
		 *
		 * @param chartPanel
		 */
		public MouseWheelZoom(final ChartPanel chartPanel) {
			_chartPanel = chartPanel;
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
				return;
			}
			if (e.getWheelRotation() < 0) {
				zoomChartAxis(e.getPoint(), true);
			} else {
				zoomChartAxis(e.getPoint(), false);
			}
		}

		private void zoomChartAxis(final Point point, final boolean increase) {
			System.out.println(point);
			final int width = _chartPanel.getMaximumDrawWidth() - _chartPanel.getMinimumDrawWidth();
			final int height = _chartPanel.getMaximumDrawHeight() - _chartPanel.getMinimumDrawWidth();
			if (increase) {
				_chartPanel.zoomInRange(point.getX() - width / 2, point.getY());
			} else {
				_chartPanel.zoomOutRange(point.getX(), point.getY());
			}
		}
	}
}
