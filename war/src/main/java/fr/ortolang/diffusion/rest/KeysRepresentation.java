package fr.ortolang.diffusion.rest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;

public class KeysRepresentation extends DiffusionRepresentation {

	private Map<String, List<Link>> entries;
	
	public KeysRepresentation() {
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

}
