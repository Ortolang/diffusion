package fr.ortolang.diffusion.rest.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.registry.PropertyNotFoundException;

@Provider
public class PropertyNotFoundExceptionMapper implements ExceptionMapper<PropertyNotFoundException> {

	public Response toResponse(PropertyNotFoundException ex) {
		return Response.status(Status.NOT_FOUND)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}