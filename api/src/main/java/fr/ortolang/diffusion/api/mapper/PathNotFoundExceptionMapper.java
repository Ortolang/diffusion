package fr.ortolang.diffusion.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.core.PathNotFoundException;

@Provider
public class PathNotFoundExceptionMapper implements ExceptionMapper<PathNotFoundException> {

    @Override
    public Response toResponse(PathNotFoundException ex) {
        return Response.status(Status.NOT_FOUND)
                .entity("Element not found at path : " + ex.getMessage()).type("text/plain")
                .build();
    }
}