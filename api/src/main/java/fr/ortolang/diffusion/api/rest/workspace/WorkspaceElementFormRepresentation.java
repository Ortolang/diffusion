package fr.ortolang.diffusion.api.rest.workspace;

import java.io.InputStream;

import javax.ws.rs.FormParam;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

public class WorkspaceElementFormRepresentation {

	@FormParam("path")
	@PartType("text/plain")
	private String path = null;
	
	@FormParam("type")
	@PartType("text/plain")
	private String type = null;

	@FormParam("name")
	@PartType("text/plain")
	private String name = "no name provided";

	@FormParam("description")
	@PartType("text/plain")
	private String description = "no description provided";

	@FormParam("format")
	@PartType("text/plain")
	private String format = "no format provided";

	@FormParam("target")
	@PartType("text/plain")
	private String target = "no target provided";

	@FormParam("preview")
	@PartType("application/octet-stream")
	private InputStream preview = null;
	private String previewHash = "";

	@FormParam("stream")
	@PartType("application/octet-stream")
	private InputStream stream = null;
	private String streamHash = "";

	public WorkspaceElementFormRepresentation() {
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public InputStream getPreview() {
		return preview;
	}

	public void setPreview(InputStream preview) {
		this.preview = preview;
	}

	public InputStream getStream() {
		return stream;
	}

	public void setStream(InputStream stream) {
		this.stream = stream;
	}

	public String getPreviewHash() {
		return previewHash;
	}

	public void setPreviewHash(String previewHash) {
		this.previewHash = previewHash;
	}

	public String getStreamHash() {
		return streamHash;
	}

	public void setStreamHash(String streamHash) {
		this.streamHash = streamHash;
	}

}
