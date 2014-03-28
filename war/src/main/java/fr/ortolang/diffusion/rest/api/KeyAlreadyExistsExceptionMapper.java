package fr.ortolang.diffusion.rest.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;

public class KeyAlreadyExistsExceptionMapper implements ExceptionMapper<KeyAlreadyExistsException> {

	public Response toResponse(KeyAlreadyExistsException ex) {
		return Response.status(Status.CONFLICT)
				.entity("A registry entry already exists for this key").type("text/plain")
				.build();
	}
}