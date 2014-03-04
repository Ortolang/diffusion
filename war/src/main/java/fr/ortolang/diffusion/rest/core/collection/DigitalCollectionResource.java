package fr.ortolang.diffusion.rest.core.collection;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.CoreServiceLocal;
import fr.ortolang.diffusion.core.entity.DigitalCollection;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

@Path("/core/collection")
@Produces({ MediaType.APPLICATION_JSON })
public class DigitalCollectionResource {
	
	private Logger logger = Logger.getLogger(DigitalCollectionResource.class.getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private CoreService core;
//	@EJB 
//	private CoreServiceLocal coreLocal;
 
    public DigitalCollectionResource() {
    }
    
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException {
    	logger.log(Level.INFO, "reading digital object with key: " + key);
    	DigitalCollection collection = core.getCollection(key);
    	DigitalCollectionRepresentation representation = DigitalCollectionRepresentation.fromDigitalCollection(collection);
    	Response response = Response.ok(representation).build();
    	return response;
    }

    
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response create( @DefaultValue("Nouvelle collection") @FormParam("name") String name
    		, @DefaultValue("") @FormParam("description") String description
    		) throws CoreServiceException, KeyAlreadyExistsException {
    	logger.log(Level.INFO, "creating digital collection");
    	String key = UUID.randomUUID().toString();
    	
    	//TODO Pourquoi pas de méthode createCollection dans l'interface du CoreLocal
		logger.log(Level.INFO, "using remote core interface");
		core.createCollection(key, name, description);
    	
    	URI newly = UriBuilder.fromUri(uriInfo.getBaseUri()).path(DigitalCollectionResource.class).path(key).build();
    	return Response.created(newly).build();
    }
    
}
