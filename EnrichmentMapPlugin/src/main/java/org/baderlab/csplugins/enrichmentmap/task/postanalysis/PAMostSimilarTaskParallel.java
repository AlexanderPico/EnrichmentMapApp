package org.baderlab.csplugins.enrichmentmap.task.postanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.GeneSet;
import org.baderlab.csplugins.enrichmentmap.model.PostAnalysisFilterType;
import org.baderlab.csplugins.enrichmentmap.model.SetOfGeneSets;
import org.baderlab.csplugins.enrichmentmap.task.CancellableParallelTask;
import org.baderlab.csplugins.enrichmentmap.util.DiscreteTaskMonitor;
import org.baderlab.csplugins.enrichmentmap.view.postanalysis.SigGeneSetDescriptor;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

import com.google.common.collect.Sets;


/**
 * Post Analysis.
 * Computes similarities between the signature gene sets that are being added and the 
 * existing gene sets in the enrichment map network.
 * 
 * Note: This task does not have side effects and is safely cancellable.
 * This task creates the EMSignatureDataSet and GenesetSimilarity objects, the next
 * task, CreateDiseaseSignatureNetworkTask has all the side effects of adding
 * nodes/edges to the network.
 */
public class PAMostSimilarTaskParallel extends CancellableParallelTask<List<SigGeneSetDescriptor>> implements ObservableTask {

	private final EnrichmentMap map;
	private final FilterMetricSet metrics; // key is name of em dataset
	private final Supplier<SetOfGeneSets> geneSetSupplier;
	
	private List<SigGeneSetDescriptor> results;
	
	
	/**
	 * It is assumed that all the FilterMetric objects have the same filter type (they are all Hypergometric for example).
	 */
	public PAMostSimilarTaskParallel(EnrichmentMap map, FilterMetricSet metrics, Supplier<SetOfGeneSets> geneSetSupplier) {
		this.map = map;
		this.geneSetSupplier = geneSetSupplier;
		this.metrics = metrics;
	}
	
	
	
	/**
	 * Returns immediately, need to wait on the executor to join all threads.
	 */
	@Override
	public List<SigGeneSetDescriptor> compute(TaskMonitor tm, ExecutorService executor) {
		SetOfGeneSets sigGeneSets = geneSetSupplier.get();
		DiscreteTaskMonitor taskMonitor = discreteTaskMonitor(tm, sigGeneSets.size());
		
		Set<String> enrichmentGeneSetNames = map.getAllGeneSetOfInterestNames();
		results = Collections.synchronizedList(new ArrayList<>());
		
		for(GeneSet sigGeneSet : sigGeneSets.getGeneSets().values()) {
			
			executor.execute(() -> {
				
				boolean first = true;
				double mostSimilar = Double.NaN;
				int largestOverlap = 0;
				boolean passes = false;
				
				loop:
				for(String geneSetName : enrichmentGeneSetNames) {
					for(String dataSetName : metrics.getDataSetNames()) {
						if(Thread.interrupted())
							break loop;
						
						EMDataSet dataSet = map.getDataSet(dataSetName);
						GeneSet enrGeneSet = dataSet.getGeneSetsOfInterest().getGeneSetByName(geneSetName);
						if(enrGeneSet != null) {
							
							int overlap = Sets.intersection(sigGeneSet.getGenes(), enrGeneSet.getGenes()).size();
							largestOverlap = Math.max(largestOverlap, overlap);
							
							FilterMetric metric = metrics.get(dataSetName);
							try {
								double value = metric.computeValue(enrGeneSet.getGenes(), sigGeneSet.getGenes(), null);
								
								if(first) {
									mostSimilar = value;
									passes = metric.passes(value);
									first = false;
								} else {
									mostSimilar = metric.moreSimilar(mostSimilar, value);
									passes |= metric.passes(value);
								}
							} catch(ArithmeticException e) {
								// ignore exception, but if every call to computeValue throws an exception then 'mostSimilar' will be NaN
							}
							
						}
					}
				}
				
				results.add(new SigGeneSetDescriptor(sigGeneSet, largestOverlap, mostSimilar, passes));
				taskMonitor.inc();
			});
		}
		
		return results;
	}
	
	public List<SigGeneSetDescriptor> getDescriptors() {
		return results;
	}
	
	public List<String> getPassingGeneSetNames() {
		return results.stream().filter(SigGeneSetDescriptor::passes).map(SigGeneSetDescriptor::getName).collect(Collectors.toList());
	}
	
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if(List.class.equals(type)) {
			return type.cast(results);
		}
		if(PostAnalysisFilterType.class.equals(type)) {
			return type.cast(metrics.getType());
		}
		return null;
	}
	
}
