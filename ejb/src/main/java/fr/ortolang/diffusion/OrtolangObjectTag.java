package fr.ortolang.diffusion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class OrtolangObjectTag implements Serializable {

	private String name;
	private List<String> keys;

	public OrtolangObjectTag() {
		keys = new ArrayList<String>();
	}

	public OrtolangObjectTag(String name, List<String> keys) {
		this.name = name;
		this.keys = keys;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getKeys() {
		return keys;
	}

	public void setKeys(List<String> keys) {
		this.keys = keys;
	}
	
	public void addKey(String key) {
		this.keys.add(key);
	}
	
	public void removeKey(String key) {
		this.keys.remove(key);
	}

}
