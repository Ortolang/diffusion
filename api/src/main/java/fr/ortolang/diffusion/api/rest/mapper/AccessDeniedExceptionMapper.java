package fr.ortolang.diffusion.api.rest.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Provider
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

	public Response toResponse(AccessDeniedException ex) {
		return Response.status(Status.UNAUTHORIZED)
				.header("WWW-Authenticate", "Basic")
				.entity("Access denied").type("text/plain")
				.build();
	}
}