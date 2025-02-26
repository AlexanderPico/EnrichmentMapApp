package org.baderlab.csplugins.enrichmentmap.task.string;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.baderlab.csplugins.enrichmentmap.task.tunables.GeneListTunable;
import org.baderlab.csplugins.enrichmentmap.util.TaskUtil;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.json.JSONResult;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.BoundedInteger;
import org.cytoscape.work.util.ListSingleSelection;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class QueryStringTask extends AbstractTask {
	
	public static final String STRING_NAMESPACE = "string";
	public static final String STRING_SPECIES_COMMAND = "list species";
	public static final String STRING_SEARCH_COMMAND = "protein query";
	
	@Tunable(description = "Genes:")
	public GeneListTunable geneList;
	
	@Tunable(description = "Species:")
	public ListSingleSelection<STRSpecies> organisms;
	
	@Tunable(description = "Confidence Cutoff:", params = "slider=true", format = "0.00")
	public BoundedDouble cutoff = new BoundedDouble(0.0, 0.4, 1.0, false, false);
	
	@Tunable(description = "Max Additional Interactors:", params = "slider=true")
	public BoundedInteger limit = new BoundedInteger(0, 0, 100, false, false);
	
	private Long result;
	
	private static long lastTaxonomyId = 9606; // H.sapiens

	@Inject private CommandExecutorTaskFactory commandExecutorTaskFactory;
	
	public static interface Factory {
		QueryStringTask create(GeneListTunable geneListTunable);
	}
	
	@AssistedInject
	public QueryStringTask(@Assisted GeneListTunable geneListTunable) {		
		this.geneList = geneListTunable;
		this.organisms = new ListSingleSelection<>();
	}
	
	@ProvidesTitle
	public String getTitle() {
		return "STRING Protein Query";
	}
	
	public void updatetOrganisms(List<STRSpecies> orgValues) {
		organisms.setPossibleValues(orgValues);
		
		if (!orgValues.isEmpty()) {
			organisms.setSelectedValue(orgValues.get(0));
			
			for (STRSpecies org : orgValues) {
				if (org.getTaxonomyId() == lastTaxonomyId) {
					organisms.setSelectedValue(org);
					break;
				}
			}
		}
	}
	
	@Override
	public void run(TaskMonitor tm) throws Exception {
		if (organisms.getSelectedValue() != null) {
			tm.setTitle("EnrichmentMap");
			tm.setStatusMessage("Querying STRING...");
			
			String query = String.join(",", geneList.getSelectedGenes());
			
			Map<String, Object> args = new HashMap<>();
			args.put("taxonID", "" + organisms.getSelectedValue().getTaxonomyId());
			args.put("query", query);
			args.put("cutoff", "" + cutoff.getValue());
			args.put("limit", "" + limit.getValue());
			
			TaskIterator ti = commandExecutorTaskFactory.createTaskIterator(
					STRING_NAMESPACE, STRING_SEARCH_COMMAND, args, TaskUtil.taskFinished(task -> {
				
						if (task.getResultClasses().contains(JSONResult.class)) {
							JSONResult json = ((ObservableTask) task).getResults(JSONResult.class);
							
							if (json != null && json.getJSON() != null) {
								Gson gson = new Gson();
								Map<?,?> map = gson.fromJson(json.getJSON(), Map.class);
								Number suid = (Number) map.get("SUID");
								result = suid != null ? suid.longValue() : null;
							} else {
								throw new RuntimeException("Unexpected error when getting search results from STRING.");
							}
						}
			}));
			insertTasksAfterCurrentTask(ti);
			
			// Save this as the default organism for next time
			lastTaxonomyId = organisms.getSelectedValue().getTaxonomyId();
		}
	}

	/**
	 * @return The SUID of the created {@link CyNetwork}
	 */
	public Long getResult() {
		return result;
	}
}
