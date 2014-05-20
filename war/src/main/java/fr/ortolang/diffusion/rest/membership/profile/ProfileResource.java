package fr.ortolang.diffusion.rest.membership.profile;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.rest.KeysRepresentation;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/membership/profiles")
@Produces({ MediaType.APPLICATION_JSON })
public class ProfileResource {
	
	private Logger logger = Logger.getLogger(ProfileResource.class.getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private MembershipService membership;
 
    public ProfileResource() {
    }
    
    @GET
    @Path("/connected")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
    public Response getConnected() throws MembershipServiceException, KeyNotFoundException, ProfileAlreadyExistsException, AccessDeniedException {
    	logger.log(Level.INFO, "reading connected profile");
    	String key = membership.getProfileKeyForConnectedIdentifier();
    	if ( MembershipService.UNAUTHENTIFIED_IDENTIFIER.equals(key) ) {
    		throw new AccessDeniedException(MembershipService.UNAUTHENTIFIED_IDENTIFIER + " is not considered as a connected identifier");
    	}
    	try { 
    		membership.readProfile(key);
    	} catch ( KeyNotFoundException e ) {
    		membership.createProfile("no name provided", "no email provided");
    	} catch ( AccessDeniedException e ) {
    	}
    	URI view = DiffusionUriBuilder.getRestUriBuilder().path(ProfileResource.class).path(key).build(); 
    	return Response.seeOther(view).build();
    }
    
    @GET
    @Path("/{key}")
    @Template( template="membership/profile.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response getProfile(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading profile for key: " + key);
    	Profile profile = membership.readProfile(key);
    	UriBuilder profiles = DiffusionUriBuilder.getRestUriBuilder().path(ProfileResource.class);
    	
    	ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
    	representation.addLink(Link.fromUri(profiles.clone().path(key).path("groups").build()).rel("groups").build());
    	return Response.ok(representation).build();
    }
    
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProfile(@PathParam(value="key") String key, ProfileRepresentation representation) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "updating profile for key: " + key);
    	membership.updateProfile(key, representation.getFullname());
    	return Response.noContent().build();
    }
    
    @GET
    @Path("/{key}/groups")
    @Template( template="membership/groups.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response listGroups(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "listing groups of profile for key: " + key);
    	Profile profile = membership.readProfile(key);
    	List<String> groups = Arrays.asList(profile.getGroups());
    	UriBuilder profiles = DiffusionUriBuilder.getRestUriBuilder().path(ProfileResource.class);

		KeysRepresentation representation = new KeysRepresentation ();
		for ( String group : groups ) {
			representation.addEntry(group, Link.fromUri(profiles.clone().path(key).path("groups").path(group).build()).rel("view").build());
		}
		return Response.ok(representation).build();
    }
    
    @GET
    @Path("/{key}/groups/{group}")
    public Response getVersion(@PathParam(value="key") String key, @PathParam(value="group") String group) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading group " + group + " of profile for key: " + key);
    	Profile profile = membership.readProfile(key);
    	if ( Arrays.asList(profile.getGroups()).contains(group) ) {
    		URI redirect = DiffusionUriBuilder.getRestUriBuilder().path(OrtolangObjectResource.class).path(group).build();
        	return Response.seeOther(redirect).build();
    	} else {
    		throw new KeyNotFoundException("this group is not in this profile");
    	}
    }

}
