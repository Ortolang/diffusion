package fr.ortolang.diffusion.rest.registry;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.registry.RegistryEntry;

public class RegistryEntryRepresentation {

	private String key;
	private String state;
	private OrtolangObjectIdentifier identifier;
	
	public RegistryEntryRepresentation() {
	}
	
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

	public static RegistryEntryRepresentation fromRegistryEntry(RegistryEntry entry) {
		RegistryEntryRepresentation representation = new RegistryEntryRepresentation();
		representation.setKey(entry.getKey());
    	representation.setState(entry.getState().name());
    	representation.setIdentifier(entry.getIdentifier());
    	return representation;
	}
}
