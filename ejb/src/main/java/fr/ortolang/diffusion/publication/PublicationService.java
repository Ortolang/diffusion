package fr.ortolang.diffusion.publication;

import java.util.Set;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface PublicationService extends OrtolangService {
	
	public static final String SERVICE_NAME = "publication";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	
	public void publish(Set<String> keys) throws PublicationServiceException, AccessDeniedException;
	
	public void submit(Set<String> keys)throws PublicationServiceException, AccessDeniedException;

}
