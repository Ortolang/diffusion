package fr.ortolang.diffusion.api.content;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.template.TemplateEngine;
import fr.ortolang.diffusion.api.template.TemplateEngineException;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
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
	
	@POST
	@Path("/export")
	@Consumes({MediaType.APPLICATION_FORM_URLENCODED})
	public Response export(@FormParam("followsymlink") @DefaultValue("false") String followSymlink, @FormParam("filename") @DefaultValue("download") String filename, @FormParam("format") @DefaultValue("zip") String format, @FormParam("paths") List<String> paths, @Context SecurityContext security, @Context Request request) {
		LOGGER.log(Level.INFO, "POST /export");
		for ( String path : paths ) {
			LOGGER.log(Level.INFO, "path: " + path);
		}
		
		return Response.serverError().entity("not implemented !!").build();
	}
	
	protected ZipOutputStream exportToZip(String key, ZipOutputStream zos, PathBuilder path) throws OrtolangException, KeyNotFoundException, AccessDeniedException, IOException {

		OrtolangObject object = browser.findObject(key);
		String type = object.getObjectIdentifier().getType();

		LOGGER.log(Level.FINE, "export collection to zip : " + path.build() + " (" + key + ")");

		ZipEntry ze = new ZipEntry(path.build() + PathBuilder.PATH_SEPARATOR);

		zos.putNextEntry(ze);
		zos.closeEntry();

		if (type.equals(Collection.OBJECT_TYPE)) {
			Set<CollectionElement> elements = ((Collection) object).getElements();

			for (CollectionElement element : elements) {

				try {
					PathBuilder pathElement = path.clone().path(element.getName());
					if (element.getType().equals(Collection.OBJECT_TYPE)) {

						exportToZip(element.getKey(), zos, pathElement);

					} else if (element.getType().equals(DataObject.OBJECT_TYPE)) {
						try {

							DataObject dataObject = (DataObject) browser.findObject(element.getKey());

							LOGGER.log(Level.FINE, "export dataobject to zip : " + pathElement.build() + " (" + element.getKey() + ")");
							ZipEntry entry = new ZipEntry(pathElement.build());
							entry.setTime(element.getModification());
							entry.setSize(dataObject.getSize());
							zos.putNextEntry(entry);
							InputStream input = core.download(element.getKey());
							try {
								IOUtils.copy(input, zos);
							} catch (IOException e) {

							} finally {
								IOUtils.closeQuietly(input);
								zos.closeEntry();
							}
						} catch (CoreServiceException | DataNotFoundException e1) {
							LOGGER.log(Level.SEVERE, "unexpected error during export to zip !!", e1);
						}
					}
				} catch (InvalidPathException e) {
					LOGGER.log(Level.SEVERE, "invalid path during export to zip !!", e);
				}

			}
		}

		return zos;
	}
	
	@GET
	@Path("/key/{key}")
	public Response key(@PathParam("key") String key, @QueryParam("fd") boolean download, @Context SecurityContext security, @Context Request request) throws TemplateEngineException, CoreServiceException, KeyNotFoundException, AccessDeniedException, InvalidPathException, OrtolangException, BinaryStoreServiceException, DataNotFoundException, URISyntaxException, BrowserServiceException {
		LOGGER.log(Level.INFO, "GET /key/" + key);
		try {
			OrtolangObjectState state = browser.getState(key);
			CacheControl cc = new CacheControl();
			cc.setPrivate(true);
			if (state.isLocked()) {
				cc.setMaxAge(691200);
				cc.setMustRevalidate(false);
			} else {
				cc.setMaxAge(0);
				cc.setMustRevalidate(true);
			}
			Date lmd = new Date(state.getLastModification() / 1000 * 1000);
			ResponseBuilder builder = null;
			if (System.currentTimeMillis() - state.getLastModification() > 1000) {
				builder = request.evaluatePreconditions(lmd);
			}
			if ( builder == null ) {
				OrtolangObject object = browser.findObject(key);
				if ( object instanceof DataObject ) {
					File content = store.getFile(((DataObject)object).getStream());
					builder = Response.ok(content).header("Content-Type", ((DataObject) object).getMimeType()).header("Content-Length", ((DataObject)object).getSize()).header("Accept-Ranges", "bytes");
					if ( download ) {
						builder = builder.header("Content-Disposition", "attachment; filename=" + object.getObjectName());
					} else {
						builder = builder.header("Content-Disposition", "filename=" + object.getObjectName());
					}
					builder.lastModified(lmd);
				} else {
					return Response.serverError().entity("only data object can be downloaded using key").build();
				}
			}
			builder.cacheControl(cc);
			return builder.build();
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				NewCookie rcookie = new NewCookie(REDIRECT_PATH_PARAM_NAME, "/key/" + key, OrtolangConfig.getInstance().getProperty("api.context"), uriInfo.getBaseUri().getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false, false);
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").queryParam(REDIRECT_PATH_PARAM_NAME, "/key/" + key).build()).cookie(rcookie).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
	@GET
	public Response workspaces(@QueryParam("O") @DefaultValue("A") String asc, @Context SecurityContext security) throws TemplateEngineException, CoreServiceException {
		LOGGER.log(Level.INFO, "GET /content");
		try {
			ContentRepresentation representation = new ContentRepresentation();
			representation.setContext(OrtolangConfig.getInstance().getProperty("api.context"));
			representation.setBase("/content");
			representation.setPath("/");
			representation.setOrder("N");
			List<String> aliases = core.listAllWorkspaceAlias();
			List<CollectionElement> elements = new ArrayList<CollectionElement> (aliases.size());
			for ( String alias : aliases ) {
				elements.add(new CollectionElement(Collection.OBJECT_TYPE, alias, -1, -1, "ortolang/workspace", ""));
			}
			if ( asc.equals("D") ) {
				Collections.sort(elements, CollectionElement.ElementNameDescComparator);
				representation.setAsc(false);
			} else {
				Collections.sort(elements, CollectionElement.ElementNameAscComparator);
				representation.setAsc(true);
			}
			representation.setElements(elements);
			return Response.ok(TemplateEngine.getInstance().process("collection", representation)).build();
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				NewCookie rcookie = new NewCookie(REDIRECT_PATH_PARAM_NAME, "/", OrtolangConfig.getInstance().getProperty("api.context"), uriInfo.getBaseUri().getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false, false);
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").queryParam(REDIRECT_PATH_PARAM_NAME, "/").build()).cookie(rcookie).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
	@GET
	@Path("/{alias}")
	public Response workspace(@PathParam("alias") String alias, @QueryParam("O") @DefaultValue("A") String asc, @Context SecurityContext security, @Context Request request) throws TemplateEngineException, CoreServiceException, AliasNotFoundException, KeyNotFoundException, BrowserServiceException {
		LOGGER.log(Level.INFO, "GET /content/" + alias);
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext(OrtolangConfig.getInstance().getProperty("api.context"));
		representation.setBase("/content");
		representation.setAlias(alias);
		representation.setPath("/" + alias);
		representation.setParentPath("/");
		representation.setOrder("N");
		try {
			String wskey = core.resolveWorkspaceAlias(alias);
			OrtolangObjectState state = browser.getState(wskey);
			CacheControl cc = new CacheControl();
			cc.setPrivate(true);
			if (state.isLocked()) {
				cc.setMaxAge(691200);
				cc.setMustRevalidate(false);
			} else {
				cc.setMaxAge(0);
				cc.setMustRevalidate(true);
			}
			Date lmd = new Date(state.getLastModification() / 1000 * 1000);
			ResponseBuilder builder = null;
			if (System.currentTimeMillis() - state.getLastModification() > 1000) {
				builder = request.evaluatePreconditions(lmd);
			}
			if ( builder == null ) {
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
				if ( asc.equals("D") ) {
					Collections.sort(elements, CollectionElement.ElementNameDescComparator);
					representation.setAsc(false);
				} else {
					Collections.sort(elements, CollectionElement.ElementNameAscComparator);
					representation.setAsc(true);
				}
				representation.setElements(elements);
				builder = Response.ok(TemplateEngine.getInstance().process("collection", representation));
				builder.lastModified(lmd);
			}
			builder.cacheControl(cc);
			return builder.build();
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				NewCookie rcookie = new NewCookie(REDIRECT_PATH_PARAM_NAME, representation.getPath(), OrtolangConfig.getInstance().getProperty("api.context"), uriInfo.getBaseUri().getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false, false);
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").queryParam(REDIRECT_PATH_PARAM_NAME, representation.getPath()).build()).cookie(rcookie).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
	@GET
	@Path("/{alias}/{snapshot}")
	public Response snapshot(@PathParam("alias") String alias, @PathParam("snapshot") String snapshot, @QueryParam("O") @DefaultValue("A") String asc, @QueryParam("C") @DefaultValue("N") String order, @Context SecurityContext security, @Context Request request) throws TemplateEngineException, CoreServiceException, AccessDeniedException, AliasNotFoundException, KeyNotFoundException, BrowserServiceException {
		LOGGER.log(Level.INFO, "GET /content/" + alias + "/" + snapshot);
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext(OrtolangConfig.getInstance().getProperty("api.context"));
		representation.setBase("/content");
		representation.setAlias(alias);
		representation.setSnapshot(snapshot);
		representation.setPath("/" + alias + "/" + snapshot);
		representation.setParentPath("/" + alias);
		representation.setOrder(order);
		try {
			String wskey = core.resolveWorkspaceAlias(alias);
			Workspace workspace = core.readWorkspace(wskey);
			String rkey;
			switch (snapshot) {
				case Workspace.LATEST:
					String sname = core.findWorkspaceLatestPublishedSnapshot(wskey);
					rkey = workspace.findSnapshotByName(sname).getKey();
					if (rkey == null) {
						return Response.status(Status.NOT_FOUND).entity("No version of this workspace has been published").type("text/plain").build();
					}
					break;
				case Workspace.HEAD:
					rkey = workspace.getHead();
					break;
				default:
					SnapshotElement selement = workspace.findSnapshotByName(snapshot);
					if (selement == null) {
						return Response.status(Status.NOT_FOUND).entity("Unable to find a snapshot with name [" + snapshot + "] in this workspace").type("text/plain").build();
					}
					rkey = selement.getKey();
					break;
			}
			
			OrtolangObjectState state = browser.getState(rkey);
			CacheControl cc = new CacheControl();
			cc.setPrivate(true);
			if (!snapshot.equals(Workspace.LATEST) && state.isLocked()) {
				cc.setMaxAge(691200);
				cc.setMustRevalidate(false);
			} else {
				cc.setMaxAge(0);
				cc.setMustRevalidate(true);
			}
			Date lmd = new Date(state.getLastModification() / 1000 * 1000);
			ResponseBuilder builder = null;
			if (System.currentTimeMillis() - state.getLastModification() > 1000) {
				builder = request.evaluatePreconditions(lmd);
			}
			if ( builder == null ) {
				Collection collection = core.readCollection(rkey);
				representation.setElements(new ArrayList<CollectionElement> (collection.getElements()));
				if ( asc.equals("D") ) {
					switch ( order ) {
					case "T" : Collections.sort(representation.getElements(), CollectionElement.ElementTypeDescComparator); break;
					case "M" : Collections.sort(representation.getElements(), CollectionElement.ElementDateDescComparator); break;
					case "S" : Collections.sort(representation.getElements(), CollectionElement.ElementSizeDescComparator); break;
					default : Collections.sort(representation.getElements(), CollectionElement.ElementNameDescComparator); break;
					}
					representation.setAsc(false);
				} else {
					switch ( order ) {
					case "T" : Collections.sort(representation.getElements(), CollectionElement.ElementTypeAscComparator); break;
					case "M" : Collections.sort(representation.getElements(), CollectionElement.ElementDateAscComparator); break;
					case "S" : Collections.sort(representation.getElements(), CollectionElement.ElementSizeAscComparator); break;
					default : Collections.sort(representation.getElements(), CollectionElement.ElementNameAscComparator); break;
					}
					representation.setAsc(true);
				}
				builder = Response.ok(TemplateEngine.getInstance().process("collection", representation));
				builder.lastModified(lmd);
			}
			builder.cacheControl(cc);
			return builder.build();
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				NewCookie rcookie = new NewCookie(REDIRECT_PATH_PARAM_NAME, representation.getPath(), OrtolangConfig.getInstance().getProperty("api.context"), uriInfo.getBaseUri().getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false, false);
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").queryParam(REDIRECT_PATH_PARAM_NAME, representation.getPath()).build()).cookie(rcookie).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
	@GET
	@Path("/{alias}/{snapshot}/{path: .*}")
	@Produces({ MediaType.MEDIA_TYPE_WILDCARD })
	public Response path(@PathParam("alias") String alias, @PathParam("snapshot") String snapshot, @PathParam("path") String path, @QueryParam("fd") boolean download, @QueryParam("O") @DefaultValue("A") String asc, @QueryParam("C") @DefaultValue("N") String order, @Context SecurityContext security, @Context Request request) throws TemplateEngineException, CoreServiceException, KeyNotFoundException, AccessDeniedException, AliasNotFoundException, InvalidPathException, OrtolangException, BinaryStoreServiceException, DataNotFoundException, URISyntaxException, BrowserServiceException {
		LOGGER.log(Level.INFO, "GET /content/" + alias + "/" + snapshot + "/" + path);
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext(OrtolangConfig.getInstance().getProperty("api.context"));
		representation.setBase("/content");
		representation.setAlias(alias);
		representation.setSnapshot(snapshot);
		representation.setPath("/" + alias + "/" + snapshot + "/" + path);
		representation.setParentPath("/" + alias + "/" + snapshot);
		representation.setOrder(order);
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
			
			OrtolangObjectState state = browser.getState(okey);
			CacheControl cc = new CacheControl();
			cc.setPrivate(true);
			if (!snapshot.equals(Workspace.LATEST) && state.isLocked()) {
				cc.setMaxAge(691200);
				cc.setMustRevalidate(false);
			} else {
				cc.setMaxAge(0);
				cc.setMustRevalidate(true);
			}
			Date lmd = new Date(state.getLastModification() / 1000 * 1000);
			ResponseBuilder builder = null;
			if (System.currentTimeMillis() - state.getLastModification() > 1000) {
				builder = request.evaluatePreconditions(lmd);
			}
			if ( builder == null ) {
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
					if ( asc.equals("D") ) {
						switch ( order ) {
						case "T" : Collections.sort(representation.getElements(), CollectionElement.ElementTypeDescComparator); break;
						case "M" : Collections.sort(representation.getElements(), CollectionElement.ElementDateDescComparator); break;
						case "S" : Collections.sort(representation.getElements(), CollectionElement.ElementSizeDescComparator); break;
						default : Collections.sort(representation.getElements(), CollectionElement.ElementNameDescComparator); break;
						}
						representation.setAsc(false);
					} else {
						switch ( order ) {
						case "T" : Collections.sort(representation.getElements(), CollectionElement.ElementTypeAscComparator); break;
						case "M" : Collections.sort(representation.getElements(), CollectionElement.ElementDateAscComparator); break;
						case "S" : Collections.sort(representation.getElements(), CollectionElement.ElementSizeAscComparator); break;
						default : Collections.sort(representation.getElements(), CollectionElement.ElementNameAscComparator); break;
						}
						representation.setAsc(true);
					}
					builder = Response.ok(TemplateEngine.getInstance().process("collection", representation));
					builder.lastModified(lmd);
				} else if ( object instanceof Link ) {
					return Response.seeOther(new URI(((Link)object).getTarget())).build();
				} else {
					return Response.serverError().entity("object type not supported").build();
				}
			}
			builder.cacheControl(cc);
			return builder.build();
		} catch ( AccessDeniedException e ) {
			if ( security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
				LOGGER.log(Level.FINE, "user is not authentified, redirecting to authentication");
				NewCookie rcookie = new NewCookie(REDIRECT_PATH_PARAM_NAME, representation.getPath(), OrtolangConfig.getInstance().getProperty("api.context"), uriInfo.getBaseUri().getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false, false);
				return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path("auth").queryParam(REDIRECT_PATH_PARAM_NAME, representation.getPath()).build()).cookie(rcookie).build();
			} else {
				LOGGER.log(Level.FINE, "user is already authentified, access denied");
				return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
			}
		}
	}
	
}
