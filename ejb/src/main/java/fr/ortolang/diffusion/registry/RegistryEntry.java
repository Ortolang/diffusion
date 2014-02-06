package fr.ortolang.diffusion.registry;

import java.io.Serializable;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;

@SuppressWarnings("serial")
public class RegistryEntry implements Serializable {

	private String key;
	private RegistryEntryState state;
	private OrtolangObjectIdentifier identifier;

	public RegistryEntry() {
	}

	public RegistryEntry(String key, RegistryEntryState state, OrtolangObjectIdentifier identifier) {
		this.key = key;
		this.state = state;
		this.identifier = identifier;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public RegistryEntryState getState() {
		return state;
	}

	public void setState(RegistryEntryState state) {
		this.state = state;
	}

	public OrtolangObjectIdentifier getIdentifier() {
		return identifier;
	}

	public void setIdentifier(OrtolangObjectIdentifier identifier) {
		this.identifier = identifier;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("{key:").append(getKey());
		buffer.append(", state:").append(getState());
		buffer.append(", identifier:").append(getIdentifier().serialize());
		buffer.append("}");
		return buffer.toString();
	}

}
