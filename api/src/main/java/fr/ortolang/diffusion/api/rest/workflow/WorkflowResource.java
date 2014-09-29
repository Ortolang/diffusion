package fr.ortolang.diffusion.api.rest.workflow;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
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

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.workflow.WorkflowService;
import fr.ortolang.diffusion.workflow.WorkflowServiceException;
import fr.ortolang.diffusion.workflow.entity.WorkflowDefinition;
import fr.ortolang.diffusion.workflow.entity.WorkflowInstance;

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
		
		GenericCollectionRepresentation<WorkflowDefinition> representation = new GenericCollectionRepresentation<WorkflowDefinition>();
		for (WorkflowDefinition def : defs) {
			representation.addEntry(def);
		}
		representation.setOffset(0);
		representation.setSize(defs.size());
		representation.setLimit(defs.size());
		return Response.ok(representation).build();
	}
	
	@POST
	@Path("/definitions")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response deployWorkflowDefinition(@MultipartForm WorkflowDefinitionFormRepresentation form) throws AccessDeniedException, WorkflowServiceException {
		logger.log(Level.INFO, "deploying workflow definition");
		if (form.getName() == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'name' is mandatory").build();
		}
		
		if (form.getContent() != null) {
			workflow.deployWorkflowDefinition(form.getName(), form.getContent());
		}
		URI location = DiffusionUriBuilder.getRestUriBuilder().path(WorkflowResource.class).path("definitions").build();
		return Response.created(location).build();
	}
	
	@GET
	@Path("/instances")
	@Template(template = "workflows/instances.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listInstances(@QueryParam("definition") String definition, @DefaultValue("true") @QueryParam("active") String active) throws WorkflowServiceException {
		logger.log(Level.INFO, "listing instances");
		List<WorkflowInstance> instances = workflow.listWorkflowInstances(definition, Boolean.parseBoolean(active));
		GenericCollectionRepresentation<WorkflowInstance> representation = new GenericCollectionRepresentation<WorkflowInstance>();
		for (WorkflowInstance instance : instances) {
			representation.addEntry(instance);
		}
		representation.setOffset(0);
		representation.setSize(instances.size());
		representation.setLimit(instances.size());
		return Response.ok(representation).build();
	}

	
	@POST
	@Path("/instances/{definition}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response startInstance(@PathParam("definition") String definition, MultivaluedMap<String, String> params) throws WorkflowServiceException, AccessDeniedException {
		logger.log(Level.INFO, "starting workflow instance for defintion: " + definition);
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
		String id = workflow.startWorkflowInstance(definition, mparams);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(WorkflowResource.class).path(definition).path(id).build();
		return Response.created(newly).build();
	}
	


}
