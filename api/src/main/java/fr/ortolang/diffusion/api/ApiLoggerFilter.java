package fr.ortolang.diffusion.api;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class ApiLoggerFilter implements ContainerRequestFilter {
    
    private static final Logger LOGGER = Logger.getLogger(ApiLoggerFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        StringBuilder message = new StringBuilder();
        //TODO retreive source ip;
        message.append(ctx.getMethod()).append(" ");
        message.append(ctx.getUriInfo().getPath());
        LOGGER.log(Level.INFO, message.toString());
    }

}
