package fr.ortolang.diffusion.security.authorisation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuthorisationService {
	
	public static final String SERVICE_NAME = "authorisation";
	
	public void createPolicy(String key, String owner) throws AuthorisationServiceException;
	
	public void clonePolicy(String key, String origin) throws AuthorisationServiceException;
	
	public void copyPolicy(String from, String to) throws AuthorisationServiceException;
	
	public void copyPolicy(String from, Set<String> to) throws AuthorisationServiceException;
	
	public void updatePolicyOwner(String key, String owner) throws AuthorisationServiceException;
	
	public String getPolicyOwner(String key) throws AuthorisationServiceException;
	
	public void setPolicyRules(String key, Map<String, List<String>> rules) throws AuthorisationServiceException;
	
	public Map<String, List<String>> getPolicyRules(String key) throws AuthorisationServiceException;
	
	public void checkPermission(String key, List<String> subjects, String permission) throws AuthorisationServiceException, AccessDeniedException;
	
	public void checkOwnership(String key, List<String> subjects) throws AuthorisationServiceException, AccessDeniedException;
	
	public void checkAuthentified(String identifier) throws AuthorisationServiceException, AccessDeniedException;
	
	public void checkSuperUser(String identifier) throws AuthorisationServiceException, AccessDeniedException;
	
}
