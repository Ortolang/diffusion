package fr.ortolang.diffusion.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.search.SearchHit;

public class SearchResult {

	private SearchHit[] hits;
	private long totalHits;
	private Map<String, List<String>> aggregations;
	private long tookInMillis;

	public SearchResult() {
		this.aggregations = new HashMap<String, List<String>>();
		this.setTotalHits(0);
		this.setTookInMillis(0);
	}

	public SearchHit[] getHits() {
		return hits;
	}
	
	public SearchHit getHit(int index) {
		if (index<0 || index >= hits.length) {
			return null;
		}
		return hits[index];
	}
	
	public int countHits() {
		return hits.length;
	}
	
	public String[] getSourceOfHits() {
		String[] hits = new String[this.hits.length];
		for (int iHit = 0; iHit < this.hits.length; iHit++) {
			hits[iHit] = this.hits[iHit].getSourceAsString();
		}
		return hits;
	}

	public void setHits(SearchHit[] hits) {
		this.hits = hits;
	}

	public Map<String, List<String>> getAggregations() {
		return aggregations;
	}

	public void setAggregations(Map<String, List<String>> aggregations) {
		this.aggregations = aggregations;
	}

	public void addAggregation(String agg, String value) {
		List<String> values = this.aggregations.get(agg);
		if (values == null) {
			values = new ArrayList<String>();
		}
		values.add(value);
		this.aggregations.put(agg, values);
	}

	public long getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(long totalHits) {
		this.totalHits = totalHits;
	}

	public long getTookInMillis() {
		return tookInMillis;
	}

	public void setTookInMillis(long tookMili) {
		this.tookInMillis = tookMili;
	}

}
