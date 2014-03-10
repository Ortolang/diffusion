package fr.ortolang.diffusion.rest.fs.folder;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
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

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DigitalCollection;
import fr.ortolang.diffusion.core.entity.DigitalReference;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.rest.catalog.CatalogEntryRepresentation;
import fr.ortolang.diffusion.rest.core.collection.DigitalCollectionRepresentation;
import fr.ortolang.diffusion.rest.datatables.DataTableCollection;
import fr.ortolang.diffusion.rest.datatables.DataTableRequestParams;
import fr.ortolang.diffusion.rest.datatables.DataTableRequestParamsException;

/**
 * A folder is identified by the key of a Reference which must target a collection.
 * @author cyril
 *
 */
@Path("/fs/folders")
@Produces({ MediaType.APPLICATION_JSON })
public class FolderResource {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private static SimpleDateFormat sdf = new SimpleDateFormat(CatalogEntryRepresentation.DATE_TIME_PATTERN);
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private BrowserService browser;
	@EJB 
	private CoreService core;
 
    public FolderResource() {
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
     * Finds the list of elements from a specific folder.
     * @param key
     * @return a ??
     * @throws CoreServiceException
     * @throws KeyNotFoundException
     * @throws OrtolangException 
     * @throws DataTableRequestParamsException 
     * @throws PropertyNotFoundException 
     */
    @GET
    @Path("/{key}/elements")
    @Produces(MediaType.APPLICATION_JSON)
    public DataTableCollection<FSEntryRepresentation> listElements( @PathParam(value="key") String key, @Context HttpServletRequest request ) throws BrowserServiceException,  CoreServiceException, KeyNotFoundException, OrtolangException, DataTableRequestParamsException, PropertyNotFoundException {
    	
    	DataTableCollection<FSEntryRepresentation> elements = new DataTableCollection<FSEntryRepresentation>();
    	
    	logger.log(Level.INFO, "list all elements from a folder with key: " + key);
    	
    	DigitalReference reference = core.readReference(key);
    	DigitalCollection collection = core.readCollection(reference.getTarget());
    	
    	if(collection.getElements()!=null && collection.getElements().size()>0) {
	    	DataTableRequestParams params = DataTableRequestParams.fromQueryParams(request.getParameterMap());
	    	
	    	long nbentries = collection.getElements().size();
	    	
	    	// Fills the datatable collection with folder representation
	    	elements.setsEcho(params.getEcho());
	    	elements.setiTotalRecords(nbentries);
	    	for(String keyElement : collection.getElements()) {
	    		FSEntryRepresentation entry = new FSEntryRepresentation();
	    		entry.setKey(keyElement); // Reference registry key
	    		
	    		OrtolangObject oo = core.findObject(keyElement);
	    		entry.setName(oo.getObjectName());
	    		//TODO description, id ??
	    		
	    		OrtolangObjectIdentifier identifier = browser.lookup(keyElement);
	    		entry.setService(identifier.getService());
	    		entry.setType(identifier.getType());
	    		entry.setOwner(browser.getProperty(keyElement, OrtolangObjectProperty.OWNER).getValue());
	    		entry.setCreationDate(sdf.format(new Date(Long.parseLong(browser.getProperty(keyElement, OrtolangObjectProperty.CREATION_TIMESTAMP).getValue()))));
	    		entry.setModificationDate(sdf.format(new Date(Long.parseLong(browser.getProperty(keyElement, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP).getValue()))));
	    		entry.setState(browser.getState(keyElement).toString());
	    		//TODO si la target est une collection alors vue folder 
	    		//TODO si la target est un object alors vue file
	    		//TODO autre ??
	//    		URI view = UriBuilder.fromUri(uriInfo.getBaseUri()).path(entry.getService()).path(entry.getType()).path(keyElement).build();
	//    		entry.setView(view.toString());
	    		elements.addEntry(entry);
	    	}
	    	
    	}
    	
    	//    	DigitalCollectionRepresentation representation = DigitalCollectionRepresentation.fromDigitalCollection(collection);
    	//    	Response response = Response.ok(representation).build();
    	//    	return response;
    	return elements;
    }


    /**
     * Update the list of element of a specific collection.
     * Add an element to a collection.
     * @param name
     * @param description
     * @return
     * @throws CoreServiceException
     * @throws KeyAlreadyExistsException
     * @throws KeyNotFoundException 
     */
    @POST
    @Path("/{key}/elements")
    @Consumes("application/x-www-form-urlencoded")
    public Response addElementToCollection( @PathParam("key") String key
    		, @FormParam("element") String element
    		) throws CoreServiceException, KeyAlreadyExistsException, KeyNotFoundException {
    	logger.log(Level.INFO, "add element "+element+" to folder "+key);
    	//TODO Use PUT instead of POST
    	DigitalReference reference = core.readReference(key);
//    	DigitalCollection collection = core.readCollection(reference.getTarget());
    	
    	core.addElementToCollection(reference.getTarget(), element);
    	
    	return Response.ok().build();
    }
    
}
