package fr.ortolang.diffusion.runtime;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.runtime.entity.ProcessDefinition;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.runtime.entity.ProcessTask;

public interface RuntimeEngine {
	
	public static final String SERVICE_NAME = "engine";
	
	public String deployProcessDefinition(String name, InputStream definition) throws RuntimeEngineException;
	
	public List<ProcessDefinition> listProcessDefinitions() throws RuntimeEngineException;
	
	public ProcessDefinition getProcessDefinition(String id) throws RuntimeEngineException;
	
	public byte[] getProcessDefinitionModel(String id) throws RuntimeEngineException;
	
	public byte[] getProcessDefinitionDiagram(String id) throws RuntimeEngineException;
	
	public void suspendProcessDefinition(String id) throws RuntimeEngineException;
	
	public ProcessDefinition findLatestProcessDefinitionsForName(String name) throws RuntimeEngineException;
	
	public void startProcessInstance(String processDefinitionId, String businessKey, Map<String, Object> variables) throws RuntimeEngineException;
	
	public ProcessInstance getProcessInstance(String businessKey) throws RuntimeEngineException;
	
	public List<ProcessInstance> listProcessInstances(String initier, boolean active) throws RuntimeEngineException;
	
	public List<ProcessTask> listAllProcessTasks() throws RuntimeEngineException;
	
	public List<ProcessTask> listCandidateGroupsProcessTasks(List<String> candidateGroups) throws RuntimeEngineException;
	
	public List<ProcessTask> listCandidateProcessTasks(String candidateUser) throws RuntimeEngineException;
	
	public List<ProcessTask> listAssignedProcessTasks(String assigneeUser) throws RuntimeEngineException;
	
	public void claimProcessTask(String id, String assigneeUser) throws RuntimeEngineException;
	
	public void completeProcessTask(String id, Map<String, Object> variables) throws RuntimeEngineException;
	
	//public void addRuntimeEngineListener(RuntimeEngineListener listener) throws RuntimeEngineException;
	
}
