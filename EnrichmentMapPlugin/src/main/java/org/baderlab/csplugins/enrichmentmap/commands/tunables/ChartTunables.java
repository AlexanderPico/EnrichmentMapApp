package org.baderlab.csplugins.enrichmentmap.commands.tunables;

import static org.baderlab.csplugins.enrichmentmap.style.ChartData.DATA_SET;
import static org.baderlab.csplugins.enrichmentmap.style.ChartType.DATASET_PIE;
import static org.baderlab.csplugins.enrichmentmap.style.ChartType.HEAT_MAP;
import static org.baderlab.csplugins.enrichmentmap.style.ChartType.HEAT_STRIPS;
import static org.baderlab.csplugins.enrichmentmap.style.ChartType.RADIAL_HEAT_MAP;

import org.baderlab.csplugins.enrichmentmap.style.ChartData;
import org.baderlab.csplugins.enrichmentmap.style.ChartType;
import org.baderlab.csplugins.enrichmentmap.style.ColorScheme;
import org.baderlab.csplugins.enrichmentmap.util.TaskUtil;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

public class ChartTunables {

	@Tunable(description = "Sets the chart data to show.")
	public ListSingleSelection<String> data;
	
	@Tunable(description = "Sets the chart type.")
	public ListSingleSelection<String> type;
	
	@Tunable(description = "Sets the chart colors.")
	public ListSingleSelection<String> colors;
	
	@Tunable
	public boolean showChartLabels = true;
	
	
	public ChartTunables() {
		data   = TaskUtil.lssFromEnumWithDefault(ChartData.values(), ChartData.NES_VALUE);
		type   = TaskUtil.lssFromEnum(RADIAL_HEAT_MAP, HEAT_STRIPS, HEAT_MAP); // don't include DATASET_PIE
		colors = TaskUtil.lssFromEnum(ColorScheme.values());
	}
	
	
	public ChartData getChartData() {
		 return ChartData.valueOf(data.getSelectedValue());
	}
	
	public ChartType getChartType() {
		return getChartData() == DATA_SET ? DATASET_PIE : ChartType.valueOf(type.getSelectedValue());
	}
	
	public ColorScheme getColorScheme() {
		return ColorScheme.valueOf(colors.getSelectedValue());
	}
	
	public boolean showChartLabels() {
		return showChartLabels;
	}
}
