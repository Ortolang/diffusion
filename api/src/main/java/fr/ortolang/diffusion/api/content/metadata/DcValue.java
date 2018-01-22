package fr.ortolang.diffusion.api.content.metadata;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class DcValue {
	private String value;
	private String lang;
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public static List<DcValue> valueOf(JsonNode jsonNode, String name) {
		JsonNode arrayNode = jsonNode.get(name);
		List<DcValue> dcValues = new ArrayList<DcValue>();
		if (arrayNode.isArray()) {
		    for (final JsonNode objNode : arrayNode) {
		        DcValue dcValue = new DcValue();
		        dcValue.setValue(objNode.get("value").asText());
		        dcValues.add(dcValue);
		    }
		}
		return dcValues;
	}
}
