package fr.ortolang.diffusion.api.search;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Results object containing all informations. 
 */
public class SearchFullResultsRepresentation {

	private String id;
	private String type;
	private Float score;
	private Map<String, Object> source;
	private Set<SearchHighlightFieldRepresentation> highlightFields;
	
	public SearchFullResultsRepresentation() {
		highlightFields = new HashSet<SearchHighlightFieldRepresentation>();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Float getScore() {
		return score;
	}
	public void setScore(Float score) {
		this.score = score;
	}
	public Map<String, Object> getSource() {
		return source;
	}
	public void setSource(Map<String, Object> source) {
		this.source = source;
	}
	public Set<SearchHighlightFieldRepresentation> getHighlightFields() {
		return highlightFields;
	}
	public void setHighlightFields(Set<SearchHighlightFieldRepresentation> highlightFields) {
		this.highlightFields = highlightFields;
	}
	public void addHighlightField(SearchHighlightFieldRepresentation shl) {
		this.highlightFields.add(shl);
	}
	
	
}
