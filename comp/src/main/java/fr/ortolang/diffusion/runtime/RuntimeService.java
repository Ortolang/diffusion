package fr.ortolang.diffusion.runtime;

import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.runtime.entity.ProcessType;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface RuntimeService extends OrtolangService {

	public static final String SERVICE_NAME = "runtime";
	public static final String[] OBJECT_TYPE_LIST = new String[] { Process.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
			{ Process.OBJECT_TYPE, "read,update,delete,start" } };
	
	public void importProcessTypes() throws RuntimeServiceException;
	
	public List<ProcessType> listProcessTypes() throws RuntimeServiceException;
	
	/* Process */
	
	public void createProcess(String key, String type, String name) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;

	public void startProcess(String key, Map<String, Object> variables) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public void updateProcessState(String pid, State state) throws RuntimeServiceException;
	
	public void appendProcessLog(String pid, String log) throws RuntimeServiceException;
	
	public void updateProcessActivity(String pid, String name) throws RuntimeServiceException;
	
	public List<Process> listProcesses(State sate) throws RuntimeServiceException, AccessDeniedException;

	public Process readProcess(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;
	
	/* Task */

	public List<HumanTask> listCandidateTasks() throws RuntimeServiceException;

	public List<HumanTask> listAssignedTasks() throws RuntimeServiceException;

	public void claimTask(String id) throws RuntimeServiceException;

	public void completeTask(String id, Map<String, Object> variables) throws RuntimeServiceException;
	
}
