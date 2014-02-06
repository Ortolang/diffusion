package fr.ortolang.diffusion.rest.registry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.registry.EntryNotFoundException;
import fr.ortolang.diffusion.registry.RegistryEntry;

@Path("/entries")
@Produces({ MediaType.APPLICATION_JSON })
public class RegistryEntryResource {
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private BrowserService browser;
 
    public RegistryEntryResource() {
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list( 
    		@DefaultValue(value="0") @QueryParam(value="offset") int offset,
    		@DefaultValue(value="10") @QueryParam(value="limit") int limit) throws BrowserServiceException {
    	List<RegistryEntryRepresentation> representation = new ArrayList<RegistryEntryRepresentation>();
    	List<RegistryEntry> entries = browser.list(offset, limit);
    	long nbentries = browser.count();
    	for ( RegistryEntry entry : entries ) {
    		representation.add(RegistryEntryRepresentation.fromRegistryEntry(entry));
    	}
    	URI self = UriBuilder.fromUri(uriInfo.getBaseUri()).path(RegistryEntryResource.class).queryParam("offset", offset).queryParam("limit", limit).build();
    	URI first = UriBuilder.fromUri(uriInfo.getBaseUri()).path(RegistryEntryResource.class).queryParam("offset", 0).queryParam("limit", limit).build();
    	URI last = UriBuilder.fromUri(uriInfo.getBaseUri()).path(RegistryEntryResource.class).queryParam("offset", ((nbentries-1)/limit)*limit).queryParam("limit", limit).build();
    	URI previous = UriBuilder.fromUri(uriInfo.getBaseUri()).path(RegistryEntryResource.class).queryParam("offset", Math.max(0, (offset-limit))).queryParam("limit", limit).build();
    	URI next = UriBuilder.fromUri(uriInfo.getBaseUri()).path(RegistryEntryResource.class).queryParam("offset", (nbentries>(offset+limit))?(offset+limit):offset).queryParam("limit", limit).build();
    	Response response = Response.ok(representation)
    			.link(self, "self").link(first, "first").link(last, "last")
    			.link(previous, "previous").link(next, "next").build();
    	return response;
    }
    
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public RegistryEntryRepresentation get( @PathParam(value="key") String key ) throws BrowserServiceException, EntryNotFoundException {
    	RegistryEntry entry = browser.lookup(key);
    	return RegistryEntryRepresentation.fromRegistryEntry(entry);
    }

}
