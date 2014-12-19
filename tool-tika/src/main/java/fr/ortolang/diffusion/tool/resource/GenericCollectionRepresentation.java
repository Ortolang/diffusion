package fr.ortolang.diffusion.tool.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "collection")
public class GenericCollectionRepresentation<T> {

	@XmlElementWrapper(name = "entries")
	private List<T> entries;
	private long size = 0;
	private long offset = 0;
	private long limit = 0;
	private URI first;
	private URI previous;
	private URI self;
	private URI next;
	private URI last;

	public GenericCollectionRepresentation() {
		entries = new ArrayList<T>();
	}

	public List<T> getEntries() {
		return entries;
	}

	public void setEntries(List<T> entries) {
		this.entries = entries;
	}

	public void addEntry(T entry) {
		this.entries.add(entry);
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}

	public URI getFirst() {
		return first;
	}

	public void setFirst(URI first) {
		this.first = first;
	}

	public URI getPrevious() {
		return previous;
	}

	public void setPrevious(URI previous) {
		this.previous = previous;
	}

	public URI getSelf() {
		return self;
	}

	public void setSelf(URI self) {
		this.self = self;
	}

	public URI getNext() {
		return next;
	}

	public void setNext(URI next) {
		this.next = next;
	}

	public URI getLast() {
		return last;
	}

	public void setLast(URI last) {
		this.last = last;
	}

}
