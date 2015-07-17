package fr.ortolang.diffusion.api.content;

import java.util.ArrayList;
import java.util.List;

import fr.ortolang.diffusion.core.entity.CollectionElement;

public class ContentRepresentation {
	
	private String context;
	private String base;
	private String alias;
	private String snapshot;
	private String path;
	private String order;
	private boolean asc;
	private boolean linkbykey;
	private String parentPath;
	private List<CollectionElement> elements;

	public ContentRepresentation() {
		context = "";
		base = "";
		alias = "";
		snapshot = "";
		path = "";
		order = "N";
		linkbykey = false;
		asc = true;
		parentPath = "";
		elements = new ArrayList<CollectionElement> ();
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getPath() {
		return path;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(String snapshot) {
		this.snapshot = snapshot;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public boolean isAsc() {
		return asc;
	}

	public void setAsc(boolean asc) {
		this.asc = asc;
	}
	
	public boolean isLinkbykey() {
		return linkbykey;
	}

	public void setLinkbykey(boolean linkbykey) {
		this.linkbykey = linkbykey;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public List<CollectionElement> getElements() {
		return elements;
	}

	public void setElements(List<CollectionElement> elements) {
		this.elements = elements;
	}
	
}
