package fr.ortolang.diffusion.tool.job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.tool.job.entity.ToolJob;

public interface ToolJobService {

	/**
	 * Return the list of jobs
	 * @return List<ToolJob>
	 * @throws ToolJobException 
	 */
	public List<ToolJob> list() throws ToolJobException;
	
	/**
	 * Read a job from its id
	 * @param id String	Id of the job
	 * @return ToolJob
	 * @throws ToolJobException 
	 */
	public ToolJob read(String id) throws ToolJobException;
	
	/**
	 * Submit a new tool job to the queue
	 * @param priority	int	Priority to set to the job
	 * @throws ToolJobException
	 * @throws IOException 
	 */
	public void submit(int priority, Map<String, String> parameters) throws ToolJobException, IOException;
	
	/**
	 * Abort a tool job
	 * @param id	String	Id of the job to abort
	 * @throws ToolJobException
	 */
	public void abort(String id) throws ToolJobException;
	
	/**
	 * Complete execution of a job
	 * @param id	String	Id of the tool job
	 * @param log	String	Execution's log
	 * @throws ToolJobException
	 */
	public void complete(String id, String log) throws ToolJobException;
	
	/**
	 * Stop execution of a job after an error
	 * @param id	String	Id of the tool job
	 * @param log	String	Execution's log with exception trace
	 * @throws ToolJobException
	 */
	public void error(String id, String log) throws ToolJobException;
	
	/**
	 * Delete recursively a working directory for a tool job
	 * @param id String	Id of the tool job
	 * @throws IOException
	 * @throws ToolJobException
	 */
	public void delete(String id) throws IOException, ToolJobException;
	
	public void extend(String id);

}
