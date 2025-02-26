/**
 **                       EnrichmentMap Cytoscape Plugin
 **
 ** Copyright (c) 2008-2009 Bader Lab, Donnelly Centre for Cellular and Biomolecular 
 ** Research, University of Toronto
 **
 ** Contact: http://www.baderlab.org
 **
 ** Code written by: Ruth Isserlin
 ** Authors: Daniele Merico, Ruth Isserlin, Oliver Stueker, Gary D. Bader
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** (at your option) any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** University of Toronto
 ** has no obligations to provide maintenance, support, updates, 
 ** enhancements or modifications.  In no event shall the
 ** University of Toronto
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** University of Toronto
 ** has been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **
 **/

// $Id$
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
// $HeadURL$

package org.baderlab.csplugins.enrichmentmap.parsers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EMDataSet.Method;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.Rank;
import org.baderlab.csplugins.enrichmentmap.model.Ranking;
import org.baderlab.csplugins.enrichmentmap.util.NullTaskMonitor;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskMonitor.Level;

/**
 * Created by User: risserlin Date: May 1, 2009 Time: 9:10:22 AM
 * <p>
 * Task to parse ranks file <br>
 * There are multiple potential rank file formats: <br>
 * GSEA input rnk file - a two column file with genes and their specified rank
 * represented as a double, commented lines have a # at the line start. GSEA
 * output rank files (xls file) - a five column file with genes and specified
 * rank but also have three bland columns.
 *
 */
public class RanksFileReaderTask extends AbstractTask implements ObservableTask {

	public enum UnsortedRanksStrategy {
		LOG_WARNING,
		FAIL_IMMEDIATELY;
	}
	
	private final String RankFileName;
	private final EMDataSet dataset;
	private String ranks_name;
	
	private final UnsortedRanksStrategy unsortedRanksStrategy;


	//distinguish between load from enrichment map input panel and heatmap interface
	private boolean loadFromHeatmap = false;
	private boolean sorted = true;
	


	public RanksFileReaderTask(String rankFileName, EMDataSet dataset, String ranks_name, boolean loadFromHeatmap, UnsortedRanksStrategy unsortedRanksStrategy) {
		this.RankFileName = rankFileName;
		this.ranks_name = ranks_name;
		this.dataset = dataset;
		this.loadFromHeatmap = loadFromHeatmap;
		this.unsortedRanksStrategy = unsortedRanksStrategy;
	}

	
	/**
	 * parse the rank file
	 */
	public void parse(TaskMonitor taskMonitor) throws IOException {
		taskMonitor = NullTaskMonitor.check(taskMonitor);
		
		List<String> lines = LineReader.readLines(RankFileName);
		
		int lineNumber = 0;
		int currentProgress = 0;
		int maxValue = lines.size();
		taskMonitor.setStatusMessage("Parsing Rank file - " + maxValue + " rows");

		EnrichmentMap map = dataset.getMap();
		// we don't know the number of scores in the rank file yet, but it can't be more than the number of lines.
		Double[] score_collector = new Double[lines.size()];

		boolean gseaDefinedRanks = false;

		Map<Integer,Rank> ranks = new HashMap<>();

		/*
		 * there are two possible Rank files: If loaded through the rpt file the
		 * file is the one generated by GSEA and will have 5 columns (name,
		 * description, empty,empty,score) If the user loaded it through the
		 * generic of specifying advanced options then it will 2 columns
		 * (name,score). The score in either case should be a double and the
		 * name a string so check for either option.
		 */

		int nScores = 0; //number of found scores
		double prevScore = Double.MAX_VALUE;
		boolean sorted = true;
		
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			//check to see if the line is commented out and should be ignored.
			if(line.startsWith("#")) {
				// look for ranks_name in comment line e.g.: "# Ranks Name : My Ranks"
				if(Pattern.matches("^# *Ranks[ _-]?Name *:.+", line)) {
					this.ranks_name = line.split(":", 2)[1];
					while(this.ranks_name.startsWith(" "))
						this.ranks_name = this.ranks_name.substring(1);
				}
				//ignore comment line
				continue;
			}

			String[] tokens = line.split("\t");

			String name = tokens[0].toUpperCase();
			double score = 0;

			//if there are 2, 3 or 5 columns in the data then the rank is the last column
			if(tokens.length == 2 || tokens.length == 3 || tokens.length == 5) {
				//ignore rows where the expected rank value is not a valid double
				try {
					//gseaDefinedRanks = true;
					score = Double.parseDouble(tokens[tokens.length-1]);
				} catch(NumberFormatException nfe) {
					if(lineNumber == 0) {
						lineNumber++;
						continue;
					} else
						throw new IllegalThreadStateException("rank value for" + tokens[0] + "is not a valid number");
				}
				nScores++;
			} else {
				System.out.println("Invalid number of tokens line of Rank File (should be 5 or 2)");
				//skip invalid line
				continue;
			}

			if((tokens.length == 5 || tokens.length == 3) || (dataset.getMethod() == Method.GSEA && !loadFromHeatmap))
				gseaDefinedRanks = true;

			//add score to array of scores
			score_collector[nScores - 1] = score;

			//check to see if the gene is in the genelist
			Integer genekey = map.getHashFromGene(name);
			if(genekey != null) {
				Rank current_ranking;
				//if their were 5 tokens in the rank file then the assumption
				//is that this is a GSEA rank file and the order of the scores
				//is indicative of the rank
				//TODO: need a better way of defining GSEA or user defined rank files.
				//Ticket #189 if the user uses the rnk file from the of the edb directory insteaad of 5 column
				// rank file from the main directory (as entered by rpt load) the leading edge
				// is calculated wrong because it only has 2 columns but still needs to have the ranks defined
				// based on the order of the scores.
				// Making the assumption that all rank files loaded for GSEA results from EM input panel are leading
				// edge compatible files.
				if((tokens.length == 5 || tokens.length == 3) || (dataset.getMethod() == Method.GSEA && !loadFromHeatmap)) {
					current_ranking = new Rank(name, score, nScores);
				} else {
					current_ranking = new Rank(name, score);
				}
				ranks.put(genekey, current_ranking);
			}

			// Calculate Percentage.  This must be a value between 0..100.
			int percentComplete = (int) (((double) currentProgress / maxValue) * 100);
			taskMonitor.setProgress(percentComplete);
			currentProgress++;
			
			if(score > prevScore) {
				sorted = false;
			}
			prevScore = score;
		}

		//the none of the genes are in the gene list
		if(ranks.isEmpty()) {
			String message = "The genes in the provided rank file were not found in the enrichments file or in the GMT file.";
			throw new RanksGeneMismatchException(RankFileName, message);
		}
		
		if(!sorted) {
			String message = "The ranks file '" + RankFileName + "' is not sorted from greatest to least.";
			if(unsortedRanksStrategy == UnsortedRanksStrategy.LOG_WARNING) {
				taskMonitor.showMessage(Level.WARN, message);
			} else {
				throw new RanksUnsortedException(RankFileName, message);
			}
		}

		//remove Null values from collector
		Double[] sort_scores = new Double[nScores];
		double[] scores = new double[nScores];
		for(int i = 0; i < nScores; i++) {
			sort_scores[i] = score_collector[i];
			scores[i] = (double) score_collector[i];
		}

		//after we have loaded in all the scores, sort the score to compute ranks
		//create hash of scores to ranks.
		HashMap<Double, Integer> score2ranks = new HashMap<Double, Integer>();
		//sorts the array in descending order
		Arrays.sort(sort_scores, Collections.reverseOrder());

		//check to see if they are p-values (if the values are between -1 and 1 , for a signed pvalue)
		//this will actually give a weird sorting behaviour if the scores are actually not p-values and
		//just signed statistics for instance as it will sort them in the opposite direction.
		if(sort_scores[0] <= 1 && sort_scores[sort_scores.length - 1] >= -1)
			Arrays.sort(sort_scores);

		for(int j = 0; j < sort_scores.length; j++) {
			//check to see if this score is already enter
			if(!score2ranks.containsKey(sort_scores[j]))
				score2ranks.put(sort_scores[j], j);
		}

		//update scores Hash to contain the ranks as well.
		//only update the ranks if we haven't already defined them using order of scores in file
		if(!gseaDefinedRanks) {
			for(Iterator<Integer> k = ranks.keySet().iterator(); k.hasNext();) {
				Integer gene_key = k.next();
				Rank current_ranking = ranks.get(gene_key);
				Integer rank = score2ranks.get(current_ranking.getScore());
				current_ranking.setRank(rank);
				// update rank2gene and gene2score as well
			}
		}
		
		//check to see if some of the dataset genes are not in this rank file
//		Set<Integer> current_genes = dataset.getDataSetGenes();
//
//		Set<Integer> current_ranks = ranks.keySet();
//
//		//intersect the genes with the ranks.  only retain the genes that have ranks.
//		Set<Integer> intersection = new HashSet<>(current_genes);
//		intersection.retainAll(current_ranks);
//
//		//see if there more genes than there are ranks
//		if(!(intersection.size() == current_genes.size())) {
//			//JOptionPane.showMessageDialog(Cytoscape.getDesktop(),"Ranks for some of the genes/proteins listed in the expression file are missing. \n These genes/proteins will be excluded from ranked listing in the heat map.");
//
//		}

		//create a new Ranking
		Ranking new_ranking = new Ranking();
		ranks.forEach(new_ranking::addRank);

		//add the Ranks to the expression file ranking
		dataset.addRanks(ranks_name, new_ranking);

	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Parsing Ranks file");
		parse(taskMonitor);
	}

	public boolean isSorted() {
		return sorted;
	}
	
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return null;
	}
}
