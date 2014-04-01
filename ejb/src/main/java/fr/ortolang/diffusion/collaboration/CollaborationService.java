package fr.ortolang.diffusion.collaboration;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.collaboration.entity.Project;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface CollaborationService extends OrtolangService {
	
	public static final String SERVICE_NAME = "collaboration";
	public static final String[] OBJECT_TYPE_LIST = new String[] { Project.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Project.OBJECT_TYPE, "read,update,delete,snapshot,release"}};
	
	public void createProject(String key, String name, String type, String category) throws CollaborationServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public Project readProject(String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateProject(String key, String name, String category) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void snapshotProject(String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void releaseProject(String key, String name) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteProject(String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException;
	
	public List<String> findMyProjects() throws CollaborationServiceException;
	
}
