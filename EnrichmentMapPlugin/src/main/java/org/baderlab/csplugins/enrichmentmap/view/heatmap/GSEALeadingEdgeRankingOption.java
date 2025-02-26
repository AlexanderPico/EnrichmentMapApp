package org.baderlab.csplugins.enrichmentmap.view.heatmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EMDataSet.Method;
import org.baderlab.csplugins.enrichmentmap.model.GSEAResult;
import org.baderlab.csplugins.enrichmentmap.model.GeneExpression;
import org.baderlab.csplugins.enrichmentmap.model.Rank;
import org.baderlab.csplugins.enrichmentmap.model.Ranking;
import org.baderlab.csplugins.enrichmentmap.view.heatmap.table.RankValue;
import org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil;

/**
 * The leading edge can only be computed if a single gene-set is selected.
 */
public class GSEALeadingEdgeRankingOption implements RankingOption {
	
	public static final Double DefaultScoreAtMax = -1000000.0;
	
	
	private final String rankingName;
	private final EMDataSet dataset;
	private final GSEAResult result;
	
	private double scoreAtMax;
	private int rankAtMax;
	
	
	public GSEALeadingEdgeRankingOption(EMDataSet dataset, GSEAResult result, String rankingName) {
		assert dataset.getMethod() == Method.GSEA;
		this.result = result;
		this.dataset = dataset;
		this.rankingName = rankingName;
	}
	
	@Override
	public String toString() {
		return "Ranks: " + rankingName + " - " + dataset.getName();
	}
	
	@Override
	public String getTableHeaderText() {
		String r = SwingUtil.abbreviate(rankingName, 11);
		String d = SwingUtil.abbreviate(dataset.getName(), 11);
		
		if(r.equals(d))
			return "<html>Ranks<br>" + r + "</html>";
		else
			return "<html>" + r + "<br>" + d + "</html>";
	}
	
	@Override
	public String getPdfHeaderText() {
		String r = SwingUtil.abbreviate(rankingName, 11);
		String d = SwingUtil.abbreviate(dataset.getName(), 11);
		
		if(r.equals(d))
			return "Ranks\n" + r;
		else
			return r + "\n" + d;
	}
	
	@Override
	public CompletableFuture<Optional<RankingResult>> computeRanking(Collection<Integer> genes) {
		Map<Integer,RankValue> ranks = getRanking(genes);
		return CompletableFuture.completedFuture(Optional.of(new RankingResult(ranks, true)));
	}
	
	
	public Map<Integer,RankValue> getRanking(Collection<Integer> genes) {
		initializeLeadingEdge();
		
		int topRank = getTopRank();
		boolean isNegative = isNegativeGS();
		
		Map<Integer,GeneExpression> expressions = dataset.getExpressionSets().getExpressionMatrix();
		Ranking ranking = dataset.getRanksByName(rankingName);
		
		Integer[] ranksSubset = new Integer[expressions.size()];
		HashMap<Integer, ArrayList<Integer>> rank2keys = new HashMap<Integer, ArrayList<Integer>>();
		
		int n = 0;
		Map<Integer, Rank> currentRanks = ranking.getRanking();
		for(Integer key : expressions.keySet()) {
			if (currentRanks.containsKey(key)) {
				ranksSubset[n] = currentRanks.get(key).getRank();
			} else {
				ranksSubset[n] = -1;
			}
			rank2keys.computeIfAbsent(ranksSubset[n], k -> new ArrayList<>()).add(key);
			n++;
		}
		
		Map<Integer,RankValue> result = new HashMap<>();
		
		int previous = -1;

		for (int m = 0; m < ranksSubset.length; m++) {
			//if the current gene doesn't have a rank then don't show it
			if (ranksSubset[m] == -1)
				continue;
			if (ranksSubset[m] == previous)
				continue;

			previous = ranksSubset[m];
			
			boolean significant = false;
			if (!isNegative && ranksSubset[m] <= topRank && topRank != 0 && topRank != -1)
				significant = true;
			else if (isNegative && ranksSubset[m] >= topRank && topRank != 0 && topRank != -1)
				significant = true;

			List<Integer> keys = rank2keys.get(ranksSubset[m]);
			
			for(Integer key : keys) {
				Rank rank = currentRanks.get(key);
				result.put(key, new RankValue(rank, significant));
			}
		}
		
		// Remove genes that we don't need
		result.keySet().retainAll(genes);
		
		BasicRankingOption.normalizeRanks(result);
		
		return result;
	}
	
	
	/**
	 * Collates the current selected nodes genes to represent the expression of
	 * the genes that are in all the selected nodes. and sets the expression
	 * sets (both if there are two datasets)
	 */
	private void initializeLeadingEdge() {
		scoreAtMax = result.getScoreAtMax();
		if(scoreAtMax == DefaultScoreAtMax) {
			scoreAtMax = result.getNES();
		}
		rankAtMax = result.getRankAtMax();
	}
	
	
	private int getTopRank() {
		int topRank = rankAtMax + 3; // MKTODO why?
		if(scoreAtMax < 0) {
			topRank = dataset.getRanksByName(rankingName).getMaxRank() - topRank;
		}
		return topRank;
	}

	private boolean isNegativeGS() {
		return scoreAtMax < 0;
	}

}
