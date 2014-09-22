package fr.ortolang.diffusion.api.rest.process;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
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
import fr.ortolang.diffusion.runtime.entity.ProcessInstance;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;

@Path("/processes")
@Produces({ MediaType.APPLICATION_JSON })
public class ProcessResource {

	private Logger logger = Logger.getLogger(ProcessResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private CoreService core;
	@EJB
	private RuntimeService runtime;

	public ProcessResource() {
	}

	@GET
	@Template( template="processes/list.vm", types={MediaType.TEXT_HTML})
	@Produces(MediaType.TEXT_HTML)
	public Response list(@QueryParam(value = "initier") String initier) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing processes for initier : " + initier);
		
		GenericCollectionRepresentation<ProcessRepresentation> representation = new GenericCollectionRepresentation<ProcessRepresentation>();
		List<String> keys = Collections.emptyList();
		if ( initier != null ) {
			keys = runtime.findProcessInstancesByInitier(initier);
			for ( String key : keys ) {
				ProcessInstance process = runtime.readProcessInstance(key);
				representation.addEntry(ProcessRepresentation.fromProcess(process));
			}
		} else {
			keys = runtime.findAllProcessInstances();
			for ( String key : keys ) {
				ProcessInstance process = runtime.readProcessInstance(key);
				representation.addEntry(ProcessRepresentation.fromProcess(process));
			}
		}
		representation.setOffset(0);
		representation.setSize(keys.size());
		representation.setLimit(keys.size());
		return Response.ok(representation).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createProcess(MultivaluedMap<String, String> params) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.INFO, "creating process (application/x-www-form-urlencoded)");
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
		runtime.createProcessInstance(key, mparams.get("name"), mparams.get("type"), mparams);
		runtime.startProcessInstance(key);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(ProcessResource.class).path(key).build();
		return Response.created(newly).build();
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response createProcess(MultipartFormDataInput input) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException, KeyNotFoundException, IOException, CoreServiceException, DataCollisionException {
		logger.log(Level.INFO, "creating process (multipart/form-data)");
		String key = UUID.randomUUID().toString();
		
		Map<String, String> mparams = new HashMap<String, String> ();
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
		runtime.createProcessInstance(key, mparams.get("name"), mparams.get("type"), mparams);
		runtime.startProcessInstance(key);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(ProcessResource.class).path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/{key}")
	@Template(template = "processes/detail.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getProcess(@PathParam(value = "key") String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading process for key: " + key);
		ProcessInstance process = runtime.readProcessInstance(key);
		ProcessRepresentation representation = ProcessRepresentation.fromProcess(process);
		return Response.ok(representation).build();
	}

}
