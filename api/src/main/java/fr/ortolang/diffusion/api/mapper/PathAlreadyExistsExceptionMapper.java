package fr.ortolang.diffusion.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.core.PathAlreadyExistsException;

@Provider
public class PathAlreadyExistsExceptionMapper implements ExceptionMapper<PathAlreadyExistsException> {

    @Override
    public Response toResponse(PathAlreadyExistsException ex) {
        return Response.status(Status.CONFLICT)
                .entity("Element already exists at path : " + ex.getMessage()).type("text/plain")
                .build();
    }
}