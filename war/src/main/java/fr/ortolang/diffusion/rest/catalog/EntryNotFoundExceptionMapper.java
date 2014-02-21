package fr.ortolang.diffusion.rest.catalog;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.registry.KeyNotFoundException;

@Provider
public class EntryNotFoundExceptionMapper implements ExceptionMapper<KeyNotFoundException> {

	public Response toResponse(KeyNotFoundException ex) {
		return Response.status(Status.NOT_FOUND)
				.entity("No registry entry found for this key").type("text/plain")
				.build();
	}
}