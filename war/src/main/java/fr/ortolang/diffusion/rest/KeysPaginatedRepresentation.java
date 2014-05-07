package fr.ortolang.diffusion.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;

public class KeysPaginatedRepresentation extends DiffusionRepresentation {

	private Map<String, List<Link>> entries;
	private long totalSize = 0;
	private int start = 0;
	private int size = 0;
	
	public KeysPaginatedRepresentation() {
		entries = new HashMap<String, List<Link>>();
	}

	public Map<String, List<Link>> getEntries() {
		return entries;
	}

	public void setEntries(Map<String, List<Link>> entries) {
		this.entries = entries;
	}
	
	public void addEntry(String key, Link... links ) {
		this.entries.put(key, Arrays.asList(links));
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
}
