package fr.ortolang.diffusion.rest.collaboration.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import fr.ortolang.diffusion.collaboration.CollaborationService;
import fr.ortolang.diffusion.collaboration.CollaborationServiceException;
import fr.ortolang.diffusion.collaboration.entity.Project;
import fr.ortolang.diffusion.core.entity.CollectionProperty;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rest.core.collection.CollectionResource;
import fr.ortolang.diffusion.rest.membership.group.GroupResource;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;


@Path("/collaboration/projects")
@Produces({ MediaType.APPLICATION_JSON })
public class ProjectResource {
	
private Logger logger = Logger.getLogger(ProjectResource.class.getName());
	
	@Context
    private UriInfo uriInfo;
	@EJB 
	private CollaborationService collaboration;
 
    public ProjectResource() {
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findProjects() throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "finding projects for connected identifier");
    	List<String> keys = collaboration.findMyProjects();
    	return Response.ok(keys).build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createProject(@FormParam(value="name") String name, @FormParam(value="type") String type) throws CollaborationServiceException, KeyAlreadyExistsException, AccessDeniedException {
    	logger.log(Level.INFO, "creating new project");
    	String key = UUID.randomUUID().toString();
    	collaboration.createProject(key, name, type);
    	URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).path(ProjectResource.class).path(key).build();
    	return Response.created(location).build();
    }
    
    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProject(@PathParam(value="key") String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading project for key: " + key);
    	Project project = collaboration.readProject(key);
    	ProjectRepresentation representation = ProjectRepresentation.fromProject(project);
    	return Response.ok(representation).build();
    }
    
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateProject(@PathParam(value="key") String key, ProjectRepresentation representation) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "updating project for key: " + key);
    	collaboration.updateProject(key, representation.getName());
    	return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{key}")
    public Response deleteProject(@PathParam(value="key") String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "deleting project for key: " + key);
    	collaboration.deleteProject(key);
    	return Response.noContent().build();
    }
    
    @GET
    @Path("/{key}/versions")
    public Response listVersions(@PathParam(value="key") String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "listing versions for project for key: " + key);
    	Project project = collaboration.readProject(key);
    	List<String> versions = project.getHistory();
    	return Response.ok(versions).build();
    }
    
    @POST
    @Path("/{key}/versions")
    public Response snapshot(@PathParam(value="key") String key, @FormParam(value="type") String type, @FormParam(value="name") String name) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "creating new versions for project with key: " + key);
    	Project project = collaboration.readProject(key);
    	String oldroot = project.getRoot();
    	if ( type.toUpperCase().equals(CollectionProperty.Version.SNAPSHOT.name()) ) {
    		collaboration.snapshotProject(key);
    	} else if ( type.toUpperCase().equals(CollectionProperty.Version.RELEASE.name()) ) {
    		collaboration.releaseProject(key, name);
    	} else {
    		return Response.status(400).entity("unable to understand type").build();
    	}
    	URI location = UriBuilder.fromUri(uriInfo.getBaseUri()).path(GroupResource.class).path(key).path("versions").path(oldroot).build();
    	return Response.created(location).build();
    }
    
    @GET
    @Path("/{key}/versions/{version}")
    public Response getVersion(@PathParam(value="key") String key, @PathParam(value="version") String version) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading version " + version + " of project for key: " + key);
    	collaboration.readProject(key);
    	URI redirect = UriBuilder.fromUri(uriInfo.getBaseUri()).path(CollectionResource.class).path(version).build();
    	return Response.seeOther(redirect).build();
    }
    
    @GET
    @Path("/{key}/members")
    public Response listMembers(@PathParam(value="key") String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "listing members of project for key: " + key);
    	Project project = collaboration.readProject(key);
    	URI redirect = UriBuilder.fromUri(uriInfo.getBaseUri()).path(GroupResource.class).path(project.getMembers()).build();
    	return Response.seeOther(redirect).build();
    }
    
    @GET
    @Path("/{key}/root")
    public Response readRootCollection(@PathParam(value="key") String key) throws CollaborationServiceException, KeyNotFoundException, AccessDeniedException {
    	logger.log(Level.INFO, "reading root collection of project for key: " + key);
    	Project project = collaboration.readProject(key);
    	URI redirect = UriBuilder.fromUri(uriInfo.getBaseUri()).path(CollectionResource.class).path(project.getRoot()).build();
    	return Response.seeOther(redirect).build();
    }

}
