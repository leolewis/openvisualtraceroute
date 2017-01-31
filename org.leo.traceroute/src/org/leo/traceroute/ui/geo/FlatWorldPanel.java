/*
Copyright (C) 2001, 2008 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
 */
package org.leo.traceroute.ui.geo;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.SkyColorLayer;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * Panel to control a flat or round world projection. The panel includes a radio
 * button to switch between flat and round globes, and a list box of map
 * projections for the flat globe. The panel is attached to a WorldWindow, and
 * changes the WorldWindow to match the users globe selection.
 *
 * @author Patrick Murris
 * @version $Id: FlatWorldPanel.java 201 2015-08-22 21:18:10Z leolewis $
 */
@SuppressWarnings("unchecked")
public class FlatWorldPanel extends JPanel {

	private final WorldWindow wwd;
	private Globe roundGlobe;
	private FlatGlobe flatGlobe;
	private JComboBox projectionCombo;

	public FlatWorldPanel(final WorldWindow wwd) {
		super(new GridLayout(0, 1, 0, 0));
		this.wwd = wwd;
		if (isFlatGlobe()) {
			this.flatGlobe = (FlatGlobe) wwd.getModel().getGlobe();
			this.roundGlobe = new Earth();
		} else {
			this.flatGlobe = new EarthFlat();
			this.roundGlobe = wwd.getModel().getGlobe();
		}
		this.makePanel();
	}

	private JPanel makePanel() {
		final JPanel controlPanel = this;
		// controlPanel.setBorder(
		// new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new
		// TitledBorder("World")));
		controlPanel.setToolTipText("Set the current projection");

		// Flat vs round buttons
		final JPanel radioButtonPanel = new JPanel(new GridLayout(0, 2, 0, 0));
		// radioButtonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4,
		// 4));
		final JRadioButton roundRadioButton = new JRadioButton("Round");
		roundRadioButton.setSelected(!isFlatGlobe());
		roundRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				projectionCombo.setEnabled(false);
				enableFlatGlobe(false);
			}
		});
		radioButtonPanel.add(roundRadioButton);
		final JRadioButton flatRadioButton = new JRadioButton("Flat");
		flatRadioButton.setSelected(isFlatGlobe());
		flatRadioButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				projectionCombo.setEnabled(true);
				enableFlatGlobe(true);
			}
		});
		radioButtonPanel.add(flatRadioButton);
		final ButtonGroup group = new ButtonGroup();
		group.add(roundRadioButton);
		group.add(flatRadioButton);

		// Projection combo
		final JPanel comboPanel = new JPanel(new GridLayout(0, 2, 0, 0));
		comboPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		comboPanel.add(new JLabel("Projection:"));
		this.projectionCombo = new JComboBox(
				new String[] { "Orthographic", "Mercator", "Lat-Lon", "Modified Sin.", "Sinusoidal" });
		// this.projectionCombo.setEnabled(isFlatGlobe());
		this.projectionCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent actionEvent) {
				if (projectionCombo.getSelectedIndex() == 0) {
					enableFlatGlobe(false);
				} else {
					enableFlatGlobe(true);
					updateProjection();
				}
			}
		});
		comboPanel.add(this.projectionCombo);

		controlPanel.add(comboPanel);
		return controlPanel;
	}

	// Update flat globe projection
	private void updateProjection() {
		if (!isFlatGlobe()) {
			return;
		}

		// Update flat globe projection
		this.flatGlobe.setProjection(this.getProjection());
		this.wwd.redraw();
	}

	private String getProjection() {
		final String item = (String) projectionCombo.getSelectedItem();
		if (item.equals("Mercator")) {
			return FlatGlobe.PROJECTION_MERCATOR;
		} else if (item.equals("Sinusoidal")) {
			return FlatGlobe.PROJECTION_SINUSOIDAL;
		} else if (item.equals("Modified Sin.")) {
			return FlatGlobe.PROJECTION_MODIFIED_SINUSOIDAL;
		}
		// Default to lat-lon
		return FlatGlobe.PROJECTION_LAT_LON;
	}

	public boolean isFlatGlobe() {
		return wwd.getModel().getGlobe() instanceof FlatGlobe;
	}

	public void enableFlatGlobe(final boolean flat) {
		if (isFlatGlobe() == flat) {
			return;
		}

		if (!flat) {
			// Switch to round globe
			wwd.getModel().setGlobe(roundGlobe);
			// Switch to orbit view and update with current position
			final FlatOrbitView flatOrbitView = (FlatOrbitView) wwd.getView();
			final BasicOrbitView orbitView = new BasicOrbitView();
			orbitView.setCenterPosition(flatOrbitView.getCenterPosition());
			orbitView.setZoom(flatOrbitView.getZoom());
			orbitView.setHeading(flatOrbitView.getHeading());
			orbitView.setPitch(flatOrbitView.getPitch());
			wwd.setView(orbitView);
			// Change sky layer
			final LayerList layers = wwd.getModel().getLayers();
			for (int i = 0; i < layers.size(); i++) {
				if (layers.get(i) instanceof SkyColorLayer) {
					layers.set(i, new SkyGradientLayer());
				}
			}
		} else {
			// Switch to flat globe
			wwd.getModel().setGlobe(flatGlobe);
			flatGlobe.setProjection(this.getProjection());
			// Switch to flat view and update with current position
			final BasicOrbitView orbitView = (BasicOrbitView) wwd.getView();
			final FlatOrbitView flatOrbitView = new FlatOrbitView();
			flatOrbitView.setCenterPosition(orbitView.getCenterPosition());
			flatOrbitView.setZoom(orbitView.getZoom());
			flatOrbitView.setHeading(orbitView.getHeading());
			flatOrbitView.setPitch(orbitView.getPitch());
			wwd.setView(flatOrbitView);
			// Change sky layer
			final LayerList layers = wwd.getModel().getLayers();
			for (int i = 0; i < layers.size(); i++) {
				if (layers.get(i) instanceof SkyGradientLayer) {
					layers.set(i, new SkyColorLayer());
				}
			}
		}

		wwd.redraw();
	}

}
