package fr.ortolang.diffusion.api.admin;

import java.io.InputStream;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class MetadataObjectFormRepresentation {

	@FormParam("key")
	@PartType("text/plain")
	private String key = "";

	@FormParam("name")
	@PartType("text/plain")
	private String name = "";

	@FormParam("filename")
	@PartType("text/plain")
	private String filename = "";

	@FormParam("stream")
	@PartType("application/octet-stream")
	private InputStream stream = null;
	private String streamHash = "";
	
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
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public InputStream getStream() {
		return stream;
	}
	public void setStream(InputStream stream) {
		this.stream = stream;
	}
	public String getStreamHash() {
		return streamHash;
	}
	public void setStreamHash(String streamHash) {
		this.streamHash = streamHash;
	}

}
