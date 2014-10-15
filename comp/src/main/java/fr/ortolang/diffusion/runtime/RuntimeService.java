package fr.ortolang.diffusion.runtime;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.entity.ProcessDefinition;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.runtime.entity.ProcessTask;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface RuntimeService extends OrtolangService {

	public static final String SERVICE_NAME = "runtime";
	public static final String[] OBJECT_TYPE_LIST = new String[] { ProcessDefinition.OBJECT_TYPE, ProcessInstance.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { ProcessDefinition.OBJECT_TYPE, "read,update,delete, start" },
			{ ProcessInstance.OBJECT_TYPE, "read,update,delete" } };

	public void createProcessDefinition(String key, InputStream definition) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;

	public List<ProcessDefinition> listProcessDefinitions() throws RuntimeServiceException;

	public ProcessDefinition readProcessDefinition(String key) throws RuntimeServiceException, KeyNotFoundException;

	public byte[] readProcessDefinitionModel(String key) throws RuntimeServiceException, KeyNotFoundException;

	public byte[] readProcessDefinitionDiagram(String key) throws RuntimeServiceException, KeyNotFoundException;

	public void suspendProcessDefinition(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;

	public ProcessDefinition findProcessDefinitionByName(String name) throws RuntimeServiceException;

	public void startProcessInstance(String key, String definition, Map<String, Object> variables) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;

	public List<ProcessInstance> listProcessInstances(String initier, boolean active) throws RuntimeServiceException, AccessDeniedException;

	public ProcessInstance readProcessInstance(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;

	public List<ProcessTask> listAllProcessTasks() throws RuntimeServiceException, AccessDeniedException;

	public List<ProcessTask> listCandidateProcessTasks() throws RuntimeServiceException;

	public List<ProcessTask> listAssignedProcessTasks() throws RuntimeServiceException;

	public void claimProcessTask(String id) throws RuntimeServiceException;

	public void completeProcessTask(String id, Map<String, Object> params) throws RuntimeServiceException;

}
