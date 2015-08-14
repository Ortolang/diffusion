package fr.ortolang.diffusion.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

@SuppressWarnings("rawtypes")
public class OrtolangClientFileBodyReader implements MessageBodyReader {

	public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return true;
	}

	public InputStream readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream) throws IOException,
			WebApplicationException {
		
		return entityStream;
	}
}
