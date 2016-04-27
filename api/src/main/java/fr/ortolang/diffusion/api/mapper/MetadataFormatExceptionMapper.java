package fr.ortolang.diffusion.api.mapper;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.OrtolangErrorCodes;
import fr.ortolang.diffusion.core.MetadataFormatException;

@Provider
public class MetadataFormatExceptionMapper implements ExceptionMapper<MetadataFormatException> {

    @Override
    public Response toResponse(MetadataFormatException ex) {
        Map<String, String> map = new HashMap<>();
        map.put("code", OrtolangErrorCodes.METADATA_FORMAT_EXCEPTION);
        map.put("message", ex.getMessage());
        return Response.status(Status.BAD_REQUEST).entity(map).build();
    }
}
