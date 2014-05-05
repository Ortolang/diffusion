package fr.ortolang.diffusion.rest.api;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;
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
	private SearchService search;
	@EJB
	private SecurityService security;

	public OrtolangObjectResource() {
	}

	@GET
	@Template("templates/objects.vm")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response list(@DefaultValue(value = "0") @QueryParam(value = "offset") int offset, @DefaultValue(value = "25") @QueryParam(value = "limit") int limit)
			throws BrowserServiceException {
		logger.log(Level.INFO, "list objects, offset=" + offset + ", limit=" + limit);
		List<String> keys = browser.list(offset, limit, "", "");
		long nbentries = browser.count("", "");
		UriBuilder objects = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class);

		OrtolangObjectsRepresentation representation = new OrtolangObjectsRepresentation();
		for ( String key : keys ) {
			representation.addEntry(key, Link.fromUri(objects.clone().path(key).build()).rel("view").build());
		}
		representation.setStart((offset<=0)?1:offset);
		representation.setSize(keys.size());
		representation.setTotalSize(nbentries);
		representation.addLink(Link.fromUri(objects.clone().queryParam("offset", 0).queryParam("limit", limit).build()).rel("first").build());
		representation.addLink(Link.fromUri(objects.clone().queryParam("offset", Math.max(0, (offset - limit))).queryParam("limit", limit).build()).rel("previous").build());
		representation.addLink(Link.fromUri(objects.clone().queryParam("offset", offset).queryParam("limit", limit).build()).rel("self").build());
		representation.addLink(Link.fromUri(objects.clone().queryParam("offset", (nbentries > (offset + limit)) ? (offset + limit) : offset).queryParam("limit", limit).build()).rel("next").build());
		representation.addLink(Link.fromUri(objects.clone().queryParam("offset", ((nbentries - 1) / limit) * limit).queryParam("limit", limit).build()).rel("last").build());
		Response response = Response.ok(representation).build();
		return response;
	}

	@GET
	@Path("/{key}")
	@Template("templates/object.vm")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response get(@PathParam(value = "key") String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting object identifier for key: " + key);
		OrtolangObjectIdentifier identifier = browser.lookup(key);
		UriBuilder base = UriBuilder.fromUri(uriInfo.getBaseUri());
		UriBuilder objects = base.clone().path(OrtolangObjectResource.class);

		OrtolangObjectRepresentation representation = OrtolangObjectRepresentation.fromOrtolangObjectIdentifier(identifier);
		representation.setKey(key);
		representation.addLink(Link.fromUri(base.path(identifier.getService()).path(identifier.getType().concat("s")).path(key).build()).rel("view").build());
		representation.addLink(Link.fromUri(objects.clone().path(key).path("properties").build()).rel("properties").build());
		representation.addLink(Link.fromUri(objects.clone().path(key).path("state").build()).rel("state").build());
		representation.addLink(Link.fromUri(objects.clone().path(key).path("owner").build()).rel("owner").build());
		representation.addLink(Link.fromUri(objects.clone().path(key).path("permissions").build()).rel("permissions").build());
		return Response.ok(representation).build();
	}

	@GET
	@Path("/{key}/properties")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response getProperties(@PathParam(value = "key") String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting object properties for key: " + key);
		List<OrtolangObjectProperty> properties = browser.listProperties(key);
		return Response.ok(properties).build();
	}

	@GET
	@Path("/{key}/properties/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProperty(@PathParam(value = "key") String key, @PathParam(value = "name") String name) throws BrowserServiceException, KeyNotFoundException,
			AccessDeniedException, PropertyNotFoundException {
		logger.log(Level.INFO, "getting object property value for key: " + key + " and name: " + name);
		OrtolangObjectProperty value = browser.getProperty(key, name);
		return Response.ok(value).build();
	}

	@PUT
	@Path("/{key}/properties/{name}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateProperty(@PathParam(value = "key") String key, @PathParam(value = "name") String name, String value) throws BrowserServiceException,
			KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "updating object property value for key: " + key + " and name: " + name);
		if (value == null || value.equals("")) {
			return Response.status(400).entity("provided value is null or empty").build();
		}
		browser.setProperty(key, name, value);
		return Response.noContent().build();
	}

	@DELETE
	@Path("/{key}/properties/{name}")
	public Response deleteProperty(@PathParam(value = "key") String key, @PathParam(value = "name") String name) throws BrowserServiceException, KeyNotFoundException,
			AccessDeniedException {
		logger.log(Level.INFO, "updating object property value for key: " + key + " and name: " + name);
		browser.setProperty(key, name, "");
		return Response.noContent().build();
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

	@PUT
	@Path("/{key}/owner")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setOwner(@PathParam(value = "key") String key, String value) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "setting new owner for key: " + key);
		security.changeOwner(key, value);
		return Response.noContent().build();
	}

	@GET
	@Path("/{key}/permissions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listPermissions(@PathParam(value = "key") String key) throws SecurityServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting permissions for key: " + key);
		Map<String, List<String>> permissions = security.listRules(key);
		return Response.ok(permissions).build();
	}

	@PUT
	@Path("/{key}/permissions")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setPermissions(@PathParam(value = "key") String key, Map<String, List<String>> permissions) throws SecurityServiceException, KeyNotFoundException,
			AccessDeniedException {
		logger.log(Level.INFO, "setting permissions for key: " + key);
		security.setRules(key, permissions);
		return Response.noContent().build();
	}

	@GET
	@Path("/{key}/permissions/{subject}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPermissions(@PathParam(value = "key") String key, @PathParam(value = "subject") String subject) throws SecurityServiceException, KeyNotFoundException,
			AccessDeniedException {
		logger.log(Level.INFO, "getting permissions for key: " + key + ", and subject: " + subject);
		Map<String, List<String>> permissions = security.listRules(key);
		return Response.ok(permissions).build();
	}

	@PUT
	@Path("/{key}/permissions/{subject}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response setPermissions(@PathParam(value = "key") String key, @PathParam(value = "subject") String subject, List<String> permissions) throws SecurityServiceException,
			KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "getting permissions for key: " + key + ", and subject: " + subject);
		if (permissions == null || permissions.size() == 0) {
			return Response.status(400).entity("provided permissions are null or empty").build();
		}
		security.setRule(key, subject, permissions);
		return Response.noContent().build();
	}

	@DELETE
	@Path("/{key}/permissions/{subject}")
	public Response deletePermissions(@PathParam(value = "key") String key, @PathParam(value = "subject") String subject) throws SecurityServiceException, KeyNotFoundException,
			AccessDeniedException {
		logger.log(Level.INFO, "removing permissions for key: " + key + " and subject: " + subject);
		security.setRule(key, subject, null);
		return Response.noContent().build();
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@QueryParam(value = "query") String query) throws SearchServiceException {
		logger.log(Level.INFO, "searching objects with query: " + query);
		List<OrtolangSearchResult> results = search.search(query);
		return Response.ok(results).build();
	}

}
