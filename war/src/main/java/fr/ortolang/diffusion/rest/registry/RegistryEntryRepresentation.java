package fr.ortolang.diffusion.rest.registry;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;

public class RegistryEntryRepresentation {

	private String key;
	private String state;
	private OrtolangObjectIdentifier identifier;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public OrtolangObjectIdentifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(OrtolangObjectIdentifier identifier) {
		this.identifier = identifier;
	}

}
