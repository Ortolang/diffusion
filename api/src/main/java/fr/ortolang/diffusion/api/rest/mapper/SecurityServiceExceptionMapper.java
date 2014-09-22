package fr.ortolang.diffusion.api.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.security.SecurityServiceException;

@Provider
public class SecurityServiceExceptionMapper implements ExceptionMapper<SecurityServiceException> {

	public Response toResponse(SecurityServiceException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}