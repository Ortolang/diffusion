package fr.ortolang.diffusion.rest.core.link;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.rest.api.OrtolangLinkableRepresentation;

@XmlRootElement(name="link")
public class LinkRepresentation extends OrtolangLinkableRepresentation {

	@XmlAttribute(name="key")
	private String key;
	private String name;
	private String target;
	private boolean dynamic;

	public LinkRepresentation() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public static LinkRepresentation fromLink(Link reference) {
		LinkRepresentation representation = new LinkRepresentation();
		representation.setKey(reference.getKey());
		representation.setName(reference.getName());
		representation.setTarget(reference.getTarget());
		representation.setDynamic(reference.isDynamic());
		return representation;
	}

}
