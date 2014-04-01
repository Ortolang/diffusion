package fr.ortolang.diffusion.security;

import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface SecurityService extends OrtolangService {
	
	public static final String SERVICE_NAME = "security";
	
	public void changeOwner(String key, String subject) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException;
	
	public String getOwner(String key) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException;
	
	public Map<String, List<String>> listRules(String key) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void setRules(String key, Map<String, List<String>> rules) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void setRule(String key, String subject, List<String> permissions) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException;
	
	public List<String> listAvailablePermissions(String key) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException;
	
}
