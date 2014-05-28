package fr.ortolang.diffusion.rest.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="collection")
public class OrtolangCollectionRepresentation extends OrtolangLinkableRepresentation {

	@XmlElementWrapper(name="entries")
	private List<OrtolangCollectionEntryRepresentation> entries;
	private long totalSize = 0;
	private int start = 0;
	private int size = 0;
	
	public OrtolangCollectionRepresentation() {
		entries = new ArrayList<OrtolangCollectionEntryRepresentation>();
	}

	public List<OrtolangCollectionEntryRepresentation> getEntries() {
		return entries;
	}

	public void setEntries(List<OrtolangCollectionEntryRepresentation> entries) {
		this.entries = entries;
	}
	
	public void addEntry(String key, OrtolangLinkRepresentation... links ) {
		this.entries.add(new OrtolangCollectionEntryRepresentation(key, links));
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
