package fr.ortolang.diffusion.xml;

public class XmlDumpNamespace {
	private String uri;
	private String schemaLocation;

	public XmlDumpNamespace(String uri) {
		this(uri, null);
	}
	
	public XmlDumpNamespace(String uri, String schemaLocation) {
		this.uri = uri;
		this.schemaLocation = schemaLocation;
	}
	
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getSchemaLocation() {
		return schemaLocation;
	}
	public void setSchemaLocation(String schemaLocation) {
		this.schemaLocation = schemaLocation;
	}
	
}
