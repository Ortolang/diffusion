package fr.ortolang.diffusion.workflow;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.workflow.entity.WorkflowDefinition;
import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;
import fr.ortolang.diffusion.workflow.entity.WorkflowTask;

public interface WorkflowService extends OrtolangService {
	
	public static final String SERVICE_NAME = "workflow";
	public static final String[] OBJECT_TYPE_LIST = new String[] { WorkflowDefinition.OBJECT_TYPE,  WorkflowInstance.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ WorkflowDefinition.OBJECT_TYPE, "read,update,delete" }};
	
	public void createWorkflowDefinition(String key, InputStream definition) throws WorkflowServiceException, AccessDeniedException;
	
	public List<WorkflowDefinition> listWorkflowDefinitions() throws WorkflowServiceException;
	
	public WorkflowDefinition getWorkflowDefinition(String key) throws WorkflowServiceException;
	
	public byte[] getWorkflowDefinitionModel(String key) throws WorkflowServiceException;
	
	public byte[] getWorkflowDefinitionDiagram(String key) throws WorkflowServiceException;
	
	public void suspendWorkflowDefinition(String key) throws WorkflowServiceException, AccessDeniedException;
	
	public WorkflowDefinition findWorkflowDefinitionForName(String name) throws WorkflowServiceException;
	
	public void createWorkflowInstance(String key, String definition, Map<String, Object> params) throws WorkflowServiceException, AccessDeniedException;
	
	public List<WorkflowInstance> listWorkflowInstances(String initier, String definition, boolean activeOnly) throws WorkflowServiceException, AccessDeniedException;
	
	public WorkflowInstance getWorkflowInstance(String key) throws WorkflowServiceException, AccessDeniedException;
	
	public List<WorkflowTask> listWorkflowTasks() throws WorkflowServiceException, AccessDeniedException;

	public List<WorkflowTask> listCandidateWorkflowTasks() throws WorkflowServiceException, AccessDeniedException;

//	public List<WorkflowTask> findAssignedWorkflowTasks() throws WorkflowServiceException, AccessDeniedException;
	
	
}
