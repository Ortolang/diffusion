package fr.ortolang.diffusion.publication.entity;

import java.util.Map;

public class PublicationSecurityPolicy {

	private String id;
	private String template;
	private Map<String, String> params;

	public PublicationSecurityPolicy() {
		super();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

}
