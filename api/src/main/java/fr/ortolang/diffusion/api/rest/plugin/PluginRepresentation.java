package fr.ortolang.diffusion.api.rest.plugin;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "plugin")
public class PluginRepresentation {

	@XmlAttribute
	private String key;
	private String name;
	private String description;
	private String detail;
	private String url;
	private List<String> formats;
	private JsonArray configForm;
	private JsonObject config;

	public PluginRepresentation() {
		super();
	}

	public PluginRepresentation(String key, String name, String description) {
		super();
		this.key = key;
		this.name = name;
		this.description = description;
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
	
	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}


	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<String> getFormats() {
		return formats;
	}

	public void setFormats(List<String> formats) {
		this.formats = formats;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public JsonArray getConfigForm() {
		return configForm;
	}

	public void setConfigForm(JsonArray configForm) {
		this.configForm = configForm;
	}

	public JsonObject getConfig() {
		return config;
	}

	public void setConfig(JsonObject form) {
		this.config = form;
	}
}
