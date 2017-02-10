package fr.ortolang.diffusion.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResult {

	private List<String> hits;
	private Map<String, List<String>> aggregations;

	public SearchResult() {
		this.hits = new ArrayList<String>();
		this.aggregations = new HashMap<String, List<String>>();
	}

	public List<String> getHits() {
		return hits;
	}

	public void setHits(List<String> hits) {
		this.hits = hits;
	}

	public Map<String, List<String>> getAggregations() {
		return aggregations;
	}

	public void setAggregations(Map<String, List<String>> aggregations) {
		this.aggregations = aggregations;
	}

	public void addHit(String hit) {
		this.hits.add(hit);
	}

	public void addAggregation(String agg, String value) {
		List<String> values = this.aggregations.get(agg);
		if (values == null) {
			values = new ArrayList<String>();
		}
		values.add(value);
		this.aggregations.put(agg, values);
	}
	
}
