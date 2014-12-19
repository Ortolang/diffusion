package fr.ortolang.diffusion.tool.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.*;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import fr.ortolang.diffusion.tool.ToolConfig;
import fr.ortolang.diffusion.tool.job.ToolJobException;
import fr.ortolang.diffusion.tool.job.ToolJobService;
import fr.ortolang.diffusion.tool.job.entity.ToolJob;

@Path("/tika")
@Produces({ MediaType.APPLICATION_JSON })
public class ToolResource {
	
	private static final String DESCRIPTION_BUNDLE_NAME = "description";
	private static final String DESCRIPTION_BUNDLE_PROPERTY_NAME = "name";
	private static final String DESCRIPTION_BUNDLE_PROPERTY_DESCRIPTION = "description";
	private static final String DESCRIPTION_BUNDLE_PROPERTY_DOCUMENTATION = "documentation";
	
	private static Logger logger = Logger.getLogger(ToolResource.class.getName());
	private static Map<String, ToolDescription> descriptions = new HashMap<String, ToolDescription> ();
	
	@Context
	private UriInfo uriInfo;

	@EJB
	private ToolJobService tjob;
	
	public ToolResource() {
	}
	
	@GET
	@Path("/description")
	public ToolDescription description(@DefaultValue("fr") @QueryParam("language") String language) {
		logger.log(Level.INFO, "GET /description");
		if ( !descriptions.containsKey(language) ) {
			Locale locale = Locale.forLanguageTag(language);
			ResourceBundle bundle = ResourceBundle.getBundle(DESCRIPTION_BUNDLE_NAME, locale);
			ToolDescription description = new ToolDescription();
			description.setName(bundle.getString(DESCRIPTION_BUNDLE_PROPERTY_NAME));
			description.setDescription(bundle.getString(DESCRIPTION_BUNDLE_PROPERTY_DESCRIPTION));
			description.setDocumentation(bundle.getString(DESCRIPTION_BUNDLE_PROPERTY_DOCUMENTATION));
			descriptions.put(language, description);
		} 
		return descriptions.get(language);
	}
	
	@GET
	@Path("/execution-form")
	@Produces(MediaType.APPLICATION_JSON)
	public Response executionForm() throws IOException {
		logger.log(Level.INFO, "GET /execution-form");
		InputStream is = getClass().getClassLoader().getResourceAsStream("execute.json");
		String config;
		config = IOUtils.toString(is);
		return Response.ok(config).build();
	}
	
	@GET
	@Path("/result-form")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resultForm() throws IOException {
		logger.log(Level.INFO, "GET /result-form");
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("result.json");
		String config;
		config = IOUtils.toString(is);
		return Response.ok(config).build();
	}
	
	@GET
	@Path("/jobs")
	public Response executions() throws ToolJobException {
		logger.log(Level.INFO, "GET /jobs");
		List<ToolJob> jobs = tjob.list();
		GenericCollectionRepresentation<ToolJob> representation = new GenericCollectionRepresentation<ToolJob>();
		for (ToolJob job : jobs) {
			representation.addEntry(job);
		}
		representation.setOffset(0);
		representation.setSize(jobs.size());
		representation.setLimit(jobs.size());
		return Response.ok(representation).build();
	}
	
	@POST
	@Path("/jobs")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response executions(@QueryParam(value = "priority") int priority, MultivaluedMap<String, String> form) 
			throws ToolJobException, JsonParseException, JsonMappingException, IOException {
		logger.log(Level.INFO, "POST /jobs");

		Map<String,String> parameters = new HashMap<String,String>();
		Iterator<String> it = form.keySet().iterator();
		while(it.hasNext()){
			String theKey = (String)it.next();
			parameters.put(theKey,form.getFirst(theKey));
		}
		
	    logger.log(Level.INFO, "sumbitting new job in queue with priority " + priority + " and with params : " + parameters);
	    tjob.submit(priority, parameters);
		
		List<ToolJob> jobs = tjob.list();
		GenericCollectionRepresentation<ToolJob> representation = new GenericCollectionRepresentation<ToolJob>();
		for (ToolJob job : jobs) {
			representation.addEntry(job);
		}
		representation.setOffset(0);
		representation.setSize(jobs.size());
		representation.setLimit(jobs.size());
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/jobs/{id}")
	public Response execution(@PathParam("id") String id) throws ToolJobException {
		logger.log(Level.INFO, "GET /jobs/" + id);
		ToolJob job = tjob.read(id);
		return Response.ok(job).build();
	}
	
	@GET
	@Path("/jobs/{id}/result")
	public Response result(@PathParam("id") String id) throws ToolJobException, IOException {
		logger.log(Level.INFO, "GET /jobs/" + id + "/result");
		String base = ToolConfig.getInstance().getProperty("tool.working.space.path");
		String resultFileName = ToolConfig.getInstance().getProperty("tool.result.filename");
		String resFile = Paths.get(base, id, resultFileName).toString();
		
		InputStream is = new FileInputStream(resFile);
		String result;
		result = IOUtils.toString(is);
		return Response.ok(result).build();
	}
	
	@GET
	@Path("/jobs/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public void download(@QueryParam(value = "path") String path, @Context HttpServletResponse response) throws IOException {
		logger.log(Level.INFO, "GET /jobs/download?path=" + path);
		if (path == null) {
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "parameter 'path' is mandatory");
			return;
		}
		
		File fileResult = new File(path);	
		if(!fileResult.exists()){
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "file " + path + " has been removed");
			return;
		}
		response.setHeader("Content-Disposition", "attachment; filename=" + fileResult.getName());	
		
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
