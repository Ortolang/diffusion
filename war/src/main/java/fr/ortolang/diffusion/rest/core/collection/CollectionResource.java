package fr.ortolang.diffusion.rest.core.collection;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/core/collections")
@Produces({ MediaType.APPLICATION_JSON })
public class CollectionResource {

	private Logger logger = Logger.getLogger(CollectionResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private CoreService core;

	public CollectionResource() {
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response create(@DefaultValue("change me") @FormParam("name") String name, @DefaultValue("no description provided") @FormParam("description") String description)
			throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating collection");
		String key = UUID.randomUUID().toString();
		core.createCollection(key, name, description);
		URI newly = UriBuilder.fromUri(uriInfo.getBaseUri()).path(CollectionResource.class).path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response read(@PathParam(value = "key") String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading collection with key: " + key);
		Collection collection = core.readCollection(key);
		CollectionRepresentation representation = CollectionRepresentation.fromCollection(collection);
		return Response.ok(representation).build();
	}

	@PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update( @PathParam(value="key") String key, CollectionRepresentation representation ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "updating collection with key: " + key);
    	core.updateCollection(key, representation.getName(), representation.getDescription());
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}")
    public Response delete( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "deleting collection with key: " + key);
    	core.deleteLink(key);
    	return Response.noContent().build();
    }
	
    @GET
    @Path("/{key}/elements")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listElements( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading elements of collection with key: " + key);
    	Collection collection = core.readCollection(key);
    	return Response.ok(collection.getElements()).build();
    }
    
    @GET
    @Path("/{key}/elements/{element}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getElement( @PathParam(value="key") String key, @PathParam(value="element") String element ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading element " + element + " of collection with key: " + key);
    	Collection collection = core.readCollection(key);
    	if ( collection.getElements().contains(element) ) {
    		URI redirect = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).path(element).build();
    		return Response.seeOther(redirect).build();
    	} else {
    		throw new KeyNotFoundException();
    	}
    }
    
    @PUT
    @Path("/{key}/elements/{element}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addElement( @PathParam(value="key") String key, @PathParam(value="element") String element ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "adding element " + element + " into collection with key: " + key);
    	core.addElementToCollection(key, element, true);
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}/elements/{element}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response target( @PathParam(value="key") String key, @PathParam(value="element") String element ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "removing element " + element + " of collection with key: " + key);
    	core.removeElementFromCollection(key, element);
    	return Response.noContent().build();
    }

}
