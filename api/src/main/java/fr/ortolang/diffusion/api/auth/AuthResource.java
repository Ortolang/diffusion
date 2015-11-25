package fr.ortolang.diffusion.api.auth;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.api.content.ContentResource;

@Path("/auth")
public class AuthResource {
    
    private static final Logger LOGGER = Logger.getLogger(ContentResource.class.getName());
    
    public static final String REDIRECT_PATH_PARAM_NAME = "redirect";
    
    @Context
    private UriInfo uriInfo;
    
    @GET
    public Response authenticate(@CookieParam(REDIRECT_PATH_PARAM_NAME) String credirect, @QueryParam(REDIRECT_PATH_PARAM_NAME) String qredirect) {
        LOGGER.log(Level.INFO, "GET /content/auth");
        UriBuilder builder = uriInfo.getBaseUriBuilder();
        if (credirect != null && credirect.length() > 0) {
            LOGGER.log(Level.FINE, "redirecting to path found in cookie : " + credirect);
            builder.path(credirect);
        } else if (qredirect != null && qredirect.length() > 0) {
            LOGGER.log(Level.FINE, "redirecting to path found in query : " + qredirect);
            builder.path(qredirect);
        }
        return Response.seeOther(builder.build()).build();
    }

}
