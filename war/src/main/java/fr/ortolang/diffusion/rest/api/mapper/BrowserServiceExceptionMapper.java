package fr.ortolang.diffusion.rest.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.browser.BrowserServiceException;

@Provider
public class BrowserServiceExceptionMapper implements ExceptionMapper<BrowserServiceException> {

	public Response toResponse(BrowserServiceException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}