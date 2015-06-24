package fr.ortolang.diffusion.api.preview;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "preview")
public class PreviewRepresentation {
	
	@XmlAttribute
	private String key;
	private String largeUrl;
	private String smallUrl;

	public PreviewRepresentation() {
	}
	
	public PreviewRepresentation(String key, String largeUrl, String smallUrl) {
		super();
		this.key = key;
		this.largeUrl = largeUrl;
		this.smallUrl = smallUrl;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getLargeUrl() {
		return largeUrl;
	}

	public void setLargeUrl(String largeUrl) {
		this.largeUrl = largeUrl;
	}

	public String getSmallUrl() {
		return smallUrl;
	}

	public void setSmallUrl(String smallUrl) {
		this.smallUrl = smallUrl;
	}

}
