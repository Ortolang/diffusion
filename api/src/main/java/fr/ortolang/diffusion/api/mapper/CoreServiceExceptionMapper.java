package fr.ortolang.diffusion.api.mapper;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.OrtolangErrorCodes;
import fr.ortolang.diffusion.core.CoreServiceException;

@Provider
public class CoreServiceExceptionMapper implements ExceptionMapper<CoreServiceException> {

    @Override
    public Response toResponse(CoreServiceException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("message", ex.getMessage());
        map.put("code", OrtolangErrorCodes.CORE_SERVICE_EXCEPTION);
        return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(map)
                .build();
    }
}
