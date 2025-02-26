package org.baderlab.csplugins.enrichmentmap.view.creation.genemania;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;

import org.baderlab.csplugins.enrichmentmap.view.util.dialog.CardDialogPage;
import org.baderlab.csplugins.enrichmentmap.view.util.dialog.CardDialogParameters;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class GenemaniaDialogParameters implements CardDialogParameters {

	public static final String ORGANISM_COLUMN_DEF  ="organism";
	public static final String ANNOTATIONS_COLUMN_DEF  = "annotations";
	public static final String GENE_NAME_COLUMN_DEF  = "gene name";
	public static final String ANNOTATION_NAME_COLUMN_DEF  = "annotation name";
	
	@Inject private Provider<GenemaniaDialogPage> genemaniaDialogPage;
	
	@Override
	public String getTitle() {
		return "Create Enrichment Map from Genemania Network";
	}

	@Override
	public List<CardDialogPage> getPages() {
		return Arrays.asList(genemaniaDialogPage.get());
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(820, 320);
	}
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(650, 320);
	}
	
	@Override
	public AbstractButton[] getExtraButtons() {
		JButton resetButton = new JButton("Reset");
		resetButton.setActionCommand("reset");
		
		return new JButton[] { resetButton };
	}
}
