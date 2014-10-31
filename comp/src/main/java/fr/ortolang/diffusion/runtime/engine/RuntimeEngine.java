package fr.ortolang.diffusion.runtime.engine;

import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.ProcessType;

public interface RuntimeEngine {
	
	public static final String SERVICE_NAME = "engine";
	
	public void deployDefinitions(String[] resources) throws RuntimeEngineException;
	
	public List<ProcessType> listProcessTypes() throws RuntimeEngineException;
	
	public ProcessType getProcessTypeById(String id) throws RuntimeEngineException;
	
	public ProcessType getProcessTypeByKey(String key) throws RuntimeEngineException;
	
	public void startProcess(String type, String key, Map<String, Object> variables) throws RuntimeEngineException;
	
	public Process getProcess(String id) throws RuntimeEngineException;
	
	public HumanTask getTask(String id) throws RuntimeEngineException;
	
	public List<HumanTask> listCandidateTasks(String user, List<String> groups) throws RuntimeEngineException;

	public List<HumanTask> listAssignedTasks(String user) throws RuntimeEngineException;
	
	public void claimTask(String id, String user) throws RuntimeEngineException;
	
	public void completeTask(String id, Map<String, Object> variables) throws RuntimeEngineException;
	
	public void notify(String type) throws RuntimeEngineException;
	
}
