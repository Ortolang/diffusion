package fr.ortolang.diffusion.api.rest.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.transaction.UserTransaction;
import javax.ws.rs.Consumes;
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

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.runtime.entity.ProcessType;
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
	@Resource
	private UserTransaction userTx;
	
	@GET
	@Path("/types")
	@Template(template = "runtime/types.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listDefinitions() throws RuntimeServiceException {
		logger.log(Level.INFO, "GET /runtime/types");
		List<ProcessType> types = runtime.listProcessTypes();
		
		GenericCollectionRepresentation<ProcessTypeRepresentation> representation = new GenericCollectionRepresentation<ProcessTypeRepresentation>();
		for (ProcessType type : types) {
			representation.addEntry(ProcessTypeRepresentation.fromProcessType(type));
		}
		representation.setOffset(0);
		representation.setSize(types.size());
		representation.setLimit(types.size());
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/processes")
	@Template(template = "runtime/processes.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listProcesses(@QueryParam("state") String state) throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.INFO, "GET /runtime/processes");
		List<Process> instances;
		if ( state != null ) {
			instances = runtime.listProcesses(State.valueOf(state));
		} else {
			instances = runtime.listProcesses(null);
		}
		
		GenericCollectionRepresentation<ProcessRepresentation> representation = new GenericCollectionRepresentation<ProcessRepresentation>();
		for (Process instance : instances) {
			ProcessRepresentation rep = ProcessRepresentation.fromProcess(instance);
			representation.addEntry(rep);
		}
		representation.setOffset(0);
		representation.setSize(instances.size());
		representation.setLimit(instances.size());
		return Response.ok(representation).build();
	}

	
	@POST
	@Path("/processes")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createProcess(MultivaluedMap<String, String> params) throws RuntimeServiceException, AccessDeniedException, KeyAlreadyExistsException {
		logger.log(Level.INFO, "POST(application/x-www-form-urlencoded) /runtime/processes");
		String key = UUID.randomUUID().toString();
		
		String definition = null;
		if ( !params.containsKey("process-type") ) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-type' is mandatory").build();
		} else {
			definition = params.remove("process-type").get(0);
		}
		String name = null;
		if ( !params.containsKey("process-name") ) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-name' is mandatory").build();
		} else {
			name = params.remove("process-name").get(0);
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
		
		try {
			runtime.createProcess(key, definition, name);
			runtime.startProcess(key, mparams);
			URI newly = DiffusionUriBuilder.getRestUriBuilder().path(RuntimeResource.class).path("processes").path(key).build();
			return Response.created(newly).build();
		} catch (SecurityException | IllegalStateException e) {
			throw new RuntimeServiceException(e);
		}
	}
	
	@POST
	@Path("/processes")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response startInstance(MultipartFormDataInput input) throws RuntimeServiceException, AccessDeniedException, KeyAlreadyExistsException, IOException, CoreServiceException, DataCollisionException {
		logger.log(Level.INFO, "POST(multipart/form-data) /runtime/processes");
		String key = UUID.randomUUID().toString();
		
		Map<String, Object> mparams = new HashMap<String, Object> ();
		Map<String, List<InputPart>> form = input.getFormDataMap();
		
		String definition = null;
		if ( !form.containsKey("process-type") ) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-type' is mandatory").build();
		} else {
			definition = form.remove("process-type").get(0).getBodyAsString();
		}
		String name = null;
		if ( !form.containsKey("process-name") ) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-name' is mandatory").build();
		} else {
			name = form.remove("process-name").get(0).getBodyAsString();
		}
		
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
		runtime.createProcess(key, definition, name);
		runtime.startProcess(key, mparams);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(RuntimeResource.class).path("processes").path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/processes/{key}")
	@Template(template = "runtime/process.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listInstances(@PathParam("key") String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.INFO, "GET /runtime/processes/" + key);
		Process process = runtime.readProcess(key);
		ProcessRepresentation representation = ProcessRepresentation.fromProcess(process);
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/tasks")
	@Template(template = "runtime/tasks.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listCandidateTasks() throws RuntimeServiceException, AccessDeniedException {
		logger.log(Level.INFO, "GET /runtime/tasks");
		List<HumanTask> ctasks = runtime.listCandidateTasks();
		List<HumanTask> atasks = runtime.listAssignedTasks();
		GenericCollectionRepresentation<HumanTaskRepresentation> representation = new GenericCollectionRepresentation<HumanTaskRepresentation>();
		for (HumanTask task : ctasks) {
			representation.addEntry(HumanTaskRepresentation.fromHumanTask(task));
		}
		for (HumanTask task : atasks) {
			representation.addEntry(HumanTaskRepresentation.fromHumanTask(task));
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
			runtime.claimTask(id);
		} else if ( action.getAction().equals("complete") ) {
			Map<String, Object> params = new HashMap<String, Object> ();
			for ( ProcessVariableRepresentation variable : action.getVariables() ) {
				params.put(variable.getName(), variable.getTypedValue());
			}
			runtime.completeTask(id, params);
		} else {
			return Response.status(Status.BAD_REQUEST).entity("action unavailable").build();
		}
		return Response.ok().build();
	}
	
}
