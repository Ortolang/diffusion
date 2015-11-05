package fr.ortolang.diffusion.api.admin;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;
import fr.ortolang.diffusion.subscription.SubscriptionService;
import fr.ortolang.diffusion.subscription.SubscriptionServiceException;

@Path("/admin")
@Produces({ MediaType.APPLICATION_JSON })
@RolesAllowed("admin")
@RunAs("system")
public class AdminResource {
    
    private static final Logger LOGGER = Logger.getLogger(AdminResource.class.getName());
    
    @EJB
    private JsonStoreService json;
    @EJB
    private IndexStoreService index;
    @EJB
	private RegistryService registry;
    @EJB
    private SubscriptionService subscription;
	
	
    @GET
    @Path("/infos/{service}")
    public Response getRegistryInfos(@PathParam(value = "service") String serviceName) throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /infos/" + serviceName);
        OrtolangService service = OrtolangServiceLocator.findService(serviceName);
        Map<String, String> infos = service.getServiceInfos();
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
    @Path("/json/documents/{key}")
    public Response getJsonDocumentForKey(@PathParam(value = "key") String key) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/json/documents/" + key);
        String document = json.systemGetDocument(key);
        return Response.ok(document).build();
    }

    @GET
    @Path("/subscription")
    public Response addSubscriptionFilters() throws SubscriptionServiceException {
        LOGGER.log(Level.INFO, "GET /subscription");
        subscription.addAdminFilters();
        return Response.ok().build();
    }
    
}
