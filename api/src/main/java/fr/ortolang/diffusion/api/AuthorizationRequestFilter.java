package fr.ortolang.diffusion.api;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Secured
public class AuthorizationRequestFilter implements ContainerRequestFilter {
    
    private static final Logger LOGGER = Logger.getLogger(AuthorizationRequestFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        LOGGER.log(Level.FINE, "Filtering resource call for admin role authorization");
        final SecurityContext secCtx = ctx.getSecurityContext();
        if (secCtx == null || !secCtx.isUserInRole("admin")) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("User cannot access the resource.").build());
        }
    }
}