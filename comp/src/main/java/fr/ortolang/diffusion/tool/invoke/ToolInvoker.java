package fr.ortolang.diffusion.tool.invoke;

import java.util.Map;


public interface ToolInvoker {
	
	public ToolInvokerResult invoke(Map<String, String> params);

}
