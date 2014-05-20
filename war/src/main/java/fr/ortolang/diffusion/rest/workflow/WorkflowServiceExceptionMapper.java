package fr.ortolang.diffusion.rest.workflow;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.workflow.WorkflowServiceException;

@Provider
public class WorkflowServiceExceptionMapper implements ExceptionMapper<WorkflowServiceException> {

	public Response toResponse(WorkflowServiceException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}