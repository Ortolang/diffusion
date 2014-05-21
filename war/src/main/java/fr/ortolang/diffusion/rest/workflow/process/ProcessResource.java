package fr.ortolang.diffusion.rest.workflow.process;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.rest.KeysPaginatedRepresentation;
import fr.ortolang.diffusion.rest.KeysRepresentation;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.workflow.WorkflowService;
import fr.ortolang.diffusion.workflow.WorkflowServiceException;
import fr.ortolang.diffusion.workflow.entity.Process;

@Path("/workflow/processs")
@Produces({ MediaType.APPLICATION_JSON })
public class ProcessResource {

	private Logger logger = Logger.getLogger(ProcessResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private WorkflowService workflow;

	public ProcessResource() {
	}

	@GET
	@Template( template="workflow/processes.vm", types={MediaType.TEXT_HTML})
	@Produces(MediaType.TEXT_HTML)
	public Response list(@QueryParam(value = "initier") String initier) throws WorkflowServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing processes for initier : " + initier);
		UriBuilder links = DiffusionUriBuilder.getRestUriBuilder().path(ProcessResource.class);
		KeysPaginatedRepresentation representation = new KeysPaginatedRepresentation ();
		if ( initier != null ) {
			List<String> keys = workflow.findProcessForInitier(initier);
			
			for ( String key : keys ) {
				representation.addEntry(key, javax.ws.rs.core.Link.fromUri(links.clone().path(key).build()).rel("view").build());
			}
		}

//		KeysRepresentation representation = new KeysRepresentation ();
		representation.addLink(javax.ws.rs.core.Link.fromUri(links.clone().build()).rel("create").build());
		return Response.ok(representation).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createProcess(MultivaluedMap<String, String> params) throws WorkflowServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating process");
		String key = UUID.randomUUID().toString();
		Map<String, String> mparams = new HashMap<String, String> ();
		for ( Entry<String, List<String>> entry : params.entrySet() ) {
			if ( entry.getValue().size() > 0 ) {
				StringBuffer values = new StringBuffer();
				for ( String value : entry.getValue() ) {
					values.append(value).append(",");
				}
				mparams.put(entry.getKey(), values.substring(0, values.length()-1));
			}
		}
		workflow.createProcess(key, mparams.get("name"), mparams.get("type"), mparams);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(ProcessResource.class).path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/{key}")
	@Template(template = "workflow/process.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getProcess(@PathParam(value = "key") String key) throws WorkflowServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading process for key: " + key);
		Process process = workflow.readProcess(key);
//		UriBuilder processes = DiffusionUriBuilder.getUriBuilder().path(ProcessResource.class);

		ProcessRepresentation representation = ProcessRepresentation.fromProcess(process);
		return Response.ok(representation).build();
	}

}
