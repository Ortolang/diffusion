package fr.ortolang.diffusion.api.workspace;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.api.ApiUriBuilder;
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.filter.CORSFilter;
import fr.ortolang.diffusion.api.runtime.ProcessRepresentation;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.*;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.*;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.event.EventServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import org.javers.core.diff.Change;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/workspaces")
@Produces({ MediaType.APPLICATION_JSON })
public class WorkspaceResource {

    private static final Logger LOGGER = Logger.getLogger(WorkspaceResource.class.getName());

    @EJB
    private CoreService core;
    @EJB
    private BrowserService browser;
    @EJB
    private BinaryStoreService binary;
    @EJB
    private MembershipService membership;
    @EJB
    private EventService event;
    @EJB
    private RuntimeService runtime;
    @EJB
    private SecurityService security;

    public WorkspaceResource() {
    }

    @GET
    @GZIP
    public Response listProfileWorkspaces(@QueryParam("md") @DefaultValue("false") boolean md)
            throws CoreServiceException, KeyNotFoundException, AccessDeniedException, BrowserServiceException, SecurityServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces");
        String profile = membership.getProfileKeyForConnectedIdentifier();

        List<String> keys = core.findWorkspacesForProfile(profile);
        GenericCollectionRepresentation<WorkspaceRepresentation> representation = new GenericCollectionRepresentation<WorkspaceRepresentation>();
        for (String key : keys) {
            Workspace workspace = core.readWorkspace(key);
            OrtolangObjectInfos infos = browser.getInfos(key);
            WorkspaceRepresentation workspaceRepresentation = WorkspaceRepresentation.fromWorkspace(workspace, infos);
            workspaceRepresentation.setOwner(security.getOwner(key));
            if (md) {
                addMetadatasToRepresentation(workspaceRepresentation);
            }
            representation.addEntry(workspaceRepresentation);
        }
        representation.setOffset(0);
        representation.setSize(keys.size());
        representation.setLimit(keys.size());
        return Response.ok(representation).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @GZIP
    public Response createWorkspace(@FormParam("type") @DefaultValue("default") String type, @FormParam("name") @DefaultValue("No Name Provided") String name, @FormParam("alias") String alias)
            throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, BrowserServiceException, KeyNotFoundException, SecurityServiceException, AliasAlreadyExistsException,
            URISyntaxException {
        LOGGER.log(Level.INFO, "POST(application/x-www-form-urlencoded) /workspaces");
        String key = java.util.UUID.randomUUID().toString();
        Workspace workspace;
        if (alias != null && alias.length() > 0) {
            workspace = core.createWorkspace(key, alias, name, type);
        } else {
            workspace = core.createWorkspace(key, name, type);
        }
        URI location = ApiUriBuilder.getApiUriBuilder().path(WorkspaceResource.class).path(key).build();
        OrtolangObjectInfos infos = browser.getInfos(workspace.getKey());
        WorkspaceRepresentation workspaceRepresentation = WorkspaceRepresentation.fromWorkspace(workspace, infos);
        workspaceRepresentation.setOwner(security.getOwner(key));
        return Response.created(location).entity(workspaceRepresentation).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response createWorkspace(WorkspaceRepresentation representation)
            throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, BrowserServiceException, KeyNotFoundException, SecurityServiceException, AliasAlreadyExistsException,
            URISyntaxException {
        LOGGER.log(Level.INFO, "POST(application/json) /workspaces");
        String key = UUID.randomUUID().toString();
        Workspace workspace;
        if (representation.getAlias() != null && representation.getAlias().length() > 0) {
            workspace = core.createWorkspace(key, representation.getAlias(), representation.getName(), representation.getType());
        } else {
            workspace = core.createWorkspace(key, representation.getName(), representation.getType());
        }
        URI location = ApiUriBuilder.getApiUriBuilder().path(WorkspaceResource.class).path(key).build();
        OrtolangObjectInfos infos = browser.getInfos(workspace.getKey());
        WorkspaceRepresentation workspaceRepresentation = WorkspaceRepresentation.fromWorkspace(workspace, infos);
        workspaceRepresentation.setOwner(security.getOwner(key));
        return Response.created(location).entity(workspaceRepresentation).build();
    }

    @GET
    @Path("/{wskey}")
    @GZIP
    public Response getWorkspace(@PathParam(value = "wskey") String wskey, @QueryParam("md") @DefaultValue("false") boolean md, @Context Request request)
            throws CoreServiceException, BrowserServiceException, KeyNotFoundException, AccessDeniedException, SecurityServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces/" + wskey);

        OrtolangObjectState state = browser.getState(wskey);
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        cc.setMustRevalidate(true);
        Date lmd = new Date(state.getLastModification() / 1000 * 1000);
        ResponseBuilder builder = null;
        if (System.currentTimeMillis() - state.getLastModification() > 1000) {
            builder = request.evaluatePreconditions(lmd);
        }

        if(builder == null){
            Workspace workspace = core.readWorkspace(wskey);
            OrtolangObjectInfos infos = browser.getInfos(wskey);
            WorkspaceRepresentation representation = WorkspaceRepresentation.fromWorkspace(workspace, infos);
            representation.setOwner(security.getOwner(wskey));
            if (md) {
                addMetadatasToRepresentation(representation);
            }
            builder = Response.ok(representation);
            builder.lastModified(lmd);
        }

        builder.cacheControl(cc);
        return builder.build();
    }

    @PUT
    @Path("/{wskey}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkspace(@PathParam(value = "wskey") String wskey, WorkspaceRepresentation representation) throws CoreServiceException, KeyAlreadyExistsException,
            AccessDeniedException, KeyNotFoundException, WorkspaceReadOnlyException {
        LOGGER.log(Level.INFO, "PUT /workspaces/" + wskey);
        if (representation.getKey() != null && representation.getKey().length() > 0) {
            core.updateWorkspace(representation.getKey(), representation.getName());
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("representation does not contains a valid key").build();
        }
    }

    @DELETE
    @Path("/{wskey}")
    @GZIP
    public Response deleteWorkspace(@PathParam(value = "wskey") String wskey)
            throws CoreServiceException, AccessDeniedException, KeyNotFoundException, RuntimeServiceException, KeyAlreadyExistsException {
        if (core.findWorkspaceLatestPublishedSnapshot(wskey) != null) {
            return Response.status(Response.Status.FORBIDDEN).entity("Cannot delete an already published workspace").build();
        }
        String key = UUID.randomUUID().toString();
        Map<String, Object> params = new HashMap<>();
        params.put("wskey", wskey);
        Process process = runtime.createProcess(key, "delete-workspace", "Delete workspace", wskey);
        runtime.startProcess(key, params);
        return Response.status(Response.Status.ACCEPTED).entity(ProcessRepresentation.fromProcess(process)).build();
    }

    @GET
    @Path("/{wskey}/elements")
    @GZIP
    public Response getWorkspaceElement(@PathParam(value = "wskey") String wskey, @QueryParam(value = "root") String root, @QueryParam(value = "path") String path,
            @QueryParam(value = "metadata") String metadata, @QueryParam(value = "policy") @DefaultValue("false") boolean policy, @Context Request request)
            throws CoreServiceException, KeyNotFoundException, InvalidPathException, OrtolangException, BrowserServiceException, PropertyNotFoundException,
            PathNotFoundException, DataNotFoundException, BinaryStoreServiceException, IOException, RegistryServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces/" + wskey + "/elements?root=" + root + "&path=" + path + "&metadata=" + metadata);
        if (path == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'path' is mandatory").build();
        }

        PathBuilder npath = PathBuilder.fromPath(path);
        String ekey = core.resolveWorkspacePath(wskey, root, npath.build());

        OrtolangObjectState state = browser.getState(ekey);
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        if ( root != null && !root.equals(Workspace.HEAD) && state.isLocked() ) {
            cc.setMaxAge(691200);
            cc.setMustRevalidate(false);
        } else {
            cc.setMaxAge(0);
            cc.setMustRevalidate(true);
        }
        long lastModification;
        if (root == null || root.equals(Workspace.HEAD)) {
            OrtolangObjectState workspaceState = browser.getState(wskey);
            lastModification = workspaceState.getLastModification();
        } else {
            lastModification = state.getLastModification();
        }
        Date lmd = new Date(lastModification / 1000 * 1000);
        ResponseBuilder builder = null;
        if (System.currentTimeMillis() - lastModification > 1000) {
            builder = request.evaluatePreconditions(lmd);
        }

        if(builder == null){
            OrtolangObject object = browser.findObject(ekey);
            WorkspaceElementRepresentation representation = WorkspaceElementRepresentation.fromOrtolangObject(object);
            if (representation != null) {
                if (metadata != null && metadata.length() > 0) {
                    LOGGER.log(Level.FINE, "searching element metadata: " + metadata);
                    MetadataElement mdElement = null;
                    for (MetadataElement element : representation.getMetadatas()) {
                        if (element.getName().equals(metadata)) {
                            LOGGER.log(Level.FINE, "element metadata key found, loading...");
                            mdElement = element;
                            break;
                        }
                    }

                    if(mdElement!=null) {
                        ekey = mdElement.getKey();
                        object = browser.findObject(ekey);
                        representation = WorkspaceElementRepresentation.fromOrtolangObject(object);
                    } else {
                        LOGGER.log(Level.FINE, "unable to find a metadata element at path: " + path);
                        return Response.status(Response.Status.NOT_FOUND).build();
                    }
                }
                OrtolangObjectInfos infos = browser.getInfos(ekey);
                representation.setInfos(infos);
                representation.setPath(npath.build());
                representation.setPathParts(npath.buildParts());
                representation.setWorkspace(wskey);
                try {
                    security.checkAnonymousPermission(ekey, "download");
                    representation.setUnrestrictedDownload(true);
                } catch (AccessDeniedException | SecurityServiceException e) {
                    representation.setUnrestrictedDownload(false);
                }

                if (policy) {
                    String publicationPolicy = core.readPublicationPolicy(wskey, root, path);
                    representation.setPublicationPolicy(publicationPolicy);
                    if (object instanceof Collection) {
                        Map<String, String> publicationPolicies = new HashMap<>();
                        String childPublicationPolicy;
                        for (CollectionElement collectionElement : ((Collection) object).getElements()) {
                            childPublicationPolicy = core.readPublicationPolicy(collectionElement.getKey());
                            publicationPolicies.put(collectionElement.getKey(), childPublicationPolicy == null ? publicationPolicy : childPublicationPolicy);
                        }
                        representation.setPublicationPolicies(publicationPolicies);
                    }

                }
                builder = Response.ok(representation);
                builder.lastModified(lmd);
            } else {
                LOGGER.log(Level.FINE, "unable to find a workspace element at path: " + path);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        }

        builder.cacheControl(cc);
        return builder.build();
    }

    @POST
    @Path("/{wskey}/elements")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @GZIP
    public Response createWorkspaceElement(@PathParam(value = "wskey") String wskey, @MultipartForm WorkspaceElementFormRepresentation form, @Context HttpHeaders headers)
            throws CoreServiceException, KeyNotFoundException, InvalidPathException, KeyAlreadyExistsException, OrtolangException, BrowserServiceException, MetadataFormatException, PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.INFO, "POST /workspaces/" + wskey + "/elements");
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
                LOGGER.log(Level.FINE, "element found at path: " + npath.build());
                OrtolangObject object = browser.findObject(ekey);
                OrtolangObject updatedObject;
                switch (form.getType()) {
                case DataObject.OBJECT_TYPE:
                    updatedObject = core.updateDataObject(wskey, npath.build(), form.getStreamHash());
                    break;
                case Link.OBJECT_TYPE:
                    updatedObject = core.updateLink(wskey, npath.build(), form.getTarget());
                    break;
                case Collection.OBJECT_TYPE:
                    throw new PathNotFoundException();
                case MetadataObject.OBJECT_TYPE:
                    boolean mdexists = false;
                    String name = URLDecoder.decode(form.getName(), contentTransferEncoding);
                    for (MetadataElement element : ((MetadataSource) object).getMetadatas()) {
                        if (element.getName().equals(name)) {
                            LOGGER.log(Level.FINE, "element metadata key found, need to update");
                            mdexists = true;
                            break;
                        }
                    }
                    if (mdexists) {
                        updatedObject = core.updateMetadataObject(wskey, npath.build(), name, form.getStreamHash(), form.getStreamFilename(), false, form.getFormat().isEmpty() ? null : form.getFormat());
                        break;
                    } else {
                        try {
                            MetadataObject metadataObject = core.createMetadataObject(wskey, npath.build(), name, form.getStreamHash(), form.getStreamFilename(), false);
                            WorkspaceElementRepresentation representation = makeRepresentation(metadataObject, wskey, npath);
                            URI newly = ApiUriBuilder.getApiUriBuilder().path(WorkspaceResource.class).path(wskey).path("elements").queryParam("path", npath.build())
                                    .queryParam("metadataname", name).build();
                            return Response.created(newly).entity(representation).build();
                        } catch(MetadataFormatException mfe) {
                            return Response.status(Response.Status.BAD_REQUEST).entity("{\"errorMessage\":\""+mfe.getMessage()+"\"}").build();
                        }
                    }
                default:
                    return Response.status(Response.Status.BAD_REQUEST).entity("unable to update element of type: " + form.getType()).build();
                }

                WorkspaceElementRepresentation representation = makeRepresentation(updatedObject, wskey, npath);
                return Response.ok().entity(representation).build();
            } catch (PathNotFoundException e) {
                if (form.getType().equals(MetadataObject.OBJECT_TYPE)) {
                    LOGGER.log(Level.FINEST, "unable to create metadata, path: " + npath.build() + " does not exists");
                    return Response.status(Response.Status.BAD_REQUEST).entity("unable to create metadata, path: " + npath.build() + " does not exists").build();
                } else {
                    OrtolangObject createdObject;
                    switch (form.getType()) {
                    case DataObject.OBJECT_TYPE:
                        createdObject = core.createDataObject(wskey, npath.build(), form.getStreamHash());
                        break;
                    case Collection.OBJECT_TYPE:
                        createdObject = core.createCollection(wskey, npath.build());
                        break;
                    case Link.OBJECT_TYPE:
                        createdObject = core.createLink(wskey, npath.build(), form.getTarget());
                        break;
                    default:
                        return Response.status(Response.Status.BAD_REQUEST).entity("unable to create element of type: " + form.getType()).build();
                    }
                    WorkspaceElementRepresentation representation = makeRepresentation(createdObject, wskey, npath);
                    URI newly = ApiUriBuilder.getApiUriBuilder().path(WorkspaceResource.class).path(wskey).path("elements").queryParam("path", npath.build()).build();
                    return Response.created(newly).entity(representation).build();
                }
            }
        } catch (DataCollisionException | UnsupportedEncodingException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "an error occured while creating workspace element: " + e.getMessage(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{wskey}/elements")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkspaceElement(@PathParam(value = "wskey") String wskey, WorkspaceElementRepresentation representation, @QueryParam(value = "destination") String destination) throws CoreServiceException,
            KeyNotFoundException, InvalidPathException, KeyAlreadyExistsException, OrtolangException, BrowserServiceException, MetadataFormatException, PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.INFO, "PUT /workspaces/" + wskey + "/elements");
        PathBuilder npath = PathBuilder.fromPath(representation.getPath());
        core.resolveWorkspacePath(wskey, "head", npath.build());
        LOGGER.log(Level.FINE, "element found at path: " + npath.build());
        OrtolangObject updatedObject = null;
        switch (representation.getType()) {
        case Collection.OBJECT_TYPE:
            if (destination != null && destination.length() > 0) {
                updatedObject = core.moveCollection(wskey, representation.getPath(), destination);
            }
            break;
        case Link.OBJECT_TYPE:
            if (destination != null && destination.length() > 0) {
                updatedObject = core.moveLink(wskey, representation.getPath(), destination);
            } else {
                updatedObject = core.updateLink(wskey, npath.build(), representation.getTarget());
            }
            break;
        case DataObject.OBJECT_TYPE:
            if (destination != null && destination.length() > 0) {
                updatedObject = core.moveDataObject(wskey, representation.getPath(), destination);
            } else {
                updatedObject = core.updateDataObject(wskey, npath.build(), representation.getStream());
            }
            break;
        case MetadataObject.OBJECT_TYPE:
            updatedObject = core.updateMetadataObject(wskey, npath.build(), representation.getName(), representation.getStream(), null, false, representation.getFormat());
            break;
        default:
            return Response.status(Response.Status.BAD_REQUEST).entity("unable to update element of type: " + representation.getType()).build();
        }
        if (updatedObject != null) {
            WorkspaceElementRepresentation updatedRepresentation = makeRepresentation(updatedObject, wskey, npath);
            return Response.ok().entity(updatedRepresentation).build();
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/{wskey}/elements")
    public Response deleteWorkspaceElement(@PathParam(value = "wskey") String wskey, @QueryParam(value = "root") String root, @QueryParam(value = "path") String path,
            @QueryParam(value = "metadataname") String metadataname, @QueryParam(value = "force") @DefaultValue("false") boolean force) throws CoreServiceException, InvalidPathException, AccessDeniedException, KeyNotFoundException,
            BrowserServiceException, CollectionNotEmptyException, PathNotFoundException, WorkspaceReadOnlyException {
        LOGGER.log(Level.INFO, "DELETE /workspaces/" + wskey + "/elements");
        if (path == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'path' is mandatory").build();
        }

        if (metadataname != null && metadataname.length() > 0) {
            core.deleteMetadataObject(wskey, path, metadataname, false);
        } else {
            String ekey = core.resolveWorkspacePath(wskey, root, path);
            OrtolangObjectIdentifier identifier = browser.lookup(ekey);
            switch (identifier.getType()) {
            case DataObject.OBJECT_TYPE:
                core.deleteDataObject(wskey, path);
                break;
            case Collection.OBJECT_TYPE:
                core.deleteCollection(wskey, path, force);
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

    @PUT
    @Path("/{wskey}/elements/bulk")
    @GZIP
    public Response moveWorkspaceElements(@PathParam(value = "wskey") String wskey, BulkActionRepresentation bulkActionRepresentation)
            throws WorkspaceReadOnlyException, RegistryServiceException, AccessDeniedException, KeyNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException,
            CoreServiceException, CollectionNotEmptyException {
        LOGGER.log(Level.INFO, "PUT /workspaces/" + wskey + "/elements/bulk (" + bulkActionRepresentation.getAction() + ")");
        if (bulkActionRepresentation.getAction() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'action' is mandatory").build();
        }
        if (BulkActionRepresentation.Actions.MOVE.toString().equalsIgnoreCase(bulkActionRepresentation.getAction())) {
            if (bulkActionRepresentation.getDestination() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'destination' is mandatory").build();
            }
            if (bulkActionRepresentation.getSources() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'sources' is mandatory").build();
            }
            core.moveElements(wskey, bulkActionRepresentation.getSources(), bulkActionRepresentation.getDestination());
            return Response.ok().build();
        } else if (BulkActionRepresentation.Actions.DELETE.toString().equalsIgnoreCase(bulkActionRepresentation.getAction())) {
            if (bulkActionRepresentation.getSources() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'sources' is mandatory").build();
            }
            core.deleteElements(wskey, bulkActionRepresentation.getSources(), bulkActionRepresentation.isForce());
            return Response.ok().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Unknown bulk action: " + bulkActionRepresentation.getAction()).build();
    }

    @POST
    @Path("/{wskey}/snapshots")
    public Response snapshotWorkspace(@PathParam(value = "wskey") String wskey)
            throws CoreServiceException, KeyNotFoundException, AccessDeniedException, WorkspaceReadOnlyException, WorkspaceUnchangedException {
        LOGGER.log(Level.INFO, "POST /workspaces/" + wskey + "/snapshots");
        core.snapshotWorkspace(wskey);
        return Response.ok().build();
    }

    @GET
    @Path("/{wskey}/elements/publication")
    public Response getPublicationPolicy(@PathParam(value = "wskey") String wskey, @QueryParam("path") String path)
            throws CoreServiceException, InvalidPathException, PathNotFoundException, KeyNotFoundException, OrtolangException, IOException, DataNotFoundException,
            BinaryStoreServiceException, RegistryServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces/" + wskey + "/elements/publication");
        if (path == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'path' is mandatory").build();
        }
        String key = core.resolveWorkspacePath(wskey, Workspace.HEAD, path);
        Collection parent = core.readCollection(key);
        OrtolangObject object;
        PathBuilder npath = PathBuilder.fromPath(path);
        Map<String, Object> map = new HashMap<>();
        ArrayList<WorkspaceElementRepresentation> elements = new ArrayList<>();
        String parentPublicationPolicy = core.readPublicationPolicy(wskey, Workspace.HEAD, path);
        WorkspaceElementRepresentation parentRepresentation = WorkspaceElementRepresentation.fromCollection(parent);
        parentRepresentation.setPublicationPolicy(parentPublicationPolicy);
        parentRepresentation.setPath(path);
        map.put("parent", parentRepresentation);
        for (CollectionElement child : parent.getElements()) {
            String publicationPolicy = core.readPublicationPolicy(child.getKey());
            object = browser.findObject(child.getKey());
            WorkspaceElementRepresentation representation = WorkspaceElementRepresentation.fromOrtolangObject(object);
            representation.setPublicationPolicy(publicationPolicy == null ? parentPublicationPolicy : publicationPolicy);
            representation.setPath(path + (npath.isRoot() ? "" : PathBuilder.PATH_SEPARATOR) + child.getName());
            elements.add(representation);
        }
        map.put("elements", elements);
        return Response.ok().entity(map).build();
    }

    @PUT
    @Path("/{wskey}/elements/publication")
    public Response setPublicationPolicy(@PathParam(value = "wskey") String wskey, @QueryParam("path") String path, @QueryParam("recursive") @DefaultValue("true") boolean recursive, PublicationPolicy publicationPolicy)
            throws BrowserServiceException, PathNotFoundException, KeyAlreadyExistsException, CoreServiceException, KeyNotFoundException, MetadataFormatException,
            WorkspaceReadOnlyException, PathAlreadyExistsException, InvalidPathException, OrtolangException, DataCollisionException, BinaryStoreServiceException, IOException {
        LOGGER.log(Level.INFO, "PUT /workspaces/" + wskey + "/elements/publication");
        JsonObject jsonObject = Json.createObjectBuilder().add("template", publicationPolicy.getTemplate()).build();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(jsonObject.toString().getBytes());
        String hash = binary.put(byteArrayInputStream);
        String key = core.resolveWorkspacePath(wskey, Workspace.HEAD, path);
        OrtolangObject object = browser.findObject(key);
        MetadataElement metadataElement = null;
        if (object instanceof MetadataSource) {
            metadataElement = ((MetadataSource) object).findMetadataByName(MetadataFormat.ACL);
        }
        if (metadataElement != null) {
            core.updateMetadataObject(wskey, path, MetadataFormat.ACL, hash, null, recursive);
        } else {
            core.createMetadataObject(wskey, path, MetadataFormat.ACL, hash, null, recursive);
        }
        return Response.ok().build();
    }

    @GET
    @Path("/{wskey}/events")
    @GZIP
    public Response listWorkspaceEvents(@PathParam(value = "wskey") String wskey, @QueryParam(value = "offset") @DefaultValue(value = "0") int offset, @QueryParam(value = "limit") @DefaultValue(value = "25") int limit)
            throws EventServiceException, BrowserServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /workspaces/" + wskey + "/events");
        long wscreation = browser.getInfos(wskey).getCreationDate();
        GenericCollectionRepresentation<OrtolangEvent> representation = new GenericCollectionRepresentation<>();
        List<OrtolangEvent> events = event.findEvents(null, wskey, null, null, wscreation, offset, limit);
        representation.setEntries(events);
        representation.setOffset(offset);
        representation.setLimit(limit);
        representation.setSize(events.size());
        return Response.ok(representation).build();
    }

    @GET
    @Path("/{wskey}/diff")
    @GZIP
    public Response diffWorkspaceContent(@PathParam(value = "wskey") String wskey, @QueryParam(value = "lsnapshot") String lsnapshot, @QueryParam(value = "rsnapshot") String rsnapshot)
            throws AccessDeniedException, CoreServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces/" + wskey + "/diff");
        List<Change> changes = core.diffWorkspaceContent(wskey, lsnapshot, rsnapshot);
        List<DiffRepresentation> representations = changes.stream().map(DiffRepresentation::fromChange).collect(Collectors.toList());
        return Response.ok(representations).build();
    }

    @GET
    @Path("/alias/{alias}/ftp")
    @GZIP
    public Response getFtpUrl(@PathParam(value = "alias") String alias) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, MembershipServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces/alias/" + alias + "/ftp");
        StringBuilder url = new StringBuilder();
        url.append("ftp://");
        String connectedIdentifier = membership.getProfileKeyForConnectedIdentifier();
        if ( connectedIdentifier.equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER )) {
            url.append("anonymous").append(":").append("password").append("@");
        } else {
            url.append(membership.getProfileKeyForConnectedIdentifier()).append(":").append(membership.generateConnectedIdentifierTOTP()).append("@");
        }
        url.append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FTP_SERVER_HOST)).append(":");
        url.append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FTP_SERVER_PORT)).append("/");
        url.append(alias);
        Map<String, String> ftpinfos = new HashMap<>(1);
        ftpinfos.put("url", url.toString());
        return Response.ok(ftpinfos).build();
    }

    @GET
    @Path("/alias/{alias}")
    @GZIP
    public Response getWorkspaceFromAlias(@PathParam(value = "alias") String alias, @QueryParam("md") @DefaultValue("false") boolean md, @Context Request request)
            throws CoreServiceException, AliasNotFoundException, AccessDeniedException, KeyNotFoundException, SecurityServiceException, BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces/alias/" + alias);
        String wskey = core.resolveWorkspaceAlias(alias);
        return getWorkspace(wskey, md, request);
    }

    @POST
    @Path("/{wskey}/owner")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response changeWorkspaceOwner(@PathParam(value = "wskey") String wskey, @FormParam(value = "newowner") String newowner)
            throws AccessDeniedException, SecurityServiceException, KeyNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "GET /workspaces/" + wskey + "/owner");
        core.changeWorkspaceOwner(wskey, newowner);
        return Response.ok().build();
    }

    private WorkspaceElementRepresentation makeRepresentation(OrtolangObject object, String wskey, PathBuilder path) throws KeyNotFoundException, AccessDeniedException, BrowserServiceException {
        WorkspaceElementRepresentation representation = WorkspaceElementRepresentation.fromOrtolangObject(object);
        representation.setWorkspace(wskey);
        representation.setPath(path.build());
        representation.setPathParts(path.buildParts());
        if (object.getObjectKey() != null) {
            OrtolangObjectInfos infos = browser.getInfos(object.getObjectKey());
            representation.setInfos(infos);
        }
        return representation;
    }

    private void addMetadatasToRepresentation(WorkspaceRepresentation representation) throws CoreServiceException, AccessDeniedException, KeyNotFoundException {
        Collection head = core.readCollection(representation.getHead());
        if (head.getMetadatas().isEmpty()) {
            representation.setMetadatas(Collections.emptyMap());
        } else {
            Map<String, String> metadatas = new HashMap<>();
            for (MetadataElement metadataElement : head.getMetadatas()) {
                metadatas.put(metadataElement.getName(), metadataElement.getKey());
            }
            representation.setMetadatas(metadatas);
        }
    }

    private static class PublicationPolicy {

        private String template;

        public String getTemplate() {
            return template;
        }

    }
}
