package org.baderlab.csplugins.enrichmentmap.commands;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;

import org.baderlab.csplugins.enrichmentmap.commands.tunables.FilterTunables;
import org.baderlab.csplugins.enrichmentmap.model.DataSetParameters;
import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters;
import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters.GreatFilter;
import org.baderlab.csplugins.enrichmentmap.resolver.DataSetResolverTask;
import org.baderlab.csplugins.enrichmentmap.task.CreateEMNetworkTask;
import org.baderlab.csplugins.enrichmentmap.task.CreateEnrichmentMapTaskFactory;
import org.baderlab.csplugins.enrichmentmap.util.DelegatingTaskMonitor;
import org.baderlab.csplugins.enrichmentmap.util.NullTaskMonitor;
import org.baderlab.csplugins.enrichmentmap.util.SimpleSyncTaskManager;
import org.baderlab.csplugins.enrichmentmap.util.TaskUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ContainsTunables;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;

import com.google.inject.Inject;


public class MastermapCommandTask extends AbstractTask implements ObservableTask {

	@Tunable(required=true, description="Absolute path to a folder containing the data files. "
			+ "The files will be scanned and automatically grouped into data sets. Sub-folders will be scanned up to one level deep.")
	public File rootFolder;
	
	@Tunable(description="Absolute path to a GMT file that will be used for every data set. Overrides other GMT files.")
	public File commonGMTFile;
	
	@Tunable(description="Absolute path to an expression file that will be used for every data set. Overrides other expression files.")
	public File commonExpressionFile;
	
	@Tunable(description="Absolute path to a class file that will be used for every data set. Overrides other class files.")
	public File commonClassFile;
	
	@ContainsTunables
	@Inject
	public FilterTunables filterArgs;
	
	@Tunable(description="A glob-style path filter. Sub-folders inside the root folder that do not match the pattern will be ignored. "
			+ "For more details on syntax see https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-")
	public String pattern;
	
	
	@Tunable(description="GREAT results can be filtered by one of: HYPER (Hypergeometric p-value), BINOM (Binomial p-value), BOTH, EITHER")
	public ListSingleSelection<String> greatFilter;
	
	
	
	@Inject private CreateEnrichmentMapTaskFactory.Factory createEMTaskFactory;
	
	
	private Long[] resultNetworkSUID = { null };
	
	
	public MastermapCommandTask() {
		greatFilter = TaskUtil.lssFromEnum(GreatFilter.values());
	}
	
	
	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm = NullTaskMonitor.check(tm);
		tm.setStatusMessage("Running EnrichmentMap Mastermap Task");
		
		if(rootFolder == null || !rootFolder.exists()) {
			throw new IllegalArgumentException("rootFolder is invalid: " + rootFolder);
		}
		
		var delegatingTM = new DelegatingTaskMonitor(tm) {
			@Override
			public void setStatusMessage(String message) {
				// Kind of a hack, used to suppress the flood of status messages from ComputeSimilarityTaskParallel
				if(message != null && message.startsWith("Computing Geneset Similarity:"))
					return;
				super.setStatusMessage(message);
			}
		};
		
		runTask(delegatingTM);
		
		tm.setStatusMessage("Done");
	}
	
	
	private void runTask(TaskMonitor tm) {
		// Scan root folder (note: throws exception if no data sets were found)
		var resolverTask = new DataSetResolverTask(rootFolder);
		
		if(pattern != null) {
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
			resolverTask.setPathMatcher(matcher);
		}
		
		var taskManager = new SimpleSyncTaskManager(tm);
		
		taskManager.execute(new TaskIterator(resolverTask)); // blocks
		var dataSets = resolverTask.getDataSetResults();
		
		setCommonFiles(dataSets);

		tm.setStatusMessage("resolved " + dataSets.size() + " data sets");
		for(var params : dataSets) {
			tm.setStatusMessage(params.toString());
		}
		tm.setStatusMessage(filterArgs.toString());
		
		EMCreationParameters params = filterArgs.getCreationParameters();
		
		var filter = GreatFilter.valueOf(greatFilter.getSelectedValue());
		params.setGreatFilter(filter);

		if(filterArgs.networkName != null && !filterArgs.networkName.trim().isEmpty()) {
			params.setNetworkName(filterArgs.networkName);
		}
		
		var tasks = createEMTaskFactory.create(params, dataSets).createTaskIterator();
		
		taskManager.execute(tasks, TaskUtil.taskFinished(CreateEMNetworkTask.class,
			(task)  -> resultNetworkSUID[0] = task.getResults(Long.class),
			(error) -> tm.showMessage(Level.ERROR, "Failed to create EnrichmentMap network. Check task log for details.")
		));
	}
	
	
	private void setCommonFiles(List<DataSetParameters> dataSets) {
		// Common gmt and expression files
		// Overwrite all the expression files if the common file has been provided
		if(commonExpressionFile != null) {
			if(!commonExpressionFile.canRead()) {
				throw new IllegalArgumentException("Cannot read commonExpressionFile: " + commonExpressionFile);
			}
			for(var dsp : dataSets) {
				dsp.getFiles().setExpressionFileName(commonExpressionFile.getAbsolutePath());
			}
		}
		
		// Overwrite all the gmt files if a common file has been provided
		if(commonGMTFile != null) {
			if(!commonGMTFile.canRead()) {
				throw new IllegalArgumentException("Cannot read commonGMTFile: " + commonGMTFile);
			}
			for(var dsp : dataSets) {
				dsp.getFiles().setGMTFileName(commonGMTFile.getAbsolutePath());
			}
		}
		
		// Overwrite all the gmt files if a common file has been provided
		if(commonClassFile != null) {
			if(!commonClassFile.canRead()) {
				throw new IllegalArgumentException("Cannot read commonClassFile: " + commonClassFile);
			}
			for(var dsp : dataSets) {
				dsp.getFiles().setClassFile(commonClassFile.getAbsolutePath());
			}
		}
	}
	
	
	@Override
	public List<Class<?>> getResultClasses() {
		return List.of(String.class, Long.class);
	}
	
	@Override
	public <R> R getResults(Class<? extends R> type) {
		Long suid = resultNetworkSUID[0];
		if(String.class.equals(type)) {
			return type.cast(String.valueOf(suid));
		}
		if(Long.class.equals(type)) {
			return type.cast(suid);
		}
		return null;
	}
	
}
