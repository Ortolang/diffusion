package fr.ortolang.diffusion.rest.core.link;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import fr.ortolang.diffusion.rest.KeysPaginatedRepresentation;
import fr.ortolang.diffusion.rest.KeysRepresentation;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/core/links")
@Produces({ MediaType.APPLICATION_JSON })
public class LinkResource {

	private Logger logger = Logger.getLogger(LinkResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private CoreService core;

	public LinkResource() {
	}
	
	@GET
	@Template( template="core/links.vm", types={MediaType.TEXT_HTML})
	@Produces(MediaType.TEXT_HTML)
	public Response list(@QueryParam(value = "target") String target) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "listing links for target : " + target);
		UriBuilder links = UriBuilder.fromUri(uriInfo.getBaseUri()).path(LinkResource.class);
		if ( target != null ) {
			List<String> keys = core.findLinksForTarget(target);
			KeysPaginatedRepresentation representation = new KeysPaginatedRepresentation ();
			for ( String key : keys ) {
				representation.addEntry(key, javax.ws.rs.core.Link.fromUri(links.clone().path(key).build()).rel("view").build());
			}
		}

		KeysRepresentation representation = new KeysRepresentation ();
		representation.addLink(javax.ws.rs.core.Link.fromUri(links.clone().build()).rel("create").build());
		return Response.ok(representation).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response create(@DefaultValue("change my name") @FormParam("name") String name, @FormParam("target") String target) throws CoreServiceException,
			KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "creating link");
		String key = UUID.randomUUID().toString();
		core.createLink(key, name, target);
		URI newly = UriBuilder.fromUri(uriInfo.getBaseUri()).path(LinkResource.class).path(key).build();
		return Response.created(newly).build();
	}
	
	@GET
	@Path("/{key}")
	@Template( template="core/link.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response read(@PathParam(value = "key") String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "reading link with key: " + key);
		Link link = core.readLink(key);
		UriBuilder links = UriBuilder.fromUri(uriInfo.getBaseUri()).path(LinkResource.class);
		
		LinkRepresentation representation = LinkRepresentation.fromLink(link);
		representation.addLink(javax.ws.rs.core.Link.fromUri(links.clone().path(key).path("target").build()).rel("target").build());
		return Response.ok(representation).build();
	}

	@PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update( @PathParam(value="key") String key, LinkRepresentation representation ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "updating link with key: " + key);
    	core.updateLink(key, representation.getName());
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}")
    public Response delete( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "deleting link with key: " + key);
    	core.deleteLink(key);
    	return Response.noContent().build();
    }
	
    @GET
    @Path("/{key}/target")
    public Response target( @PathParam(value="key") String key ) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading target for link with key: " + key);
    	Link link = core.readLink(key);
    	URI redirect = UriBuilder.fromUri(uriInfo.getBaseUri()).path(OrtolangObjectResource.class).path(link.getTarget()).build();
    	return Response.seeOther(redirect).build();
    }
    
}
