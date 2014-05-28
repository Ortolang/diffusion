package fr.ortolang.diffusion.rest.api;

import java.net.URI;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="link")
public class OrtolangLinkRepresentation {

	private String title;
	private String rel;
	private String type;
	private URI uri;

	private OrtolangLinkRepresentation(String title, String rel, String type, URI uri) {
		this.title = title;
		this.rel = rel;
		this.type = type;
		this.uri = uri;
	}

	private OrtolangLinkRepresentation(URI uri) {
		this("", "", "", uri);
	}
	
	public OrtolangLinkRepresentation() {
	}

	public static OrtolangLinkRepresentation fromUri(URI uri) {
		return new OrtolangLinkRepresentation(uri);
	}

	public OrtolangLinkRepresentation title(String title) {
		this.title = title;
		return this;
	}

	public OrtolangLinkRepresentation rel(String rel) {
		this.rel = rel;
		return this;
	}

	public OrtolangLinkRepresentation type(String type) {
		this.type = type;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getRel() {
		return rel;
	}

	public void setRel(String rel) {
		this.rel = rel;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

}
