package fr.ortolang.diffusion.rest.core.link;

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
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

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.core.collection.CollectionResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/core/links")
@Produces({ MediaType.APPLICATION_JSON })
public class LinkResource {

	private Logger logger = Logger.getLogger(CollectionResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private CoreService core;

	public LinkResource() {
	}

	@GET
	@Path("/{key}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response read(@PathParam(value = "key") String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading link with key: " + key);
		Link reference = core.readLink(key);
		LinkRepresentation representation = LinkRepresentation.fromLink(reference);
		Response response = Response.ok(representation).build();
		return response;
	}

	@POST
	@Consumes("application/x-www-form-urlencoded")
	public Response create(@DefaultValue("change my name") @FormParam("name") String name, @FormParam("target") String target) throws CoreServiceException,
			KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating link from form");
		String key = UUID.randomUUID().toString();

		core.createLink(key, name, target);

		URI newly = UriBuilder.fromUri(uriInfo.getBaseUri()).path(LinkResource.class).path(key).build();
		return Response.created(newly).build();
	}

}
