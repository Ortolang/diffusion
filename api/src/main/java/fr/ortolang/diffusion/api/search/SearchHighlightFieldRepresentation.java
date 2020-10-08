package fr.ortolang.diffusion.api.search;

import java.util.HashSet;
import java.util.Set;

public class SearchHighlightFieldRepresentation {

	private String field;
	private Set<String> highlights;
	
	public SearchHighlightFieldRepresentation() {
		highlights = new HashSet<String>();
	}
	
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public Set<String> getHighlights() {
		return highlights;
	}
	public void setHighlights(Set<String> highlights) {
		this.highlights = highlights;
	}
	public void addHighlight(String hl) {
		this.highlights.add(hl);
	}
}
