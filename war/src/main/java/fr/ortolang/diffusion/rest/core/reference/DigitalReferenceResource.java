package fr.ortolang.diffusion.rest.core.reference;

import java.net.URI;
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

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DigitalReference;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.core.collection.DigitalCollectionResource;

@Path("/core/reference")
@Produces({ MediaType.APPLICATION_JSON })
public class DigitalReferenceResource {

	private Logger logger = Logger.getLogger(DigitalCollectionResource.class.getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private CoreService core;
 
    public DigitalReferenceResource() {
    }

    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException {
    	logger.log(Level.INFO, "reading digital reference with key: " + key);
    	DigitalReference reference = core.readReference(key);
    	DigitalReferenceRepresentation representation = DigitalReferenceRepresentation.fromDigitalReference(reference);
    	Response response = Response.ok(representation).build();
    	return response;
    }

    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response create( @DefaultValue("Nouvelle reference") @FormParam("name") String name
    		, @DefaultValue("true") @FormParam("dynamic") boolean dynamic
    		, @FormParam("target") String target
    		) throws CoreServiceException, KeyAlreadyExistsException {
    	logger.log(Level.INFO, "creating digital collection");
    	String key = UUID.randomUUID().toString();
    	
    	//TODO Pourquoi pas de méthode createCollection dans l'interface du CoreLocal
		logger.log(Level.INFO, "using remote core interface");
		core.createReference(key, dynamic, name, target); //TODO tester si target null
    	
    	URI newly = UriBuilder.fromUri(uriInfo.getBaseUri()).path(DigitalReferenceResource.class).path(key).build();
    	return Response.created(newly).build();
    }
    
}
