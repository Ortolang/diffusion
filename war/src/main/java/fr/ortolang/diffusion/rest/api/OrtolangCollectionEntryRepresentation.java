package fr.ortolang.diffusion.rest.api;

import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="entry")
public class OrtolangCollectionEntryRepresentation extends OrtolangLinkableRepresentation {
	
	private String key;
	
	public OrtolangCollectionEntryRepresentation() {
		this.setLinks(new ArrayList<OrtolangLinkRepresentation> ());
	}
	
	public OrtolangCollectionEntryRepresentation(String key, OrtolangLinkRepresentation... links) {
		this.key = key;
		this.setLinks(Arrays.asList(links));
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
}
