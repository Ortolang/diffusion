package fr.ortolang.diffusion.api.rest.content;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.api.rest.template.TemplateEngine;
import fr.ortolang.diffusion.api.rest.template.TemplateEngineException;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Path("/content")
@Produces({ MediaType.TEXT_HTML })
public class ContentResource {
	
	private static final String REDIRECT_PATH_PARAM_NAME = "redirect";
	private static final Logger LOGGER = Logger.getLogger(ContentResource.class.getName());
	
	@EJB
	private CoreService core;
	@EJB
	private BrowserService browser;
	@EJB
	private BinaryStoreService store;
	@Context
	private UriInfo uriInfo;
	
	@GET
	public Response workspaces() throws TemplateEngineException, CoreServiceException {
		LOGGER.log(Level.INFO, "GET /content");
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/rest/content");
		representation.setPath("/");
		List<String> aliases = core.listAllWorkspaceAlias();
		List<CollectionElement> elements = new ArrayList<CollectionElement> (aliases.size());
		for ( String alias : aliases ) {
			elements.add(new CollectionElement(Collection.OBJECT_TYPE, alias, -1, -1, "ortolang/workspace", ""));
		}
		Collections.sort(elements, CollectionElement.ElementNameAscComparator);
		//TODO allow sorting in reverse order
		representation.setElements(elements);
		return Response.ok(TemplateEngine.getInstance().process("collection", representation)).build();
	}
	
	@GET
	@Path("/{alias}")
	public Response workspace(@PathParam("alias") String alias, @Context SecurityContext security) throws TemplateEngineException, CoreServiceException, AliasNotFoundException, KeyNotFoundException {
		LOGGER.log(Level.INFO, "GET /content/" + alias);
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/rest/content");
		representation.setAlias(alias);
		representation.setPath("/" + alias);
		representation.setParentPath("/");
		try {
			String wskey = core.resolveWorkspaceAlias(alias);
			Workspace workspace = core.readWorkspace(wskey);
			List<CollectionElement> elements = new ArrayList<CollectionElement> (workspace.getSnapshots().size());
			String latest = core.findWorkspaceLatestPublishedSnapshot(wskey);
			if ( latest != null && latest.length() > 0 ) {
				elements.add(new CollectionElement(Collection.OBJECT_TYPE, Workspace.LATEST, -1, -1, "ortolang/snapshot", latest));
			}
			elements.add(new CollectionElement(Collection.OBJECT_TYPE, Workspace.HEAD, -1, -1, "ortolang/snapshot", workspace.getHead()));
			for ( SnapshotElement snapshot : workspace.getSnapshots() ) {
				elements.add(new CollectionElement(Collection.OBJECT_TYPE, snapshot.getName(), -1, -1, "ortolang/snapshot", snapshot.getKey()));
			}
			representation.setElements(elements);
			return Response.ok(TemplateEngine.getInstance().process("collection", representation)).build();
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").build()).cookie(new NewCookie(REDIRECT_PATH_PARAM_NAME, representation.getPath())).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
	@GET
	@Path("/{alias}/{snapshot}")
	public Response snapshot(@PathParam("alias") String alias, @PathParam("snapshot") String snapshot, @Context SecurityContext security) throws TemplateEngineException, CoreServiceException, AccessDeniedException, AliasNotFoundException, KeyNotFoundException {
		LOGGER.log(Level.INFO, "GET /content/" + alias + "/" + snapshot);
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/rest/content");
		representation.setAlias(alias);
		representation.setSnapshot(snapshot);
		representation.setPath("/" + alias + "/" + snapshot);
		representation.setParentPath("/" + alias);
		try {
			String wskey = core.resolveWorkspaceAlias(alias);
			Workspace workspace = core.readWorkspace(wskey);
			String rkey = null;
			if ( snapshot.equals(Workspace.LATEST) ) {
				rkey = core.findWorkspaceLatestPublishedSnapshot(wskey);
				if ( rkey == null ) {
					return Response.status(Status.NOT_FOUND).entity("No version of this workspace has been published").type("text/plain").build();
				}
			} else if ( snapshot.equals(Workspace.HEAD) ) {
				rkey = workspace.getHead();
			} else {
				SnapshotElement selement = workspace.findSnapshotByName(snapshot);
				if ( selement == null ) {
					return Response.status(Status.NOT_FOUND).entity("Unable to find a snapshot with name [" + snapshot + "] in this workspace").type("text/plain").build();
				}
				rkey = selement.getKey();
			}
			
			Collection collection = core.readCollection(rkey);
			representation.setElements(new ArrayList<CollectionElement> (collection.getElements()));
			//TODO sort elements
			return Response.ok(TemplateEngine.getInstance().process("collection", representation)).build();
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").build()).cookie(new NewCookie(REDIRECT_PATH_PARAM_NAME, representation.getPath())).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
	@GET
	@Path("/{alias}/{snapshot}/{path: .*}")
	@Produces({ MediaType.MEDIA_TYPE_WILDCARD })
	public Response path(@PathParam("alias") String alias, @PathParam("snapshot") String snapshot, @PathParam("path") String path, @QueryParam("fd") boolean download, @Context SecurityContext security) throws TemplateEngineException, CoreServiceException, KeyNotFoundException, AccessDeniedException, AliasNotFoundException, InvalidPathException, OrtolangException, BinaryStoreServiceException, DataNotFoundException, URISyntaxException {
		LOGGER.log(Level.INFO, "GET /content/" + alias + "/" + snapshot + "/" + path);
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/rest/content");
		representation.setAlias(alias);
		representation.setSnapshot(snapshot);
		representation.setPath("/" + alias + "/" + snapshot + "/" + path);
		representation.setParentPath("/" + alias + "/" + snapshot);
		try {
			String wskey = core.resolveWorkspaceAlias(alias);
			if ( snapshot.equals(Workspace.LATEST) ) {
				snapshot = core.findWorkspaceLatestPublishedSnapshot(wskey);
				if ( snapshot == null ) {
					return Response.status(Status.NOT_FOUND).entity("No version of this workspace has been published").type("text/plain").build();
				}
			}
			PathBuilder npath = PathBuilder.fromPath(path);
			String okey = core.resolveWorkspacePath(wskey, snapshot, npath.build());
			
			OrtolangObject object = browser.findObject(okey);
			if ( object instanceof DataObject ) {
				File content = store.getFile(((DataObject)object).getStream());
				if ( download ) {
					return Response.ok(content).header("Content-Disposition", "attachment; filename=" + object.getObjectName()).header("Content-Type", ((DataObject) object).getMimeType()).header("Content-Length", ((DataObject)object).getSize()).build();
				} else {
					return Response.ok(content).header("Content-Disposition", "filename=" + object.getObjectName()).header("Content-Type", ((DataObject) object).getMimeType()).header("Content-Length", ((DataObject)object).getSize()).header("Accept-Ranges", "bytes").build();
				}
			} else if ( object instanceof Collection ) {
				representation.setElements(new ArrayList<CollectionElement> (((Collection)object).getElements()));
				//TODO sort elements
				return Response.ok(TemplateEngine.getInstance().process("collection", representation)).build();
			} else if ( object instanceof Link ) {
				return Response.seeOther(new URI(((Link)object).getTarget())).build();
			} else {
				return Response.serverError().entity("object type not supported").build();
			}
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").build()).cookie(new NewCookie(REDIRECT_PATH_PARAM_NAME, representation.getPath())).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
	@GET
	@Path("/auth")
	public Response authenticate(@CookieParam(REDIRECT_PATH_PARAM_NAME) String credirect, @QueryParam(REDIRECT_PATH_PARAM_NAME) String qredirect) {
		LOGGER.log(Level.INFO, "GET /content/auth");
		UriBuilder builder = uriInfo.getBaseUriBuilder().path(ContentResource.class);
		if ( credirect != null && credirect.length() > 0 ) {
			LOGGER.log(Level.FINE, "redirecting to path found in cookie : " + credirect);
			builder.path(credirect);
		} else if ( qredirect != null && qredirect.length() > 0 ) {
			LOGGER.log(Level.FINE, "redirecting to path found in query : " + qredirect);
			builder.path(qredirect);
		} 
		return Response.seeOther(builder.build()).build();
	}

}
