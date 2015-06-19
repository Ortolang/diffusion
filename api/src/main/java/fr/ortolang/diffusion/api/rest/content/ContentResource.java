package fr.ortolang.diffusion.api.rest.content;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.api.rest.template.TemplateEngine;
import fr.ortolang.diffusion.api.rest.template.TemplateEngineException;

@Path("/content")
@Produces({ MediaType.TEXT_HTML })
public class ContentResource {
	
	private static final Logger LOGGER = Logger.getLogger(ContentResource.class.getName());

	public ContentResource() {
	}
	
	@GET
	public Response workspaces() throws TemplateEngineException {
		LOGGER.log(Level.INFO, "GET /content");
		Map<String, String> root = new HashMap<String, String>();
		root.put("path", "/");
		return Response.ok(TemplateEngine.getInstance().process("collection", root)).build();
	}

}
