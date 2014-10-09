package fr.ortolang.diffusion.workflow.engine;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;


public interface ProcessEngineService {
	
	public static final String SERVICE_NAME = "process-engine";
	
	public RepositoryService getRepositoryService() throws ProcessEngineServiceException;
	
	public RuntimeService getRuntimeService() throws ProcessEngineServiceException;
	
	public TaskService getTaskService() throws ProcessEngineServiceException;
	
}
