package fr.ortolang.diffusion.api.rest.runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jgroups.util.UUID;

import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.ProcessDefinition;
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.runtime.entity.ProcessTask;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;

@Path("/runtime")
@Produces({ MediaType.APPLICATION_JSON })
public class RuntimeResource {
	
	private Logger logger = Logger.getLogger(RuntimeResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private CoreService core;
	@EJB
	private RuntimeService runtime;
	
	@GET
	@Path("/definitions")
	@Template(template = "runtime/definitions.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listDefinitions() throws RuntimeServiceException {
		logger.log(Level.INFO, "GET /runtime/definitions");
		List<ProcessDefinition> defs = runtime.listProcessDefinitions();
		
		GenericCollectionRepresentation<ProcessDefinitionRepresentation> representation = new GenericCollectionRepresentation<ProcessDefinitionRepresentation>();
		for (ProcessDefinition def : defs) {
			representation.addEntry(ProcessDefinitionRepresentation.fromProcessDefinition(def));
		}
		representation.setOffset(0);
		representation.setSize(defs.size());
		representation.setLimit(defs.size());
		return Response.ok(representation).build();
	}
	
	@POST
	@Path("/definitions")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createProcessDefinition(@MultipartForm ProcessDefinitionFormRepresentation form) throws AccessDeniedException, RuntimeServiceException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "POST /runtime/definitions");
		String key = UUID.randomUUID().toString();
		
		if (form.getContent() != null) {
			runtime.createProcessDefinition(key, form.getContent());
		}
		URI location = DiffusionUriBuilder.getRestUriBuilder().path(RuntimeResource.class).path("definitions").path(key).build();
		return Response.created(location).build();
	}
	
	@GET
	@Path("/definitions/{key}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getDefinition(@PathParam("key") String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.INFO, "GET /runtime/definitions/" + key);
		ProcessDefinition definition = runtime.readProcessDefinition(key);
		ProcessDefinitionRepresentation representation = ProcessDefinitionRepresentation.fromProcessDefinition(definition);
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/definitions/{key}/model")
	public void getDefinitionModel(@PathParam("key") String key, @Context HttpServletResponse response) throws RuntimeServiceException, AccessDeniedException, IOException, KeyNotFoundException {
		logger.log(Level.INFO, "GET /runtime/definitions/" + key + "/model");
		byte[] model = runtime.readProcessDefinitionModel(key);
		
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
	public void getDefinitionDiagram(@PathParam("key") String key, @Context HttpServletResponse response) throws RuntimeServiceException, AccessDeniedException, IOException, KeyNotFoundException {
		logger.log(Level.INFO, "GET /runtime/definitions/" + key + "/diagram");
		byte[] diagram = runtime.readProcessDefinitionDiagram(key);
		
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
	@Template(template = "runtime/instances.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listInstances(@QueryParam("initier") String initier, @DefaultValue("true") @QueryParam("active") String active) throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.INFO, "GET /runtime/instances");
		List<ProcessInstance> instances = runtime.listProcessInstances(initier, Boolean.parseBoolean(active));
		
		GenericCollectionRepresentation<ProcessInstanceRepresentation> representation = new GenericCollectionRepresentation<ProcessInstanceRepresentation>();
		for (ProcessInstance instance : instances) {
			ProcessInstanceRepresentation rep = ProcessInstanceRepresentation.fromProcessDefinition(instance);
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
	public Response startInstance(MultivaluedMap<String, String> params) throws RuntimeServiceException, AccessDeniedException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "POST(application/x-www-form-urlencoded) /runtime/instances");
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
		runtime.startProcessInstance(key, definition, mparams);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(RuntimeResource.class).path("instances").path(key).build();
		return Response.created(newly).build();
	}
	
	@POST
	@Path("/instances")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response startInstance(MultipartFormDataInput input) throws RuntimeServiceException, AccessDeniedException, KeyAlreadyExistsException, IOException, CoreServiceException, DataCollisionException {
		logger.log(Level.INFO, "POST(multipart/form-data) /runtime/instances");
		String key = UUID.randomUUID().toString();
		
		Map<String, Object> mparams = new HashMap<String, Object> ();
		Map<String, List<InputPart>> form = input.getFormDataMap();
		for ( Entry<String, List<InputPart>> entry : form.entrySet() ) {
			if ( entry.getValue().size() > 0 ) {
				StringBuffer values = new StringBuffer();
				for ( InputPart value : entry.getValue() ) {
					if ( value.getHeaders().containsKey("Content-Disposition") && value.getHeaders().getFirst("Content-Disposition").contains("filename=") ) {
						logger.log(Level.FINE, "seems this part [" + entry.getKey() + "] is a file");
						InputStream is = value.getBody(InputStream.class, null);
						String hash = core.put(is);
						values.append(hash).append(",");
					} else {
						logger.log(Level.FINE, "seems this part  [" + entry.getKey() + "] is a simple text value");
						values.append(value.getBodyAsString()).append(",");
					}
				}
				mparams.put(entry.getKey(), values.substring(0, values.length()-1));
			}
		}
		runtime.startProcessInstance(key, (String)mparams.get("definition"), mparams);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(RuntimeResource.class).path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/tasks")
	@Template(template = "runtime/tasks.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listCandidateTasks() throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.INFO, "GET /runtime/tasks");
		List<ProcessTask> ctasks = runtime.listCandidateProcessTasks();
		List<ProcessTask> atasks = runtime.listAssignedProcessTasks();
		GenericCollectionRepresentation<ProcessTaskRepresentation> representation = new GenericCollectionRepresentation<ProcessTaskRepresentation>();
		for (ProcessTask task : ctasks) {
			representation.addEntry(ProcessTaskRepresentation.fromProcessTask(task));
		}
		for (ProcessTask task : atasks) {
			representation.addEntry(ProcessTaskRepresentation.fromProcessTask(task));
		}
		representation.setOffset(0);
		representation.setSize(ctasks.size()+atasks.size());
		representation.setLimit(ctasks.size()+atasks.size());
		return Response.ok(representation).build();
	}
	
	@POST
	@Path("/tasks/{id}")
	@Consumes( MediaType.APPLICATION_JSON)
	public Response performTaskAction(@PathParam("id") String id, ProcessTaskActionRepresentation action) throws RuntimeServiceException {
		logger.log(Level.INFO, "POST /runtime/tasks");
		if ( action.getAction().equals("claim") ) {
			runtime.claimProcessTask(id);
		} else if ( action.getAction().equals("complete") ) {
			Map<String, Object> params = new HashMap<String, Object> ();
			for ( ProcessVariableRepresentation variable : action.getVariables() ) {
				params.put(variable.getName(), variable.getTypedValue());
			}
			runtime.completeProcessTask(id, params);
		} else {
			return Response.status(Status.BAD_REQUEST).entity("action unavailable").build();
		}
		return Response.ok().build();
	}
	
}
