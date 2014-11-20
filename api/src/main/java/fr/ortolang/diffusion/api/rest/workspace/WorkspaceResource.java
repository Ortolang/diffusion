package fr.ortolang.diffusion.api.rest.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jgroups.util.UUID;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.filter.CORSFilter;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Path("/workspaces")
@Produces({ MediaType.APPLICATION_JSON })
public class WorkspaceResource {

	private Logger logger = Logger.getLogger(WorkspaceResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private CoreService core;
	@EJB
	private BrowserService browser;
	@EJB
	private MembershipService membership;

	public WorkspaceResource() {
	}

	@GET
	@Template(template = "workspaces/list.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listWorkspaces() throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "GET /workspaces");
		String profile = membership.getProfileKeyForConnectedIdentifier();

		List<String> keys = core.findWorkspacesForProfile(profile);
		GenericCollectionRepresentation<WorkspaceRepresentation> representation = new GenericCollectionRepresentation<WorkspaceRepresentation>();
		for (String key : keys) {
			Workspace workspace = core.readWorkspace(key);
			representation.addEntry(WorkspaceRepresentation.fromWorkspace(workspace));
		}
		representation.setOffset(0);
		representation.setSize(keys.size());
		representation.setLimit(keys.size());
		return Response.ok(representation).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response createWorkspace(@FormParam("type") @DefaultValue("default") String type, @FormParam("name") @DefaultValue("No Name Provided") String name)
			throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "POST(application/x-www-form-urlencoded) /workspaces");
		String key = UUID.randomUUID().toString();
		core.createWorkspace(key, name, type);
		URI location = DiffusionUriBuilder.getRestUriBuilder().path(WorkspaceResource.class).path(key).build();
		return Response.created(location).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createWorkspace(WorkspaceRepresentation representation) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "POST(application/json) /workspaces");
		String key = UUID.randomUUID().toString();
		core.createWorkspace(key, representation.getName(), representation.getType());
		URI location = DiffusionUriBuilder.getRestUriBuilder().path(WorkspaceResource.class).path(key).build();
		return Response.created(location).build();
	}

	@GET
	@Path("/{wskey}")
	@Template(template = "workspaces/detail.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getWorkspace(@PathParam(value = "wskey") String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "GET /workspaces/" + wskey);
		Workspace workspace = core.readWorkspace(wskey);
		WorkspaceRepresentation representation = WorkspaceRepresentation.fromWorkspace(workspace);
		return Response.ok(representation).build();
	}

	@PUT
	@Path("/{wskey}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateWorkspace(@PathParam(value = "wskey") String wskey, WorkspaceRepresentation representation) throws CoreServiceException, KeyAlreadyExistsException,
			AccessDeniedException, KeyNotFoundException {
		logger.log(Level.INFO, "PUT /workspaces/" + wskey);
		if (representation.getKey() != null && representation.getKey().length() > 0) {
			core.updateWorkspace(representation.getKey(), representation.getName());
			return Response.ok().build();
		} else {
			return Response.status(Response.Status.BAD_REQUEST).entity("representation does not contains a valid key").build();
		}
	}

	@GET
	@Path("/{wskey}/elements")
	@Template(template = "workspaces/browse.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getWorkspaceElement(@PathParam(value = "wskey") String wskey, @QueryParam(value = "root") String root, @QueryParam(value = "path") String path,
			@QueryParam(value = "metadata") String metadata) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, OrtolangException,
			BrowserServiceException, PropertyNotFoundException {
		logger.log(Level.INFO, "GET /workspaces/" + wskey + "/elements?root=" + root + "&path=" + path + "&metadata=" + metadata);
		if (path == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'path' is mandatory").build();
		}

		PathBuilder npath = PathBuilder.fromPath(path);
		String ekey = core.resolveWorkspacePath(wskey, root, npath.build());
		OrtolangObject object = browser.findObject(ekey);
		WorkspaceElementRepresentation representation = WorkspaceElementRepresentation.fromOrtolangObject(object);

		if (representation != null) {
			if (metadata != null && metadata.length() > 0) {
				logger.log(Level.INFO, "searching element metadata: " + metadata);
				for (MetadataElement element : representation.getMetadatas()) {
					if (element.getName().equals(metadata)) {
						logger.log(Level.FINE, "element metadata key found, loading...");
						ekey = element.getKey();
						object = browser.findObject(ekey);
						representation = WorkspaceElementRepresentation.fromOrtolangObject(object);
						break;
					}
				}
			}
			OrtolangObjectInfos infos = browser.getInfos(ekey);
			representation.setCreation(infos.getCreationDate());
			representation.setModification(infos.getLastModificationDate());
			representation.setPath(npath.build());
			representation.setPathParts(npath.buildParts());
			representation.setWorkspace(wskey);
			return Response.ok(representation).build();
		}
		logger.log(Level.FINE, "unable to find a workspace element at path: " + path);
		return Response.status(Response.Status.NOT_FOUND).build();
	}

	@GET
	@Path("/{wskey}/download")
	public void download(@PathParam(value = "wskey") String wskey, @QueryParam(value = "root") String root, @QueryParam(value = "path") String path,
			@QueryParam(value = "metadata") String metadata, @Context HttpServletResponse response) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException,
			OrtolangException, DataNotFoundException, IOException, CoreServiceException, InvalidPathException {
		logger.log(Level.INFO, "GET /workspaces/" + wskey + "/download?root=" + root + "&path=" + path + "&metadata=" + metadata);
		if (path == null) {
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "parameter 'path' is mandatory");
			return;
		}

		PathBuilder npath = PathBuilder.fromPath(path);
		String ekey = core.resolveWorkspacePath(wskey, root, npath.build());
		OrtolangObject object = browser.findObject(ekey);

		if (metadata != null && metadata.length() > 0) {
			for (MetadataElement element : ((MetadataSource) object).getMetadatas()) {
				if (element.getName().equals(metadata)) {
					logger.log(Level.FINE, "element metadata key found, loading...");
					ekey = element.getKey();
					object = browser.findObject(ekey);
					response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
					response.setContentType(((MetadataObject) object).getContentType());
					response.setContentLength((int) ((MetadataObject) object).getSize());
					InputStream input = core.download(ekey);
					try {
						IOUtils.copy(input, response.getOutputStream());
					} finally {
						IOUtils.closeQuietly(input);
					}
					return;
				}
			}
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "metadata not found with name: " + metadata + " at path: " + npath.build());
			return;
		} else if (object instanceof DataObject) {
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
			response.setContentType(((DataObject) object).getMimeType());
			response.setContentLength((int) ((DataObject) object).getSize());
			InputStream input = core.download(ekey);
			try {
				IOUtils.copy(input, response.getOutputStream());
			} finally {
				IOUtils.closeQuietly(input);
			}
		} else {
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "no content to download for this path");
		}
	}

	@GET
	@Path("/{wskey}/preview")
	public void preview(@PathParam(value = "wskey") String wskey, @QueryParam(value = "root") String root, @QueryParam(value = "path") String path,
			@Context HttpServletResponse response) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, OrtolangException, DataNotFoundException,
			IOException, CoreServiceException, InvalidPathException {
		logger.log(Level.INFO, "GET /workspaces/" + wskey + "/preview?root=" + root + "&path=" + path);
		if (path == null) {
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "parameter 'path' is mandatory");
			return;
		}

		PathBuilder npath = PathBuilder.fromPath(path);
		String ekey = core.resolveWorkspacePath(wskey, root, npath.build());
		OrtolangObject object = browser.findObject(ekey);

		if (object instanceof DataObject) {
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
			response.setContentType(((DataObject) object).getMimeType());
			response.setContentLength((int) ((DataObject) object).getSize());
			InputStream input = core.preview(ekey);
			try {
				IOUtils.copy(input, response.getOutputStream());
			} finally {
				IOUtils.closeQuietly(input);
			}
		} else {
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "no content to download for this path");
		}
	}

	@POST
	@Path("/{wskey}/elements")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response writeWorkspaceElement(@PathParam(value = "wskey") String wskey, @MultipartForm WorkspaceElementFormRepresentation form, @Context HttpHeaders headers)
			throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, KeyAlreadyExistsException, OrtolangException, BrowserServiceException {
		logger.log(Level.INFO, "POST /workspaces/" + wskey + "/elements");
		try {
			String contentTransferEncoding = "UTF-8";
			if (headers != null) {
				if (headers.getRequestHeader(CORSFilter.CONTENT_TRANSFER_ENCODING) != null && !headers.getRequestHeader(CORSFilter.CONTENT_TRANSFER_ENCODING).isEmpty()) {
					contentTransferEncoding = headers.getRequestHeader(CORSFilter.CONTENT_TRANSFER_ENCODING).get(0);
				}
			}

			if (form.getPath() == null) {
				return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'path' is mandatory").build();
			}

			if (form.getType() == null) {
				return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'type' is mandatory").build();
			}

			if (form.getStream() != null) {
				form.setStreamHash(core.put(form.getStream()));
			}

			if (form.getPreview() != null) {
				form.setPreviewHash(core.put(form.getPreview()));
			}

			PathBuilder npath = PathBuilder.fromPath(form.getPath());
			try {
				String ekey = core.resolveWorkspacePath(wskey, "head", npath.build());
				logger.log(Level.INFO, "element found at path: " + npath.build());
				OrtolangObject object = browser.findObject(ekey);
				switch (form.getType()) {
				case Collection.OBJECT_TYPE:
					core.updateCollection(wskey, npath.build(), form.getDescription());
					return Response.ok().build();
				case DataObject.OBJECT_TYPE:
					core.updateDataObject(wskey, npath.build(), form.getDescription(), form.getStreamHash());
					return Response.ok().build();
				case MetadataObject.OBJECT_TYPE:
					boolean mdexists = false;
					String name = URLDecoder.decode(form.getName(), contentTransferEncoding);
					for (MetadataElement element : ((MetadataSource) object).getMetadatas()) {
						if (element.getName().equals(name)) {
							logger.log(Level.FINE, "element metadata key found, need to update");
							mdexists = true;
							break;
						}
					}
					if (mdexists) {
						core.updateMetadataObject(wskey, npath.build(), name, form.getFormat(), form.getStreamHash());
						return Response.ok().build();
					} else {
						core.createMetadataObject(wskey, npath.build(), name, form.getFormat(), form.getStreamHash());
						URI newly = DiffusionUriBuilder.getRestUriBuilder().path(WorkspaceResource.class).path(wskey).path("elements").queryParam("path", npath.build())
								.queryParam("metadataname", name).build();
						return Response.created(newly).build();
					}
				default:
					return Response.status(Response.Status.BAD_REQUEST).entity("unable to update element of type: " + form.getType()).build();
				}
			} catch (InvalidPathException e) {
				if (form.getType().equals(MetadataObject.OBJECT_TYPE)) {
					logger.log(Level.FINEST, "unable to create metadata, path: " + npath.build() + " does not exists");
					return Response.status(Response.Status.BAD_REQUEST).entity("unable to create metadata, path: " + npath.build() + " does not exists").build();
				} else {
					switch (form.getType()) {
					case DataObject.OBJECT_TYPE:
						core.createDataObject(wskey, npath.build(), form.getDescription(), form.getStreamHash());
						break;
					case Collection.OBJECT_TYPE:
						core.createCollection(wskey, npath.build(), form.getDescription());
						break;
					case Link.OBJECT_TYPE:
						core.createLink(wskey, npath.build(), form.getTarget());
						break;
					default:
						return Response.status(Response.Status.BAD_REQUEST).entity("unable to create element of type: " + form.getType()).build();
					}
					URI newly = DiffusionUriBuilder.getRestUriBuilder().path(WorkspaceResource.class).path(wskey).path("elements").queryParam("path", npath.build()).build();
					return Response.created(newly).build();
				}
			}
		} catch (DataCollisionException | UnsupportedEncodingException e) {
			logger.log(Level.SEVERE, "an error occured while creating workspace element: " + e.getMessage(), e);
			return Response.serverError().entity(e.getMessage()).build();
		}
	}

	@PUT
	@Path("/{wskey}/elements")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response writeWorkspaceElementRepresentation(@PathParam(value = "wskey") String wskey, WorkspaceElementRepresentation representation) throws CoreServiceException,
			KeyNotFoundException, InvalidPathException, AccessDeniedException, KeyAlreadyExistsException, OrtolangException, BrowserServiceException {
		logger.log(Level.INFO, "PUT /workspaces/" + wskey + "/elements");
		PathBuilder npath = PathBuilder.fromPath(representation.getPath());
		try {
			core.resolveWorkspacePath(wskey, "head", npath.build());
			logger.log(Level.INFO, "element found at path: " + npath.build());
			if (representation.getType().equals(Collection.OBJECT_TYPE)) {
				core.updateCollection(wskey, npath.build(), representation.getDescription());
				return Response.ok().build();
			} else {
				return Response.status(Response.Status.BAD_REQUEST).entity("unable to update element of type: " + representation.getType()).build();
			}
		} catch (InvalidPathException e) {
			if (representation.getType().equals(Collection.OBJECT_TYPE)) {
				core.createCollection(wskey, npath.build(), representation.getDescription());
			} else {
				return Response.status(Response.Status.BAD_REQUEST).entity("unable to create element of type: " + representation.getType()).build();
			}
			URI newly = DiffusionUriBuilder.getRestUriBuilder().path(WorkspaceResource.class).path(wskey).path("elements").queryParam("path", npath.build()).build();
			return Response.created(newly).build();
		}
	}

	@DELETE
	@Path("/{wskey}/elements")
	public Response deleteWorkspaceElement(@PathParam(value = "wskey") String wskey, @QueryParam(value = "root") String root, @QueryParam(value = "path") String path,
			@QueryParam(value = "metadataname") String metadataname) throws CoreServiceException, InvalidPathException, AccessDeniedException, KeyNotFoundException,
			BrowserServiceException {
		logger.log(Level.INFO, "DELETE /workspaces/" + wskey + "/elements");
		if (path == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'path' is mandatory").build();
		}

		if (metadataname != null && metadataname.length() > 0) {
			core.deleteMetadataObject(wskey, path, metadataname);
		} else {
			String ekey = core.resolveWorkspacePath(wskey, root, path);
			OrtolangObjectIdentifier identifier = browser.lookup(ekey);
			switch (identifier.getType()) {
			case DataObject.OBJECT_TYPE:
				core.deleteDataObject(wskey, path);
				break;
			case Collection.OBJECT_TYPE:
				core.deleteCollection(wskey, path);
				break;
			case Link.OBJECT_TYPE:
				core.deleteLink(wskey, path);
				break;
			default:
				return Response.serverError().entity("Unknown type").build();
			}
		}
		return Response.noContent().build();
	}

	@POST
	@Path("/{wskey}/snapshots")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response snapshotWorkspace(@PathParam(value = "wskey") String wskey, @FormParam(value = "snapshotname") String name) throws CoreServiceException, KeyNotFoundException,
			AccessDeniedException {
		logger.log(Level.INFO, "POST /workspaces/" + wskey + "/snapshots");
		if (name == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'snapshotname' is mandatory").build();
		}
		core.snapshotWorkspace(wskey, name);
		return Response.ok().build();
	}

}
