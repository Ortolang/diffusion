package fr.ortolang.diffusion.tool;

import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.tool.entity.Tool;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

public interface ToolService extends OrtolangService {
	
	public static final String SERVICE_NAME = "tool";
	public static final String[] OBJECT_TYPE_LIST = new String[] { Tool.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Tool.OBJECT_TYPE, "read,update,delete,invoke" }};
	
	public void declareTool(String key, String name, String description, String documentation, String invokerClass) throws ToolServiceException;
	
	public List<Tool> listTools() throws ToolServiceException;
	
	public Tool readTool(String key) throws ToolServiceException, AccessDeniedException; 
	
	public ToolInvokerResult invokeTool(String key, Map<String, String> params) throws ToolServiceException, AccessDeniedException;

}
