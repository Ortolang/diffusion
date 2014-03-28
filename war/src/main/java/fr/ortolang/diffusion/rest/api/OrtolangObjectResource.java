package fr.ortolang.diffusion.rest.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/objects")
@Produces({ MediaType.APPLICATION_JSON })
public class OrtolangObjectResource {

	private Logger logger = Logger.getLogger(OrtolangObjectResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private BrowserService browser;
	@EJB
	private SecurityService security;

	public OrtolangObjectResource() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response list(@DefaultValue(value = "0") @QueryParam(value = "offset") int offset, @DefaultValue(value = "25") @QueryParam(value = "limit") int limit)
			throws BrowserServiceException {
		logger.log(Level.INFO, "list objects, offset=" + offset + ", limit=" + limit);
		List<String> keys = browser.list(offset, limit, "", "");
		long nbentries = browser.count("", "");
		URI self = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).queryParam("offset", offset).queryParam("limit", limit).build();
		URI first = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).queryParam("offset", 0).queryParam("limit", limit).build();
		URI last = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).queryParam("offset", ((nbentries - 1) / limit) * limit).queryParam("limit", limit)
				.build();
		URI previous = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).queryParam("offset", Math.max(0, (offset - limit))).queryParam("limit", limit)
				.build();
		URI next = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).queryParam("offset", (nbentries > (offset + limit)) ? (offset + limit) : offset)
				.queryParam("limit", limit).build();
		Response response = Response.ok(keys).link(self, "self").link(first, "first").link(last, "last").link(previous, "previous").link(next, "next").build();
		return response;
	}

	@GET
	@Path("/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@PathParam(value = "key") String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting object identifier for key: " + key);
		OrtolangObjectIdentifier identifier = browser.lookup(key);
		URI view = UriBuilder.fromUri(uriInfo.getBaseUri()).path(identifier.getService()).path(identifier.getType()).build();
		URI properties = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).path(key).path("propeties").build();
		URI state = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).path(key).path("state").build();
		return Response.ok(identifier).link(view, "view").link(properties, "properties").link(state, "state").build();
	}

	@GET
	@Path("/{key}/properties")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProperties(@PathParam(value = "key") String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting object properties for key: " + key);
		List<OrtolangObjectProperty> properties = browser.listProperties(key);
		return Response.ok(properties).build();
	}

	@GET
	@Path("/{key}/state")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getState(@PathParam(value = "key") String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting object state for key: " + key);
		OrtolangObjectState state = browser.getState(key);
		return Response.ok(state).build();
	}
	
	@GET
	@Path("/{key}/owner")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOwner(@PathParam(value = "key") String key) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting owner for key: " + key);
		String owner = security.getOwner(key);
		return Response.ok(owner).build();
	}
	
	@GET
	@Path("/{key}/permissions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@PathParam(value = "key") String key) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting permissions for key: " + key);
		Map<String, List<String>> permissions = security.listRules(key);
		return Response.ok(permissions).build();
	}

}
