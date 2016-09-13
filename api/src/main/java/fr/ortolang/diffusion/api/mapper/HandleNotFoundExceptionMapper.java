package fr.ortolang.diffusion.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import fr.ortolang.diffusion.store.handle.HandleNotFoundException;

public class HandleNotFoundExceptionMapper implements ExceptionMapper<HandleNotFoundException> {

    @Override
    public Response toResponse(HandleNotFoundException ex) {
        return Response.status(Status.NOT_FOUND).entity(ex.getMessage()).type("text/plain").build();
    }
}