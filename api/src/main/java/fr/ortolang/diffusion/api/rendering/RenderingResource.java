package fr.ortolang.diffusion.api.rendering;

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
