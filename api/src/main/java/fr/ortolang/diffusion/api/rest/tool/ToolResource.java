package fr.ortolang.diffusion.api.rest.tool;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.tool.ToolService;
import fr.ortolang.diffusion.tool.ToolServiceException;
import fr.ortolang.diffusion.tool.entity.Tool;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

@Path("/tools")
@Produces({ MediaType.APPLICATION_JSON })
public class ToolResource {

	private Logger logger = Logger.getLogger(ToolResource.class.getName());

	@EJB
	private BrowserService browser;
	@EJB
	private SearchService search;
	@EJB
	private SecurityService security;
	@EJB
	private CoreService core;
	@EJB
	private ToolService tool;
	
	
	@Context
	private UriInfo uriInfo;
	
	public ToolResource() {
	}
	
	@GET
	@Template( template="tools/list.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response list() throws ToolServiceException {
		logger.log(Level.INFO, "list availables tools");
		
		List<Tool> tools = tool.listTools();
		
		GenericCollectionRepresentation<Tool> representation = new GenericCollectionRepresentation<Tool> ();
		for(Tool tool: tools) {
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
				
		Tool representation = tool.readTool(key);
		
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
		
		logger.log(Level.INFO, "result of tool " + key + " : " + invokeResult.getOutput());
		final ResponseBuilder response = Response.ok(invokeResult);
		return response.build();
	}
	

}