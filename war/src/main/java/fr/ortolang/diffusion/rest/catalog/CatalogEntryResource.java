package fr.ortolang.diffusion.rest.catalog;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.rest.datatables.DataTableCollection;
import fr.ortolang.diffusion.rest.datatables.DataTableRequestParams;
import fr.ortolang.diffusion.rest.datatables.DataTableRequestParamsException;

@Path("/catalog/entries")
@Produces({ MediaType.APPLICATION_JSON })
public class CatalogEntryResource {

	private static Logger logger = Logger.getLogger(CatalogEntryResource.class.getName());
	private static SimpleDateFormat sdf = new SimpleDateFormat(CatalogEntryRepresentation.DATE_TIME_PATTERN);
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private BrowserService browser;
 
    public CatalogEntryResource() {
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DataTableCollection<CatalogEntryRepresentation> list( @Context HttpServletRequest request ) throws BrowserServiceException, KeyNotFoundException, PropertyNotFoundException, DataTableRequestParamsException {
    	logger.log(Level.INFO, "listing catalog entries for datatable");
    	DataTableRequestParams params = DataTableRequestParams.fromQueryParams(request.getParameterMap());
    	
    	List<String> keys = browser.list(params.getOffset(), params.getLength(), "", "");
    	long nbentries = browser.count("", "");
    	
    	DataTableCollection<CatalogEntryRepresentation> collection = new DataTableCollection<CatalogEntryRepresentation>();
    	collection.setsEcho(params.getEcho());
    	collection.setiTotalRecords(nbentries);
    	for ( String key : keys ) {
    		CatalogEntryRepresentation entry = new CatalogEntryRepresentation();
    		entry.setKey(key);
    		OrtolangObjectIdentifier identifier = browser.lookup(key);
    		entry.setService(identifier.getService());
    		entry.setType(identifier.getType());
    		entry.setOwner(browser.getProperty(key, OrtolangObjectProperty.OWNER).getValue());
    		entry.setCreationDate(sdf.format(new Date(Long.parseLong(browser.getProperty(key, OrtolangObjectProperty.CREATION_TIMESTAMP).getValue()))));
    		entry.setModificationDate(sdf.format(new Date(Long.parseLong(browser.getProperty(key, OrtolangObjectProperty.LAST_UPDATE_TIMESTAMP).getValue()))));
    		OrtolangObjectState state = browser.getState(key);
    		entry.setLocked(state.isLocked()+"");
    		entry.setHidden(state.isHidden()+"");
    		entry.setDeleted(state.isDeleted()+"");
    		URI view = UriBuilder.fromUri(uriInfo.getBaseUri()).path(entry.getService()).path(entry.getType()).path(key).build();
    		entry.setView(view.toString());
    		collection.addEntry(entry);
    	}
    	return collection;
    }
    
    
    
}
