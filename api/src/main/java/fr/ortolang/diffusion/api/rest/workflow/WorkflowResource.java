package fr.ortolang.diffusion.api.rest.workflow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jgroups.util.UUID;

import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.workflow.WorkflowService;
import fr.ortolang.diffusion.workflow.WorkflowServiceException;
import fr.ortolang.diffusion.workflow.entity.WorkflowDefinition;
import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;
import fr.ortolang.diffusion.workflow.entity.WorkflowTask;

@Path("/workflow")
@Produces({ MediaType.APPLICATION_JSON })
public class WorkflowResource {
	
	private Logger logger = Logger.getLogger(WorkflowResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private WorkflowService workflow;
	
	@GET
	@Path("/definitions")
	@Template(template = "workflows/definitions.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listDefinitions() throws WorkflowServiceException {
		logger.log(Level.INFO, "listing definitions");
		List<WorkflowDefinition> defs = workflow.listWorkflowDefinitions();
		
		GenericCollectionRepresentation<WorkflowDefinitionRepresentation> representation = new GenericCollectionRepresentation<WorkflowDefinitionRepresentation>();
		for (WorkflowDefinition def : defs) {
			representation.addEntry(WorkflowDefinitionRepresentation.fromWorkflowDefinition(def));
		}
		representation.setOffset(0);
		representation.setSize(defs.size());
		representation.setLimit(defs.size());
		return Response.ok(representation).build();
	}
	
	@POST
	@Path("/definitions")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createWorkflowDefinition(@MultipartForm WorkflowDefinitionFormRepresentation form) throws AccessDeniedException, WorkflowServiceException {
		logger.log(Level.INFO, "deploying workflow definition");
		String key = UUID.randomUUID().toString();
		
		if (form.getContent() != null) {
			workflow.createWorkflowDefinition(key, form.getContent());
		}
		URI location = DiffusionUriBuilder.getRestUriBuilder().path(WorkflowResource.class).path("definitions").path(key).build();
		return Response.created(location).build();
	}
	
	@GET
	@Path("/definitions/{key}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getDefinition(@PathParam("key") String key) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "getting workflow definition for key: " + key);
		WorkflowDefinition definition = workflow.getWorkflowDefinition(key);
		WorkflowDefinitionRepresentation representation = WorkflowDefinitionRepresentation.fromWorkflowDefinition(definition);
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/definitions/{key}/model")
	public void getDefinitionModel(@PathParam("key") String key, @Context HttpServletResponse response) throws WorkflowServiceException, AccessDeniedException, IOException {
		logger.log(Level.INFO, "getting workflow definition model for key: " + key);
		byte[] model = workflow.getWorkflowDefinitionModel(key);
		
		response.setHeader("Content-Disposition", "attachment; filename=" + key + ".bpmn");
		response.setContentType("text/xml");
		response.setContentLength(model.length);
		ByteArrayInputStream bais = new ByteArrayInputStream(model);
		try {
			IOUtils.copy(bais, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(bais);
		}
		return;
	}
	
	@GET
	@Path("/definitions/{key}/diagram")
	public void getDefinitionDiagram(@PathParam("key") String key, @Context HttpServletResponse response) throws WorkflowServiceException, AccessDeniedException, IOException {
		logger.log(Level.INFO, "getting workflow definition diagram for key: " + key);
		byte[] diagram = workflow.getWorkflowDefinitionDiagram(key);
		
		response.setHeader("Content-Disposition", "attachment; filename=" + key + ".png");
		response.setContentType("image/png");
		response.setContentLength(diagram.length);
		ByteArrayInputStream bais = new ByteArrayInputStream(diagram);
		try {
			IOUtils.copy(bais, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(bais);
		}
		return;
	}
	
	@GET
	@Path("/instances")
	@Template(template = "workflows/instances.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listInstances(@QueryParam("initier") String initier, @QueryParam("definition") String definition, @DefaultValue("true") @QueryParam("active") String active) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "listing instances");
		List<WorkflowInstance> instances = workflow.listWorkflowInstances(initier, definition, Boolean.parseBoolean(active));
		
		GenericCollectionRepresentation<WorkflowInstanceRepresentation> representation = new GenericCollectionRepresentation<WorkflowInstanceRepresentation>();
		for (WorkflowInstance instance : instances) {
			WorkflowInstanceRepresentation rep = WorkflowInstanceRepresentation.fromWorkflowDefinition(instance);
			representation.addEntry(rep);
		}
		representation.setOffset(0);
		representation.setSize(instances.size());
		representation.setLimit(instances.size());
		return Response.ok(representation).build();
	}

	
	@POST
	@Path("/instances")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createInstance(MultivaluedMap<String, String> params) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "creating new workflow instance");
		String key = UUID.randomUUID().toString();
		
		String definition = null;
		if ( !params.containsKey("definition") ) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'definition' is mandatory").build();
		} else {
			definition = params.getFirst("definition");
		}
		
		Map<String, Object> mparams = new HashMap<String, Object> ();
		for ( Entry<String, List<String>> entry : params.entrySet() ) {
			if ( entry.getValue().size() > 0 ) {
				StringBuffer values = new StringBuffer();
				for ( String value : entry.getValue() ) {
					values.append(value).append(",");
				}
				mparams.put(entry.getKey(), values.substring(0, values.length()-1));
			}
		}
		workflow.createWorkflowInstance(key, definition, mparams);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(WorkflowResource.class).path("instances").path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/tasks/candidate")
	@Template(template = "workflows/tasks.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listCandidateTasks() throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "listing candidate tasks");
		List<WorkflowTask> tasks = workflow.listCandidateWorkflowTasks();
		GenericCollectionRepresentation<WorkflowTaskRepresentation> representation = new GenericCollectionRepresentation<WorkflowTaskRepresentation>();
		for (WorkflowTask task : tasks) {
			representation.addEntry(WorkflowTaskRepresentation.fromWorkflowTask(task));
		}
		representation.setOffset(0);
		representation.setSize(tasks.size());
		representation.setLimit(tasks.size());
		return Response.ok(representation).build();
	}
	
//	@GET
//	@Path("/tasks/assigned")
//	@Template(template = "workflows/tasks.vm", types = { MediaType.TEXT_HTML })
//	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
//	public Response listAssignedTasks() throws WorkflowServiceException, AccessDeniedException {
//		logger.log(Level.INFO, "listing assigned tasks");
//		List<WorkflowTask> tasks = workflow.listAssignedWorkflowTasks();
//		GenericCollectionRepresentation<WorkflowTask> representation = new GenericCollectionRepresentation<WorkflowTask>();
//		for (WorkflowTask task : tasks) {
//			representation.addEntry(task);
//		}
//		representation.setOffset(0);
//		representation.setSize(tasks.size());
//		representation.setLimit(tasks.size());
//		return Response.ok(representation).build();
//	}
//	

	
	

}
