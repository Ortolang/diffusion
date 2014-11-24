package fr.ortolang.diffusion.api.rest.mapper;

import fr.ortolang.diffusion.core.InvalidPathException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidPathExceptionMapper implements ExceptionMapper<InvalidPathException> {

	public Response toResponse(InvalidPathException ex) {
		return Response.status(Status.NOT_FOUND)
				.entity("Element not found at path : " + ex.getMessage()).type("text/plain")
				.build();
	}
}