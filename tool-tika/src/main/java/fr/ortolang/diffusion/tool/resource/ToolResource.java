package fr.ortolang.diffusion.tool.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/tika")
@Produces({ MediaType.APPLICATION_JSON })
public class ToolResource {
	
	private static final String DESCRITPION_BUNDLE_NAME = "description";
	private static final String DESCRITPION_BUNDLE_PROPERTY_NAME = "name";
	private static final String DESCRITPION_BUNDLE_PROPERTY_DESCRIPTION = "description";
	private static final String DESCRITPION_BUNDLE_PROPERTY_DOCUMENTATION = "documentation";
	
	private static Logger logger = Logger.getLogger(ToolResource.class.getName());
	private static Map<String, ToolDescription> descriptions = new HashMap<String, ToolDescription> ();
	
	@Context
	private UriInfo uriInfo;
	
	public ToolResource() {
	}
	
	@GET
	@Path("/description")
	public ToolDescription description(@DefaultValue("fr") @QueryParam("language") String language) {
		logger.log(Level.INFO, "GET /description");
		if ( !descriptions.containsKey(language) ) {
			Locale locale = Locale.forLanguageTag(language);
			ResourceBundle bundle = ResourceBundle.getBundle(DESCRITPION_BUNDLE_NAME, locale);
			ToolDescription description = new ToolDescription();
			description.setName(bundle.getString(DESCRITPION_BUNDLE_PROPERTY_NAME));
			description.setDescription(bundle.getString(DESCRITPION_BUNDLE_PROPERTY_DESCRIPTION));
			description.setDocumentation(bundle.getString(DESCRITPION_BUNDLE_PROPERTY_DOCUMENTATION));
			descriptions.put(language, description);
		} 
		return descriptions.get(language);
	}
	
	@GET
	@Path("/execution-form")
	public Response executionForm() throws IOException {
		logger.log(Level.INFO, "GET /execution-form");
		InputStream is = getClass().getClassLoader().getResourceAsStream("execute.json");
		String config;
		config = IOUtils.toString(is);
		return Response.ok(config).build();
	}
	
	@GET
	@Path("/result-form")
	public Response resultForm() throws IOException {
		logger.log(Level.INFO, "GET /result-form");
		InputStream is = getClass().getClassLoader().getResourceAsStream("result.json");
		String config;
		config = IOUtils.toString(is);
		return Response.ok(config).build();
	}
	
	@GET
	@Path("/jobs")
	public Response executions() {
		logger.log(Level.INFO, "GET /jobs");
		return Response.ok(Collections.EMPTY_SET).build();
	}
	
	@POST
	@Path("/jobs")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response executions(MultipartFormDataInput form) {
		logger.log(Level.INFO, "POST /jobs");
		return Response.ok(Collections.EMPTY_SET).build();
	}
	
	@GET
	@Path("/jobs/{id}")
	public Response execution(@PathParam("id") String id) {
		logger.log(Level.INFO, "GET /jobs/" + id);
		return Response.ok().build();
	}
	

}
