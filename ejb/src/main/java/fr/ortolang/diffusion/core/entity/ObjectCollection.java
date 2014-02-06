package fr.ortolang.diffusion.core.entity;

import java.util.List;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;

@SuppressWarnings("serial")
public class ObjectCollection extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "collection";
	
	private String id;
	private String name;
	private String key;
	private boolean mutable;
	private List<String> content;

	@Override
	public String getObjectName() {
		return name;
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return null;
	}

	@Override
	public String getObjectKey() {
		return key;
	}

}
