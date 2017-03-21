package fr.ortolang.diffusion.api.search;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.search.SearchResult;

@XmlRootElement(name = "result")
public class SearchResultRepresentation {

    @XmlAttribute
    private List<String> hits;
    private long totalHits;
    private Map<String, List<String>> aggregations;
	
	public SearchResultRepresentation() {
	}

	public static SearchResultRepresentation fromSearchResult (SearchResult result) {
		SearchResultRepresentation representation = new SearchResultRepresentation();
		representation.setHits(result.getHits());
		representation.setTotalHits(result.getTotalHits());
		representation.setAggregations(result.getAggregations());
		return representation;
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

	public long getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(long totalHits) {
		this.totalHits = totalHits;
	}
}
