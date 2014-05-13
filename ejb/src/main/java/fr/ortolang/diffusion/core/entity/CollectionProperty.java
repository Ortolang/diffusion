package fr.ortolang.diffusion.core.entity;

import fr.ortolang.diffusion.OrtolangObjectProperty;

public class CollectionProperty {
	
	public static final String ROOT = OrtolangObjectProperty.SYSTEM_PROPERTY_PREFIX + "collection.root";
	public static final String VERSION = OrtolangObjectProperty.SYSTEM_PROPERTY_PREFIX + "collection.version";
	
	public enum Version {
		WORK, 
		SNAPSHOT,
		RELEASE
	}
	
}
