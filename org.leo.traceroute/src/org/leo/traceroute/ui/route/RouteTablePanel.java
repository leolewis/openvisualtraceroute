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
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.network.DNSLookupService;
import org.leo.traceroute.core.route.ITraceRoute;
import org.leo.traceroute.core.route.RoutePoint;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.IConfigProvider;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.resources.CountryFlagManager.Resolution;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.whois.WhoIsPanel;

/**
 * RoutePanel $Id: RouteTablePanel.java 287 2016-10-30 20:35:16Z leolewis $
 *
 * @author Leo Lewis
 */
public class RouteTablePanel extends AbstractRoutePanel implements IConfigProvider {

	/**
	 * Column enum
	 */
	public enum Column {

		NUMBER("#", Integer.class, 28, true),
		COUNTRY_FLAG("", ImageIcon.class, 24, false),
		COUNTRY(Resources.getLabel("country"), String.class, 115, true),
		TOWN(Resources.getLabel("town"), String.class, 115, true),
		LAT(Resources.getLabel("lat"), Float.class, 95, true),
		LON(Resources.getLabel("lon"), Float.class, 95, true),
		IP(Resources.getLabel("ip"), String.class, 125, true),
		HOSTNAME(Resources.getLabel("hostname"), String.class, 190, true),
		LATENCY(Resources.getLabel("latency"), Integer.class, 35, true),
		DNS_LOOKUP(Resources.getLabel("dns.lookup"), Integer.class, 45, true),
		DISTANCE_TO_PREVIOUS(Resources.getLabel("distance.previous.node"), Integer.class, 45, true),
		WHO_IS(Resources.getLabel("whois"), String.class, 15, false),

		;

		/** Label */
		private final String _label;
		/** Class */
		private final Class<?> _class;
		/** width */
		private final int _width;
		/** if the column will be exported */
		private final boolean _export;

		/**
		 * Constructor
		 *
		 * @param label
		 * @param clazz
		 */
		private Column(final String label, final Class<?> clazz, final int width, final boolean export) {
			_label = label;
			_class = clazz;
			_width = width;
			_export = export;
		}

		/**
		 * Label of the column
		 *
		 * @return label
		 */
		public String getLabel() {
			return _label;
		}

		/**
		 * Return the value of the field width
		 *
		 * @return the value of width
		 */
		public int getWidth() {
			return _width;
		}

		/**
		 * Class of the column
		 *
		 * @return class
		 */
		public Class<?> getClazz() {
			return _class;
		}

		/**
		 * Return the value of the field export
		 * @return the value of export
		 */
		public boolean isExport() {
			return _export;
		}

		/**
		 * Get value for the given route point
		 *
		 * @param point
		 * @return value
		 */
		public Object getValue(final RoutePoint point) {
			switch (this) {
			case NUMBER:
				return point.getNumber();
			case COUNTRY_FLAG:
				return point.getCountryFlag(Resolution.R16);
			case COUNTRY:
				return point.getCountry();
			case TOWN:
				return point.getTown();
			case LAT:
				return point.getLat();
			case LON:
				return point.getLon();
			case IP:
				return point.getIp();
			case HOSTNAME:
				return point.getHostname();
			case LATENCY:
				return point.getLatency();
			case DNS_LOOKUP:
				return point.getDnsLookUpTime() == DNSLookupService.UNDEF ? "~" : point.getDnsLookUpTime();
			case DISTANCE_TO_PREVIOUS:
				return point.getDistanceToPrevious();
			case WHO_IS:
				return point.getIp();
			default:
				return null;
			}
		}
	}

	/** Index to column */
	private final Map<Integer, Column> _indexToColumn = new HashMap<>();
	{
		int i = 0;
		for (final Column column : Column.values()) {
			_indexToColumn.put(i++, column);
		}
	}

	/** Width */
	public static final int WIDTH = 550;

	/**  */
	private static final long serialVersionUID = 733606227606524464L;

	/** Table model */
	private RouteTableModel _model;

	/** Table */
	private JTable _table;

	/** Search is pending */
	private volatile boolean _searching = false;

	/** Focus adjusting */
	private volatile boolean _focusAdjusting = false;

	/** If the dns lookup function is enable for the current route */
	private volatile boolean _dnsLookup = false;

	/**
	 * Constructor
	 *
	 * @param services
	 */
	public RouteTablePanel(final ServiceFactory services) {
		super(services);
		init();
	}

	/**
	 * Initialization of the panel
	 */
	@SuppressWarnings("serial")
	private void init() {
		// the table of the route points
		_model = new RouteTableModel(_route);
		_table = new JTable(_model) {
			@Override
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					@Override
					public String getToolTipText(final MouseEvent e) {
						final Point p = e.getPoint();
						final int index = columnModel.getColumnIndexAtX(p.x);
						final int realIndex = columnModel.getColumn(index).getModelIndex();
						return _model.getColumnName(realIndex);
					}
				};
			}
		};
		final JScrollPane scrollPane = new JScrollPane(_table);
		_table.setFillsViewportHeight(true);
		add(scrollPane, BorderLayout.CENTER);
		_table.setShowGrid(false);
		_table.setPreferredScrollableViewportSize(new Dimension(WIDTH, 250));
		_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_table.setAutoCreateRowSorter(true);
		_table.getTableHeader().setReorderingAllowed(true);
		// on selection change, notify the route to focus on the selection
		_table.getSelectionModel().addListSelectionListener(e -> {
			if (!_focusAdjusting) {
				try {
					_focusAdjusting = true;
					if (!e.getValueIsAdjusting() && _table.getSelectedRow() != -1) {
						final int index = _table.convertRowIndexToModel(_table.getSelectedRow());
						if (index >= 0 && index < _route.getRoute().size()) {
							_route.focus(_route.getRoute().get(index), true);
						}
					}
				} finally {
					_focusAdjusting = false;
				}
			}
		});
		// special renderer
		for (int colIndex = 0; colIndex < _table.getColumnCount(); colIndex++) {
			final TableColumn col = _table.getColumnModel().getColumn(colIndex);
			col.setCellRenderer(new RouteCellRenderer());
			_table.getColumnModel().getColumn(colIndex).setPreferredWidth(_indexToColumn.get(colIndex).getWidth());
			if (_indexToColumn.get(colIndex) == Column.WHO_IS) {
				col.setCellEditor(new ButtonCellEditor());
			}
		}
		ToolTipManager.sharedInstance().registerComponent(_table);
		Env.INSTANCE.registerConfigProvider(this);
	}

	@Override
	public String name() {
		return getClass().getSimpleName();
	}

	/**
	 * @see org.leo.traceroute.install.Env.IConfigProvider#load(java.util.Map)
	 */
	@Override
	public void load(final Map<String, String> config) {
		for (int colIndex = 0; colIndex < _table.getColumnCount(); colIndex++) {
			final Column column = _indexToColumn.get(colIndex);
			final String width = config.get(column.name());
			if (width != null) {
				_table.getColumnModel().getColumn(colIndex).setPreferredWidth(Integer.parseInt(width));
			}
		}
	}

	/**
	 * @see org.leo.traceroute.install.Env.IConfigProvider#save()
	 */
	@Override
	public Map<String, String> save() {
		final Map<String, String> widths = new HashMap<>();
		for (int colIndex = 0; colIndex < _table.getColumnCount(); colIndex++) {
			final Column column = _indexToColumn.get(colIndex);
			final int w = _table.getColumnModel().getColumn(colIndex).getPreferredWidth();
			widths.put(column.name(), String.valueOf(w));
		}
		return widths;
	}

	/**
	 * @see org.leo.traceroute.core.route.IRouteListener#newRoute(boolean)
	 */
	@Override
	public void newRoute(final boolean dnsLookup) {
		_searching = true;
		_dnsLookup = dnsLookup;
		_model.fireTableDataChanged();
	}

	@Override
	public void routePointAdded(final RoutePoint p) {
		_model.fireTableDataChanged();
	}

	@Override
	public void routeDone(final long tracerouteTime, final long lengthInKm) {
		traceRouteEnded();
	}

	@Override
	public void error(final Exception exception, final Object origin) {
		traceRouteEnded();
	}

	@Override
	public void routeCancelled() {
		traceRouteEnded();
	}

	@Override
	public void routeTimeout() {
		traceRouteEnded();
	}

	@Override
	public void maxHops() {
		traceRouteEnded();
	}

	/**
	 * Trace root ended
	 */
	private void traceRouteEnded() {
		_searching = false;
		_model.fireTableDataChanged();
	}

	@Override
	public void focusRoute(final RoutePoint point, final boolean isTracing, final boolean animation) {
		if (!_focusAdjusting) {
			try {
				_focusAdjusting = true;
				final int index = _route.getRoute().indexOf(point);
				_table.getSelectionModel().setSelectionInterval(index, index);
				_table.scrollRectToVisible(new Rectangle(_table.getCellRect(index, 0, true)));
			} finally {
				_focusAdjusting = false;
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		_model.dispose();
		Env.INSTANCE.unregisterConfigProvider(this);
	}

	/**
	 * RouteTableModel
	 */
	private class RouteTableModel extends AbstractTableModel {

		/**  */
		private static final long serialVersionUID = 760575517959483510L;

		/** Route (Data model) */
		private final ITraceRoute _route;

		/**
		 * Constructor
		 *
		 * @param route
		 */
		public RouteTableModel(final ITraceRoute route) {
			_route = route;
		}

		@Override
		public int getColumnCount() {
			return Column.values().length;
		}

		@Override
		public int getRowCount() {
			return _route.getRoute().size();
		}

		@Override
		public Object getValueAt(final int row, final int col) {
			final RoutePoint point = _route.getRoute().get(row);
			return _indexToColumn.get(col).getValue(point);
		}

		@Override
		public Class<?> getColumnClass(final int column) {
			return _indexToColumn.get(column).getClazz();
		}

		@Override
		public String getColumnName(final int column) {
			return _indexToColumn.get(column).getLabel();
		}

		@Override
		public boolean isCellEditable(final int row, final int column) {
			if (_indexToColumn.get(column) == Column.WHO_IS) {
				final RoutePoint point = _route.getRoute().get(row);
				return point != null && !point.isUnknown();
			}
			return false;
		}

		public void dispose() {

		}
	}

	/**
	 * RouteCellRenderer
	 *
	 * @author Leo Lewis
	 */
	private class RouteCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1622508565550177571L;

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row,
				final int column) {
			final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (c instanceof JLabel) {
				final JLabel label = (JLabel) c;
				// bg selection color
				if (isSelected) {
					label.setBackground(new Color(200, 200, 255));
				} else {
					// otherwise, alternate bg color
					if (row % 2 == 0) {
						label.setBackground(new Color(245, 245, 245));
					} else {
						label.setBackground(new Color(254, 254, 254));
					}
				}
				final Column col = _indexToColumn.get(column);
				if (col == Column.COUNTRY_FLAG) {
					label.setText("");
					label.setIcon((ImageIcon) value);
				} else if (col == Column.WHO_IS) {
					final JButton button = new JButton("?");
					button.setMargin(new Insets(0, 0, 0, 0));
					if (Env.INSTANCE.getOs() == OS.win) {
						button.setBorder(null);
					}
					final RoutePoint point = _route.getRoute().get(row);
					button.setToolTipText(Column.WHO_IS.getLabel());
					button.setPreferredSize(new Dimension(Column.WHO_IS.getWidth(), c.getHeight()));
					button.setEnabled(!_searching && point != null && !point.isUnknown());
					return button;
				} else {
					if ((col == Column.LATENCY || col == Column.DNS_LOOKUP) && value.equals(0L)) {
						if (!_dnsLookup && col == Column.DNS_LOOKUP) {
							label.setText("");
						} else {
							label.setText("<1");
						}
					}
				}
				label.setToolTipText(label.getText());
			}
			c.setFont(Env.INSTANCE.getFont());
			return c;
		}
	}

	/**
	 * ButtonCellEditor
	 *
	 * @author Leo Lewis
	 */
	public class ButtonCellEditor extends DefaultCellEditor {

		private static final long serialVersionUID = 5338833733700590170L;

		public ButtonCellEditor() {
			super(new JCheckBox());

		}

		@Override
		public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
			final Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
			final JButton button = new JButton("?");
			button.setMargin(new Insets(0, 0, 0, 0));
			button.setToolTipText(Column.WHO_IS.getLabel());
			button.setPreferredSize(new Dimension(Column.WHO_IS.getWidth(), c.getHeight()));
			button.setMaximumSize(button.getPreferredSize());
			if (Env.INSTANCE.getOs() == OS.win) {
				button.setBorder(null);
			}
			button.setEnabled(!_searching);
			button.addActionListener(e -> {
				final RoutePoint point = _route.getRoute().get(_table.convertRowIndexToModel(row));
				WhoIsPanel.showWhoIsDialog(RouteTablePanel.this, _services, point);
				if (table.isEditing()) {
					table.getCellEditor().stopCellEditing();
				}
				_whois.clear();
			});
			return button;
		}

	}
}
