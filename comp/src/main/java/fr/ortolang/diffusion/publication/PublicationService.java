package fr.ortolang.diffusion.publication;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface PublicationService extends OrtolangService {
	
	public static final String SERVICE_NAME = "publication";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	
	public void publish(String key, PublicationContext context) throws PublicationServiceException, AccessDeniedException;
	
	public void review(String key) throws PublicationServiceException, AccessDeniedException;
	
}
