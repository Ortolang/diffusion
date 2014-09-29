package fr.ortolang.diffusion.workflow;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.workflow.entity.WorkflowDefinition;
import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;

public interface WorkflowService extends OrtolangService {
	
	public static final String SERVICE_NAME = "workflow";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	
	public String deployWorkflowDefinition(String name, InputStream definition) throws WorkflowServiceException, AccessDeniedException;
	
	public List<WorkflowDefinition> listWorkflowDefinitions() throws WorkflowServiceException;
	
	public String startWorkflowInstance(String definition, Map<String, Object> params) throws WorkflowServiceException, AccessDeniedException;
	
	public List<WorkflowInstance> listWorkflowInstances(String definition, boolean activeOnly) throws WorkflowServiceException;
	
}
