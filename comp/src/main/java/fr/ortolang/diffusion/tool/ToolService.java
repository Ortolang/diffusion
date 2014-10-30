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
	
	/**
	 * Declare a new tool
	 * @param key String
	 * @param name String
	 * @param description String
	 * @param documentation String
	 * @param invokerClass String
	 * @param formConfig String
	 * @throws ToolServiceException
	 */
	public void declareTool(String key, String name, String description, String documentation, String invokerClass, String formConfig) throws ToolServiceException;
	
	/**
	 * Return a list of available tools
	 * @return List<Tool>
	 * @throws ToolServiceException
	 */
	public List<Tool> listTools() throws ToolServiceException;
	
	/**
	 * Read a tool from a key
	 * @param key String
	 * @return Tool
	 * @throws ToolServiceException
	 * @throws AccessDeniedException
	 */
	public Tool readTool(String key) throws ToolServiceException, AccessDeniedException; 
	
	/**
	 * Return the JSON form from a key
	 * @param key String
	 * @return String
	 * @throws ToolServiceException
	 * @throws AccessDeniedException
	 */
	public String getFormConfig(String key) throws ToolServiceException, AccessDeniedException;
	
	/**
	 * Invoke a tool from a key and a map of parameters
	 * @param key String
	 * @param params	Map<String, String>
	 * @return ToolInvokerResult
	 * @throws ToolServiceException
	 * @throws AccessDeniedException
	 */
	public ToolInvokerResult invokeTool(String key, Map<String, String> params) throws ToolServiceException, AccessDeniedException;

}
