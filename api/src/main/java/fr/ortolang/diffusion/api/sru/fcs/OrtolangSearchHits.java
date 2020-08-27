package fr.ortolang.diffusion.api.sru.fcs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import fr.ortolang.diffusion.core.indexing.DataObjectContentIndexableContent;
import fr.ortolang.diffusion.search.SearchResult;
import fr.ortolang.diffusion.store.handle.HandleStoreService;

public class OrtolangSearchHits {

	private List<OrtolangSearchHit> hits;
	private long totalHits;
	
	public OrtolangSearchHits(List<OrtolangSearchHit> hits, long totalHits) {
		this.hits = hits;
		this.totalHits = totalHits;
	}

	public OrtolangSearchHit getHit(int index) {
		return hits.get(index);
	}
	
	public List<OrtolangSearchHit> getHits() {
		return hits;
	}

	public void setHits(List<OrtolangSearchHit> hits) {
		this.hits = hits;
	}

	public long getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(long totalHits) {
		this.totalHits = totalHits;
	}
	
	public static OrtolangSearchHits valueOf(SearchResult result) {
		int ioHit = 1;
		List<OrtolangSearchHit> oHits = new ArrayList<OrtolangSearchHit>();
		for (SearchHit hit : result.getHits()) {
			for (Entry<String, HighlightField> field : hit.getHighlightFields().entrySet()) {
				if (field.getKey().equals(DataObjectContentIndexableContent.CONTENT_FIELD)) {
					for(Text text : field.getValue().fragments()) {        			
						oHits.add(new OrtolangSearchHit(String.valueOf(ioHit++), extractPid(hit), extractName(hit), text.string()));
					}
				}
			}
		}
		return new OrtolangSearchHits(oHits, result.getTotalHits());
	}
	

	/**
	 * Extracts the PID of the result hit.
	 * @param hit the result hit
	 * @return null if there is no pid
	 */
	private static String extractPid(SearchHit hit) {
		Map<String, Object> source = hit.getSource();
		if (source.containsKey("pid")) {
			return HandleStoreService.HDL_PROXY_URL + source.get("pid").toString();
		} else {
			return null;
		}
	}

	/**
	 * Extracts the Name of the result hit.
	 * @param hit the result hit
	 * @return null if there is no name
	 */
	private static String extractName(SearchHit hit) {
		Map<String, Object> source = hit.getSource();
		if (source.containsKey("name")) {
			return source.get("name").toString();
		} else {
			return null;
		}
	}
	
}
