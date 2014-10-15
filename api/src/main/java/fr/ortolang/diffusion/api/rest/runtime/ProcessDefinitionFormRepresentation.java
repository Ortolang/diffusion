package fr.ortolang.diffusion.api.rest.runtime;

import java.io.InputStream;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class ProcessDefinitionFormRepresentation {

	@FormParam("content")
	@PartType("application/octet-stream")
	private InputStream content = null;

	public ProcessDefinitionFormRepresentation() {
	}

	public InputStream getContent() {
		return content;
	}

	public void setContent(InputStream content) {
		this.content = content;
	}

}
