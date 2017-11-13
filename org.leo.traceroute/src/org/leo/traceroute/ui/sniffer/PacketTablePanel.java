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
package org.leo.traceroute.ui.sniffer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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

import org.apache.commons.io.FileUtils;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint.Protocol;
import org.leo.traceroute.core.sniffer.IPacketsSniffer;
import org.leo.traceroute.install.Env;
import org.leo.traceroute.install.Env.IConfigProvider;
import org.leo.traceroute.install.Env.OS;
import org.leo.traceroute.resources.CountryFlagManager.Resolution;
import org.leo.traceroute.resources.Resources;
import org.leo.traceroute.ui.whois.WhoIsPanel;

/**
 * PacketTablePanel $Id: PacketTablePanel.java 287 2016-10-30 20:35:16Z leolewis $
 *
 * @author Leo Lewis
 */
public class PacketTablePanel extends AbstractSnifferPanel implements IConfigProvider {

	/**
	 * Column enum
	 */
	public enum Column {

		NUMBER("#", Integer.class, 25, true),
		PROTOCOL(Resources.getLabel("protocol.label"), Protocol.class, 25, true),
		COUNTRY_FLAG("", String.class, 18, false),
		LOCATION(Resources.getLabel("location"), String.class, 50, true),
		TIME(Resources.getLabel("time"), String.class, 90, true),
		SRC_PORT("Src port", Integer.class, 30, true),
		DEST_PORT("Dest port", Integer.class, 30, true),
		DEST_IP("Dest IP", String.class, 80, true),
		DEST_HOSTNAME("Dest Hostname", String.class, 170, true),
		DATA_LENGTH(Resources.getLabel("data.length"), String.class, 30, true),
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
		public Object getValue(final AbstractPacketPoint point) {
			switch (this) {
			case NUMBER:
				return point.getNumber();
			case COUNTRY_FLAG:
				return point.getCountryFlag(Resolution.R16);
			case LOCATION:
				return getText(point);
			case PROTOCOL:
				return point.getProtocol();
			case DEST_IP:
				return point.getIp();
			case DEST_HOSTNAME:
				return point.getHostname();
			case DEST_PORT:
				return point.getDestPort();
			case SRC_PORT:
				return point.getSourcePort();
			case TIME:
				return point.getDate();
			case DATA_LENGTH:
				return FileUtils.byteCountToDisplaySize(point.getDataLength());
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
	private CaptureTableModel _model;

	/** Table */
	private JTable _table;

	/** Search is pending */
	private volatile boolean _running = false;
	private volatile int _previousSelectedIndex;
	private volatile boolean _selectionAdjusting = false;

	/**
	 * Constructor
	 *
	 * @param route
	 * @param networkInterfaceChooser
	 */
	public PacketTablePanel(final ServiceFactory services) {
		super(services);
		init();
	}

	/**
	 * Initialization of the panel
	 *
	 * @param networkInterfaceChooser
	 */
	@SuppressWarnings("serial")
	private void init() {
		// the table of the route points
		_model = new CaptureTableModel(_sniffer);
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
			if (!e.getValueIsAdjusting() && _table.getSelectedRow() != -1) {
				final int index = _table.convertRowIndexToModel(_table.getSelectedRow());
				if (index >= 0 && index < _sniffer.getCapture().size() && _previousSelectedIndex != index) {
					if (!_selectionAdjusting) {
						try {
							_selectionAdjusting = true;
							_sniffer.focus(_sniffer.getCapture().get(index), true);
							_previousSelectedIndex = index;
						} finally {
							_selectionAdjusting = false;
						}
					}
				}
			}
		});
		// special renderer
		for (int colIndex = 0; colIndex < _table.getColumnCount(); colIndex++) {
			final TableColumn col = _table.getColumnModel().getColumn(colIndex);
			col.setCellRenderer(new PacketCellRenderer());
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
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#startCapture()
	 */
	@Override
	public void startCapture() {
		_running = true;
		_previousSelectedIndex = -5;
		_model.fireTableDataChanged();
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#packetAdded(org.leo.traceroute.core.sniffer.PacketPoint)
	 */
	@Override
	public void packetAdded(final AbstractPacketPoint point) {
		final int selection = _table.getSelectionModel().getMaxSelectionIndex();
		_model.fireTableDataChanged();
		_table.getSelectionModel().setSelectionInterval(selection, selection);
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#captureStopped()
	 */
	@Override
	public void captureStopped() {
		_running = false;
	}

	/**
	 * @see org.leo.traceroute.core.sniffer.IPacketListener#focusPacket(org.leo.traceroute.core.sniffer.PacketPoint, boolean, boolean)
	 */
	@Override
	public void focusPacket(final AbstractPacketPoint point, final boolean isCapturing, final boolean animation) {
		if (!_selectionAdjusting) {
			_selectionAdjusting = true;
			try {
				final int index = _sniffer.getCapture().indexOf(point);
				_table.getSelectionModel().setSelectionInterval(index, index);
				_table.scrollRectToVisible(new Rectangle(_table.getCellRect(index, 0, true)));
			} finally {
				_selectionAdjusting = false;
			}
		}
	}

	/**
	 * Dispose the panel
	 */
	@Override
	public void dispose() {
		super.dispose();
		_model.dispose();
	}

	/**
	 * RouteTableModel
	 *
	 * @author Leo Lewis
	 */
	private class CaptureTableModel extends AbstractTableModel {

		/**  */
		private static final long serialVersionUID = 760575517959483510L;

		/** Sniffer (Data model) */
		private final IPacketsSniffer _sniffer;

		/**
		 * Constructor
		 *
		 * @param sniffer
		 */
		public CaptureTableModel(final IPacketsSniffer sniffer) {
			_sniffer = sniffer;
		}

		/**
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return Column.values().length;
		}

		/**
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		@Override
		public int getRowCount() {
			return _sniffer.getCapture().size();
		}

		/**
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(final int row, final int col) {
			final AbstractPacketPoint point = _sniffer.getCapture().get(row);
			return _indexToColumn.get(col).getValue(point);
		}

		@Override
		public Class<?> getColumnClass(final int modelCol) {
			return _indexToColumn.get(modelCol).getClazz();
		}

		/**
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(final int column) {
			return _indexToColumn.get(column).getLabel();
		}

		/**
		 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
		 */
		@Override
		public boolean isCellEditable(final int row, final int col) {
			if (_indexToColumn.get(col) == Column.WHO_IS) {
				return true;
			}
			return false;
		}

		/**
		 * Dispose the model
		 */
		public void dispose() {

		}
	}

	/**
	 * PacketCellRenderer
	 *
	 */
	private class PacketCellRenderer extends DefaultTableCellRenderer {

		/**  */
		private static final long serialVersionUID = 1622508565550177571L;

		/**
		 * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
		 *      java.lang.Object, boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row,
				final int column) {
			final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (c instanceof JLabel) {
				final JLabel label = (JLabel) c;
				c.setFont(new Font(c.getFont().getName(), c.getFont().getStyle(), 9));
				// bg selection color
				if (isSelected) {
					label.setBackground(new Color(250, 220, 220));
				} else {
					final int modelRow = table.convertRowIndexToModel(row);
					final AbstractPacketPoint point = _sniffer.getCapture().get(modelRow);
					// otherwise, alternate bg color
					Color bg = new Color(255, 255, 255);
					final int h = 250 + ((row % 2 == 0) ? -0 : 0);
					final int comp = 230 + ((row % 2 == 0) ? -35 : 0);
					switch (point.getProtocol()) {
					case ICMP:
						bg = new Color(h, h, comp);
						break;
					case TCP:
						bg = new Color(comp, h, comp);
						break;
					case UDP:
						bg = new Color(comp, comp, h);
						break;
					}
					label.setBackground(bg);
				}
				final Column col = _indexToColumn.get(column);
				if (col == Column.COUNTRY_FLAG) {
					label.setText("");
					label.setIcon((ImageIcon) value);
				} else if (col == Column.WHO_IS) {
					final JButton button = new JButton("?");
					if (Env.INSTANCE.getOs() == OS.win) {
						button.setBorder(null);
					}
					button.setMargin(new Insets(0, 0, 0, 0));
					button.setToolTipText(Column.WHO_IS.getLabel());
					button.setPreferredSize(new Dimension(Column.WHO_IS.getWidth(), c.getHeight()));
					return button;
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

		/**  */
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
			if (Env.INSTANCE.getOs() == OS.win) {
				button.setBorder(null);
			}
			button.addActionListener(e -> {
				final AbstractPacketPoint point = _sniffer.getCapture().get(_table.convertRowIndexToModel(row));
				WhoIsPanel.showWhoIsDialog(PacketTablePanel.this, _services, point);
				if (table.isEditing()) {
					table.getCellEditor().stopCellEditing();
				}
				_whois.clear();
			});
			return button;
		}

	}
}
