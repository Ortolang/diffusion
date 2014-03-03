package fr.ortolang.diffusion.rest.membership.profile;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
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
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;

@Path("/membership/profile")
@Produces({ MediaType.APPLICATION_JSON })
public class ProfileResource {
	
private Logger logger = Logger.getLogger(OrtolangObjectResource.class.getName());
	
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
    	logger.log(Level.INFO, "getting connected profile");
    	String key = membership.getProfileKeyForConnectedIdentifier();
    	URI connected = UriBuilder.fromUri(uriInfo.getBaseUri()).path(ProfileResource.class).path(key).build(); 
    	return Response.seeOther(connected).build();
    }
    
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException {
    	logger.log(Level.INFO, "getting profile for key: " + key);
    	Profile profile = membership.readProfile(key);
    	return Response.ok(profile).build();
    }

}
