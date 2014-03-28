package fr.ortolang.diffusion.collaboration.entity;

import fr.ortolang.diffusion.OrtolangObjectProperty;

public class ProjectProperty {
	
	public static final String LEVEL = OrtolangObjectProperty.SYSTEM_PROPERTY_PREFIX + "project.level";
	public static final String VERSION = OrtolangObjectProperty.SYSTEM_PROPERTY_PREFIX + "project.version";
	public static final String TYPE = "project.type";
	public static final String CATEGORY = "project.category";
	
	public enum Level {
		TOP
	}
	
	public enum Version {
		WORK, 
		SNAPSHOT,
		RELEASE
	}
	
	public enum Type {
		PERSONNAL,
		COLLABORATIVE
	}
	
	public enum Category {
		CORPUS,
		LEXICON,
		DICTIONNARY,
		METALEXICOGRAPHY,
		TOOL
	}

}
