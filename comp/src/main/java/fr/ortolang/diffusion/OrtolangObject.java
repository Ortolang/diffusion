package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class OrtolangObject implements Serializable {
	
	public static final String OBJECT_TYPE = "object";
	
	public abstract String getObjectName();
	
	public abstract String getObjectKey();
	
	public abstract OrtolangObjectIdentifier getObjectIdentifier();
	
	@Override
    public String toString() {
    	return "{identifier:" + getObjectIdentifier() + "; name:" + getObjectName() + "}";
    }
	
}