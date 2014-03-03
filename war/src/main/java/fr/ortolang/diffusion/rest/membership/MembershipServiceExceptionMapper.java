package fr.ortolang.diffusion.rest.membership;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.membership.MembershipServiceException;

@Provider
public class MembershipServiceExceptionMapper implements ExceptionMapper<MembershipServiceException> {

	public Response toResponse(MembershipServiceException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}