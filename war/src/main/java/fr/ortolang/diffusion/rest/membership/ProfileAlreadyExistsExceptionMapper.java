package fr.ortolang.diffusion.rest.membership;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;

@Provider
public class ProfileAlreadyExistsExceptionMapper implements ExceptionMapper<ProfileAlreadyExistsException> {

	public Response toResponse(ProfileAlreadyExistsException ex) {
		return Response.status(Status.CONFLICT)
				.entity("A profile already exists for this identifier").type("text/plain")
				.build();
	}
}