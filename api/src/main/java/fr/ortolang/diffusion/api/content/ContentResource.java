package fr.ortolang.diffusion.api.content;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.ApiHelper;
import fr.ortolang.diffusion.api.auth.AuthResource;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.PathNotFoundException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.message.MessageService;
import fr.ortolang.diffusion.message.MessageServiceException;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.MessageAttachment;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.template.TemplateEngine;
import fr.ortolang.diffusion.template.TemplateEngineException;

@Path("/content")
@Produces({ MediaType.TEXT_HTML })
public class ContentResource {

    private static final Logger LOGGER = Logger.getLogger(ContentResource.class.getName());

    private static final ClassLoader TEMPLATE_ENGINE_CL = ContentResource.class.getClassLoader();

    private static final String anonymousBase64;

    @EJB
    private CoreService core;
    @EJB
    private MessageService message;
    @EJB
    private BrowserService browser;
    @EJB
    private BinaryStoreService store;
    @EJB
    private SecurityService security;
    @Context
    private UriInfo uriInfo;

    private static Map<String, Map<String, Object>> exportations;

    static  {
        exportations = new HashMap<>();
        anonymousBase64 = Base64.getEncoder().encodeToString("anonymous".getBytes());
    }

    @GET
    @Path("/exportations/{id}")
    @SuppressWarnings("unchecked")
    public Response resumeExportation(@PathParam("id") String id, @Context SecurityContext securityContext) throws UnsupportedEncodingException {
        Map<String, Object> params = exportations.get(id);
        Response response = export(false, (String) params.get("followsymlink"), (String) params.get("filename"), (String) params.get("format"), (List<String>) params.get("path"),
                securityContext);
        exportations.remove(id);
        return response;
    }


    @POST
    @Path("/export")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.TEXT_HTML, MediaType.WILDCARD })
    public Response exportPost(final @QueryParam("scope") @DefaultValue("") String scope, final @FormParam("followsymlink") @DefaultValue("false") String followSymlink, @FormParam("filename") @DefaultValue("download") String filename,
            @FormParam("format") @DefaultValue("zip") String format, final @FormParam("path") List<String> paths, @Context SecurityContext securityContext) throws UnsupportedEncodingException {
        LOGGER.log(Level.INFO, "POST /export");
        return export(!scope.startsWith(anonymousBase64), followSymlink, filename, format, paths, securityContext);
    }

    @GET
    @Path("/export")
    @Produces({ MediaType.TEXT_HTML, MediaType.WILDCARD })
    public Response exportGet(final @QueryParam("scope") @DefaultValue("") String scope, final @QueryParam("followsymlink") @DefaultValue("false") String followSymlink, @QueryParam("filename") @DefaultValue("download") String filename,
            @QueryParam("format") @DefaultValue("zip") String format, final @QueryParam("path") List<String> paths, @Context SecurityContext securityContext) throws UnsupportedEncodingException {
        LOGGER.log(Level.INFO, "GET /export");
        return export(!scope.startsWith(anonymousBase64), followSymlink, filename, format, paths, securityContext);
    }

    private Response export(boolean connected, String followSymlink, String filename, String format, List<String> paths, SecurityContext securityContext) throws UnsupportedEncodingException {
        if (connected && securityContext.getUserPrincipal() == null) {
            LOGGER.log(Level.FINE, "user is not authenticated, redirecting to authentication");
            Map<String, Object> params = new HashMap<>();
            params.put("followSymlink", followSymlink);
            params.put("filename", filename);
            params.put("format", format);
            params.put("path", paths);
            String id = UUID.randomUUID().toString();
            exportations.put(id ,params);
            String redirect = "/content/exportations/" + id;
            String encodedRedirect = Base64.getUrlEncoder().encodeToString(redirect.getBytes());
            NewCookie rcookie = new NewCookie(AuthResource.REDIRECT_PATH_PARAM_NAME, encodedRedirect, OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT),
                    uriInfo.getBaseUri().getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false, false);
            UriBuilder builder = UriBuilder.fromResource(ContentResource.class);
            builder.queryParam("followsymlink", followSymlink).queryParam("filename", filename).queryParam("format", format).queryParam("path", paths);
            return Response.seeOther(uriInfo.getBaseUriBuilder().path(AuthResource.class).queryParam(AuthResource.REDIRECT_PATH_PARAM_NAME, encodedRedirect).build()).cookie(rcookie).build();
        }
        ResponseBuilder builder = handleExport(false, filename, format, paths);
        return builder.build();
    }

    private ResponseBuilder handleExport(boolean followSymlink, String filename, String format, final List<String> paths) throws UnsupportedEncodingException {
        ResponseBuilder builder;
        switch (format) {
        case "zip": {
            LOGGER.log(Level.FINE, "exporting using format zip");
            builder = Response.ok();
            builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, "utf-8") + ".zip");
            builder.type("application/zip");
            StreamingOutput stream = output -> {
                try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(output)) {
                    for (String path : paths) {
                        try {
                            String key = resolveContentPath(path);
                            ArchiveEntryFactory factory = (name, time, size) -> {
                                ZipArchiveEntry entry = new ZipArchiveEntry(name);
                                if (time != -1) {
                                    entry.setTime(time);
                                }
                                if (size != -1) {
                                    entry.setSize(size);
                                }
                                return entry;
                            };
                            exportToArchive(key, out, factory, PathBuilder.fromPath(path), false);
                        } catch (AccessDeniedException e) {
                            LOGGER.log(Level.FINEST, "access denied during export to zip", e);
                        } catch (BrowserServiceException | CoreServiceException | AliasNotFoundException | KeyNotFoundException | OrtolangException e) {
                            LOGGER.log(Level.INFO, "unable to export path to zip", e);
                        } catch (InvalidPathException e) {
                            LOGGER.log(Level.FINEST, "invalid path during export to zip", e);
                        } catch (PathNotFoundException e) {
                            LOGGER.log(Level.FINEST, "path not found during export to zip", e);
                        } catch (ExportToArchiveIOException e) {
                            LOGGER.log(Level.SEVERE, "unexpected IO error during export to archive, stopping export", e);
                            break;
                        }
                    }
                }
            };
            builder.entity(stream);
            break;
        }
        case "tar": {
            LOGGER.log(Level.FINE, "exporting using format tar");
            builder = Response.ok();
            builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, "utf-8") + ".tar.gz");
            builder.type("application/x-gzip");
            StreamingOutput stream = output -> {
                try (GzipCompressorOutputStream gout = new GzipCompressorOutputStream(output); TarArchiveOutputStream out = new TarArchiveOutputStream(gout)) {
                    for (String path : paths) {
                        try {
                            String key = resolveContentPath(path);
                            ArchiveEntryFactory factory = (name, time, size) -> {
                                TarArchiveEntry entry = new TarArchiveEntry(name);
                                if (time != -1) {
                                    entry.setModTime(time);
                                }
                                if (size != -1) {
                                    entry.setSize(size);
                                }
                                return entry;
                            };
                            exportToArchive(key, out, factory, PathBuilder.fromPath(path), false);
                        } catch (BrowserServiceException | CoreServiceException | AliasNotFoundException | KeyNotFoundException | OrtolangException e) {
                            LOGGER.log(Level.INFO, "unable to export path to tar", e);
                        } catch (InvalidPathException e) {
                            LOGGER.log(Level.FINEST, "invalid path during export to tar", e);
                        } catch (PathNotFoundException e) {
                            LOGGER.log(Level.FINEST, "path not found during export to tar", e);
                        } catch (ExportToArchiveIOException e) {
                            LOGGER.log(Level.SEVERE, "unexpected IO error during export to archive, stopping export", e);
                            break;
                        }
                    }
                }
            };
            builder.entity(stream);
            break;
        }
        default:
            builder = Response.status(Status.BAD_REQUEST).entity("export format [" + format + "] is not supported");
        }
        return builder;
    }

    private String resolveContentPath(String path) throws CoreServiceException, InvalidPathException, AccessDeniedException, AliasNotFoundException, KeyNotFoundException, PathNotFoundException {
        PathBuilder pbuilder = PathBuilder.fromPath(path);
        String[] pparts = pbuilder.buildParts();
        if (pparts.length < 2) {
            throw new InvalidPathException("invalid path, format is : /{alias}/{root}/{path}");
        }
        String wskey = core.resolveWorkspaceAlias(pparts[0]);
        if (pparts[1].equals(Workspace.LATEST)) {
            pparts[1] = core.findWorkspaceLatestPublishedSnapshot(wskey);
            if (pparts[1] == null) {
                throw new InvalidPathException("unable to find latest published snapshot for workspace alias: " + pparts[0]);
            }
        }
        return core.resolveWorkspacePath(wskey, pparts[1], pbuilder.relativize(2).build());
    }

    private void exportToArchive(String key, ArchiveOutputStream aos, ArchiveEntryFactory factory, PathBuilder path, boolean followsymlink) throws OrtolangException, KeyNotFoundException,
            BrowserServiceException, ExportToArchiveIOException {
        OrtolangObject object;
        try {
            object = browser.findObject(key);
        } catch (BrowserServiceException e) {
            return;
        }
        OrtolangObjectInfos infos = browser.getInfos(key);
        String type = object.getObjectIdentifier().getType();

        switch (type) {
        case Collection.OBJECT_TYPE:
            try {
                Set<CollectionElement> elements = ((Collection) object).getElements();
                ArchiveEntry centry = factory.createArchiveEntry(path.build() + "/", infos.getLastModificationDate(), 0L);
                try {
                    aos.putArchiveEntry(centry);
                    for (CollectionElement element : elements) {
                        try {
                            PathBuilder pelement = path.clone().path(element.getName());
                            exportToArchive(element.getKey(), aos, factory, pelement, followsymlink);
                        } catch (InvalidPathException e) {
                            LOGGER.log(Level.SEVERE, "unexpected error during export to zip !!", e);
                        }
                    }
                } catch ( IOException e ) {
                    throw new ExportToArchiveIOException("unable to put archive entry for collection at path: " + path.build(), e);
                }
            } finally {
                try {
                    aos.closeArchiveEntry();
                } catch ( IOException e ) {
                    throw new ExportToArchiveIOException("unable to close archive entry for collection at path: " + path.build(), e);
                }
            }
            break;
        case DataObject.OBJECT_TYPE:
            try (InputStream input = core.download(object.getObjectKey())) {
                DataObject dataObject = (DataObject) object;
                ArchiveEntry oentry = factory.createArchiveEntry(path.build(), infos.getLastModificationDate(), dataObject.getSize());
                try {
                    aos.putArchiveEntry(oentry);
                    IOUtils.copy(input, aos);
                } catch (IOException e) {
                    throw new ExportToArchiveIOException("unable to export dataobject at path: " + path.build(), e);
                } finally {
                    try {
                        aos.closeArchiveEntry();
                    } catch ( IOException e ) {
                        throw new ExportToArchiveIOException("unable to close archive entry for collection at path: " + path.build(), e);
                    }
                }
            } catch (IOException e) {
                throw new ExportToArchiveIOException("unable to get input stream for dataobject at path: " + path.build(), e);
            } catch (AccessDeniedException e) {
                return;
            } catch (CoreServiceException | DataNotFoundException e) {
                LOGGER.log(Level.SEVERE, "unexpected error during export to zip", e);
            } 
            break;
        case Link.OBJECT_TYPE:
            if (followsymlink) {
                LOGGER.log(Level.SEVERE, "link export is not managed yet");
                // TODO in case of following symlink, add cyclic detection
            }
            break;
        }
    }
    
    @GET
    @Path("/attachments/{mkey}/{hash}")
    @Produces({ MediaType.TEXT_HTML, MediaType.WILDCARD })
    public Response attachment(@PathParam(value = "mkey") String mkey, @PathParam(value = "hash") String hash, @Context SecurityContext securityContext) throws MessageServiceException, KeyNotFoundException, DataNotFoundException, UnsupportedEncodingException, BinaryStoreServiceException {
        LOGGER.log(Level.INFO, "GET /attachments/" + mkey + "/" + hash);
        try {
            Message msg = message.readMessage(mkey);
            MessageAttachment attachment = msg.findAttachmentByHash(hash);
            if ( attachment == null ) {
                throw new DataNotFoundException("unable to find attachment");
            }
            ResponseBuilder builder = Response.ok(store.getFile(attachment.getHash()));
            builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(attachment.getName(), "utf-8"));
            builder.header("Content-Length", attachment.getSize());
            builder.type(attachment.getType());
            return builder.build();
        } catch (AccessDeniedException e) {
            return redirectToAuth("/content/attachments/" + mkey + "/" + hash, true, Collections.emptyMap(), securityContext);
        }
    }

    @GET
    @Path("/key/{key}")
    @Produces({ MediaType.TEXT_HTML, MediaType.WILDCARD })
    public Response key(@PathParam("key") String key, @QueryParam("fd") boolean download, @QueryParam("O") @DefaultValue("A") String asc, @QueryParam("C") @DefaultValue("N") String order,
            @QueryParam("l") @DefaultValue("true") boolean login, @Context SecurityContext securityContext, @Context Request request) throws TemplateEngineException, CoreServiceException, KeyNotFoundException,
            InvalidPathException, OrtolangException, BinaryStoreServiceException, DataNotFoundException, URISyntaxException, BrowserServiceException,
            UnsupportedEncodingException, SecurityServiceException {
        LOGGER.log(Level.INFO, "GET /content/key/" + key);
        try {
            OrtolangObjectState state = browser.getState(key);
            CacheControl cc = new CacheControl();
            cc.setPrivate(true);
            ApiHelper.setCacheControlFromState(state, cc);
            Date lmd = new Date(state.getLastRefresh() / 1000 * 1000);
            ResponseBuilder builder = null;
            if (System.currentTimeMillis() - state.getLastRefresh() > 1000) {
                builder = request.evaluatePreconditions(lmd);
            }
            if (builder == null) {
                OrtolangObject object = browser.findObject(key);
                if (object instanceof DataObject) {
                    String sha1 = ((DataObject) object).getStream();
                    File content = store.getFile(sha1);
                    security.checkPermission(key, "download");
                    builder = Response.ok(content).header("Content-Type", ((DataObject) object).getMimeType()).header("Content-Length", ((DataObject) object).getSize());
                    if (download) {
                        builder = builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(object.getObjectName(), "utf-8"));
                    } else {
                        builder = builder.header("Content-Disposition", "filename*=UTF-8''" + URLEncoder.encode(object.getObjectName(), "utf-8"));
                    }
                    builder.lastModified(lmd);
                } else if (object instanceof MetadataObject) {
                    File content = store.getFile(((MetadataObject) object).getStream());
                    security.checkPermission(key, "download");
                    builder = Response.ok(content).header("Content-Type", ((MetadataObject) object).getContentType()).header("Content-Length", ((MetadataObject) object).getSize());
                    if (download) {
                        builder = builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(object.getObjectName(), "utf-8"));
                    } else {
                        builder = builder.header("Content-Disposition", "filename*=UTF-8''" + URLEncoder.encode(object.getObjectName(), "utf-8"));
                    }
                    builder.lastModified(lmd);
                } else if (object instanceof Collection) {
                    ContentRepresentation representation = new ContentRepresentation();
                    representation.setContext(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT));
                    representation.setBase("/content/key");
                    representation.setAlias("");
                    representation.setRoot("");
                    representation.setPath("/" + key);
                    representation.setParentPath("");
                    representation.setOrder(order);
                    representation.setLinkbykey(true);
                    representation.setElements(new ArrayList<>(((Collection) object).getElements()));
                    sort(representation, asc, order);
                    builder = Response.ok(TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process("collection", representation));
                    builder.lastModified(lmd);
                } else if (object instanceof Link) {
                    return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path(((Link) object).getTarget()).build()).build();
                } else {
                    return Response.serverError().entity("object type not supported").build();
                }
            }
            builder.cacheControl(cc);
            return builder.build();
        } catch (AccessDeniedException e) {
            Map<String, Object> params = new HashMap<>();
            params.put("fd", download);
            params.put("O", asc);
            params.put("C", order);
            return redirectToAuth("/content/key/" + key, login, params, securityContext);
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response workspaces(@QueryParam("O") @DefaultValue("A") String asc, @QueryParam("l") @DefaultValue("true") boolean login, @Context SecurityContext securityContext) throws TemplateEngineException,
            CoreServiceException {
        LOGGER.log(Level.INFO, "GET /content");
        try {
            ContentRepresentation representation = new ContentRepresentation();
            representation.setContext(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT));
            representation.setBase("/content");
            representation.setPath("/");
            representation.setOrder("N");
            List<String> aliases = core.listAllWorkspaceAlias();
            List<CollectionElement> elements = new ArrayList<>(aliases.size());
            for (String alias : aliases) {
                elements.add(new CollectionElement(Collection.OBJECT_TYPE, alias, -1, -1, "ortolang/workspace", ""));
            }
            sort(representation, elements, asc);
            representation.setElements(elements);
            return Response.ok(TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process("collection", representation)).build();
        } catch (AccessDeniedException e) {
            Map<String, Object> params = new HashMap<>();
            params.put("O", asc);
            return redirectToAuth("/content", login, params, securityContext);
        }
    }

    @GET
    @Path("/{alias}")
    @Produces(MediaType.TEXT_HTML)
    public Response workspace(@PathParam("alias") String alias, @QueryParam("O") @DefaultValue("A") String asc, @QueryParam("l") @DefaultValue("true") boolean login,
            @Context SecurityContext securityContext, @Context Request request) throws TemplateEngineException, CoreServiceException, AliasNotFoundException, KeyNotFoundException, BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /content/" + alias);
        ContentRepresentation representation = new ContentRepresentation();
        representation.setContext(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT));
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
            ApiHelper.setCacheControlFromState(state, cc);
            Date lmd = new Date(state.getLastRefresh() / 1000 * 1000);
            ResponseBuilder builder = null;
            if (System.currentTimeMillis() - state.getLastRefresh() > 1000) {
                builder = request.evaluatePreconditions(lmd);
            }
            if (builder == null) {
                Workspace workspace = core.readWorkspace(wskey);
                List<CollectionElement> elements = new ArrayList<>();
                String latest = core.findWorkspaceLatestPublishedSnapshot(wskey);
                if (latest != null && latest.length() > 0) {
                    elements.add(new CollectionElement(Collection.OBJECT_TYPE, Workspace.LATEST, -1, -1, "ortolang/snapshot", latest));
                }
                elements.add(new CollectionElement(Collection.OBJECT_TYPE, Workspace.HEAD, -1, -1, "ortolang/snapshot", workspace.getHead()));
                for (SnapshotElement snapshot : workspace.getSnapshots()) {
                    elements.add(new CollectionElement(Collection.OBJECT_TYPE, snapshot.getName(), -1, -1, "ortolang/snapshot", snapshot.getKey()));
                }
                for (TagElement tag : workspace.getTags()) {
                    elements.add(new CollectionElement(Collection.OBJECT_TYPE, tag.getName(), -1, -1, "ortolang/tag", workspace.findSnapshotByName(tag.getSnapshot()).getKey()));
                }
                sort(representation, elements, asc);
                representation.setElements(elements);
                builder = Response.ok(TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process("collection", representation));
                builder.lastModified(lmd);
            }
            builder.cacheControl(cc);
            return builder.build();
        } catch (AccessDeniedException e) {
            Map<String, Object> params = new HashMap<>();
            params.put("O", asc);
            return redirectToAuth(representation.getBase() + representation.getPath(), login, params, securityContext);
        }
    }

    @GET
    @Path("/{alias}/{root}")
    @Produces(MediaType.TEXT_HTML)
    public Response snapshot(@PathParam("alias") String alias, @PathParam("root") final String root, @QueryParam("O") @DefaultValue("A") String asc, @QueryParam("C") @DefaultValue("N") String order,
            @QueryParam("l") @DefaultValue("true") boolean login, @Context SecurityContext securityContext, @Context Request request) throws TemplateEngineException, CoreServiceException,
            AccessDeniedException, AliasNotFoundException, KeyNotFoundException, BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /content/" + alias + "/" + root);
        ContentRepresentation representation = new ContentRepresentation();
        representation.setContext(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT));
        representation.setBase("/content");
        representation.setAlias(alias);
        representation.setRoot(root);
        representation.setPath("/" + alias + "/" + root);
        representation.setParentPath("/" + alias);
        representation.setOrder(order);
        try {
            String wskey = core.resolveWorkspaceAlias(alias);
            Workspace workspace = core.readWorkspace(wskey);
            String rkey;
            switch (root) {
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
                String snapshot = root;
                TagElement telement = workspace.findTagByName(snapshot);
                if (telement != null) {
                    snapshot = telement.getSnapshot();
                }
                SnapshotElement selement = workspace.findSnapshotByName(snapshot);
                if (selement == null) {
                    return Response.status(Status.NOT_FOUND).entity("Unable to find a root tag or snapshot with name [" + root + "] in this workspace").type("text/plain").build();
                }
                rkey = selement.getKey();
                break;
            }

            OrtolangObjectState state = browser.getState(rkey);
            CacheControl cc = new CacheControl();
            cc.setPrivate(true);
            if (!root.equals(Workspace.LATEST) && state.isLocked()) {
                cc.setMaxAge(691200);
                cc.setMustRevalidate(false);
            } else {
                cc.setMaxAge(0);
                cc.setMustRevalidate(true);
            }
            Date lmd = new Date(state.getLastRefresh() / 1000 * 1000);
            ResponseBuilder builder = null;
            if (System.currentTimeMillis() - state.getLastRefresh() > 1000) {
                builder = request.evaluatePreconditions(lmd);
            }
            if (builder == null) {
                Collection collection = core.readCollection(rkey);
                representation.setElements(new ArrayList<>(collection.getElements()));
                sort(representation, asc, order);
                builder = Response.ok(TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process("collection", representation));
                builder.lastModified(lmd);
            }
            builder.cacheControl(cc);
            return builder.build();
        } catch (AccessDeniedException e) {
            Map<String, Object> params = new HashMap<>();
            params.put("O", asc);
            params.put("C", order);
            return redirectToAuth(representation.getBase() + representation.getPath(), login, params, securityContext);
        }
    }

    @GET
    @Path("/{alias}/{root}/{path: .*}")
    @Produces({ MediaType.TEXT_HTML, MediaType.WILDCARD })
    public Response path(@PathParam("alias") String alias, @PathParam("root") final String root, @PathParam("path") String path, @QueryParam("fd") boolean download,
            @QueryParam("O") @DefaultValue("A") String asc, @QueryParam("C") @DefaultValue("N") String order, @QueryParam("l") @DefaultValue("true") boolean login, @Context SecurityContext securityContext,
            @Context Request request) throws TemplateEngineException, CoreServiceException, KeyNotFoundException, AliasNotFoundException, InvalidPathException,
            OrtolangException, BinaryStoreServiceException, DataNotFoundException, URISyntaxException, BrowserServiceException, UnsupportedEncodingException, SecurityServiceException,
            PathNotFoundException {
        LOGGER.log(Level.INFO, "GET /content/" + alias + "/" + root + "/" + path);
        ContentRepresentation representation = new ContentRepresentation();
        representation.setContext(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT));
        representation.setBase("/content");
        representation.setAlias(alias);
        representation.setRoot(root);
        representation.setPath("/" + alias + "/" + root + "/" + path);
        representation.setParentPath("/" + alias + "/" + root);
        representation.setOrder(order);
        try {
            String wskey = core.resolveWorkspaceAlias(alias);
            boolean cacheableRoot = false;
            String rroot = null;
            if (root.equals(Workspace.LATEST)) {
                rroot = core.findWorkspaceLatestPublishedSnapshot(wskey);
                if (rroot == null) {
                    return Response.status(Status.NOT_FOUND).entity("No version of this workspace has been published").type("text/plain").build();
                }
            } else if (!root.equals(Workspace.HEAD)) {
                Workspace workspace = core.readWorkspace(wskey);
                if (!workspace.containsTagName(root)) {
                    cacheableRoot = true;
                }
            }
            if (rroot == null) {
                rroot = root;
            }
            PathBuilder npath = PathBuilder.fromPath(path);
            String okey = core.resolveWorkspacePath(wskey, rroot, npath.build());
            OrtolangObjectState state = browser.getState(okey);

            CacheControl cc = new CacheControl();
            cc.setPrivate(true);
            if (cacheableRoot) {
                cc.setMaxAge(691200);
                cc.setMustRevalidate(false);
            } else {
                cc.setMaxAge(0);
                cc.setMustRevalidate(true);
            }
            Date lmd = new Date(state.getLastRefresh() / 1000 * 1000);
            ResponseBuilder builder = null;
            if (System.currentTimeMillis() - state.getLastRefresh() > 1000) {
                builder = request.evaluatePreconditions(lmd);
            }
            if (builder == null) {
                OrtolangObject object = browser.findObject(okey);
                if (object instanceof DataObject) {
                    security.checkPermission(okey, "download");
                    File content = store.getFile(((DataObject) object).getStream());
                    builder = Response.ok(content).header("Content-Type", ((DataObject) object).getMimeType()).header("Content-Length", ((DataObject) object).getSize());
                    if (download) {
                        builder = builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(object.getObjectName(), "utf-8"));
                    } else {
                        builder = builder.header("Content-Disposition", "filename*=UTF-8''" + URLEncoder.encode(object.getObjectName(), "utf-8"));
                    }
                    builder.lastModified(lmd);
                } else if (object instanceof Collection) {
                    representation.setElements(new ArrayList<>(((Collection) object).getElements()));
                    sort(representation, asc, order);
                    builder = Response.ok(TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process("collection", representation));
                    builder.lastModified(lmd);
                } else if (object instanceof Link) {
                    return Response.seeOther(uriInfo.getBaseUriBuilder().path(ContentResource.class).path(((Link) object).getTarget()).build()).build();
                } else {
                    return Response.serverError().entity("object type not supported").build();
                }
            }
            builder.cacheControl(cc);
            return builder.build();
        } catch (AccessDeniedException e) {
            Map<String, Object> params = new HashMap<>();
            params.put("fd", download);
            params.put("O", asc);
            params.put("C", order);
            return redirectToAuth(representation.getBase() + representation.getPath(), login, params, securityContext);
        }
    }

    private Response redirectToAuth(String path, boolean login, Map<String, Object> params, SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null || securityContext.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
            if (login) {
                UriBuilder builder = UriBuilder.fromPath(path);
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    builder.queryParam(param.getKey(), param.getValue());
                }
                String redirect = builder.build().toString();
                String encodedRedirect = Base64.getUrlEncoder().encodeToString(redirect.getBytes());
                LOGGER.log(Level.FINE, "user is not authenticated, redirecting to authentication");
                NewCookie rcookie = new NewCookie(AuthResource.REDIRECT_PATH_PARAM_NAME, encodedRedirect, OrtolangConfig.getInstance().getProperty(
                        OrtolangConfig.Property.API_CONTEXT), uriInfo.getBaseUri().getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false,
                        false);
                return Response
                        .seeOther(
                                uriInfo.getBaseUriBuilder().path(AuthResource.class).queryParam(AuthResource.REDIRECT_PATH_PARAM_NAME, encodedRedirect).build())
                        .cookie(rcookie).build();
            } else {
                LOGGER.log(Level.FINE, "user is not authenticated, but login redirect disabled");
                return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
            }
        } else {
            LOGGER.log(Level.FINE, "user is already authenticated, access denied");
            return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
        }
    }

    private void sort(ContentRepresentation representation, String asc, String order) {
        if ("D".equals(asc)) {
            switch (order) {
            case "T":
                Collections.sort(representation.getElements(), CollectionElement.ElementTypeDescComparator);
                break;
            case "M":
                Collections.sort(representation.getElements(), CollectionElement.ElementDateDescComparator);
                break;
            case "S":
                Collections.sort(representation.getElements(), CollectionElement.ElementSizeDescComparator);
                break;
            default:
                Collections.sort(representation.getElements(), CollectionElement.ElementNameDescComparator);
                break;
            }
            representation.setAsc(false);
        } else {
            switch (order) {
            case "T":
                Collections.sort(representation.getElements(), CollectionElement.ElementTypeAscComparator);
                break;
            case "M":
                Collections.sort(representation.getElements(), CollectionElement.ElementDateAscComparator);
                break;
            case "S":
                Collections.sort(representation.getElements(), CollectionElement.ElementSizeAscComparator);
                break;
            default:
                Collections.sort(representation.getElements(), CollectionElement.ElementNameAscComparator);
                break;
            }
            representation.setAsc(true);
        }
    }

    private void sort(ContentRepresentation representation, List<CollectionElement> elements, String asc) {
        if ("D".equals(asc)) {
            Collections.sort(elements, CollectionElement.ElementNameDescComparator);
            representation.setAsc(false);
        } else {
            Collections.sort(elements, CollectionElement.ElementNameAscComparator);
            representation.setAsc(true);
        }
    }

    interface ArchiveEntryFactory {

        ArchiveEntry createArchiveEntry(String name, long modificationDate, long size);
    }

}
