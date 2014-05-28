package fr.ortolang.diffusion.rest.api;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;

public abstract class OrtolangLinkableRepresentation {

	@XmlElementWrapper(name="links")
	private List<OrtolangLinkRepresentation> links = new ArrayList<OrtolangLinkRepresentation>();

	public void setLinks(List<OrtolangLinkRepresentation> links) {
		this.links = links;
	}

	public List<OrtolangLinkRepresentation> getLinks() {
		return this.links;
	}

	public void addLink(OrtolangLinkRepresentation link) {
		this.links.add(link);
	}

}
