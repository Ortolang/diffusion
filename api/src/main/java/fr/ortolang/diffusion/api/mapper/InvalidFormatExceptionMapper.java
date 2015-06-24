package fr.ortolang.diffusion.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@Provider
public class InvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {

	@Override
	public Response toResponse(InvalidFormatException ex) {
		return Response.status(Status.NOT_FOUND)
				.entity("Metadata content is not valid: " + ex.getMessage()).type("text/plain")
				.build();
	}
}