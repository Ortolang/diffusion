package fr.ortolang.diffusion.rest.datatables;

import java.util.ArrayList;
import java.util.List;

public class DataTableCollection<T> {

	private int sEcho = 0;
	private long iTotalRecords = 0;
	private long iTotalDisplayRecords = 0;
	private List<T> aaData = new ArrayList<T>();

	public DataTableCollection() {
	}

	public DataTableCollection(int sEcho, long iTotalRecords, long iTotalDisplayRecords, List<T> aaData) {
		this.sEcho = sEcho;
		this.iTotalRecords = iTotalRecords;
		this.iTotalDisplayRecords = iTotalDisplayRecords;
		this.aaData = aaData;
	}

	public int getsEcho() {
		return sEcho;
	}

	public void setsEcho(int sEcho) {
		this.sEcho = sEcho;
	}

	public long getiTotalRecords() {
		return iTotalRecords;
	}

	public void setiTotalRecords(long iTotalRecords) {
		this.iTotalRecords = iTotalRecords;
	}

	public long getiTotalDisplayRecords() {
		return iTotalDisplayRecords;
	}

	public void setiTotalDisplayRecords(long iTotalDisplayRecords) {
		this.iTotalDisplayRecords = iTotalDisplayRecords;
	}

	public List<T> getAaData() {
		return aaData;
	}

	public void setAaData(List<T> aaData) {
		this.aaData = aaData;
	}
	
	public void addEntry(T entry) {
		aaData.add(entry);
	}
	
	public void removeEntry(T entry) {
		aaData.remove(entry);
	}

}