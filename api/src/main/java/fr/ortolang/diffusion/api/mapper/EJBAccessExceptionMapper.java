package fr.ortolang.diffusion.api.mapper;

import javax.ejb.EJBAccessException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

public class EJBAccessExceptionMapper implements ExceptionMapper<EJBAccessException> {

    @Override
    public Response toResponse(EJBAccessException ex) {
        return Response.status(Status.UNAUTHORIZED).entity("Access denied: " + ex.getMessage()).type("text/plain").build();
    }
}