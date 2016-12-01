package fr.ortolang.diffusion.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.oai.exception.OaiServiceException;

@Provider
public class OaiServiceExceptionMapper implements ExceptionMapper<OaiServiceException> {

	@Override
	public Response toResponse(OaiServiceException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}
