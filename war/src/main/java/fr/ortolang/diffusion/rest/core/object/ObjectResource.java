package fr.ortolang.diffusion.rest.core.object;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.util.GenericType;

import com.healthmarketscience.rmiio.RemoteInputStreamServer;
import com.healthmarketscience.rmiio.RemoteOutputStreamServer;
import com.healthmarketscience.rmiio.SimpleRemoteInputStream;
import com.healthmarketscience.rmiio.SimpleRemoteOutputStream;

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.CoreServiceLocal;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.rest.KeysRepresentation;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/core/objects")
@Produces({ MediaType.APPLICATION_JSON })
public class ObjectResource {
	
	private Logger logger = Logger.getLogger(ObjectResource.class.getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private CoreService core;
	@EJB 
	private CoreServiceLocal coreLocal;
 
    public ObjectResource() {
    }
    
    @GET
	@Template( template="core/objects.vm", types={MediaType.TEXT_HTML})
	@Produces(MediaType.TEXT_HTML)
	public Response list() throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing all objects");
		UriBuilder objects = DiffusionUriBuilder.getRestUriBuilder().path(ObjectResource.class);

		KeysRepresentation representation = new KeysRepresentation ();
		representation.addLink(Link.fromUri(objects.clone().build()).rel("create").build());
		return Response.ok(representation).build();
	}
    
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response create( MultipartFormDataInput input ) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	logger.log(Level.INFO, "creating dataobject");
    	String key = UUID.randomUUID().toString();
    	Map<String, List<InputPart>> formParts = input.getFormDataMap();
    	try {
        	String name = "No name provided";
	    	List<InputPart> inParts = formParts.get("name");
	    	for (InputPart inputPart : inParts) {
	    		name = inputPart.getBodyAsString();
	    	}
	    	String description = "No description provided";
	    	inParts = formParts.get("description");
	    	for (InputPart inputPart : inParts) {
	    		description = inputPart.getBodyAsString();
	    	}
	    	InputStream is = null;
	    	inParts = formParts.get("file");
	    	for (InputPart inputPart : inParts) {
	    		is = inputPart.getBody(new GenericType<InputStream>() { });
	    	}
	    	if ( coreLocal != null ) {
	    		logger.log(Level.INFO, "using local core interface for optimisation");
	    		coreLocal.createDataObject(key, name, description, is);
	    	} else {
	    		logger.log(Level.INFO, "using remote core interface");
	    		RemoteInputStreamServer ris = new SimpleRemoteInputStream(is);
	    		core.createDataObject(key, name, description, ris.export());
	    	}
	    	URI newly = DiffusionUriBuilder.getRestUriBuilder().path(ObjectResource.class).path(key).build();
	    	return Response.created(newly).build();
    	} catch ( IOException ioe ) {
    		return Response.serverError().entity(ioe.getMessage()).build();
    	}
    }
    
    @GET
    @Path("/{key}")
    @Template( template="core/object.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response read( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading dataobject with key: " + key);
    	DataObject object = core.readDataObject(key);
    	UriBuilder objects = DiffusionUriBuilder.getRestUriBuilder().path(ObjectResource.class);
    	
    	ObjectRepresentation representation = ObjectRepresentation.fromDataObject(object);
    	representation.addLink(Link.fromUri(objects.clone().path(key).path("content").build()).rel("content").build());
    	representation.addLink(Link.fromUri(objects.clone().path(key).path("metadatas").build()).rel("metadatas").build());
    	return Response.ok(representation).build();
    }
    
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update( @PathParam(value="key") String key, ObjectRepresentation representation ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "updating dataobject with key: " + key);
    	core.updateDataObject(key, representation.getName(), representation.getDescription());
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}")
    public Response delete( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "deleting dataobject with key: " + key);
    	core.deleteDataObject(key);
    	return Response.noContent().build();
    }
    
    @GET
    @Path("/{key}/content")
    public void getContent( @PathParam(value="key") String key, @Context HttpServletResponse response ) throws CoreServiceException, KeyNotFoundException, IOException, AccessDeniedException {
    	logger.log(Level.INFO, "reading dataobject content with key: " + key);
    	DataObject object = core.readDataObject(key);
    	response.setHeader("Content-Disposition", "attachment; filename=" + object.getName());
    	response.setContentLength((int)object.getSize());
    	response.setContentType(object.getContentType());
    	if ( coreLocal != null ) {
    		logger.log(Level.INFO, "using local core interface for optimisation");
    		coreLocal.readDataObjectContent(key, response.getOutputStream());
    	} else {
    		logger.log(Level.INFO, "using remote core interface");
    		RemoteOutputStreamServer ros = new SimpleRemoteOutputStream(response.getOutputStream());
    		core.readDataObjectContent(key, ros.export());
    	}
    }
    
    @POST
    @Path("/{key}/content")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response udpateContent( @PathParam(value="key") String key, MultipartFormDataInput input ) throws CoreServiceException, KeyNotFoundException, IOException, AccessDeniedException {
    	logger.log(Level.INFO, "updating dataobject content with key: " + key);
    	Map<String, List<InputPart>> formParts = input.getFormDataMap();
    	try {
        	InputStream is = null;
	    	List<InputPart> inParts = formParts.get("file");
	    	for (InputPart inputPart : inParts) {
	    		is = inputPart.getBody(new GenericType<InputStream>() { });
	    	}
	    	if ( coreLocal != null ) {
	    		logger.log(Level.INFO, "using local core interface for optimisation");
	    		coreLocal.updateDataObjectContent(key, is);
	    	} else {
	    		logger.log(Level.INFO, "using remote core interface");
	    		RemoteInputStreamServer ris = new SimpleRemoteInputStream(is);
	    		core.updateDataObjectContent(key, ris.export());
	    	}
	    	return Response.noContent().build();
    	} catch ( IOException ioe ) {
    		return Response.serverError().entity(ioe.getMessage()).build();
    	}
    }
    
    @GET
    @Path("/{key}/metadatas")
    @Template( template="core/metas.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
    public Response listMetadatas( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading metadatas of object with key: " + key);
    	DataObject object= core.readDataObject(key);
    	UriBuilder objects = DiffusionUriBuilder.getRestUriBuilder().path(ObjectResource.class);
		
    	KeysRepresentation representation = new KeysRepresentation ();
		for ( String metadata : object.getMetadatas() ) {
			representation.addEntry(metadata, Link.fromUri(objects.clone().path(key).path("metadatas").path(metadata).build()).rel("view").build());
		}
		return Response.ok(representation).build();
    }
    
    @GET
    @Path("/{key}/metadatas/{metadata}")
    public Response getMetadata( @PathParam(value="key") String key, @PathParam(value="metadata") String metadata ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading metadata " + metadata + " of object with key: " + key);
    	DataObject object = core.readDataObject(key);
    	if ( object.getMetadatas().contains(metadata) ) {
    		URI redirect = DiffusionUriBuilder.getRestUriBuilder().path(OrtolangObjectResource.class).path(metadata).build();
    		return Response.seeOther(redirect).build();
    	} else {
    		throw new KeyNotFoundException("this element is not in this collection");
    	}
    }
    
}
