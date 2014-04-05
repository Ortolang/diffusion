package fr.ortolang.diffusion.rest.membership.profile;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConnected() throws MembershipServiceException, KeyNotFoundException {
    	logger.log(Level.INFO, "reading connected profile");
    	String key = membership.getProfileKeyForConnectedIdentifier();
    	URI view = UriBuilder.fromUri(uriInfo.getBaseUri()).path(ProfileResource.class).path(key).build(); 
    	return Response.ok(key).link(view, "view").build();
    }
    
    @POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createProfile(@DefaultValue("change my name") @FormParam("fullname") String fullname, @DefaultValue("change my email") @FormParam("email") String email) throws MembershipServiceException, KeyAlreadyExistsException, ProfileAlreadyExistsException {
    	logger.log(Level.INFO, "creating profile for connected identifier");
    	membership.createProfile(fullname, email);
    	return Response.noContent().build();
    }
    
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProfile(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading profile for key: " + key);
    	Profile profile = membership.readProfile(key);
    	ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response listGroups(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "listing groups of profile for key: " + key);
    	Profile profile = membership.readProfile(key);
    	List<String> groups = Arrays.asList(profile.getGroups());
    	return Response.ok(groups).build();
    }

}
