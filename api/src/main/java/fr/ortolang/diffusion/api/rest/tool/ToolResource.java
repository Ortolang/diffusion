package fr.ortolang.diffusion.api.rest.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jackson.map.ObjectMapper;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.tool.ToolService;
import fr.ortolang.diffusion.tool.ToolServiceException;
import fr.ortolang.diffusion.tool.entity.Tool;
import fr.ortolang.diffusion.tool.entity.ToolPlugin;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

@Path("/tools")
@Produces({ MediaType.APPLICATION_JSON })
public class ToolResource {

	private Logger logger = Logger.getLogger(ToolResource.class.getName());

	@EJB
	private ToolService tool;
	@Context
	private UriInfo uriInfo;
	
	public ToolResource() {
	}
	
	@GET
	@Template( template="tools/list.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response listPlugin() throws ToolServiceException {
		logger.log(Level.INFO, "list availables tool plugins : ");
		
		List<ToolPlugin> toolplugins = tool.listToolPlugins();
		
		GenericCollectionRepresentation<ToolPlugin> representation = new GenericCollectionRepresentation<ToolPlugin> ();
		for(ToolPlugin tool: toolplugins) {
			logger.log(Level.INFO, tool.getName());
		    representation.addEntry(tool);
		}
		representation.setOffset(0);
		representation.setSize(toolplugins.size());
		representation.setLimit(toolplugins.size());
		
		Response response = Response.ok(representation).build();
		return response;
	}
	
	@GET
	@Path("/list")	
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response list() throws ToolServiceException {
		logger.log(Level.INFO, "list availables tools from servers : ");
		
		List<Tool> tools = tool.listTools();
		
		GenericCollectionRepresentation<Tool> representation = new GenericCollectionRepresentation<Tool> ();
		for(Tool tool: tools) {
			logger.log(Level.INFO, tool.getName());
		    representation.addEntry(tool);
		}
		representation.setOffset(0);
		representation.setSize(tools.size());
		representation.setLimit(tools.size());
		
		Response response = Response.ok(representation).build();
		return response;
	}
	
	@GET
	@Path("/{key}")
	@Template( template="tools/detail.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response get(@PathParam(value = "key") String key) throws ToolServiceException, AccessDeniedException {
		logger.log(Level.INFO, "read tool for key: " + key);
				
		ToolPlugin representation = tool.readTool(key);
		
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/{key}/config")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response getConfig(@PathParam(value = "key") String key) throws ToolServiceException, AccessDeniedException {
		logger.log(Level.INFO, "get plugin config for key: " + key);
		String representation = tool.getFormConfig(key);
		return Response.ok(representation).build();
	}
	
	@POST
	@Path("/{key}/config-new")
	@Consumes("application/json")
	public Response postConfig(@PathParam(value = "key") String key, String form) throws IOException, ToolServiceException, AccessDeniedException {
		logger.log(Level.INFO, "post plugin config for key: " + key);
		
		Map<String, String> params = new HashMap<>();
	    ObjectMapper mapper = new ObjectMapper();
	    params = mapper.readValue(form, new TypeReference<HashMap<String, String>>() { });
		
	    logger.log(Level.INFO, "invoking tool " + key + " with params : " + params);
		ToolInvokerResult invokeResult = tool.invokeTool(key, params);
		
		logger.log(Level.INFO, "result of tool " + key + " : " + invokeResult.getLog());
		final ResponseBuilder response = Response.ok(invokeResult);
		return response.build();
	}
	
	@GET
	@Path("/{key}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public void download(@PathParam(value = "key") String key, @QueryParam(value = "path") String path, @QueryParam(value = "name") String name,@Context HttpServletResponse response) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException,
			OrtolangException, DataNotFoundException, IOException, CoreServiceException, InvalidPathException {
		logger.log(Level.INFO, "GET /tools/" + key + "/download?path=" + path + "&name=" + name);
		if (path == null) {
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "parameter 'path' is mandatory");
			return;
		}
		
		File fileResult = new File(path);		
		if (name == null) {
			response.setHeader("Content-Disposition", "attachment; filename=" + fileResult.getName());					
		} else {
			response.setHeader("Content-Disposition", "attachment; filename=" + name);					
		}	
		//TODO this seems to be unusefull...
//		FileInputStream fis = new FileInputStream(fileResult);
//        InputStreamReader isr = new InputStreamReader(fis);
//		response.setCharacterEncoding(isr.getEncoding());
		response.setContentType(new MimetypesFileTypeMap().getContentType(fileResult));
		response.setContentLength((int) fileResult.length());
		InputStream input = new FileInputStream(path);
		try {
			IOUtils.copy(input, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(input);
		}
	}
}