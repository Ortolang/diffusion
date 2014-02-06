package fr.ortolang.diffusion.rest.registry;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.rest.CollectionRepresentation;

@Path("/entries")
@Produces({ MediaType.APPLICATION_JSON })
public class RegistryEntryResource {
	
	@Context
    private UriInfo context;
	@EJB 
	private BrowserService browser;
 
    public RegistryEntryResource() {
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public CollectionRepresentation<RegistryEntryRepresentation> list( 
    		@DefaultValue(value="10") @QueryParam(value="limit") int limit, 
    		@DefaultValue(value="0") @QueryParam(value="offset") int offset ) {
    	CollectionRepresentation<RegistryEntryRepresentation> entries = new CollectionRepresentation<RegistryEntryRepresentation>();
    	//TODO load list and put navigation links
    	return entries;
    }
    
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public RegistryEntryRepresentation get( @PathParam(value="key") String key ) {
    	RegistryEntryRepresentation entry = new RegistryEntryRepresentation();
    	//TODO load entry content and put links to object
    	return entry;
    }

}
