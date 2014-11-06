package fr.ortolang.diffusion.tool.dumbtool;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.tool.invoke.ToolInvoker;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

public class DumbToolInvoker implements ToolInvoker {
	
	private Logger logger = Logger.getLogger(DumbToolInvoker.class.getName());
	
	private CoreService core;
	private boolean initialized = false;
	
	public DumbToolInvoker() {
	}
	
	private void init() throws Exception {
		logger.log(Level.INFO, "Initializing Dumb Tool for testing");
		logger.log(Level.INFO, "Dumb Tool initialized");
	}
	
	public CoreService getCoreService() throws Exception {
		if (core == null) {
			core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
		}
		return core;
	}

	protected void setCoreService(CoreService core) {
		this.core = core;
	}

	@Override
	public ToolInvokerResult invoke(Map<String, String> params) {
		ToolInvokerResult result = new ToolInvokerResult();
		result.setStart(System.currentTimeMillis());
		result.setLog("");
		try {
			if ( !initialized ) {
				this.init();
			}
			
			String strOutput = "Hello world !";
			
			for (Map.Entry<String, String> entry : params.entrySet())
			{
				strOutput += "\n * " + entry.getKey() + " : " + entry.getValue();
			}
						
			result.setLog(result.getLog() + "dumb tool subprocess finished successfully\r\n");
			result.setOutput(strOutput);
			result.setStatus(ToolInvokerResult.Status.SUCCESS);
			result.setStop(System.currentTimeMillis());
			return result;
			
		} catch ( Exception e ) {
			//e.printStackTrace();
			result.setLog(result.getLog() + "unexpected error occured during tool execution: " + e.getMessage() + "\r\n");
			result.setStatus(ToolInvokerResult.Status.ERROR);
			result.setStop(System.currentTimeMillis());
		}
		return result;
	}
	
}
