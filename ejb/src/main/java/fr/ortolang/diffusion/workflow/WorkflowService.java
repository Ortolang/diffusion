package fr.ortolang.diffusion.workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.workflow.entity.Process;
import fr.ortolang.diffusion.workflow.process.ProcessDefinition;

public interface WorkflowService extends OrtolangService {
	
	public static final String SERVICE_NAME = "workflow";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	
	public void createProcess(String key, String name, String type, Map<String, String> params) throws WorkflowServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public Process readProcess(String key) throws WorkflowServiceException, KeyNotFoundException, AccessDeniedException;
	
	public List<String> findProcessForInitier(String initier) throws WorkflowServiceException, AccessDeniedException;
	
	public Set<ProcessDefinition> listProcessDefinitions() throws WorkflowServiceException;

}
