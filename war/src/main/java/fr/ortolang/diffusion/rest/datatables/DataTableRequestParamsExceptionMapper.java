package fr.ortolang.diffusion.rest.datatables;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DataTableRequestParamsExceptionMapper implements ExceptionMapper<DataTableRequestParamsException> {

	public Response toResponse(DataTableRequestParamsException ex) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity("An error occured : " + ex.getMessage()).type("text/plain")
				.build();
	}
}