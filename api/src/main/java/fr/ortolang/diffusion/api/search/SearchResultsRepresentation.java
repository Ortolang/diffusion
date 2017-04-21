package fr.ortolang.diffusion.api.search;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.search.SearchResult;

@XmlRootElement(name = "result")
public class SearchResultsRepresentation {

    @XmlAttribute
    private Object[] hits;
    private long totalHits;
    private Map<String, List<String>> aggregations;
	
	public SearchResultsRepresentation() {
	}

	public static SearchResultsRepresentation fromSearchResult (SearchResult result) {
		SearchResultsRepresentation representation = new SearchResultsRepresentation();
		representation.setHits(result.getHits());
		representation.setTotalHits(result.getTotalHits());
		representation.setAggregations(result.getAggregations());
		return representation;
	}

	public Object[] getHits() {
		return hits;
	}

	public void setHits(Object[] hits) {
		this.hits = hits;
	}

	public Map<String, List<String>> getAggregations() {
		return aggregations;
	}

	public void setAggregations(Map<String, List<String>> aggregations) {
		this.aggregations = aggregations;
	}

	public long getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(long totalHits) {
		this.totalHits = totalHits;
	}
}
