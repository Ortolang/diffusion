package fr.ortolang.diffusion;

public interface OrtolangIndexableService {
	
	public abstract OrtolangIndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException;
	
	public abstract OrtolangIndexableSemanticContent getIndexableSemanticContent(String key) throws OrtolangException;
	
}
