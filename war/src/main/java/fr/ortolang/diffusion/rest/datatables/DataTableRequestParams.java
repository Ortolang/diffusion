package fr.ortolang.diffusion.rest.datatables;

import java.util.Map;

public class DataTableRequestParams {

	private static final String ECHO = "sEcho";
	private static final String OFFSET = "iDisplayStart";
	private static final String LENGTH = "iDisplayLength";
	private static final String SEARCH_QUERY = "sSearch";
	private static final String NB_COLUMNS = "iColumns";
	
	private int echo;
	private int nbColumns;
	private int offset;
	private int length;
	private String searchQuery;
	
	public DataTableRequestParams(int nbColumns) {
		this.nbColumns = nbColumns;
		echo = 0;
		offset = 0;
		length = 0;
		searchQuery = "";
	}
	
	public static DataTableRequestParams fromQueryParams(Map<String, String[]> params) throws DataTableRequestParamsException {
		if (params.containsKey(NB_COLUMNS)) {
			DataTableRequestParams dtparams = new DataTableRequestParams(Integer.parseInt(params.get(NB_COLUMNS)[0]));
			dtparams.setEcho(Integer.parseInt(params.get(ECHO)[0]));
			dtparams.setOffset(Integer.parseInt(params.get(OFFSET)[0]));
			dtparams.setLength(Integer.parseInt(params.get(LENGTH)[0]));
			if ( params.containsKey(SEARCH_QUERY) ) {
				dtparams.setSearchQuery(params.get(SEARCH_QUERY)[0]);
			}
			return dtparams;
		} else {
			throw new DataTableRequestParamsException("Param " + NB_COLUMNS + " is mandatory");
		}
	}

	public int getEcho() {
		return echo;
	}

	public void setEcho(int echo) {
		this.echo = echo;
	}

	public int getNbColumns() {
		return nbColumns;
	}

	public void setNbColumns(int nbColumns) {
		this.nbColumns = nbColumns;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getSearchQuery() {
		return searchQuery;
	}

	public void setSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
	}

}