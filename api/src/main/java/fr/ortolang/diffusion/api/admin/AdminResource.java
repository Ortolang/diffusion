package fr.ortolang.diffusion.api.admin;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.store.json.JsonStoreServiceAdmin;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;

@Path("/admin")
@Produces({ MediaType.APPLICATION_JSON })
public class AdminResource {
    
    private static final Logger LOGGER = Logger.getLogger(AdminResource.class.getName());
    
    @EJB
    private JsonStoreServiceAdmin json;

    @GET
    @Path("/json")
    public Response getJsonStoreInfos() throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/json");
        Map<String, String> infos = json.getStoreInfos();
        return Response.ok(infos).build();
    }
    
    @GET
    @Path("/json/documents/{key}")
    public Response getJsonDocumentForKey(@PathParam(value = "key") String key) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/json/documents/" + key);
        String document = json.getDocument(key);
        return Response.ok(document).build();
    }
    
}
