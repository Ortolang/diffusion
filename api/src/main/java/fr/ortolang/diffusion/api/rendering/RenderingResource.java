package fr.ortolang.diffusion.api.rendering;

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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.auth.AuthResource;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rendering.RenderingService;
import fr.ortolang.diffusion.rendering.RenderingServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.thumbnail.ThumbnailServiceException;

@Path("/render")
@Produces({ MediaType.TEXT_HTML })
public class RenderingResource {
    
    private static final Logger LOGGER = Logger.getLogger(RenderingResource.class.getName());
    
    @EJB
    private BrowserService browser;
    @EJB
    private RenderingService render;
    @Context
    private UriInfo uriInfo;
    
    @GET
    @Path("/key/{key}")
    @Produces({ MediaType.TEXT_HTML })
    public Response getView(@PathParam(value = "key") String key, @QueryParam("engine") String engine, @QueryParam("l") @DefaultValue("true") boolean login, @Context SecurityContext security,
            @Context Request request) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, OrtolangException, ThumbnailServiceException, RenderingServiceException, CoreServiceException, BinaryStoreServiceException {
        LOGGER.log(Level.INFO, "GET /render/key/" + key);
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
            if (builder == null) {
                File view = render.getView(key, engine);
                builder = Response.ok(view).header("Content-Type", MediaType.TEXT_HTML_TYPE);
                builder.lastModified(lmd);
            }
            builder.cacheControl(cc);
            return builder.build();
        } catch (AccessDeniedException e) {
            if (security.getUserPrincipal() == null || security.getUserPrincipal().getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                if (login) {
                    LOGGER.log(Level.FINE, "user is not authenticated, redirecting to authentication");
                    NewCookie rcookie = new NewCookie(AuthResource.REDIRECT_PATH_PARAM_NAME, "/viewer/" + key, OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT), uriInfo.getBaseUri()
                            .getHost(), 1, "Redirect path after authentication", 300, new Date(System.currentTimeMillis() + 300000), false, false);
                    return Response.seeOther(uriInfo.getBaseUriBuilder().path(AuthResource.class).queryParam(AuthResource.REDIRECT_PATH_PARAM_NAME, "/viewer/" + key).build()).cookie(rcookie)
                            .build();
                } else {
                    LOGGER.log(Level.FINE, "user is not authenticated, but login redirect disabled");
                    return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
                }
            } else {
                LOGGER.log(Level.FINE, "user is already authenticated, access denied");
                return Response.status(Status.UNAUTHORIZED).entity("You are not authorized to access this content").build();
            }
        }
    }

}
