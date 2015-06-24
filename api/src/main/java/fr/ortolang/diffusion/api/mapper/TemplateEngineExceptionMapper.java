package fr.ortolang.diffusion.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import fr.ortolang.diffusion.api.template.TemplateEngineException;

public class TemplateEngineExceptionMapper implements ExceptionMapper<TemplateEngineException> {

	@Override
	public Response toResponse(TemplateEngineException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}