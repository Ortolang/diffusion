package fr.ortolang.diffusion.api.admin;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.registry.RegistryServiceAdmin;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.store.index.IndexStoreServiceAdmin;
import fr.ortolang.diffusion.store.json.JsonStoreServiceAdmin;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;

@Path("/admin")
@Produces({ MediaType.APPLICATION_JSON })
public class AdminResource {
    
    private static final Logger LOGGER = Logger.getLogger(AdminResource.class.getName());
    
    @EJB
    private JsonStoreServiceAdmin json;
    
    @EJB
    private IndexStoreServiceAdmin index;

    @EJB
	private RegistryServiceAdmin registry;
	
	
    @GET
    @Path("/registry")
    public Response getRegistryInfos() throws RegistryServiceException {
        LOGGER.log(Level.INFO, "GET /admin/registry");
        Map<String, String> infos = registry.getServiceInfos();
        return Response.ok(infos).build();
    }
    
    @GET
	@Path("/registry/entries")
	public Response listEntries(@QueryParam("filter") String filter) throws RegistryServiceException {
		LOGGER.log(Level.INFO, "GET /admin/registry/entries?filter=" + filter);
		List<RegistryEntry> entries = registry.systemListEntries(filter);
		return Response.ok(entries).build();
	}
	
    @GET
    @Path("/json")
    public Response getJsonStoreInfos() throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/json");
        Map<String, String> infos = json.getServiceInfos();
        return Response.ok(infos).build();
    }
    
    @GET
    @Path("/json/documents/{key}")
    public Response getJsonDocumentForKey(@PathParam(value = "key") String key) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/json/documents/" + key);
        String document = json.getDocument(key);
        return Response.ok(document).build();
    }
    
    @GET
    @Path("/index")
    public Response getIndexStoreInfos() throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/index");
        Map<String, String> infos = index.getServiceInfos();
        return Response.ok(infos).build();
    }
    
}
