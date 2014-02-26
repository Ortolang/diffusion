package fr.ortolang.diffusion;

public interface OrtolangIndexableService {
	
	public abstract OrtolangIndexableContent getIndexableContent(String key) throws OrtolangException;

}
