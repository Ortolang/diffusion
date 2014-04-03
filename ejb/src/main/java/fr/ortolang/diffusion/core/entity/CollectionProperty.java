package fr.ortolang.diffusion.core.entity;

import fr.ortolang.diffusion.OrtolangObjectProperty;

public class CollectionProperty {
	
	public static final String LEVEL = OrtolangObjectProperty.SYSTEM_PROPERTY_PREFIX + "collection.level";
	public static final String VERSION = OrtolangObjectProperty.SYSTEM_PROPERTY_PREFIX + "collection.version";
	
	public enum Level {
		TOP
	}
	
	public enum Version {
		WORK, 
		SNAPSHOT,
		RELEASE
	}
	
}
