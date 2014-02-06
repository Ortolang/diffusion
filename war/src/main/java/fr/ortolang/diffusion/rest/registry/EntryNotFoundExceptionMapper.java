package fr.ortolang.diffusion.rest.registry;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.registry.EntryNotFoundException;

@Provider
public class EntryNotFoundExceptionMapper implements ExceptionMapper<EntryNotFoundException> {

	public Response toResponse(EntryNotFoundException ex) {
		return Response.status(Status.NOT_FOUND)
				.entity("No registry entry found for this key").type("text/plain")
				.build();
	}
}