package fr.ortolang.diffusion.api.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.api.runtime.HumanTaskRepresentation;
import fr.ortolang.diffusion.api.runtime.ProcessRepresentation;
import fr.ortolang.diffusion.api.runtime.ProcessTypeRepresentation;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;
import fr.ortolang.diffusion.subscription.SubscriptionService;
import fr.ortolang.diffusion.subscription.SubscriptionServiceException;

@Path("/admin")
@Produces({ MediaType.APPLICATION_JSON })
@RolesAllowed("admin")
public class AdminResource {

    private static final Logger LOGGER = Logger.getLogger(AdminResource.class.getName());

    @EJB
    private JsonStoreService json;
    @EJB
    private IndexStoreService index;
    @EJB
    private HandleStoreService handle;
    @EJB
    private RegistryService registry;
    @EJB
    private RuntimeService runtime;
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

    @PUT
    @Path("/registry/entries/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateEntry(@PathParam("key") String key, RegistryEntry entry) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
        LOGGER.log(Level.INFO, "UPDATE /admin/registry/entries/" + key);
        // TODO compare what changes in order to update state...
        return Response.ok().build();
    }

    @DELETE
    @Path("/registry/entries/{key}")
    public Response deleteEntry(@PathParam("key") String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
        LOGGER.log(Level.INFO, "DELETE /admin/registry/entries/" + key);
        registry.delete(key, true);
        return Response.ok().build();
    }

    @GET
    @Path("/runtime/types")
    public Response listDefinitions() throws RuntimeServiceException {
        LOGGER.log(Level.INFO, "GET /admin/runtime/types");
        List<ProcessTypeRepresentation> types = runtime.listProcessTypes(false).stream().map(ProcessTypeRepresentation::fromProcessType).collect(Collectors.toCollection(ArrayList::new));
        return Response.ok(types).build();
    }

    @GET
    @Path("/runtime/processes")
    public Response listProcesses(@QueryParam("state") String state) throws RegistryServiceException, RuntimeServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /admin/runtime/processes?state=" + state);
        State estate = null;
        if (state != null && state.length() > 0) {
            try {
                estate = State.valueOf(state);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity("unknown state: " + e.getMessage()).build();
            }
        }
        List<ProcessRepresentation> entries = runtime.systemListProcesses(estate).stream().map(ProcessRepresentation::fromProcess).collect(Collectors.toCollection(ArrayList::new));
        return Response.ok(entries).build();
    }

    @GET
    @Path("/runtime/tasks")
    public Response listTasks() throws RegistryServiceException, RuntimeServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /admin/runtime/tasks");
        List<HumanTaskRepresentation> entries = runtime.systemListTasks().stream().map(HumanTaskRepresentation::fromHumanTask).collect(Collectors.toCollection(ArrayList::new));
        return Response.ok(entries).build();
    }
    
    @GET
    @Path("/handles")
    public Response listAllHandles(@QueryParam("o") int offset, @QueryParam("l") int limit, @QueryParam("filter") String filter) throws RegistryServiceException, RuntimeServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /admin/handles");
        List<HumanTaskRepresentation> entries = runtime.systemListTasks().stream().map(HumanTaskRepresentation::fromHumanTask).collect(Collectors.toCollection(ArrayList::new));
        return Response.ok(entries).build();
    }

    @GET
    @Path("/json/{key}")
    public Response getJsonDocumentForKey(@PathParam(value = "key") String key) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/json/" + key);
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
