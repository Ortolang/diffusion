package fr.ortolang.diffusion.api.search;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import fr.ortolang.diffusion.search.SearchResult;

@XmlRootElement(name = "result")
public class SearchResultRepresentation {

    private long totalHits;
	private long tookInMillis;
    private Object[] hits;
    private Map<String, List<String>> aggregations;
	
	public SearchResultRepresentation() {
	}

	public static SearchResultRepresentation fromSearchResult (SearchResult result) {
		return fromSearchResult(result, true);
	}
	
	public static SearchResultRepresentation fromSearchResult (SearchResult result, boolean source) {
		SearchResultRepresentation representation = new SearchResultRepresentation();
		
		if (source) {
			representation.setHits(result.getSourceOfHits());
		} else {
			Set<SearchFullResultsRepresentation> fullResults = new HashSet<SearchFullResultsRepresentation>();
			for (SearchHit hit : result.getHits()) {
				SearchFullResultsRepresentation fullResult = new SearchFullResultsRepresentation();
				fullResult.setId(hit.getId());
				fullResult.setType(hit.getType());
				fullResult.setScore(hit.getScore());
				fullResult.setSource(hit.getSource());
				for(Entry<String, HighlightField> field : hit.getHighlightFields().entrySet()) {
					SearchHighlightFieldRepresentation hl = new SearchHighlightFieldRepresentation();
					hl.setField(field.getKey());
					for(Text text : field.getValue().fragments()) {
						hl.addHighlight(text.string());
					}
					fullResult.addHighlightField(hl);
				}
				fullResults.add(fullResult);
			}
			representation.setHits(fullResults.toArray());
			
		}
		representation.setTotalHits(result.getTotalHits());
		representation.setAggregations(result.getAggregations());
		representation.setTookInMillis(result.getTookInMillis());
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

	public long getTookInMillis() {
		return tookInMillis;
	}

	public void setTookInMillis(long tookInMillis) {
		this.tookInMillis = tookInMillis;
	}
}
