package fr.ortolang.diffusion.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.runtime.process.ProcessDefinition;
import fr.ortolang.diffusion.runtime.task.Task;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface RuntimeService extends OrtolangService {
	
	public static final String SERVICE_NAME = "runtime";
	public static final String[] OBJECT_TYPE_LIST = new String[] { ProcessInstance.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ ProcessInstance.OBJECT_TYPE, "read,update,delete,execute" }};
	
	public Set<ProcessDefinition> listProcessDefinitions() throws RuntimeServiceException;
	
	public void createProcessInstance(String key, String name, String type, Map<String, String> context) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	//public void updateProcessInstanceContext(String key, Map<String, String> context) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public ProcessInstance readProcessInstance(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;
	
	public List<String> findProcessInstancesByInitier(String initier) throws RuntimeServiceException, AccessDeniedException;
	
	public List<String> findAllProcessInstances() throws RuntimeServiceException, AccessDeniedException;
	
	public void startProcessInstance(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void abortProcessInstance(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void notifyTaskExecution(Task task) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;
	
}
