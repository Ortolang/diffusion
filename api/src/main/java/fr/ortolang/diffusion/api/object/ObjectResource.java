package fr.ortolang.diffusion.api.object;

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

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

import fr.ortolang.diffusion.api.ApiHelper;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectVersion;
import fr.ortolang.diffusion.api.ApiUriBuilder;
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathNotFoundException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;

@Path("/objects")
@Produces({ MediaType.APPLICATION_JSON })
public class ObjectResource {

    private static final Logger LOGGER = Logger.getLogger(ObjectResource.class.getName());

    @EJB
    private BrowserService browser;
    @EJB
    private SecurityService security;
    @EJB
    private CoreService core;
    @EJB
    private HandleStoreService handleStore;

    public ObjectResource() {
    }

    @GET
    @GZIP
    public Response list(@DefaultValue(value = "0") @QueryParam(value = "offset") int offset, @DefaultValue(value = "25") @QueryParam(value = "limit") int limit,
            @QueryParam(value = "service") String service, @QueryParam(value = "type") String type, @QueryParam(value = "status") String status) throws BrowserServiceException, URISyntaxException {
        LOGGER.log(Level.INFO, "GET /objects?offset=" + offset + "&limit=" + limit + "&status=" + status);
        List<String> keys = browser.list(offset, limit, (service != null && service.length() > 0) ? service : "", (type != null && type.length() > 0) ? type : "",
                (status != null && status.length() > 0) ? OrtolangObjectState.Status.valueOf(status) : null);
        long nbentries = browser.count((service != null && service.length() > 0) ? service : "", (type != null && type.length() > 0) ? type : "",
                (status != null && status.length() > 0) ? OrtolangObjectState.Status.valueOf(status) : null);
        UriBuilder objects = ApiUriBuilder.getApiUriBuilder().path(ObjectResource.class);

        GenericCollectionRepresentation<String> representation = new GenericCollectionRepresentation<String>();
        for (String key : keys) {
            representation.addEntry(key);
        }
        representation.setOffset((offset <= 0) ? 1 : offset);
        representation.setSize(nbentries);
        representation.setLimit(keys.size());
        representation.setFirst(objects.clone().queryParam("offset", 0).queryParam("limit", limit).build());
        representation.setPrevious(objects.clone().queryParam("offset", Math.max(0, offset - limit)).queryParam("limit", limit).build());
        representation.setSelf(objects.clone().queryParam("offset", offset).queryParam("limit", limit).build());
        representation.setNext(objects.clone().queryParam("offset", nbentries > offset + limit ? (offset + limit) : offset).queryParam("limit", limit).build());
        representation.setLast(objects.clone().queryParam("offset", ((nbentries - 1) / limit) * limit).queryParam("limit", limit).build());
        return Response.ok(representation).build();
    }

    @GET
    @Path("/{key}")
    @GZIP
    public Response get(@PathParam(value = "key") String key, @Context Request request) throws BrowserServiceException, KeyNotFoundException, SecurityServiceException,
            OrtolangException {
        LOGGER.log(Level.INFO, "GET /objects/" + key);

        OrtolangObjectState state = browser.getState(key);
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        ApiHelper.setCacheControlFromState(state, cc);
        Date lmd = new Date(state.getLastModification() / 1000 * 1000);
        ResponseBuilder builder = null;
        if (System.currentTimeMillis() - state.getLastModification() > 1000) {
            builder = request.evaluatePreconditions(lmd);
        }

        if (builder == null) {
            OrtolangObject object = browser.findObject(key);
            OrtolangObjectInfos infos = browser.getInfos(key);
            String owner = security.getOwner(key);

            ObjectRepresentation representation = new ObjectRepresentation();
            representation.setKey(key);
            representation.setService(object.getObjectIdentifier().getService());
            representation.setType(object.getObjectIdentifier().getType());
            representation.setId(object.getObjectIdentifier().getId());
            representation.setStatus(state.getStatus());
            representation.setLock(state.getLock());
            if (state.isHidden()) {
                representation.setVisibility("hidden");
            } else {
                representation.setVisibility("visible");
            }
            representation.setOwner(owner);
            representation.setObject(object);
            representation.setAuthor(infos.getAuthor());
            representation.setCreationDate(infos.getCreationDate() + "");
            representation.setLastModificationDate(infos.getLastModificationDate() + "");

            builder = Response.ok(representation);
            builder.lastModified(lmd);
        }

        builder.cacheControl(cc);
        return builder.build();
    }

    @GET
    @Path("/{key}/history")
    @GZIP
    public Response history(@PathParam(value = "key") String key, @Context Request request) throws OrtolangException, KeyNotFoundException,
            InvalidPathException, BrowserServiceException, SecurityServiceException, CoreServiceException, PathNotFoundException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "/history");
        OrtolangObjectState state = browser.getState(key);
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        ApiHelper.setCacheControlFromState(state, cc);
        Date lmd = new Date(state.getLastModification() / 1000 * 1000);
        ResponseBuilder builder = null;
        if (System.currentTimeMillis() - state.getLastModification() > 1000) {
            builder = request.evaluatePreconditions(lmd);
        }

        if (builder == null) {
            List<OrtolangObjectVersion> versions = browser.getHistory(key);
            builder = Response.ok(versions);
            builder.lastModified(lmd);
        }
        builder.cacheControl(cc);
        return builder.build();
    }

    @GET
    @Path("/{key}/properties")
    @GZIP
    public Response properties(@PathParam(value = "key") String key, @Context Request request) throws OrtolangException, KeyNotFoundException,
            InvalidPathException, BrowserServiceException, SecurityServiceException, CoreServiceException, PathNotFoundException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "/properties");
        OrtolangObjectState state = browser.getState(key);
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        ApiHelper.setCacheControlFromState(state, cc);
        Date lmd = new Date(state.getLastModification() / 1000 * 1000);
        ResponseBuilder builder = null;
        if (System.currentTimeMillis() - state.getLastModification() > 1000) {
            builder = request.evaluatePreconditions(lmd);
        }

        if (builder == null) {
            List<OrtolangObjectProperty> properties = browser.listProperties(key);
            builder = Response.ok(properties);
            builder.lastModified(lmd);
        }
        builder.cacheControl(cc);
        return builder.build();
    }

    @GET
    @Path("/{key}/permissions")
    @GZIP
    public Response permissions(@PathParam(value = "key") String key, @Context Request request) throws OrtolangException, KeyNotFoundException,
            InvalidPathException, BrowserServiceException, SecurityServiceException, CoreServiceException, PathNotFoundException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "/permissions");
        OrtolangObjectState state = browser.getState(key);
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        ApiHelper.setCacheControlFromState(state, cc);
        Date lmd = new Date(state.getLastModification() / 1000 * 1000);
        ResponseBuilder builder = null;
        if (System.currentTimeMillis() - state.getLastModification() > 1000) {
            builder = request.evaluatePreconditions(lmd);
        }

        if (builder == null) {
            Map<String, List<String>> permissions = security.listRules(key);
            builder = Response.ok(permissions);
            builder.lastModified(lmd);
        }
        builder.cacheControl(cc);
        return builder.build();
    }

    @GET
    @Path("/{key}/pids")
    @GZIP
    public Response pids(@PathParam(value = "key") String key, @Context Request request)
            throws OrtolangException, KeyNotFoundException, BrowserServiceException,
            HandleStoreServiceException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "/pids");
        OrtolangObjectState state = browser.getState(key);
        CacheControl cc = new CacheControl();
        cc.setPrivate(true);
        ApiHelper.setCacheControlFromState(state, cc);
        Date lmd = new Date(state.getLastModification() / 1000 * 1000);
        ResponseBuilder builder = null;
        if (System.currentTimeMillis() - state.getLastModification() > 1000) {
            builder = request.evaluatePreconditions(lmd);
        }

        if (builder == null) {
            List<String> handles = handleStore.listHandlesForKey(key);
            builder = Response.ok(handles);
            builder.lastModified(lmd);
        }
        builder.cacheControl(cc);
        return builder.build();
    }

    @GET
    @Path("/{key}/element")
    @GZIP
    public Response resolve(@PathParam(value = "key") String key, @QueryParam(value = "path") String relativePath, @Context Request request) throws OrtolangException, KeyNotFoundException,
            InvalidPathException, BrowserServiceException, SecurityServiceException, CoreServiceException, PathNotFoundException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "?path=" + relativePath);
        return get(core.resolvePathFromCollection(key, relativePath), request);
    }

    @GET
    @Path("/{key}/size")
    @GZIP
    public Response getObjectSize(@PathParam(value = "key") String key) throws OrtolangException, KeyNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "/size");
        OrtolangObjectSize ortolangObjectSize = core.getSize(key);
        return Response.ok(ortolangObjectSize).build();
    }

    @POST
    @Path("/{key}/index")
    @GZIP
    public Response reindex(@PathParam(value = "key") String key) throws AccessDeniedException, KeyNotFoundException, BrowserServiceException {
        LOGGER.log(Level.INFO, "POST /objects/" + key + "/index");
        browser.index(key);
        return Response.ok().build();
    }

    @GET
    @Path("/{key}/authorized")
    @GZIP
    public Response isAuthorized(@PathParam(value = "key") String key, @DefaultValue(value = "download") @QueryParam(value = "permission") String permission) throws OrtolangException, KeyNotFoundException,
            InvalidPathException, BrowserServiceException, SecurityServiceException, CoreServiceException, PathNotFoundException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "/authorized");
        Map<String, Boolean> map = new HashMap<>(1);
        try {
            security.checkPermission(key, permission);
            map.put(permission, true);
        } catch (AccessDeniedException e) {
            map.put(permission, false);
        }
        return Response.ok(map).build();
    }
}
