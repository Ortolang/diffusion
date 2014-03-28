package fr.ortolang.diffusion.rest.collaboration;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.collaboration.CollaborationServiceException;

@Provider
public class CollaborationServiceExceptionMapper implements ExceptionMapper<CollaborationServiceException> {

	public Response toResponse(CollaborationServiceException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}