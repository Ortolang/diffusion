package fr.ortolang.diffusion.rest.core.metadata;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
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
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.rest.api.OrtolangCollectionRepresentation;
import fr.ortolang.diffusion.rest.api.OrtolangLinkRepresentation;
import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/core/metadatas")
@Produces({ MediaType.APPLICATION_JSON })
public class MetadataResource {
	
	private Logger logger = Logger.getLogger(MetadataResource.class.getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private CoreService core;
	@EJB 
	private CoreServiceLocal coreLocal;
 
    public MetadataResource() {
    }
    
    @GET
	@Template( template="core/metadatas.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
	public Response list(@QueryParam(value = "target") String target) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing metadatas for target: " + target);
		UriBuilder metadatas = DiffusionUriBuilder.getRestUriBuilder().path(MetadataResource.class);
		
		OrtolangCollectionRepresentation representation = new OrtolangCollectionRepresentation ();
		representation.setStart(0);
		representation.setSize(0);
		representation.setTotalSize(0);
		if ( target != null ) {
			List<String> keys = core.findMetadataObjectsForTarget(target);
			representation.setSize(keys.size());
			representation.setTotalSize(keys.size());
			for ( String key : keys ) {
				representation.addEntry(key, OrtolangLinkRepresentation.fromUri(metadatas.clone().path(key).build()).rel("view"));
			}
		}
		representation.addLink(OrtolangLinkRepresentation.fromUri(metadatas.clone().build()).rel("create"));
		return Response.ok(representation).build();
	}
    
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response create( MultipartFormDataInput input ) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	logger.log(Level.INFO, "creating metadataobject");
    	String key = UUID.randomUUID().toString();
    	Map<String, List<InputPart>> formParts = input.getFormDataMap();
    	try {
        	String name = "No name provided";
	    	List<InputPart> inParts = formParts.get("name");
	    	for (InputPart inputPart : inParts) {
	    		name = inputPart.getBodyAsString();
	    	}
	    	String target = null;
	    	inParts = formParts.get("target");
	    	for (InputPart inputPart : inParts) {
	    		target = inputPart.getBodyAsString();
	    	}
	    	//TODO que faire du format ?? le mettre dans les param du createMetadata ou le generer auto ??
//	    	String format = null;
//	    	inParts = formParts.get("format");
//	    	if(inParts!=null) {
//	    	for (InputPart inputPart : inParts) {
//	    		format = inputPart.getBodyAsString();
//	    	}
	    	InputStream is = null;
	    	inParts = formParts.get("file");
	    	for (InputPart inputPart : inParts) {
	    		is = inputPart.getBody(new GenericType<InputStream>() { });
	    	}
	    	if ( coreLocal != null ) {
	    		logger.log(Level.INFO, "using local core interface for optimisation");
	    		coreLocal.createMetadataObject(key, name, is, target);
	    	} else {
	    		logger.log(Level.INFO, "using remote core interface");
	    		RemoteInputStreamServer ris = new SimpleRemoteInputStream(is);
	    		core.createMetadataObject(key, name, ris.export(), target);
	    	}
	    	URI newly = DiffusionUriBuilder.getRestUriBuilder().path(MetadataResource.class).path(key).build();
	    	return Response.created(newly).build();
    	} catch ( IOException ioe ) {
    		return Response.serverError().entity(ioe.getMessage()).build();
    	}
    }
    
    @GET
    @Path("/{key}")
    @Template( template="core/metadata.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response read( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading metadataobject with key: " + key);
    	MetadataObject meta = core.readMetadataObject(key);
    	UriBuilder metadatas = DiffusionUriBuilder.getRestUriBuilder().path(MetadataResource.class);
    	
    	MetadataRepresentation representation = MetadataRepresentation.fromMetadataObject(meta);
    	representation.addLink(OrtolangLinkRepresentation.fromUri(metadatas.clone().path(key).path("target").build()).rel("target"));
    	representation.addLink(OrtolangLinkRepresentation.fromUri(metadatas.clone().path(key).path("content").build()).rel("content"));
    	return Response.ok(representation).build();
    }
    
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update( @PathParam(value="key") String key, MetadataRepresentation representation ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "updating metadataobject with key: " + key);
    	core.updateMetadataObject(key, representation.getName());
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}")
    public Response delete( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "deleting metadataobject with key: " + key);
    	core.deleteMetadataObject(key);
    	return Response.noContent().build();
    }
    
    @GET
    @Path("/{key}/content")
    public void readContent( @PathParam(value="key") String key, @Context HttpServletResponse response ) throws CoreServiceException, KeyNotFoundException, IOException, AccessDeniedException {
    	logger.log(Level.INFO, "reading metadataobject content with key: " + key);
    	MetadataObject meta = core.readMetadataObject(key);
    	response.setHeader("Content-Disposition", "attachment; filename=" + meta.getName());
    	response.setContentLength((int)meta.getSize());
    	response.setContentType(meta.getContentType());
    	if ( coreLocal != null ) {
    		logger.log(Level.INFO, "using local core interface for optimisation");
    		coreLocal.readMetadataObjectContent(key, response.getOutputStream());
    	} else {
    		logger.log(Level.INFO, "using remote core interface");
    		RemoteOutputStreamServer ros = new SimpleRemoteOutputStream(response.getOutputStream());
    		core.readMetadataObjectContent(key, ros.export());
    	}
    	
    }
    
    @POST
    @Path("/{key}/content")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response udpateContent( @PathParam(value="key") String key, MultipartFormDataInput input ) throws CoreServiceException, KeyNotFoundException, IOException, AccessDeniedException {
    	logger.log(Level.INFO, "updating metadataobject content with key: " + key);
    	Map<String, List<InputPart>> formParts = input.getFormDataMap();
    	try {
        	InputStream is = null;
	    	List<InputPart> inParts = formParts.get("file");
	    	for (InputPart inputPart : inParts) {
	    		is = inputPart.getBody(new GenericType<InputStream>() { });
	    	}
	    	if ( coreLocal != null ) {
	    		logger.log(Level.INFO, "using local core interface for optimisation");
	    		coreLocal.updateMetadataObjectContent(key, is);
	    	} else {
	    		logger.log(Level.INFO, "using remote core interface");
	    		RemoteInputStreamServer ris = new SimpleRemoteInputStream(is);
	    		core.updateMetadataObjectContent(key, ris.export());
	    	}
	    	return Response.noContent().build();
    	} catch ( IOException ioe ) {
    		return Response.serverError().entity(ioe.getMessage()).build();
    	}
    }
    
    @GET
    @Path("/{key}/target")
    public Response target( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading target for metadataobject with key: " + key);
    	MetadataObject meta = core.readMetadataObject(key);
    	URI redirect = DiffusionUriBuilder.getRestUriBuilder().path(OrtolangObjectResource.class).path(meta.getTarget()).build();
    	return Response.seeOther(redirect).build();
    }
    
}
