package fr.ortolang.diffusion.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Link;

public abstract class DiffusionRepresentation {
	
	private List<Link> links = new ArrayList<Link> ();
	
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	
	public List<Link> getLinks() {
		return this.links;
	}
	
	public void addLink(Link link) {
		this.links.add(link);
	}

}
