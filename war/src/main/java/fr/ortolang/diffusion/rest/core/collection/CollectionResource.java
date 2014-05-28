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
import fr.ortolang.diffusion.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.rest.api.OrtolangCollectionRepresentation;
import fr.ortolang.diffusion.rest.api.OrtolangLinkRepresentation;
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
	
	@GET
	@Template( template="core/collections.vm", types={MediaType.TEXT_HTML})
	@Produces(MediaType.TEXT_HTML)
	public Response list() throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing all collections");
		UriBuilder collections = DiffusionUriBuilder.getRestUriBuilder().path(CollectionResource.class);

		OrtolangCollectionRepresentation representation = new OrtolangCollectionRepresentation ();
		representation.setStart(0);
		representation.setSize(0);
		representation.setTotalSize(0);
		representation.addLink(OrtolangLinkRepresentation.fromUri(collections.clone().build()).rel("create"));
		return Response.ok(representation).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response create(@DefaultValue("change me") @FormParam("name") String name, @DefaultValue("no description provided") @FormParam("description") String description)
			throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating collection");
		String key = UUID.randomUUID().toString();
		core.createCollection(key, name, description);
		URI newly = DiffusionUriBuilder.getRestUriBuilder().path(CollectionResource.class).path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/{key}")
	@Template( template="core/collection.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response read(@PathParam(value = "key") String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading collection with key: " + key);
		Collection collection = core.readCollection(key);
		UriBuilder collections = DiffusionUriBuilder.getRestUriBuilder().path(CollectionResource.class);
		
		CollectionRepresentation representation = CollectionRepresentation.fromCollection(collection);
		representation.addLink(OrtolangLinkRepresentation.fromUri(collections.clone().path(key).path("elements").build()).rel("elements"));
		representation.addLink(OrtolangLinkRepresentation.fromUri(collections.clone().path(key).path("metadatas").build()).rel("metadatas"));
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
    @Template( template="core/elements.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
    public Response listElements( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading elements of collection with key: " + key);
    	Collection collection = core.readCollection(key);
    	UriBuilder collections = DiffusionUriBuilder.getRestUriBuilder().path(CollectionResource.class);
		
    	OrtolangCollectionRepresentation representation = new OrtolangCollectionRepresentation ();
		for ( String element : collection.getElements() ) {
			representation.addEntry(element, OrtolangLinkRepresentation.fromUri(collections.clone().path(key).path("elements").path(element).build()).rel("view"));
		}
		return Response.ok(representation).build();
    }
    
    @GET
    @Path("/{key}/elements/{element}")
    public Response getElement( @PathParam(value="key") String key, @PathParam(value="element") String element ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading element " + element + " of collection with key: " + key);
    	Collection collection = core.readCollection(key);
    	if ( collection.getElements().contains(element) ) {
    		URI redirect = DiffusionUriBuilder.getRestUriBuilder().path(OrtolangObjectResource.class).path(element).build();
    		return Response.seeOther(redirect).build();
    	} else {
    		throw new KeyNotFoundException("this element is not in this collection");
    	}
    }
    
    @PUT
    @Path("/{key}/elements/{element}")
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
    
    @GET
    @Path("/{key}/metadatas")
    @Template( template="core/metas.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
    public Response listMetadatas( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading metadatas of collection with key: " + key);
    	Collection collection = core.readCollection(key);
    	UriBuilder collections = DiffusionUriBuilder.getRestUriBuilder().path(CollectionResource.class);
		
    	OrtolangCollectionRepresentation representation = new OrtolangCollectionRepresentation ();
    	representation.setStart(0);
		representation.setSize(collection.getMetadatas().size());
		representation.setTotalSize(collection.getMetadatas().size());
		for ( String metadata : collection.getMetadatas() ) {
			representation.addEntry(metadata, OrtolangLinkRepresentation.fromUri(collections.clone().path(key).path("metadatas").path(metadata).build()).rel("view"));
		}
		return Response.ok(representation).build();
    }
    
    @GET
    @Path("/{key}/metadatas/{metadata}")
    public Response getMetadata( @PathParam(value="key") String key, @PathParam(value="metadata") String metadata ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading metadata " + metadata + " of collection with key: " + key);
    	Collection collection = core.readCollection(key);
    	if ( collection.getMetadatas().contains(metadata) ) {
    		URI redirect = DiffusionUriBuilder.getRestUriBuilder().path(OrtolangObjectResource.class).path(metadata).build();
    		return Response.seeOther(redirect).build();
    	} else {
    		throw new KeyNotFoundException("this element is not in this collection");
    	}
    }
    
}
