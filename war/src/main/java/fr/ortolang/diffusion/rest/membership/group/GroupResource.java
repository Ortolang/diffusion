package fr.ortolang.diffusion.rest.membership.group;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.rest.Template;
import fr.ortolang.diffusion.rest.api.OrtolangCollectionRepresentation;
import fr.ortolang.diffusion.rest.api.OrtolangLinkRepresentation;
import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/membership/groups")
@Produces({ MediaType.APPLICATION_JSON })
public class GroupResource {
	
	private Logger logger = Logger.getLogger(GroupResource.class.getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private MembershipService membership;
 
    public GroupResource() {
    }
    
    @GET
    @Path("/{key}")
    @Template( template="membership/group.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response getGroup(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading group for key: " + key);
    	Group group = membership.readGroup(key);
    	UriBuilder groups = DiffusionUriBuilder.getRestUriBuilder().path(GroupResource.class);
    	
    	GroupRepresentation representation = GroupRepresentation.fromGroup(group);
    	representation.addLink(OrtolangLinkRepresentation.fromUri(groups.clone().path(key).path("members").build()).rel("members"));
    	return Response.ok(representation).build();
    }
    
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateGroup(@PathParam(value="key") String key, GroupRepresentation representation) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "updating group for key: " + key);
    	membership.updateGroup(key, representation.getName(), representation.getDescription());
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}")
    public Response deleteGroup(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "deleting group for key: " + key);
    	membership.deleteGroup(key);
    	return Response.noContent().build();
    }
    
    @PUT
    @Path("/{key}/join")
    public Response joinGroup(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "joining group for key: " + key);
    	membership.joinGroup(key);
    	return Response.noContent().build();
    }
    
    @PUT
    @Path("/{key}/leave")
    public Response leaveGroup(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "leaving group for key: " + key);
    	membership.leaveGroup(key);
    	return Response.noContent().build();
    }
    
    @GET
    @Path("/{key}/members")
    @Template( template="membership/members.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response listMembers(@PathParam(value="key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "listing members of group for key: " + key);
    	Group group = membership.readGroup(key);
    	List<String> members = Arrays.asList(group.getMembers());
    	UriBuilder groups = DiffusionUriBuilder.getRestUriBuilder().path(GroupResource.class);

    	OrtolangCollectionRepresentation representation = new OrtolangCollectionRepresentation ();
    	representation.setStart(0);
		representation.setSize(members.size());
		representation.setTotalSize(members.size());
		for ( String member : members ) {
			representation.addEntry(member, OrtolangLinkRepresentation.fromUri(groups.clone().path(key).path("members").path(member).build()).rel("view"));
		}
		return Response.ok(representation).build();
    }
    
    @GET
    @Path("/{key}/members/{member}")
    public Response getMember(@PathParam(value="key") String key, @PathParam(value="member") String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading member: " + member + " of group for key: " + key);
    	Group group = membership.readGroup(key);
    	List<String> members = Arrays.asList(group.getMembers());
    	if ( members.contains(member) ) {
    		URI redirect = DiffusionUriBuilder.getRestUriBuilder().path(OrtolangObjectResource.class).path(member).build();
    		return Response.seeOther(redirect).build();
    	} else {
    		throw new KeyNotFoundException("this member is not in this group");
    	}
    }
    
    @PUT
    @Path("/{key}/members/{member}")
    public Response addMember(@PathParam(value="key") String key, @PathParam(value="member") String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "adding member: " + member + " in group with key: " + key);
    	membership.addMemberInGroup(key, member);
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}/members/{member}")
    public Response removeMember(@PathParam(value="key") String key, @PathParam(value="member") String member) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "removing member: " + member + " from group with key: " + key);
    	membership.removeMemberFromGroup(key, member);
    	return Response.noContent().build();
    }

}
