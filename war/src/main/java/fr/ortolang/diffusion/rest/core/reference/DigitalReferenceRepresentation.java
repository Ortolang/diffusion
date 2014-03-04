package fr.ortolang.diffusion.rest.core.reference;

import fr.ortolang.diffusion.core.entity.DigitalReference;

public class DigitalReferenceRepresentation {

	private String key;
	private String name;
	private boolean dynamic;
	private String target;
	
	public DigitalReferenceRepresentation() {
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
	
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}
	
	public static DigitalReferenceRepresentation fromDigitalReference(DigitalReference reference) {
		DigitalReferenceRepresentation representation = new DigitalReferenceRepresentation();
		representation.setKey(reference.getKey());
		representation.setName(reference.getName());
		representation.setTarget(reference.getTarget());
		representation.setDynamic(reference.isDynamic());
		return representation;
	}

}
