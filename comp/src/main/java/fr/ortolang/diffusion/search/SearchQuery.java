package fr.ortolang.diffusion.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchQuery {

	private Map<String, String[]> query;
	private String index;
	private String type;
	private Integer size;
	private String orderProp;
	private String orderDir;
	private String[] includes;
	private String[] excludes;
	private String[] aggregations;
	private Highlight highlight;
	
	public SearchQuery() {
		query = new HashMap<String, String[]>();
	}

	public Map<String, String[]> getQuery() {
		return query;
	}

	public void setQuery(Map<String, String[]> query) {
		this.query = query;
	}
	
	public void addQuery(String name, String[] values) {
		this.query.put(name, values);
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public String[] getIncludes() {
		return includes;
	}

	public void setIncludes(String[] includes) {
		this.includes = includes;
	}

	public String[] getExcludes() {
		return excludes;
	}

	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

	public String[] getAggregations() {
		return aggregations;
	}

	public void setAggregations(String[] aggregations) {
		this.aggregations = aggregations;
	}

	public String getOrderProp() {
		return orderProp;
	}
	
	public boolean hasOrder() {
		return orderProp != null && orderDir != null;
	}

	public void setOrderProp(String orderProp) {
		this.orderProp = orderProp;
	}

	public String getOrderDir() {
		return orderDir;
	}

	public void setOrderDir(String orderDir) {
		this.orderDir = orderDir;
	}

	public Highlight getHighlight() {
		return highlight;
	}

	public void setHighlight(Highlight highlight) {
		this.highlight = highlight;
	}
	
	public boolean hasHighlight() {
		return highlight != null;
	}

}
