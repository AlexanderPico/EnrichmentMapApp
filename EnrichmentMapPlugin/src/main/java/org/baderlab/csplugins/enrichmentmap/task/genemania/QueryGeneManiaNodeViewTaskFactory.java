package org.baderlab.csplugins.enrichmentmap.task.genemania;

import java.util.List;

import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.style.EMStyleBuilder.Columns;
import org.baderlab.csplugins.enrichmentmap.task.tunables.GeneListTunable;
import org.baderlab.csplugins.enrichmentmap.view.heatmap.GSEALeadingEdgeRankingOption;
import org.baderlab.csplugins.enrichmentmap.view.heatmap.RankingOptionFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

import com.google.inject.Inject;

public class QueryGeneManiaNodeViewTaskFactory implements NodeViewTaskFactory {

	@Inject private GeneManiaTaskFactory geneManiaTaskFactory;
	@Inject private EnrichmentMapManager emManager;
	@Inject private RankingOptionFactory rankingOptionFactory;
	
	@Override
	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView networkView) {
		CyNode node = nodeView.getModel();
		CyNetwork network = networkView.getModel();
		EnrichmentMap map = emManager.getEnrichmentMap(network.getSUID());
		
		CyRow row = network.getRow(node);
		List<String> genes = Columns.NODE_GENES.get(row, map.getParams().getAttributePrefix());
		
		List<GSEALeadingEdgeRankingOption> rankOptions = rankingOptionFactory.getGSEADataSetSetRankOptions(map);
		
		
		GeneListTunable geneListTunable = new GeneListTunable(map, genes, null, rankOptions);
		TaskIterator tasks = geneManiaTaskFactory.createTaskIterator(geneListTunable);
		return tasks;
	}

	@Override
	public boolean isReady(View<CyNode> nodeView, CyNetworkView networkView) {
		return emManager.isEnrichmentMap(networkView);
	}

}
