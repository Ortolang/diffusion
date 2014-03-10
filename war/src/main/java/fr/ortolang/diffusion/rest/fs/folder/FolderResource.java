package fr.ortolang.diffusion.rest.fs.folder;

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

import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DigitalCollection;
import fr.ortolang.diffusion.core.entity.DigitalReference;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.core.collection.DigitalCollectionRepresentation;

/**
 * A folder is identified by the key of a Reference which must target a collection.
 * @author cyril
 *
 */
@Path("/fs/folders")
@Produces({ MediaType.APPLICATION_JSON })
public class FolderResource {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private BrowserService browser;
	@EJB 
	private CoreService core;
 
    public FolderResource() {
    }
    
    /**
     * Finds a specific folder.
     * @param key
     * @return a DigitalCollectionRepresentation
     * @throws CoreServiceException
     * @throws KeyNotFoundException
     */
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException {
    	logger.log(Level.INFO, "reading folder with key: " + key);
    	
    	DigitalReference reference = core.readReference(key);
    	
    	DigitalCollection collection = core.readCollection(reference.getTarget());
    	
    	//TODO a representation plus riche avec les propriétés (registre), ...
    	DigitalCollectionRepresentation representation = DigitalCollectionRepresentation.fromDigitalCollection(collection);
    	Response response = Response.ok(representation).build();
    	return response;
    }

    /**
     * Create a folder (collection + dynamic reference).
     * @param name
     * @param description
     * @return
     * @throws CoreServiceException
     * @throws KeyAlreadyExistsException
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response create( @DefaultValue("Nouvelle collection") @FormParam("name") String name
    		, @DefaultValue("") @FormParam("description") String description
    		) throws CoreServiceException, KeyAlreadyExistsException {
    	logger.log(Level.INFO, "creating folder");
    	// Generate a key for the registry
    	String key = UUID.randomUUID().toString();
    	
    	//TODO Pourquoi pas de méthode createCollection dans l'interface du CoreLocal
		logger.log(Level.INFO, "using remote core interface");
		core.createCollection(key, name, description);

    	String keyRef = UUID.randomUUID().toString();
    	logger.log(Level.INFO, "create reference to target "+key);
		core.createReference(keyRef, true, name, key);
    	
    	URI newly = UriBuilder.fromUri(uriInfo.getBaseUri()).path(FolderResource.class).path(keyRef).build();
    	return Response.created(newly).build();
    }
    
    
}
