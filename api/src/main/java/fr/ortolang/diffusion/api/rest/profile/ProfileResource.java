package fr.ortolang.diffusion.api.rest.profile;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/profiles")
@Produces({ MediaType.APPLICATION_JSON })
public class ProfileResource {

	private Logger logger = Logger.getLogger(ProfileResource.class.getName());

	@EJB
	private MembershipService membership;

	public ProfileResource() {
	}

	@GET
	@Path("/connected")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getConnected() throws MembershipServiceException, KeyNotFoundException, ProfileAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "GET /profiles/connected");
		String key = membership.getProfileKeyForConnectedIdentifier();
		if (MembershipService.UNAUTHENTIFIED_IDENTIFIER.equals(key)) {
			throw new AccessDeniedException(MembershipService.UNAUTHENTIFIED_IDENTIFIER + " is not considered as a connected identifier");
		}
		try {
			membership.readProfile(key);
		} catch (KeyNotFoundException e) {
			membership.createProfile("no name provided", "no email provided");
		} catch (AccessDeniedException e) {
		}
		URI view = DiffusionUriBuilder.getRestUriBuilder().path(ProfileResource.class).path(key).build();
		return Response.seeOther(view).build();
	}

	@GET
	@Path("/{key}")
	@Template(template = "profiles/detail.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getProfile(@PathParam(value = "key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "GET /profiles/" + key);
		Profile profile = membership.readProfile(key);
		ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
		return Response.ok(representation).build();
	}

	@PUT
	@Path("/{key}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateProfile(@PathParam(value = "key") String key, ProfileRepresentation representation) throws MembershipServiceException, KeyNotFoundException,
			AccessDeniedException {
		logger.log(Level.INFO, "PUT /profiles/" + key);
		membership.updateProfile(key, representation.getFullname(), representation.getEmail());
		return Response.noContent().build();
	}

}
