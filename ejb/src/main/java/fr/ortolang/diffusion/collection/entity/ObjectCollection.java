package fr.ortolang.diffusion.collection.entity;

import java.util.List;

import fr.ortolang.diffusion.DiffusionObject;
import fr.ortolang.diffusion.DiffusionObjectIdentifier;

public class ObjectCollection extends DiffusionObject {
	
	public static final String OBJECT_TYPE = "collection";
	
	private String id;
	private String name;
	private boolean mutable;
	private List<String> content;

	@Override
	public String getDiffusionObjectName() {
		return name;
	}

	@Override
	public DiffusionObjectIdentifier getDiffusionObjectIdentifier() {
		return null;
	}

}
