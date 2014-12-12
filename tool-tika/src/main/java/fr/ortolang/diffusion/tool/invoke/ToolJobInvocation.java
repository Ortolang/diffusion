package fr.ortolang.diffusion.tool.invoke;

import java.io.IOException;
import java.nio.file.Path;

import fr.ortolang.diffusion.tool.job.entity.ToolJob;


/**
 * Actual class for tool invocation called from ToolJobInvocationListener in the job's queue
 * It should be specific to the tool
 */
public interface ToolJobInvocation {
	
	
	
	/**
	 * Execute tool job
	 * @param job
	 * @param logFile 
	 * @return ToolInvocationResult
	 * @throws SecurityException 
	 * @throws IOException 
	 */
	public ToolJobInvocationResult invoke(ToolJob job, Path logFile) throws SecurityException, IOException;


}
