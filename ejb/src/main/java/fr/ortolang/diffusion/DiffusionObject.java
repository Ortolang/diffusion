package fr.ortolang.diffusion;

public abstract class DiffusionObject {
	
	public abstract String getDiffusionObjectName();
	
	public abstract DiffusionObjectIdentifier getDiffusionObjectIdentifier();
	
	@Override
    public String toString() {
    	return "{identifier:" + getDiffusionObjectIdentifier() + "; name:" + getDiffusionObjectName() + "}";
    }
	
}
