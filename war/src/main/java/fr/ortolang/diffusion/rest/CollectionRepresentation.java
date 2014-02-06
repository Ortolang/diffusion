package fr.ortolang.diffusion.rest;

import java.util.ArrayList;
import java.util.Collection;

public class CollectionRepresentation<T> {

	private Collection<T> entries;
	private Collection<LinkRepresentation> links;
	
	public CollectionRepresentation() {
		entries = new ArrayList<T> ();
		links = new ArrayList<LinkRepresentation> ();
	}

	public Collection<T> getEntries() {
		return entries;
	}

	public void setEntries(Collection<T> entries) {
		this.entries = entries;
	}
	
	public void addEntry(T entry) {
		entries.add(entry);
	}

	public Collection<LinkRepresentation> getLinks() {
		return links;
	}

	public void setLinks(Collection<LinkRepresentation> links) {
		this.links = links;
	}

}
